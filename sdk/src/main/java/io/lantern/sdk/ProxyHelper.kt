package io.lantern.sdk

import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Proxy
import okhttp3.OkHttpClient
import okhttp3.Request

object ProxyHelper {
    // Default proxy configuration
    private var proxyHost: String = "127.0.0.1"
    private var proxyPort: Int = 8080
    private var httpClient: OkHttpClient = createHttpClient()

    // Update the proxy settings
    fun setProxy(host: String, port: Int) {
        proxyHost = host
        proxyPort = port
        httpClient = createHttpClient() // Recreate the client with the new proxy
    }

    private fun createHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyHost, proxyPort)))
            .build()
    }

    // Forward request through proxy
    fun proxyRequest(url: String): ProxyResponse? {
        return try {
            val request = Request.Builder()
                .url(url)
                .build()

            val response = httpClient.newCall(request).execute()
            ProxyResponse(
                mimeType = response.header("Content-Type", "text/plain") ?: "text/plain",
                encoding = response.header("Content-Encoding", "UTF-8") ?: "UTF-8",
                inputStream = response.body?.byteStream() ?: InputStream.nullInputStream()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    data class ProxyResponse(
        val mimeType: String,
        val encoding: String,
        val inputStream: InputStream
    )
}
