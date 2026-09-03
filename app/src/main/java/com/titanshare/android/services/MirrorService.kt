package com.titanshare.android.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.titanshare.android.data.mirror.ScreenMirrorCapture
import com.titanshare.android.data.mirror.TcpMirrorStreamer

/**
 * Foreground service that mirrors the device screen to the paired Linux PC.
 *
 * Start with [startCommand] extras:
 *   EXTRA_RESULT_CODE  — MediaProjection result code
 *   EXTRA_RESULT_DATA  — MediaProjection Intent
 *   EXTRA_HOST         — the daemon's IP
 *   EXTRA_PORT         — the daemon's mirror TCP port (from START_MIRROR reply)
 *
 * Progress is surfaced to any [Listener] so the mirror UI can show live FPS,
 * resolution and quality without polling the service internals.
 */
class MirrorService : Service() {

    companion object {
        private const val TAG = "MirrorService"
        private const val CHANNEL_ID = "titanshare_mirror"
        private const val NOTIFICATION_ID = 5001

        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val EXTRA_HOST = "host"
        const val EXTRA_PORT = "port"

        const val ACTION_STOP = "com.titanshare.android.MIRROR_STOP"

        // Static hook so the ViewModel can observe mirror stats without binding
        // to the service (avoids a ServiceConnection + Binder dance).
        @Volatile
        var activeListener: Listener? = null

        fun start(context: Context, resultCode: Int, resultData: Intent?, host: String, port: Int) {
            val i = Intent(context, MirrorService::class.java).apply {
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, resultData)
                putExtra(EXTRA_HOST, host)
                putExtra(EXTRA_PORT, port)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MirrorService::class.java))
        }
    }

    /** Simple callback interface for UI-driven stats. */
    interface Listener {
        fun onMirrorStarted(fps: Int, width: Int, height: Int, quality: Int)
        fun onMirrorStats(framesSent: Long, framesDropped: Long, fps: Int, quality: Int)
        fun onMirrorStopped(error: String?)
    }

    private var mediaProjection: MediaProjection? = null
    private var projectionCallback: MediaProjection.Callback? = null
    private var capture: ScreenMirrorCapture? = null
    private var streamer: TcpMirrorStreamer? = null

    private val handler = Handler(Looper.getMainLooper())
    private var statsTick: Runnable? = null

    // ─── Service lifecycle ─────────────────────────────────────────────────

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopMirror("Stopped by user")
            return START_NOT_STICKY
        }

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val resultData: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        }
        val host = intent?.getStringExtra(EXTRA_HOST) ?: ""
        val port = intent?.getIntExtra(EXTRA_PORT, -1) ?: -1

        if (resultCode == 0 || resultData == null || host.isEmpty() || port <= 0) {
            stopMirror("Invalid mirror parameters")
            return START_NOT_STICKY
        }

        startForegroundCompat()

        if (mediaProjection == null) {
            val err = beginMirror(resultCode, resultData, host, port)
            if (err != null) {
                Log.e(TAG, "beginMirror failed: $err")
                stopMirror(err)
                return START_NOT_STICKY
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        tearDown()
        super.onDestroy()
    }

    // ─── Actions ───────────────────────────────────────────────────────────

    // ─── Mirror boot ───────────────────────────────────────────────────────

    /** Returns null on success, or a human-readable reason on failure. */
    private fun beginMirror(resultCode: Int, resultData: Intent, host: String, port: Int): String? {
        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val proj = mpm.getMediaProjection(resultCode, resultData) ?: run {
            Log.e(TAG, "getMediaProjection returned null (resultCode=$resultCode)")
            return "Screen capture permission was not granted (getMediaProjection returned null)"
        }
        mediaProjection = proj

        // Required on Android 14+ (targetSdk 34): register a Callback BEFORE
        // createVirtualDisplay(), otherwise createVirtualDisplay() throws
        // IllegalStateException. Also lets us clean up when the system/user
        // stops the projection session.
        registerProjectionCallback(proj)

        // Read the source display size + density synchronously.
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        val streamer = TcpMirrorStreamer(host, port)
        if (!streamer.connect()) {
            Log.e(TAG, "TcpMirrorStreamer.connect failed: ${streamer.lastError}")
            return "Could not connect to the mirror receiver at $host:$port — ${streamer.lastError}"
        }
        this.streamer = streamer

        val capture = ScreenMirrorCapture(proj, width, height, streamer)
        try {
            capture.start(density)
        } catch (e: SecurityException) {
            Log.e(TAG, "createVirtualDisplay denied: ${e.message}")
            return "Screen capture was denied — retry and grant permission"
        } catch (e: IllegalStateException) {
            Log.e(TAG, "createVirtualDisplay invalid state: ${e.message}")
            return "Screen capture session is invalid — retry and grant permission"
        }
        this.capture = capture

        activeListener?.onMirrorStarted(
            fps = capture.currentFps.toInt(),
            width = width,
            height = height,
            quality = capture.currentQuality,
        )
        beginStatsTicker()

        Log.i(TAG, "Mirror running -> $host:$port ${width}x$height")
        return null
    }

    /**
     * Registers the mandatory MediaProjection callback (required before
     * createVirtualDisplay() on targetSdk 34+) and tears down cleanly when the
     * system stops the projection session.
     */
    private fun registerProjectionCallback(proj: MediaProjection) {
        val cb = object : MediaProjection.Callback() {
            override fun onStop() {
                Log.i(TAG, "MediaProjection session stopped by system")
                stopMirror(null)
            }
        }
        proj.registerCallback(cb, Handler(Looper.getMainLooper()))
        projectionCallback = cb
    }

    private fun beginStatsTicker() {
        statsTick?.let { handler.removeCallbacks(it) }
        val tick = object : Runnable {
            override fun run() {
                val st = streamer ?: return
                val cap = capture ?: return
                if (!isMirroring()) return
                activeListener?.onMirrorStats(
                    framesSent = st.framesSent,
                    framesDropped = st.framesDropped + cap.framesSkipped,
                    fps = computeFps(st),
                    quality = cap.currentQuality,
                )
                handler.postDelayed(this, 1000)
            }
        }
        statsTick = tick
        handler.postDelayed(tick, 1000)
    }

    private fun computeFps(st: TcpMirrorStreamer): Int {
        val now = System.nanoTime()
        val t = st.lastFrameTimestampNs
        if (t == 0L) return 0
        val deltaMs = (now - t) / 1_000_000
        return if (deltaMs <= 0) 0 else (1000 / deltaMs).coerceIn(0, 60).toInt()
    }

    private fun isMirroring(): Boolean =
        mediaProjection != null

    // ─── Teardown ──────────────────────────────────────────────────────────

    private fun stopMirror(error: String?) {
        tearDown()
        stopSelf()
        activeListener?.onMirrorStopped(error)
    }

    private fun tearDown() {
        if (statsTick != null) { handler.removeCallbacks(statsTick!!); statsTick = null }
        capture?.stop()
        capture = null
        streamer?.disconnect()
        streamer = null
        projectionCallback?.let { mediaProjection?.unregisterCallback(it) }
        projectionCallback = null
        mediaProjection?.stop()
        mediaProjection = null
        stopForegroundCompat()
    }

    // ─── Foreground notification ───────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Screen Mirror", NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Mirroring")
            .setContentText("Streaming to Linux PC")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun startForegroundCompat() {
        val n = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIFICATION_ID, n)
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
    }
}
