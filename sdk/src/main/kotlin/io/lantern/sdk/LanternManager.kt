package io.lantern.sdk

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.*
import java.util.concurrent.atomic.AtomicReference
import lantern.LanternClient

/**
 * LanternManager provides an API to use an embedded Lantern. It wraps the LanternClient and
 * configures Lantern as a system proxy for HTTP traffic.
 */
object LanternManager {
    private const val TAG = "LanternManager"
    private val lanternClient = LanternClient()

    /**
     * Configures Lantern with the specified application name and configuration directory.
     * This must be called before starting Lantern.
     *
     * @param context The application context (used to resolve file paths if necessary).
     * @param appName A unique identifier for the application (used for assigning proxies and tracking usage).
     * @param customConfigDir The directory where Lantern configuration files will be stored.
     */
    fun setup(
        context: Context,
        appName: String,
        customConfigDir: String? = null,
    ) {
        val configDir = if (customConfigDir.isNullOrBlank()) {
            // Default to the app's internal files directory
            File(context.filesDir, "lantern_config")
        } else {
            File(customConfigDir)
        }

        if (!configDir.exists()) {
            configDir.mkdirs()
        }
        lanternClient.setup(appName, configDir.absolutePath)
    }

    fun isRunning(): Boolean {
        return lanternClient.isRunning()
    }

    /**
     * Starts Lantern and sets it as the system proxy. If Lantern is already running, just uses the
     * already running Lantern.
     *
     * @param context            context used for creating Lantern configuration directory
     * @param addr               the HTTP proxy address Lantern should be started at
     * @param proxyAll           if true, traffic to all domains will be proxied. If false, only domains on Lantern's whitelist, or domains detected as blocked, will be proxied.
     * @return the InetSocketAddress at which the Lantern HTTP proxy is listening for connections
     */
    @Synchronized
    @Throws(Exception::class)
    fun startLantern(
        addr: String,
        proxyAll: Boolean,
    ): InetSocketAddress {
        val result = lanternClient.start(addr, proxyAll)
        ProxyHelper.setProxy(result.addr)
        return addrFromString(result.addr)
    }

    /**
     * Stops Lantern's active proxying but keeps it running in the background for configuration updates.
     * Future calls to startLantern() will reuse the running instance.
     */
    @Synchronized
    fun stopLantern() {
        try {
            lanternClient.stop()
            ProxyHelper.setProxy("")
            println("Lantern stopped")
        } catch (e: Exception) {
            println("Failed to stop Lantern: ${e.message}")
        }
    }


    /**
     * Returns the port where Lantern's HTTP proxy is listening.
     *
     * @return The proxy port number.
     */
    fun getProxyPort(): Int {
        val result = lanternClient.httpProxyPort()
        return result.toInt()
    }

    /**
     * Converts a host:port string into an InetSocketAddress by first making a fake URL using that
     * address.
     *
     * @param addr
     * @return
     */
    @Throws(Exception::class)
    private fun addrFromString(addr: String): InetSocketAddress {
        val uri = URI("my://$addr")
        return InetSocketAddress("127.0.0.1", uri.port)
    }

    private fun deviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    private fun configDir(context: Context): String {
        return File(context.filesDir, ".Lantern").absolutePath
    }

    data class ProxyResponse(
        val mimeType: String,
        val encoding: String,
        val inputStream: InputStream
    )
}
