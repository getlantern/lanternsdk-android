package org.getlantern.lantern.sdk.example

import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebSettings.PluginState
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceResponse
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import io.lantern.sdk.Lantern
import io.lantern.sdk.ProxyHelper

class MainActivity : AppCompatActivity() {

    private lateinit var logsView: TextView
    private lateinit var startProxyButton: Button
    private lateinit var stopProxyButton: Button
    private lateinit var testRequestButton: Button
    private lateinit var launchWebViewButton: Button
    private lateinit var scrollView: ScrollView
    private lateinit var webView: WebView

    private var proxyRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Lantern.setup(this, "Example", "Example/Config")

        setContentView(R.layout.activity_main)

        logsView = findViewById(R.id.logs)
        startProxyButton = findViewById(R.id.startProxyButton)
        stopProxyButton = findViewById(R.id.stopProxyButton)
        testRequestButton = findViewById(R.id.testRequestButton)
        launchWebViewButton = findViewById(R.id.launchWebViewButton)
        scrollView = findViewById(R.id.logsContainer)
        webView = findViewById(R.id.webView)

        startProxyButton.setOnClickListener { startProxy() }
        stopProxyButton.setOnClickListener { stopProxy() }
        testRequestButton.setOnClickListener { testHttpRequest() }
        launchWebViewButton.setOnClickListener { openWebView() }
    }

    private fun startProxy() {
        // Start the HTTP proxy using the SDK
        val proxyAddr = ":8080"
        val proxyAllTraffic = true
        val result = Lantern.start(this, proxyAddr, proxyAllTraffic)
        val proxyPort = result.port
        ProxyHelper.setProxy("127.0.0.1", proxyPort)
        appendLog("Proxy started on port $proxyPort\n")
        proxyRunning = true

        startProxyButton.visibility = View.GONE
        stopProxyButton.visibility = View.VISIBLE
        testRequestButton.visibility = View.VISIBLE
        launchWebViewButton.visibility = View.VISIBLE
    }

    // Stop the HTTP proxy
    private fun stopProxy() {
        Lantern.stop()

        appendLog("Proxy stopped\n")
        proxyRunning = false

        startProxyButton.visibility = View.VISIBLE
        stopProxyButton.visibility = View.GONE
        testRequestButton.visibility = View.GONE
        launchWebViewButton.visibility = View.GONE
        webView.visibility = View.GONE
        logsView.text = ""
    }

    private fun openWebView() {
        if (!proxyRunning) {
            appendLog("Proxy is not running. Start the proxy first.\n")
            return
        }

        // Configure the WebView to use the proxy
        setProxyForWebView()

        webView.apply {
            settings.loadWithOverviewMode = true
            settings.javaScriptEnabled = true
            settings.pluginState = PluginState.ON
            visibility = View.VISIBLE
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    return false
                }
                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest): WebResourceResponse? {
                    val url = request.url.toString()
                    val proxyResponse = ProxyHelper.proxyRequest(url) ?: return null
                    return WebResourceResponse(
                        proxyResponse.mimeType,
                        proxyResponse.encoding,
                        proxyResponse.inputStream
                    )
                }
            }
            loadUrl("https://whatismyipaddress.com")
        }
    }

    private fun setProxyForWebView() {
        val proxyHost = "127.0.0.1"
        val proxyPort = Lantern.getProxyPort()

        val proxySelector = object : ProxySelector() {
            override fun select(uri: URI?): List<Proxy> {
                return listOf(Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyHost, proxyPort)))
            }

            override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
                Log.e("Proxy", "Connection to $uri failed: $ioe")
            }
        }
        ProxySelector.setDefault(proxySelector)
    }

    private fun appendLog(message: String) {
        logsView.append(message)
        scrollView.post {
            scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun testHttpRequest() {
        if (!proxyRunning) {
            appendLog("Proxy is not running. Start the proxy first.\n")
            return
        }

        // Configure OkHttp client to use the proxy
        val proxyHost = "127.0.0.1"
        val proxyPort = Lantern.getProxyPort()
        val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyHost, proxyPort))

        val client = OkHttpClient.Builder()
            .proxy(proxy)
            .build()

        val request = Request.Builder()
            .url("https://jsonplaceholder.typicode.com/posts/1") // Test API
            .build()

        // Launch a coroutine to handle the request
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response: Response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                // Update the UI on the main thread
                withContext(Dispatchers.Main) {
                    appendLog("Request sent to ${request.url}\n")
                    appendLog("Response: $responseBody\n\n")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    appendLog("Error: ${e.message}\n\n")
                }
            }
        }
    }
}
