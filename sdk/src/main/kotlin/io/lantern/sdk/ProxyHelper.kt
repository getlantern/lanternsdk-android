package io.lantern.sdk

import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.io.InputStream
import java.net.*
import java.net.InetSocketAddress
import java.net.Proxy

object ProxyHelper {
    private const val TAG = "ProxyHelper"
    private var httpClient = OkHttpClient.Builder().build()

    /**
     * Sets the system proxy.
     */
    fun setProxy(proxyAddr: String) {
        if (proxyAddr.isBlank()) {
            clearProxy()
            return
        }
        val uri = URI("my://$proxyAddr")
        val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(uri.host, uri.port))

        ProxySelector.setDefault(
            object : ProxySelector() {
                override fun select(uri: URI?): List<Proxy> = listOf(proxy)

                override fun connectFailed(
                    uri: URI?,
                    sa: SocketAddress?,
                    ioe: IOException?,
                ) {
                    Log.e(TAG, "Proxy connection failed for $uri: $ioe")
                }
            },
        )
        Log.d(TAG, "Proxy set to: $proxy")
        // Update HTTP client with proxy
        httpClient = OkHttpClient.Builder().proxy(proxy).build()
    }

    /**
     * Clears the system proxy by resetting to the default ProxySelector.
     */
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

    /**
     * Forwards a network request through the proxy using OkHttp.
     * Returns a ProxyResponse with the content type, encoding, and response body.
     *
     * @param url The URL to request.
     * @return ProxyResponse with the response data, or null if the request failed.
     */
    private fun proxyRequest(url: String): ProxyResponse? =
        try {
            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()
            ProxyResponse(
                mimeType = "text/html",
                encoding = response.header("Content-Encoding", "UTF-8") ?: "UTF-8",
                inputStream = response.body?.byteStream() ?: InputStream.nullInputStream(),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to proxy request: ${e.message}")
            null
        }

    /**
     * Creates a custom WebViewClient for handling WebView requests and responses.
     *
     * The client intercepts requests, forwards them through the proxy, and
     * returns the proxied response to the WebView. This ensures all traffic
     * goes through Lantern's proxy.
     *
     * @return A configured WebViewClient.
     */
    fun createWebViewClient(): WebViewClient {
        return object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest,
            ): Boolean = false

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest,
            ): WebResourceResponse? {
                val url = request.url.toString()
                val response = proxyRequest(url) ?: return null
                return WebResourceResponse(
                    response.mimeType,
                    response.encoding,
                    response.inputStream,
                )
            }
        }
    }

    /**
     * Sends an HTTP request through the configured proxy and handles the response.
     *
     * @param url The URL to send the HTTP request to.
     * @param onSuccess Callback invoked with the response body on a successful request.
     * @param onError Callback invoked with an error message if the request fails.
     */
    fun testHttpRequest(
        url: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        // Launch a coroutine to handle the request
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder().url(url).build()
                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: "No response body"

                // Invoke the success callback on the main thread
                withContext(Dispatchers.Main) {
                    onSuccess(responseBody)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error sending HTTP request: ${e.message}")
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Unknown error")
                }
            }
        }
    }

    /**
     * Converts a host:port string into an InetSocketAddress.
     */
    fun addrFromString(addr: String): InetSocketAddress {
        val uri = URI("my://$addr")
        return InetSocketAddress("127.0.0.1", uri.port)
    }

    /**
     * Data class to represent the response from a proxied request.
     */
    data class ProxyResponse(
        val mimeType: String,
        val encoding: String,
        val inputStream: InputStream,
    )
}
