package com.titanshare.android.data.model

/**
 * A discovered TitanShare daemon on the LAN.
 */
data class Device(
    val name: String,          // mDNS service instance name (hostname)
    val host: String,          // Resolved IP address
    val port: Int = 9999,
    val version: String = "",
)
