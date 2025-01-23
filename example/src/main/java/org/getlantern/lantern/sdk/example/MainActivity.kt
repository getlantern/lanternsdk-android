package org.getlantern.lantern.sdk.example

import android.os.Bundle
import android.view.View
import android.webkit.WebSettings.PluginState
import android.webkit.WebView
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.lantern.sdk.LanternManager
import io.lantern.sdk.ProxyHelper

class MainActivity : AppCompatActivity() {
    private lateinit var logsView: TextView
    private lateinit var startProxyButton: Button
    private lateinit var stopProxyButton: Button
    private lateinit var testRequestButton: Button
    private lateinit var launchWebViewButton: Button
    private lateinit var scrollView: ScrollView
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LanternManager.setup(this, "Example")

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
        val proxyAddr = ":8080"
        val proxyAllTraffic = true
        val result = LanternManager.startLantern(proxyAddr, proxyAllTraffic)

        if (result == null) {
            appendLog("Failed to start the proxy: LanternManager returned null")
            return
        }

        val proxyPort = result.port
        appendLog("Proxy started on port $proxyPort")
        toggleButtons(true)
    }

    // Stop the HTTP proxy
    private fun stopProxy() {
        LanternManager.stopLantern()

        appendLog("Proxy stopped")
        toggleButtons(false)
    }

    private fun openWebView() {
        if (!LanternManager.isRunning()) {
            appendLog("Proxy is not running. Start the proxy first.")
            return
        }

        webView.apply {
            visibility = View.VISIBLE

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                allowFileAccess = true
                allowContentAccess = true
                pluginState = PluginState.ON
            }

            webViewClient = ProxyHelper.createWebViewClient()
            loadUrl("https://ifconfig.co")
        }
    }

    private fun appendLog(message: String) {
        logsView.append("$message\n")
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
    }

    private fun testHttpRequest() {
        if (!LanternManager.isRunning()) {
            appendLog("Lantern is not running. Start the proxy first.")
            return
        }
        val url = "https://jsonplaceholder.typicode.com/posts/1" // Test API
        ProxyHelper.testHttpRequest(
            url = url,
            onSuccess = { response ->
                appendLog("Request sent to $url")
                appendLog("Response: $response")
            },
            onError = { error ->
                appendLog("Error: $error")
            },
        )
    }

    private fun toggleButtons(isProxyRunning: Boolean) {
        startProxyButton.visibility = if (isProxyRunning) View.GONE else View.VISIBLE
        stopProxyButton.visibility = if (isProxyRunning) View.VISIBLE else View.GONE
        launchWebViewButton.visibility = if (isProxyRunning) View.VISIBLE else View.GONE
        testRequestButton.visibility = if (isProxyRunning) View.VISIBLE else View.GONE
    }
}
