package com.titanshare.android.data.network

import android.util.Log
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

/**
 * Helper for establishing TLS encrypted socket connections over local LAN.
 * Configured for peer-to-peer connections using self-signed certificates.
 */
object SslHelper {
    private const val TAG = "SslHelper"

    private val trustAllCerts = arrayOf<TrustManager>(
        object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
    )

    val hostnameVerifier = HostnameVerifier { _, _ -> true }

    fun createSslContext(): SSLContext {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, SecureRandom())
        return sslContext
    }

    fun createSslSocketFactory(): SSLSocketFactory {
        return createSslContext().socketFactory
    }

    fun createSslSocket(ip: String, port: Int, timeoutMs: Int = 8000): SSLSocket {
        val factory = createSslSocketFactory()
        val socket = factory.createSocket(ip, port) as SSLSocket
        socket.soTimeout = timeoutMs
        
        val supported = socket.supportedProtocols
        val enabledProtocols = supported.filter { it == "TLSv1.3" || it == "TLSv1.2" }
        if (enabledProtocols.isNotEmpty()) {
            socket.enabledProtocols = enabledProtocols.toTypedArray()
        }

        socket.startHandshake()
        Log.i(TAG, "🔒 TLS Handshake complete with $ip:$port [Protocol: ${socket.session.protocol}]")
        return socket
    }
}
