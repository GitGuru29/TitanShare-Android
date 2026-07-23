package com.titanshare.android.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.titanshare.android.data.model.Device
import com.titanshare.android.data.model.SystemInfo
import com.titanshare.android.data.network.DaemonClient
import com.titanshare.android.data.network.DiscoveryManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {

    // ─── Discovery ────────────────────────────────────────────────────────────
    val discoveryManager = DiscoveryManager(application)
    val discoveredDevices: StateFlow<List<Device>> = discoveryManager.devices
    val isScanning: StateFlow<Boolean> = discoveryManager.isScanning

    // ─── Selected Device & PIN ────────────────────────────────────────────────
    private val _selectedDevice = MutableStateFlow<Device?>(null)
    val selectedDevice: StateFlow<Device?> = _selectedDevice.asStateFlow()

    private val _pinInput = MutableStateFlow("")
    val pinInput: StateFlow<String> = _pinInput.asStateFlow()

    // ─── Connection ───────────────────────────────────────────────────────────
    val client = DaemonClient()
    val connectionState = client.state

    private val _pairingError = MutableStateFlow<String?>(null)
    val pairingError: StateFlow<String?> = _pairingError.asStateFlow()

    // ─── System Info ─────────────────────────────────────────────────────────
    private val _systemInfo = MutableStateFlow<SystemInfo?>(null)
    val systemInfo: StateFlow<SystemInfo?> = _systemInfo.asStateFlow()

    private var statsJob: Job? = null

    private var wakeLock: android.os.PowerManager.WakeLock? = null
    private var wifiLock: android.net.wifi.WifiManager.WifiLock? = null

    // ─── File Transfer (Android → Linux) ─────────────────────────────────────
    private val _fileProgress = MutableStateFlow(0f)
    val fileProgress: StateFlow<Float> = _fileProgress.asStateFlow()

    private val _fileStatus = MutableStateFlow<String?>(null)
    val fileStatus: StateFlow<String?> = _fileStatus.asStateFlow()

    // Transfer speed in Mbps (megabits per second)
    private val _transferSpeed = MutableStateFlow(0.0)
    val transferSpeed: StateFlow<Double> = _transferSpeed.asStateFlow()

    // Speed tracking state
    private var lastSpeedBytes = 0L
    private var lastSpeedTime  = 0L

    // ─── File Receive (Linux → Android) ──────────────────────────────────────
    private val _linuxFiles = MutableStateFlow<List<DaemonClient.LinuxFile>>(emptyList())
    val linuxFiles: StateFlow<List<DaemonClient.LinuxFile>> = _linuxFiles.asStateFlow()

    private val _receiveProgress = MutableStateFlow(0f)
    val receiveProgress: StateFlow<Float> = _receiveProgress.asStateFlow()

    private val _receiveStatus = MutableStateFlow<String?>(null)
    val receiveStatus: StateFlow<String?> = _receiveStatus.asStateFlow()

    private val _receiveSpeed = MutableStateFlow(0.0)
    val receiveSpeed: StateFlow<Double> = _receiveSpeed.asStateFlow()

    // ─── Toast / Snackbar ─────────────────────────────────────────────────────
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // ─── Screen Mirror ────────────────────────────────────────────────────────
    private val _mirrorActive = MutableStateFlow(false)
    val mirrorActive: StateFlow<Boolean> = _mirrorActive.asStateFlow()

    private val _mirrorError = MutableStateFlow<String?>(null)
    val mirrorError: StateFlow<String?> = _mirrorError.asStateFlow()

    /** UDP port returned by the daemon after START_PHONE_MIRROR. Passed to the service. */
    var mirrorUdpPort: Int = 5001
        private set

    // ─────────────────────────────────────────────────────────────────────────

    fun startDiscovery()  = discoveryManager.startDiscovery()
    fun stopDiscovery()   = discoveryManager.stopDiscovery()

    fun selectDevice(device: Device) {
        _selectedDevice.value = device
        _pairingError.value   = null
        _pinInput.value       = ""
    }

    fun updatePin(pin: String) {
        if (pin.length <= 6 && pin.all { it.isDigit() })
            _pinInput.value = pin
    }

    fun clearPairingError() { _pairingError.value = null }

    /** Connect + authenticate. Returns true on success. */
    fun pair(onSuccess: () -> Unit) {
        val device = _selectedDevice.value ?: return
        val pin    = _pinInput.value
        _pairingError.value = null
        viewModelScope.launch {
            val ok = client.connect(device.host, device.port, pin)
            if (ok) {
                val pm = getApplication<Application>().getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
                wakeLock = pm.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "TitanShare::WakeLock")
                wakeLock?.acquire()
                val wm = getApplication<Application>().getSystemService(android.content.Context.WIFI_SERVICE) as android.net.wifi.WifiManager
                wifiLock = wm.createWifiLock(android.net.wifi.WifiManager.WIFI_MODE_FULL_HIGH_PERF, "TitanShare::WifiLock")
                wifiLock?.acquire()

                stopDiscovery()
                startStatsPolling()
                onSuccess()
            } else {
                _pairingError.value = "Wrong PIN or connection refused"
            }
        }
    }

    fun disconnect() {
        stopStatsPolling()
        client.disconnect()
        wakeLock?.let { if (it.isHeld) it.release() }
        wifiLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        wifiLock = null
        _selectedDevice.value = null
        _systemInfo.value     = null
    }

    // ─── Stats polling ────────────────────────────────────────────────────────

    private fun startStatsPolling() {
        statsJob?.cancel()
        statsJob = viewModelScope.launch {
            while (isActive) {
                val info = client.getSystemInfo()
                _systemInfo.value = info

                // If DaemonClient detected a dead socket it will have set State.Error
                // or State.Disconnected — stop polling so we don't spin uselessly.
                val currentState = client.state.value
                if (currentState is DaemonClient.State.Error ||
                    currentState is DaemonClient.State.Disconnected) {
                    break
                }

                delay(3000L)
            }
        }
    }

    fun stopStatsPolling() { statsJob?.cancel() }

    // ─── App Lifecycle (called from MainActivity) ─────────────────────────────

    /**
     * Call this from MainActivity.onResume().
     * If the socket dropped while we were in the background, silently reconnect
     * using the last known host/port and PIN.
     */
    fun onAppResume() {
        val currentState = client.state.value
        val host = client.lastHost
        val port = client.lastPort
        val pin  = _pinInput.value

        // Only attempt reconnect if we were previously connected and lost it
        if (host != null && port > 0 &&
            (currentState is DaemonClient.State.Disconnected ||
             currentState is DaemonClient.State.Error)) {

            viewModelScope.launch {
                val ok = client.connect(host, port, pin)
                if (ok) {
                    startStatsPolling()
                } else {
                    // Let the UI handle the error state shown by DaemonClient
                }
            }
        } else if (currentState is DaemonClient.State.Connected && statsJob?.isActive != true) {
            // Connected but stats polling stopped (e.g. after a recoverable hiccup)
            startStatsPolling()
        }
    }

    // ─── Remote Commands ──────────────────────────────────────────────────────

    fun shutdown()   = send("Shutdown")     { client.shutdown() }
    fun reboot()     = send("Reboot")       { client.reboot() }
    fun sleep()      = send("Sleep")        { client.sleep() }
    fun lock()       = send("Lock")         { client.lock() }
    fun unlock()     = send("Unlock")       { client.unlock() }
    fun wakeup()     = send("Wake")         { client.wakeup() }
    fun volumeUp()   = sendSilent           { client.volumeUp() }
    fun volumeDown() = sendSilent           { client.volumeDown() }
    fun mute()       = sendSilent           { client.mute() }

    fun mouseMove(dx: Int, dy: Int) = viewModelScope.launch { client.mouseMove(dx, dy) }
    fun mouseScroll(dy: Int)        = viewModelScope.launch { client.mouseScroll(dy) }
    fun mouseClick(btn: String = "left") = viewModelScope.launch { client.mouseClick(btn) }
    fun typeText(text: String)      = viewModelScope.launch { client.typeText(text) }
    fun pressKey(key: String)       = viewModelScope.launch { client.pressKey(key) }

    // ─── Screen Mirror commands ───────────────────────────────────────────────

    fun onMirrorServiceStarted() {
        _mirrorActive.value = true
        _mirrorError.value  = null
    }

    fun onMirrorServiceStopped(error: String?) {
        _mirrorActive.value = false
        _mirrorError.value  = error
    }

    fun clearMirrorError() { _mirrorError.value = null }

    fun prepareAndStartMirror(onReady: () -> Unit) {
        viewModelScope.launch {
            val port = client.startPhoneMirror()
            if (port < 0) {
                _mirrorError.value = "Daemon rejected mirror start — reconnect and try again"
                return@launch
            }
            mirrorUdpPort = port
            onReady()
        }
    }

    fun stopMirror() {
        viewModelScope.launch { client.stopPhoneMirror() }
    }

    // ─── File Transfer ────────────────────────────────────────────────────────

    fun sendFile(name: String, size: Long, inputStream: java.io.InputStream) {
        _fileProgress.value  = 0f
        _fileStatus.value    = "Sending…"
        _transferSpeed.value = 0.0
        lastSpeedBytes       = 0L
        lastSpeedTime        = System.currentTimeMillis()

        viewModelScope.launch {
            val ok = client.sendFile(name, size, inputStream) { sent, total ->
                if (total > 0) {
                    _fileProgress.value = sent.toFloat() / total.toFloat()
                }

                // Calculate speed in Mbps
                val now = System.currentTimeMillis()
                val dtMs = now - lastSpeedTime
                if (dtMs >= 250) { // Update speed every 250ms to avoid jitter
                    val deltaBytes = sent - lastSpeedBytes
                    val deltaSec   = dtMs / 1000.0
                    val megabitsPerSec = (deltaBytes * 8.0) / (deltaSec * 1_000_000.0)
                    _transferSpeed.value = megabitsPerSec
                    lastSpeedBytes = sent
                    lastSpeedTime  = now
                }
            }
            _fileStatus.value    = if (ok) "✅ Sent!" else "❌ Failed"
            _fileProgress.value  = if (ok) 1f else 0f
            _transferSpeed.value = 0.0
        }
    }

    fun clearFileStatus() {
        _fileStatus.value    = null
        _fileProgress.value  = 0f
        _transferSpeed.value = 0.0
    }

    // ─── Linux → Android Receive ──────────────────────────────────────────────

    fun refreshLinuxFiles() {
        viewModelScope.launch {
            _linuxFiles.value = client.getLinuxFiles()
        }
    }

    fun receiveFile(context: android.content.Context, filename: String, fileSize: Long) {
        _receiveProgress.value = 0f
        _receiveStatus.value   = "Downloading…"
        _receiveSpeed.value    = 0.0
        var lastBytes = 0L
        var lastTime  = System.currentTimeMillis()

        viewModelScope.launch {
            val saved = client.receiveFile(context, filename) { received, total ->
                if (total > 0) _receiveProgress.value = received.toFloat() / total.toFloat()

                // Speed calculation every 250ms
                val now = System.currentTimeMillis()
                val dtMs = now - lastTime
                if (dtMs >= 250) {
                    val deltaBytes = received - lastBytes
                    val deltaSec   = dtMs / 1000.0
                    _receiveSpeed.value = (deltaBytes * 8.0) / (deltaSec * 1_000_000.0)
                    lastBytes = received
                    lastTime  = now
                }
            }
            _receiveStatus.value   = if (saved != null) "✅ Saved to Downloads/TitanShare/$saved" else "❌ Download failed"
            _receiveProgress.value = if (saved != null) 1f else 0f
            _receiveSpeed.value    = 0.0
            if (saved != null) refreshLinuxFiles() // refresh list
        }
    }

    fun clearReceiveStatus() {
        _receiveStatus.value   = null
        _receiveProgress.value = 0f
        _receiveSpeed.value    = 0.0
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun send(label: String, block: suspend () -> Unit) =
        viewModelScope.launch {
            block()
            _toastMessage.value = label
        }

    private fun sendSilent(block: suspend () -> Unit) =
        viewModelScope.launch { block() }

    fun clearToast() { _toastMessage.value = null }

    override fun onCleared() {
        super.onCleared()
        stopDiscovery()
        client.disconnect()
    }
}
