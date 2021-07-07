package io.lantern.sdk;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@RunWith(AndroidJUnit4.class)
public class LanternTest {
    @Test
    public void testStartStop() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        String ipWithoutLantern = fetchIP();
        Lantern.start(context, "lantern", true, 60000);
        String ipWithLanternStarted = fetchIP();
        Lantern.stop();
        String ipWithLanternStopped = fetchIP();
        Lantern.start(context, "lantern", true, 30000);
        String ipWithLanternRestarted = fetchIP();

        assertNotEquals(ipWithoutLantern, ipWithLanternStarted);
        assertEquals(ipWithoutLantern, ipWithLanternStopped);
        assertEquals(ipWithLanternStarted, ipWithLanternRestarted);
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