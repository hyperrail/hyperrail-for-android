/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.util.health;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import be.hyperrail.android.BuildConfig;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


/**
 * Created in be.hyperrail.android.util on 13/03/2018.
 */

public class HealthStateChecker {

    private final HealthStateCheckerListener connectionReceiverListener;
    private static final String USER_AGENT = "HyperRail for Android - " + BuildConfig.VERSION_NAME;
    private final OkHttpClient client;

    public HealthStateChecker(Context context, HealthStateCheckerListener listener) {
        connectionReceiverListener = listener;
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.retryOnConnectionFailure(true)
                .connectTimeout(3, TimeUnit.SECONDS)
                .callTimeout(20, TimeUnit.SECONDS)
                .followRedirects(true);
        client = builder.build();
        checkHealth();
    }

    private void checkHealth() {
        Request httpRequest = new Request.Builder()
                .url("https://hyperrail.be/status.json")
                .headers(Headers.of("User-agent", USER_AGENT))
                .build();


        Call call = client.newCall(httpRequest);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    String body = response.body().string();
                    JSONObject json = new JSONObject(body);
                    connectionReceiverListener.onSystemHealthChanged(new HealthState(json));
                } catch (JSONException e) {
                    Log.e("HealthStateChecker", "onResponse: " + e.getMessage(), e);
                }
            }
        });
    }


}

