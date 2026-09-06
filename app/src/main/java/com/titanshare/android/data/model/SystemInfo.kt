package com.titanshare.android.data.model

/**
 * Parsed response from CMD:get_info
 */
data class SystemInfo(
    val brand: String        = "LENOVO",
    val model: String        = "82KB",
    val osVersion: String    = "Arch Linux 6.18.47-1-lts",
    val cpuLoad: String      = "6.3",
    val cpuTemp: String      = "43",
    val cpuCoresUsage: List<Float> = listOf(4f, 11f, 5f, 3f, 8f, 4f, 5f, 7f),
    val cpuFreqGhz: String   = "0.40",
    val cpuCoreCount: Int    = 8,
    val cpuModel: String     = "Intel i5-1135G7 (8 threads)",
    val gpuModel: String     = "NVIDIA GeForce MX350",
    val ramUsage: String     = "43.0",
    val ramUsed: String      = "6.0",
    val ramTotal: String     = "15.0",
    val storageUsage: String = "42.7",
    val storageUsed: String  = "199",
    val storageTotal: String = "467",
    val battery: Int         = 99,
    val netSpeed: String     = "12.4 Mbps",
)
