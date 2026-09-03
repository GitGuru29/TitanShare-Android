package com.titanshare.android.data.mirror

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import java.nio.ByteBuffer

/**
 * Captures the device screen via [MediaProjection] + [VirtualDisplay] and pushes
 * each frame as a JPEG into a [TcpMirrorStreamer].
 *
 * The capture/encode loop runs on a dedicated [HandlerThread] which owns the
 * [ImageReader] and its listener. A proper Looper/Handler is required so
 * [ImageReader.setOnImageAvailableListener] can target this thread (passing
 * `null` would crash with "handler is null but the current thread is not a
 * looper"). It reuses a single [Bitmap] and scratch buffer to avoid per-frame
 * allocations.
 */
class ScreenMirrorCapture(
    private val projection: MediaProjection,
    private val width: Int,
    private val height: Int,
    private val streamer: TcpMirrorStreamer,
) {
    private val tag = "ScreenMirrorCapture"

    // ─── Adaptive quality state ─────────────────────────────────────────────
    private var quality = 70                // JPEG quality (5..95)
    private var targetFps = 20f             // adaptive frame rate

    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var running = false

    // Handler thread owning the ImageReader + its listener.
    private var captureThread: HandlerThread? = null
    private var captureHandler: Handler? = null

    // Scratch buffers
    private var scratch: Bitmap? = null
    private var row = ByteArray(0)          // tight-packed single RGBA row
    private var frameBytes = ByteArray(0)   // tight-packed full frame

    @Volatile var framesEncoded: Long = 0
        private set
    @Volatile var framesSkipped: Long = 0
        private set

    /** Current adaptive settings, exposed for the UI stats readout. */
    @Volatile var currentQuality: Int = 70
        private set
    @Volatile var currentFps: Float = 20f
        private set

    val isRunning: Boolean get() = running

    /**
     * Starts the capture loop. [densityDpi] is the source display density so the
     * mirrored frame matches the physical screen scale.
     */
    fun start(densityDpi: Int) {
        if (running) return
        running = true

        val thread = HandlerThread("titanshare-mirror-capture").apply { start() }
        val handler = Handler(thread.looper)
        captureThread = thread
        captureHandler = handler

        handler.post {
            try {
                startCaptureOnLoop(densityDpi, handler)
            } catch (e: Exception) {
                Log.e(tag, "startCaptureOnLoop error: ${e.message}")
                throw e
            }
        }
    }

    /** Runs on the capture HandlerThread's looper. */
    private fun startCaptureOnLoop(densityDpi: Int, handler: Handler) {
        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader = reader
        scratch = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        frameBytes = ByteArray(width * height * 4)
        row = ByteArray(width * 4)

        virtualDisplay = projection.createVirtualDisplay(
            "titanshare-mirror",
            width, height,
            /*densityDpi=*/ densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null, null,
        )

        var lastTime = System.nanoTime()
        var lastDropCounter = streamer.framesDropped

        reader.setOnImageAvailableListener({ r ->
            if (!running) { r.close(); return@setOnImageAvailableListener }

            // Adaptive pacing: keep the same real-time rate we targeted.
            val now = System.nanoTime()
            val frameIntervalNs = (1_000_000_000L / targetFps).toLong()
            val elapsed = now - lastTime
            if (elapsed < frameIntervalNs) {
                // Consume + discard the image to avoid backpressure stalls.
                r.acquireLatestImage()?.close()
                framesSkipped++
                return@setOnImageAvailableListener
            }
            lastTime = now

            val image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                encodeAndSend(image)
            } catch (e: Exception) {
                Log.e(tag, "encodeAndSend error: ${e.message}")
            } finally {
                image.close()
            }

            // Adaptive quality: when the daemon is dropping frames, drop
            // quality/bitrate to keep latency low; when idle, raise it.
            val drops = streamer.framesDropped
            if (drops != lastDropCounter) {
                lastDropCounter = drops
                quality = (quality - 5).coerceAtLeast(30)
                targetFps = (targetFps - 2f).coerceAtLeast(8f)
            } else if (framesEncoded % 60 == 0L) {
                quality = (quality + 1).coerceAtMost(85)
                targetFps = (targetFps + 0.5f).coerceAtMost(30f)
            }
            currentQuality = quality
            currentFps = targetFps
        }, handler)

        Log.i(tag, "Mirror capture started ${width}x$height")
    }

    /**
     * Copies the RGBA [image] into the scratch [Bitmap] (handling row stride)
     * and compresses it to JPEG for transmission.
     */
    private fun encodeAndSend(image: Image) {
        val plane = image.planes[0]
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val src = plane.buffer

        // The ImageReader buffer may include row padding; unpack into a
        // tight-packed RGBA frame that Bitmap.copyPixelsFromBuffer understands.
        var dstOffset = 0
        for (y in 0 until height) {
            src.position(y * rowStride)
            src.get(row, 0, width * pixelStride)
            System.arraycopy(row, 0, frameBytes, dstOffset, width * pixelStride)
            dstOffset += width * pixelStride
        }

        val bmp = scratch ?: return
        bmp.copyPixelsFromBuffer(ByteBuffer.wrap(frameBytes, 0, dstOffset))

        val baos = java.io.ByteArrayOutputStream(quality * width * height / 2000)
        bmp.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        val jpeg = baos.toByteArray()
        baos.close()

        streamer.sendFrame(jpeg)
        framesEncoded++
    }

    /** Stops capture, tears down the VirtualDisplay and projection register. */
    fun stop() {
        if (!running) return
        running = false
        virtualDisplay?.release()
        virtualDisplay = null
        captureHandler?.post {
            imageReader?.close()
            imageReader = null
            scratch?.recycle()
            scratch = null
        }
        captureThread?.quitSafely()
        captureThread = null
        captureHandler = null
        Log.i(tag, "Mirror capture stopped (encoded=$framesEncoded)")
    }
}
