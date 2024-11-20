package org.getlantern.lantern.sdk.example

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.net.InetSocketAddress
import java.net.Proxy
import io.lantern.sdk.Lantern

class MainActivity : AppCompatActivity() {

    private lateinit var logsView: TextView
    private lateinit var startProxyButton: Button
    private lateinit var stopProxyButton: Button
    private lateinit var testRequestButton: Button

    private var proxyRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Lantern.setup(this)

        setContentView(R.layout.activity_main)

        logsView = findViewById(R.id.logs)
        startProxyButton = findViewById(R.id.startProxyButton)
        stopProxyButton = findViewById(R.id.stopProxyButton)
        testRequestButton = findViewById(R.id.testRequestButton)

        startProxyButton.setOnClickListener { startProxy() }
        stopProxyButton.setOnClickListener { stopProxy() }
        testRequestButton.setOnClickListener { testHttpRequest() }
    }

    private fun startProxy() {
        // Start the HTTP proxy using the SDK
        val result = Lantern.startHTTPProxy(this, "127.0.0.1:8484")
        val proxyPort = result.port
        logsView.append("Proxy started on port $proxyPort\n")
        proxyRunning = true

        startProxyButton.visibility = View.GONE
        stopProxyButton.visibility = View.VISIBLE
        testRequestButton.visibility = View.VISIBLE
    }

    // Stop the HTTP proxy
    private fun stopProxy() {
        Lantern.stop()

        logsView.append("Proxy stopped\n")
        proxyRunning = false

        startProxyButton.visibility = View.VISIBLE
        stopProxyButton.visibility = View.GONE
        testRequestButton.visibility = View.GONE
    }

    private fun testHttpRequest() {
        if (!proxyRunning) {
            logsView.append("Proxy is not running. Start the proxy first.\n")
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
                    logsView.append("Request sent to ${request.url}\n")
                    logsView.append("Response: $responseBody\n\n")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    logsView.append("Error: ${e.message}\n\n")
                }
            }
        }
    }
}
