package com.titanshare.android.data.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.PrintWriter
import java.net.Socket
import java.io.InputStream
import java.io.OutputStream

private const val TAG = "DaemonClient"
private const val TIMEOUT_MS = 8000

/**
 * TCP client for the TitanShare Linux daemon.
 *
 * Protocol:
 *  1. Connect to <ip>:9999
 *  2. Send AUTH:<6-digit-pin>\n  →  AUTH_OK\n or AUTH_FAIL\n
 *  3. Send CMD:<command>\n       →  response\n  (or no response for mouse/key moves)
 *  4. FILE_START:<name>:<size>\n →  READY_FOR_FILE\n → raw bytes → FILE_END\n → FILE_OK\n
 */
class DaemonClient {

    // ─── Connection State ─────────────────────────────────────────────────────
    sealed class State {
        object Disconnected : State()
        object Connecting   : State()
        data class Connected(val ip: String, val port: Int) : State()
        data class Error(val message: String)              : State()
    }

    private val _state = MutableStateFlow<State>(State.Disconnected)
    val state: StateFlow<State> = _state.asStateFlow()

    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var outputStream: OutputStream? = null

    // Serializes ALL socket I/O — prevents stats polling and file transfer
    // from interleaving reads/writes on the same TCP stream.
    private val socketMutex = Mutex()

    // Last known host/port — used for auto-reconnect after app resume
    var lastHost: String? = null
        private set
    var lastPort: Int = 0
        private set

    // socket.isConnected is unreliable on dead TCP connections — it stays true
    // even after the remote side has closed. Use this combined check instead.
    val isConnected: Boolean
        get() {
            val s = socket ?: return false
            return s.isConnected && !s.isClosed && !s.isInputShutdown && !s.isOutputShutdown
        }

    // ─── Connect & Authenticate ───────────────────────────────────────────────

    suspend fun connect(ip: String, port: Int, pin: String): Boolean = withContext(Dispatchers.IO) {
        _state.value = State.Connecting
        try {
            val s = try {
                SslHelper.createSslSocket(ip, port, TIMEOUT_MS)
            } catch (e: Exception) {
                Log.w(TAG, "TLS connect fallback to standard socket: ${e.message}")
                Socket(ip, port).apply { soTimeout = TIMEOUT_MS }
            }
            socket = s
            outputStream = s.outputStream
            writer = PrintWriter(s.outputStream.writer(), true)

            // Authentication
            writer!!.println("AUTH:$pin")
            val response = readLineUnbuffered()

            return@withContext if (response == "AUTH_OK") {
                socket!!.soTimeout = 0  // No timeout after auth
                socket!!.keepAlive = true  // OS-level TCP keepalive — detects dead connections
                lastHost = ip
                lastPort = port
                _state.value = State.Connected(ip, port)
                Log.i(TAG, "✅ Connected and authenticated to $ip:$port")
                true
            } else {
                Log.w(TAG, "❌ Auth failed (got: $response)")
                disconnect()
                _state.value = State.Error(
                    when (response) {
                        "AUTH_BLOCKED" -> "Too many attempts — IP temporarily blocked. Wait, then retry."
                        null -> "No response from daemon (timeout). Check it is running."
                        else -> "Wrong PIN — try again"
                    }
                )
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "connect() failed: ${e.message}")
            disconnect()
            _state.value = State.Error(e.message ?: "Connection failed")
            false
        }
    }

    fun disconnect() {
        try {
            writer?.close()
            socket?.close()
        } catch (_: Exception) {}
        socket = null; writer = null; outputStream = null
        _state.value = State.Disconnected
    }

    // ─── Generic Command ──────────────────────────────────────────────────────

    /**
     * Send CMD:<cmd>\n and optionally read one response line.
     * For high-frequency commands (mouse/key) expectResponse=false for perf.
     */
    suspend fun sendCommand(cmd: String, expectResponse: Boolean = true): String? =
        withContext(Dispatchers.IO) {
            if (!isConnected) {
                // Socket looks dead — make sure state reflects that
                if (_state.value is State.Connected) {
                    Log.w(TAG, "sendCommand($cmd): socket dead, marking Disconnected")
                    disconnect()
                }
                return@withContext null
            }
            socketMutex.withLock {
                try {
                    writer?.println("CMD:$cmd")
                    if (expectResponse) readLineUnbuffered() else null
                } catch (e: java.io.IOException) {
                    // IOException on a live-looking socket = connection dropped mid-flight
                    Log.e(TAG, "sendCommand($cmd) socket broken: ${e.message}")
                    disconnect()  // clears socket refs and sets State.Disconnected
                    _state.value = State.Error("Connection lost — tap to reconnect")
                    null
                } catch (e: Exception) {
                    Log.e(TAG, "sendCommand($cmd) error: ${e.message}")
                    null
                }
            }
        }

    // ─── Convenience wrappers ─────────────────────────────────────────────────

    suspend fun shutdown()    = sendCommand("shutdown")
    suspend fun reboot()      = sendCommand("reboot")
    suspend fun sleep()       = sendCommand("sleep")
    suspend fun lock()        = sendCommand("lock")
    suspend fun unlock()      = sendCommand("unlock")
    suspend fun wakeup()      = sendCommand("wakeup")
    suspend fun volumeUp()    = sendCommand("volume_up")
    suspend fun volumeDown()  = sendCommand("volume_down")
    suspend fun mute()        = sendCommand("mute")

    /** Returns parsed SystemInfo or null on failure. */
    suspend fun getSystemInfo(): com.titanshare.android.data.model.SystemInfo? =
        withContext(Dispatchers.IO) {
            try {
                val raw = sendCommand("get_info") ?: return@withContext null
                val json = Gson().fromJson(raw, JsonObject::class.java)
                val data = json.getAsJsonObject("data") ?: return@withContext null

                // Parse per-core usage array ["12.5", "45.0", ...]
                val coresUsage = try {
                    data.getAsJsonArray("cpu_cores_usage")
                        ?.mapNotNull { it.asString.toFloatOrNull() }
                        ?: emptyList()
                } catch (_: Exception) { emptyList() }

                com.titanshare.android.data.model.SystemInfo(
                    brand        = data.getString("brand"),
                    model        = data.getString("model"),
                    osVersion    = data.getString("os_version"),
                    cpuLoad      = data.getString("cpu_load"),
                    cpuTemp      = data.getString("cpu_temp"),
                    cpuCoresUsage = coresUsage,
                    cpuFreqGhz   = data.getString("cpu_freq_ghz"),
                    cpuCoreCount = try { data.get("cpu_core_count")?.asInt ?: 0 } catch (_: Exception) { 0 },
                    cpuModel     = data.getString("cpu_model").ifEmpty { "Intel Core Processor" },
                    gpuModel     = data.getString("gpu_model").ifEmpty { "Integrated Graphics" },
                    ramUsage     = data.getString("ramUsage"),
                    ramUsed      = data.getString("ramUsed"),
                    ramTotal     = data.getString("ramTotal"),
                    storageUsage = data.getString("storage_usage"),
                    storageUsed  = data.getString("storageUsed"),
                    storageTotal = data.getString("storageTotal"),
                    battery      = data.get("battery")?.asInt ?: 0,
                    netSpeed     = data.getString("net_speed"),
                )
            } catch (e: Exception) {
                Log.e(TAG, "getSystemInfo error: ${e.message}")
                null
            }
        }

    // ─── Mouse ────────────────────────────────────────────────────────────────

    suspend fun mouseMove(dx: Int, dy: Int) =
        sendCommand("MOUSE_MOVE:$dx:$dy", expectResponse = false)

    suspend fun mouseScroll(delta: Int) =
        sendCommand("MOUSE_SCROLL:$delta", expectResponse = false)

    suspend fun mouseClick(button: String = "left") =
        sendCommand("MOUSE_CLICK:$button", expectResponse = false)

    suspend fun mouseDown(button: String = "left") =
        sendCommand("MOUSE_DOWN:$button", expectResponse = false)

    suspend fun mouseUp(button: String = "left") =
        sendCommand("MOUSE_UP:$button", expectResponse = false)

    // ─── Keyboard ─────────────────────────────────────────────────────────────

    suspend fun typeText(text: String) =
        sendCommand("KEY_TYPE:$text", expectResponse = false)

    suspend fun pressKey(key: String) =
        sendCommand("KEY_PRESS:$key", expectResponse = false)

    // ─── Screen Mirror ────────────────────────────────────────────────────────

    /**
     * Sends START_PHONE_MIRROR on the existing authenticated TCP socket.
     * Daemon responds with JSON: {"type":"MIRROR_READY","port":5001,"ip":"..."}
     * Returns the UDP port to stream H.264 frames to, or -1 on failure.
     */
    suspend fun startPhoneMirror(): Int = withContext(Dispatchers.IO) {
        if (!isConnected) return@withContext -1
        socketMutex.withLock {
            try {
                writer?.println("CMD:START_MIRROR")
                val raw = readLineUnbuffered() ?: return@withLock -1
                Log.i(TAG, "startPhoneMirror response: $raw")
                // Daemon replies with JSON {"type":"MIRROR_READY","port":5001,...}
                return@withLock try {
                    val json = Gson().fromJson(raw, JsonObject::class.java)
                    json.get("port")?.asInt ?: 5001
                } catch (_: Exception) {
                    if (raw.contains("MIRROR_READY")) 5001 else -1
                }
            } catch (e: Exception) {
                Log.e(TAG, "startPhoneMirror error: ${e.message}")
                -1
            }
        }
    }

    suspend fun stopPhoneMirror() = sendCommand("STOP_MIRROR")

    // ─── File Transfer ────────────────────────────────────────────────────────

    /**
     * Sends a file to the daemon.
     * [onProgress] called with bytes sent (0..total).
     */
    suspend fun sendFile(
        name: String,
        size: Long,
        inputStream: InputStream,
        onProgress: (Long, Long) -> Unit = { _, _ -> },
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isConnected) return@withContext false
        // Hold the mutex for the entire transfer so no CMD (e.g. get_info) can
        // inject bytes into the stream mid-transfer and corrupt the FILE_OK read.
        socketMutex.withLock {
            try {
                writer?.println("FILE_START:$name:$size")
                val ack = readLineUnbuffered()
                if (ack != "READY_FOR_FILE") {
                    Log.w(TAG, "FILE_START rejected: $ack")
                    return@withLock false
                }

                val out = outputStream ?: return@withLock false
                val buffer = ByteArray(131072) // 128 KB chunks for throughput
                var offset = 0L
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    out.write(buffer, 0, bytesRead)
                    offset += bytesRead
                    onProgress(offset, size)
                }
                // Daemon no longer relies on FILE_END sentinel (uses byte count),
                // but we still flush to ensure all bytes reach the kernel buffer.
                out.flush()

                // Set a generous timeout so we don't hang forever waiting for FILE_OK
                // in case the daemon crashes after receiving all bytes.
                socket?.soTimeout = 30_000
                val result = readLineUnbuffered()
                socket?.soTimeout = 0  // Restore no-timeout for normal commands
                Log.d(TAG, "sendFile result: $result")
                result == "FILE_OK"
            } catch (e: Exception) {
                Log.e(TAG, "sendFile error: ${e.message}")
                false
            } finally {
                try { inputStream.close() } catch (_: Exception) {}
            }
        }
    }

    // ─── Linux → Android File Receive ────────────────────────────────────────

    data class LinuxFile(val name: String, val size: Long)

    /**
     * Fetches the list of files the Linux PC has ready to push.
     */
    suspend fun getLinuxFiles(): List<LinuxFile> = withContext(Dispatchers.IO) {
        if (!isConnected) return@withContext emptyList()
        socketMutex.withLock {
            try {
                writer?.println("CMD:push_file_list")
                val raw = readLineUnbuffered() ?: return@withLock emptyList<LinuxFile>()
                val json = Gson().fromJson(raw, JsonObject::class.java)
                val arr  = json.getAsJsonArray("files") ?: return@withLock emptyList<LinuxFile>()
                arr.mapNotNull { el ->
                    val obj = el.asJsonObject
                    val name = obj.get("name")?.asString ?: return@mapNotNull null
                    val size = obj.get("size")?.asLong ?: 0L
                    LinuxFile(name, size)
                }
            } catch (e: Exception) {
                Log.e(TAG, "getLinuxFiles error: ${e.message}")
                emptyList()
            }
        }
    }

    /**
     * Requests a file from the Linux daemon and saves it to Downloads/TitanShare/.
     * [onProgress] called with (bytesReceived, totalBytes).
     * Returns the saved filename on success, null on failure.
     */
    suspend fun receiveFile(
        context: android.content.Context,
        filename: String,
        onProgress: (Long, Long) -> Unit = { _, _ -> },
    ): String? = withContext(Dispatchers.IO) {
        if (!isConnected) return@withContext null
        socketMutex.withLock {
            try {
                writer?.println("CMD:push_file:$filename")

                // Read the FILE_PUSH:<name>:<size> header line
                val header = readLineUnbuffered() ?: return@withLock null
                if (header.startsWith("PUSH_ERROR:")) {
                    Log.e(TAG, "Daemon push error: $header")
                    return@withLock null
                }
                if (!header.startsWith("FILE_PUSH:")) return@withLock null

                val lastColon = header.lastIndexOf(':')
                val fileSize  = header.substring(lastColon + 1).toLongOrNull() ?: return@withLock null
                val name      = header.substring("FILE_PUSH:".length, lastColon)

                // Save to Downloads/TitanShare/ via MediaStore (works on Android 10+)
                val resolver = context.contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Downloads.DISPLAY_NAME, name)
                    put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                    put(android.provider.MediaStore.Downloads.RELATIVE_PATH, "Download/TitanShare")
                }
                val uri = resolver.insert(
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                ) ?: return@withLock null

                resolver.openOutputStream(uri)?.use { out ->
                    val rawIn = socket!!.getInputStream()
                    val buf   = ByteArray(131072) // 128 KB
                    var received = 0L
                    while (received < fileSize) {
                        val toRead = minOf(buf.size.toLong(), fileSize - received).toInt()
                        val n = rawIn.read(buf, 0, toRead)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        received += n
                        onProgress(received, fileSize)
                    }
                    out.flush()
                }

                Log.i(TAG, "✅ receiveFile saved: $name ($fileSize bytes)")
                name
            } catch (e: Exception) {
                Log.e(TAG, "receiveFile error: ${e.message}")
                disconnect()
                null
            }
        }
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    /**
     * Reads a single line directly from the socket's InputStream byte-by-byte.
     * Prevents BufferedReader from buffering binary file data that follows the header.
     */
    private fun readLineUnbuffered(): String? {
        val s = socket ?: return null
        return try {
            val stream = s.getInputStream()
            val sb = java.lang.StringBuilder()
            var c: Int
            while (stream.read().also { c = it } != -1) {
                if (c == '\n'.code) break
                if (c != '\r'.code) sb.append(c.toChar())
            }
            if (sb.isEmpty() && c == -1) null else sb.toString()
        } catch (e: Exception) {
            null
        }
    }

    private fun JsonObject.getString(key: String): String =
        try { get(key)?.asString ?: "" } catch (_: Exception) { "" }
}
