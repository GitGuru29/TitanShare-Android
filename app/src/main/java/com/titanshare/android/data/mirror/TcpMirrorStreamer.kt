package com.titanshare.android.data.mirror

import android.util.Log
import java.io.BufferedOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Sends a stream of length-prefixed JPEG frames over TCP to the Linux mirror
 * receiver.
 *
 * Wire format (matches the daemon's MirrorReceiver):
 *     [uint32 BE : payload length][payload bytes (JPEG)]
 *
 * A dedicated background thread owns the socket. [sendFrame] is called from the
 * capture thread; it hands the JPEG bytes to the sender thread via a bounded,
 * condition-variable guarded queue so a slow network never stalls the UI and
 * the sender thread sleeps instead of spinning.
 */
class TcpMirrorStreamer(
    private val host: String,
    private val port: Int,
) {
    private val tag = "TcpMirrorStreamer"

    @Volatile private var running = false
    private var senderThread: Thread? = null
    private var socket: Socket? = null
    private var out: BufferedOutputStream? = null

    // Bounded FIFO of JPEG payloads awaiting transmission. Bounding gives us
    // natural backpressure: if the receiver is slow we drop whole frames
    // instead of accumulating unbounded latency.
    private val maxQueuedFrames = 8
    private val queue = ArrayDeque<ByteArray>()
    private val queueLock = Object()

    // Stats (read by UI for the FPS/quality indicator)
    @Volatile var framesSent: Long = 0
        private set
    @Volatile var framesDropped: Long = 0
        private set
    @Volatile var bytesSent: Long = 0
        private set
    @Volatile var lastFrameTimestampNs: Long = 0
        private set

    val isRunning: Boolean get() = running

    @Volatile private var connectError: String? = null
    val lastError: String? get() = connectError

    /**
     * Opens the socket and starts the sender thread. Returns true on success.
     *
     * The blocking [Socket.connect] is done on a dedicated background thread:
     * this method must be safe to call from any thread (including the Android
     * main thread, which forbids blocking network I/O and would otherwise throw
     * a message-less NetworkOnMainThreadException). The caller blocks on a latch
     * until the handshake completes, then returns the result synchronously.
     */
    fun connect(): Boolean {
        if (running) return true
        connectError = null

        val latch = CountDownLatch(1)
        val connected = AtomicBoolean(false)

        Thread({
            try {
                val s = try {
                    com.titanshare.android.data.network.SslHelper.createSslSocket(host, port, 8000)
                } catch (e: Exception) {
                    Log.w(tag, "TLS mirror stream fallback to standard socket: ${e.message}")
                    val raw = Socket()
                    raw.connect(InetSocketAddress(host, port), 8000)
                    raw
                }
                s.tcpNoDelay = true
                socket = s
                out = BufferedOutputStream(s.getOutputStream(), 262144)
                connected.set(true)
            } catch (e: Exception) {
                connectError = e.message ?: "Connection failed"
                Log.e(tag, "connect() failed: ${e.message}")
            } finally {
                latch.countDown()
            }
        }, "titanshare-mirror-connect").apply {
            isDaemon = true
            start()
        }

        // Wait for the handshake (or the socket's own 8s connect timeout) to
        // settle so failures surface immediately to the caller.
        latch.await(9, TimeUnit.SECONDS)
        if (!connected.get()) return false

        running = true
        senderThread = Thread({ senderLoop() }, "titanshare-mirror-sender").apply {
            isDaemon = true
            start()
        }
        return true
    }

    /** Enqueues a JPEG frame for transmission. Non-blocking. */
    fun sendFrame(jpeg: ByteArray) {
        if (!running) return
        synchronized(queueLock) {
            if (queue.size >= maxQueuedFrames) {
                // Avoid unbounded latency — drop the *oldest* frame.
                queue.removeFirstOrNull()
                framesDropped++
            }
            queue.addLast(jpeg)
            queueLock.notifyAll()
        }
    }

    /** Stops the sender thread and closes the socket. */
    fun disconnect() {
        running = false
        synchronized(queueLock) { queueLock.notifyAll() }
        senderThread?.join(2000)
        senderThread = null
        try { socket?.close() } catch (_: IOException) {}
        socket = null
        out = null
        synchronized(queueLock) { queue.clear() }
    }

    private fun senderLoop() {
        while (running) {
            val frame: ByteArray
            synchronized(queueLock) {
                while (queue.isEmpty() && running) {
                    try { queueLock.wait() } catch (_: InterruptedException) { return }
                }
                if (!running) return
                frame = queue.removeFirst()
            }

            try {
                val o = out ?: return
                // Length prefix, big-endian
                o.write((frame.size ushr 24) and 0xFF)
                o.write((frame.size ushr 16) and 0xFF)
                o.write((frame.size ushr 8) and 0xFF)
                o.write(frame.size and 0xFF)
                o.write(frame)
                o.flush()

                bytesSent += frame.size
                framesSent++
                lastFrameTimestampNs = System.nanoTime()
            } catch (e: IOException) {
                connectError = "Connection lost: ${e.message}"
                Log.e(tag, "send failed: ${e.message}")
                running = false
                synchronized(queueLock) { queueLock.notifyAll() }
                return
            }
        }
    }
}
