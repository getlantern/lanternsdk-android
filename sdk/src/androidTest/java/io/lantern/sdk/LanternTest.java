package io.lantern.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.InetSocketAddress;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@RunWith(AndroidJUnit4.class)
public class LanternTest {
    static final String appName = "pangea";

    @Test
    public void testStartStop() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        String ipWithoutLantern = fetchIP();
        InetSocketAddress firstAddress = Lantern.start(context, appName, true, 60000);
        String ipWithLanternStarted = fetchIP();
        Lantern.stop();
        String ipWithLanternStopped = fetchIP();
        InetSocketAddress secondAddress = Lantern.start(context, appName, true, 30000);
        String ipWithLanternRestarted = fetchIP();

        assertNotEquals(ipWithoutLantern, ipWithLanternStarted);
        assertEquals(ipWithoutLantern, ipWithLanternStopped);
        assertEquals(ipWithLanternStarted, ipWithLanternRestarted);

        assertNotNull(firstAddress);
        assertEquals("127.0.0.1", firstAddress.getHostString());
        assertEquals(firstAddress, secondAddress);
    }

    @Test
    public void testReportIssue() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        Lantern.reportIssue(
                context,
                appName,
                null,
                "lanternsdk unit test issue",
                10
        );
    }

    private static String fetchIP() throws Exception {
        Request request = new Request.Builder()
                .url("https://api.ipify.org")
                .build();
        OkHttpClient client = new OkHttpClient();
        Call call = client.newCall(request);
        Response response = call.execute();
        return response.body().string();
    }
}