package io.lantern.sdk

import android.content.Context
import android.provider.Settings
import lantern.LanternClient
import java.io.File
import java.net.*

/**
 * LanternManager provides an API to use an embedded Lantern. It wraps the LanternClient and
 * configures Lantern as a system proxy for HTTP traffic.
 */
object LanternManager {
    private const val TAG = "LanternManager"
    private val lanternClient = LanternClient()

    /**
     * Configures Lantern with the specified application name and configuration directory.
     * Defaults to an internal app directory if `customConfigDir` is not provided.
     * This must be called before starting Lantern.
     */
    fun setup(
        context: Context,
        appName: String,
        customConfigDir: String? = null,
    ) {
        val configDir = customConfigDir?.let { File(it) } ?: File(context.filesDir, "lantern_config")
        if (!configDir.exists()) configDir.mkdirs()
        lanternClient.setup(appName, configDir.absolutePath)
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
    fun startLantern(
        addr: String,
        proxyAll: Boolean,
    ): InetSocketAddress? {
        return try {
            val result = lanternClient.start(addr, proxyAll)
            ProxyHelper.setProxy(result.addr)
            return ProxyHelper.addrFromString(result.addr)
        } catch (e: Exception) {
            println("Failed to stop Lantern: ${e.message}")
            null
        }
    }

    /**
     * Stops Lantern's active proxying and clears the system proxy. It keeps running in the
     * background for configuration updates. Future calls to startLantern() will reuse the
     * running instance.
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
     * Checks if Lantern is running.
     */
    fun isRunning(): Boolean = lanternClient.isRunning()

    /**
     * Returns the Lantern HTTP proxy port.
     */
    fun getProxyPort(): Int = lanternClient.httpProxyPort().toInt()

    private fun deviceId(context: Context): String = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
}
