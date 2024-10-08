package io.lantern.sdk;

import android.content.Context;
import android.os.Build;
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
import lanternsdk.ProxyAddr;

/**
 * Provides an API to use an embedded Lantern. After starting Lantern, all URL connections opened
 * with standard methods like HttpURLConnection will be proxied by Lantern.
 */
public class Lantern {
    static InetSocketAddress lanternAddr = null;
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
     * @return the InetSocketAddress at which the Lantern HTTP proxy is listening for connections
     * @throws Exception if Lantern was unable to start within startTimeoutMillis
     */
    synchronized public static InetSocketAddress start(
            Context context,
            String appName,
            boolean proxyAll,
            long startTimeoutMillis) throws Exception {
        if (lanternAddr == null) {
            // need to start Lantern
            ProxyAddr proxyAddr = Lanternsdk.start(
                    appName,
                    configDir(context),
                    deviceId(context),
                    proxyAll,
                    startTimeoutMillis);
            lanternAddr = addrFromString(proxyAddr.getHTTPAddr());
        }
        proxyAddr.set(lanternAddr);
        return lanternAddr;
    }

    /**
     * Reports an issue to the Lantern support team.
     *
     * @param context
     * @param appName     unique identifier for the current application (used for assigning proxies and tracking usage)
     * @param userEmail   the user's email address (okay to leave this blank)
     * @param description a text description of the issue
     * @param maxLogMB    the maximum size of logs to attach to the issue report in MB (10 is a reasonable value)
     */
    public static void reportIssue(
            Context context,
            String appName,
            String userEmail,
            String description,
            int maxLogMB) throws Exception {
        Lanternsdk.reportIssueAndroid(
                appName,
                configDir(context),
                deviceId(context),
                Build.DEVICE,
                Build.MODEL,
                "" + Build.VERSION.SDK_INT + " (" + Build.VERSION.RELEASE + ")",
                userEmail,
                description,
                maxLogMB
        );
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

    private static String deviceId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    private static String configDir(Context context) {
        return new File(
                context.getFilesDir(),
                ".lantern"
        ).getAbsolutePath();
    }
}
