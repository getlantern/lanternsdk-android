package io.lantern.sdk

import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.*
import okhttp3.OkHttpClient
import okhttp3.Request

object ProxyHelper {
    // Default proxy configuration
    private const val TAG = "ProxyHelper"
    private var httpClient = OkHttpClient.Builder().build()

    private fun clearProxy() {
        try {
            // Reset to the default ProxySelector (clears custom proxy settings)
            ProxySelector.setDefault(ProxySelector.getDefault())
            httpClient = OkHttpClient.Builder().build()
            Log.d(TAG, "Proxy settings cleared")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setProxy(proxyAddr:String) {
        if (proxyAddr == "") {
            clearProxy()
            return
        }
        val uri = URI("my://$proxyAddr")
        val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", uri.port))
        createHTTPClient("127.0.0.1", uri.port)
        ProxySelector.setDefault(object : ProxySelector() {
            override fun select(uri: URI?): List<Proxy> = listOf(proxy)
            override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {}
        })
        Log.d(TAG, "Proxy set to: $proxy")
    }

    // Update the proxy settings
    private fun createHTTPClient(proxyHost: String, proxyPort: Int) {
        httpClient = OkHttpClient.Builder()
        .proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyHost, proxyPort)))
        .build()
    }

    // Forward request through proxy
    fun proxyRequest(
        url: String,
    ): ProxyResponse? {
        return try {
            val request = Request.Builder()
                .url(url).build()

            val response = httpClient.newCall(request).execute()
            ProxyResponse(
                mimeType = "text/html",
                encoding = "UTF-8",
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
