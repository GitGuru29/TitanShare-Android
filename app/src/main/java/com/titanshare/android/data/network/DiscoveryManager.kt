package com.titanshare.android.data.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.titanshare.android.data.model.Device
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "DiscoveryManager"
private const val SERVICE_TYPE = "_titanshare._tcp."

/**
 * Android NSD discovery stops firing onServiceFound after ~2 min because
 * the mDNS cache considers the service "already known". To work around this
 * we periodically restart the NSD listener every [RESTART_INTERVAL_MS] so
 * the system issues fresh mDNS queries.
 */
private const val RESTART_INTERVAL_MS = 30_000L  // restart every 30s

/**
 * Wraps Android NsdManager to discover TitanShare daemons on the LAN.
 * Emits discovered [Device]s via [devices] StateFlow.
 */
class DiscoveryManager(context: Context) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val handler = Handler(Looper.getMainLooper())

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = _devices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var discoveryListener: NsdManager.DiscoveryListener? = null

    // Track pending resolutions so we don't spam resolve for the same service
    private val resolving = mutableSetOf<String>()
    private val discovered = mutableMapOf<String, Device>() // key = service name

    // Periodic restart runnable — forces fresh mDNS queries
    private val restartRunnable = Runnable {
        if (_isScanning.value) {
            Log.d(TAG, "Cycling NSD listener for fresh discovery...")
            stopDiscoveryInternal()
            // Small delay before restarting to let NSD clean up
            handler.postDelayed({ startDiscoveryInternal(keepDevices = true) }, 500)
            scheduleRestart()
        }
    }

    fun startDiscovery() {
        if (_isScanning.value) return
        discovered.clear()
        _devices.value = emptyList()
        resolving.clear()
        startDiscoveryInternal(keepDevices = false)
        scheduleRestart()
    }

    private fun startDiscoveryInternal(keepDevices: Boolean) {
        if (!keepDevices) {
            discovered.clear()
            _devices.value = emptyList()
        }
        resolving.clear()

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Start discovery failed: $errorCode")
                _isScanning.value = false
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Stop discovery failed: $errorCode")
            }

            override fun onDiscoveryStarted(serviceType: String) {
                Log.d(TAG, "Discovery started for $serviceType")
                _isScanning.value = true
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "Discovery stopped")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service found: ${serviceInfo.serviceName}")
                if (serviceInfo.serviceType == SERVICE_TYPE &&
                    !resolving.contains(serviceInfo.serviceName)) {
                    resolving.add(serviceInfo.serviceName)
                    resolveService(serviceInfo)
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service lost: ${serviceInfo.serviceName}")
                discovered.remove(serviceInfo.serviceName)
                resolving.remove(serviceInfo.serviceName)
                _devices.value = discovered.values.toList()
            }
        }

        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
            override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "Resolve failed for ${info.serviceName}: $errorCode")
                resolving.remove(info.serviceName)
            }

            override fun onServiceResolved(info: NsdServiceInfo) {
                Log.d(TAG, "Resolved: ${info.serviceName} -> ${info.host?.hostAddress}:${info.port}")
                // Clear from resolving set so future cycles can re-resolve
                resolving.remove(info.serviceName)

                val ip = info.host?.hostAddress ?: return

                // Extract TXT record attributes if available (API 34+)
                val version = try {
                    @Suppress("UNCHECKED_CAST")
                    val attrs = info.attributes as? Map<String, ByteArray>
                    attrs?.get("ver")?.let { String(it) } ?: ""
                } catch (_: Exception) { "" }

                val device = Device(
                    name = info.serviceName,
                    host = ip,
                    port = info.port.takeIf { it > 0 } ?: 9999,
                    version = version,
                )
                discovered[info.serviceName] = device
                _devices.value = discovered.values.toList()
            }
        })
    }

    private fun scheduleRestart() {
        handler.removeCallbacks(restartRunnable)
        handler.postDelayed(restartRunnable, RESTART_INTERVAL_MS)
    }

    private fun stopDiscoveryInternal() {
        try {
            discoveryListener?.let { nsdManager.stopServiceDiscovery(it) }
        } catch (e: Exception) {
            Log.w(TAG, "stopDiscoveryInternal: ${e.message}")
        }
        discoveryListener = null
    }

    fun stopDiscovery() {
        handler.removeCallbacks(restartRunnable)
        stopDiscoveryInternal()
        _isScanning.value = false
    }
}
