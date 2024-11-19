package io.lantern.sdk

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.io.File
import java.io.IOException
import java.net.*
import java.util.concurrent.atomic.AtomicReference
import internalsdk.Internalsdk

/**
 * Provides an API to use an embedded Lantern. After starting Lantern, all URL connections opened
 * with standard methods like HttpURLConnection will be proxied by Lantern.
 */
object Lantern {
    private var LanternAddr: InetSocketAddress? = null
    private val proxyAddr = AtomicReference<SocketAddress?>()

    fun init() {
        ProxySelector.setDefault(object : ProxySelector() {
            override fun select(uri: URI?): List<Proxy> {
                val result = mutableListOf<Proxy>()
                val addr = proxyAddr.get()
                if (addr == null) {
                    result.add(Proxy.NO_PROXY)
                } else {
                    result.add(Proxy(Proxy.Type.HTTP, addr))
                }
                return result
            }

            override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
                // Do nothing
            }
        })
    }

    /**
     * Starts Lantern and sets it as the system proxy. If Lantern is already running, just uses the
     * already running Lantern.
     *
     * @param context            context used for creating Lantern configuration directory
     * @param appName            unique identifier for the current application (used for assigning proxies and tracking usage)
     * @param proxyAll           if true, traffic to all domains will be proxied. If false, only domains on Lantern's whitelist, or domains detected as blocked, will be proxied.
     * @param startTimeoutMillis how long to wait for Lantern to start before throwing an exception
     * @return the InetSocketAddress at which the Lantern HTTP proxy is listening for connections
     * @throws Exception if Lantern was unable to start within startTimeoutMillis
     */
    @Synchronized
    @Throws(Exception::class)
    fun start(
        context: Context,
        appName: String,
        proxyAll: Boolean,
        startTimeoutMillis: Long
    ): InetSocketAddress {
        if (LanternAddr == null) {
            // Need to start Lantern
            val proxyAddr = Internalsdk.start(
                configDir(context),
                deviceId(context),
                null,
                null
            )
            LanternAddr = addrFromString(proxyAddr.httpAddr)
        }
        proxyAddr.set(LanternAddr)
        return LanternAddr!!
    }

    /**
     * Stops circumventing with Lantern. Lantern will actually continue running in the background
     * in order to keep its configuration up-to-date. Subsequent calls to start() will reuse the
     * running Lantern and complete quickly.
     */
    @Synchronized
    fun stop() {
        proxyAddr.set(null)
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
        return InetSocketAddress(uri.host, uri.port)
    }

    private fun deviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    private fun configDir(context: Context): String {
        return File(context.filesDir, ".Lantern").absolutePath
    }
}
