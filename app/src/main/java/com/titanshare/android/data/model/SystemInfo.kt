package com.titanshare.android.data.model

/**
 * Parsed response from CMD:get_info
 */
data class SystemInfo(
    val brand: String        = "Unknown",
    val model: String        = "Unknown",
    val osVersion: String    = "Unknown",
    val cpuLoad: String      = "0.0",
    val cpuTemp: String      = "0.0",
    // Per-core usage — list of floats 0..100, one per logical core
    val cpuCoresUsage: List<Float> = emptyList(),
    // CPU frequency in GHz (average across cores)
    val cpuFreqGhz: String   = "0.00",
    // Number of logical cores
    val cpuCoreCount: Int    = 0,
    val ramUsage: String     = "0.0",
    val ramUsed: String      = "0",
    val ramTotal: String     = "0",
    val storageUsage: String = "0.0",
    val storageUsed: String  = "0",
    val storageTotal: String = "0",
    val battery: Int         = 0,
    val netSpeed: String     = "0 Mbps",
)
