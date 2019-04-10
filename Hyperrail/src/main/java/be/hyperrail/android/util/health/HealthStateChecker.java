/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.util.health;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import be.opentransport.BuildConfig;

/**
 * Created in be.hyperrail.android.util on 13/03/2018.
 */

public class HealthStateChecker {

    private final HealthStateCheckerListener connectionReceiverListener;
    private final RequestQueue requestQueue;
    private static final String USER_AGENT = "HyperRail for Android - " + BuildConfig.VERSION_NAME;

    public HealthStateChecker(Context context, HealthStateCheckerListener listener) {
        connectionReceiverListener = listener;
        this.requestQueue = Volley.newRequestQueue(context);
        checkHealth();
    }

    private void checkHealth() {
        Response.Listener<JSONObject> successListener = response -> {
            connectionReceiverListener.onSystemHealthChanged(new HealthState(response));
        };


        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, "https://hyperrail.be/status.json", null, successListener, null) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-agent", USER_AGENT);
                return headers;
            }
        };
        requestQueue.add(jsObjRequest);
    }


}

