package io.lantern.sdk;

import android.content.Context;
import android.provider.Settings;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import lanternsdk.Lanternsdk;
import lanternsdk.StartResult;

/**
 * Provides an API to use an embedded Lantern. After starting Lantern, all URL connections opened
 * with standard methods like HttpURLConnection will be proxied by Lantern.
 */
public class Lantern {
    static SocketAddress lanternAddr = null;
    static AtomicReference<SocketAddress> proxyAddr = new AtomicReference<SocketAddress>();

    static {
        ProxySelector.setDefault(new ProxySelector() {
            @Override
            public List<Proxy> select(URI uri) {
                List<Proxy> result = new ArrayList<Proxy>();
                SocketAddress addr = proxyAddr.get();
                if (addr == null) {
                    result.add(Proxy.NO_PROXY);
                } else {
                    result.add(new Proxy(Proxy.Type.HTTP, addr));
                }
                return result;
            }

            @Override
            public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
            }
        });
    }

    /**
     * Starts Lantern and sets it as the system proxy. If Lantern is already running, just uses the
     * already running Lantern.
     *
     * @param context            context used for creating Lantern configuration directory
     * @param appName            unique identifier for the current application (used for assigning proxies and tracking usage)
     * @param proxyAll           if true, traffic to all domains will be proxied. If false, only domains on Lantern's whitelist, or domains detected as blocked, will be proxied.
     * @param startTimeoutMillis how long to wait for Lantern to start before throwing an exception
     * @throws Exception if Lantern was unable to start within startTimeoutMillis
     */
    synchronized public static void start(Context context, String appName, boolean proxyAll, long startTimeoutMillis) throws Exception {
        if (lanternAddr == null) {
            // need to start Lantern
            String configDir = new File(
                    context.getFilesDir(),
                    ".lantern"
            ).getAbsolutePath();
            String deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            StartResult startResult =
                    Lanternsdk.start(appName, configDir, deviceId, proxyAll, startTimeoutMillis);
            lanternAddr = addrFromString(startResult.getHTTPAddr());
        }
        proxyAddr.set(lanternAddr);
    }

    /**
     * Stops circumventing with Lantern. Lantern will actually continue running in the background
     * in order to keep its configuration up-to-date. Subsequent calls to start() will reuse the
     * running Lantern and complete quickly.
     */
    synchronized public static void stop() {
        proxyAddr.set(null);
    }

    /**
     * Converts a host:port string into an InetSocketAddress by first making a fake URL using that
     * address.
     *
     * @param addr
     * @return
     */
    private static InetSocketAddress addrFromString(String addr) throws Exception {
        URI uri = new URI("my://" + addr);
        return new InetSocketAddress(uri.getHost(), uri.getPort());
    }
}
