/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.be.experimental.linkedconnections;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;
import com.bluelinelabs.logansquare.LoganSquare;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.AddTrace;
import com.google.firebase.perf.metrics.Trace;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.format.ISODateTimeFormat;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import be.hyperrail.opentransportdata.be.experimental.BuildConfig;
import be.hyperrail.opentransportdata.common.contracts.MeteredDataSource;
import be.hyperrail.opentransportdata.common.contracts.TransportDataErrorResponseListener;
import be.hyperrail.opentransportdata.common.contracts.TransportDataSuccessResponseListener;

/**
 * Created in be.hyperrail.android.irail.implementation.linkedconnections on 15/03/2018.
 */

public class LinkedConnectionsProvider {

    private static final String GTFS_REGULAR = "gtfs:Regular";
    private static final String GTFS_DROP_OFF_TYPE = "gtfs:dropOffType";
    private static final String GTFS_PICKUP_TYPE = "gtfs:pickupType";

    private static final String BASE_URL = "https://graph.irail.be/sncb/connections?departureTime=";
    private final LinkedConnectionsOfflineCache mLinkedConnectionsOfflineCache;
    private final RequestQueue requestQueue;
    private final RetryPolicy requestPolicy;
    private final ConnectivityManager mConnectivityManager;

    private boolean mCacheEnabled = true;

    private static final String UA = "OpenTransport-be-experimental for Android - " + BuildConfig.VERSION_NAME;

    private boolean isInternetAvailable() {
        NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    LinkedConnectionsProvider(Context context) {
        this.mLinkedConnectionsOfflineCache = new LinkedConnectionsOfflineCache(context);

        BasicNetwork network;
        network = new BasicNetwork(new HurlStack());
        File cacheDir = new File(context.getCacheDir(), "volley");
        this.requestQueue = new RequestQueue(new DiskBasedCache(cacheDir, 48 * 1024 * 1024), network);
        requestQueue.start();

        this.requestPolicy = new DefaultRetryPolicy(
                1000,
                2,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        );
        mConnectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    void getLinkedConnectionsByDate(DateTime startTime, final TransportDataSuccessResponseListener<LinkedConnections> successListener, final TransportDataErrorResponseListener errorListener, Object tag) {
        startTime = startTime.withMillisOfSecond(0);
        startTime = startTime.withSecondOfMinute(0);
        String url = getLinkedConnectionsUrl(startTime);

        getLinkedConnectionsByUrl(url, successListener, errorListener, tag);
    }

    @NonNull
    private String getLinkedConnectionsUrl(DateTime timestamp) {
        return BASE_URL +
                timestamp.withZone(DateTimeZone.UTC).toString(ISODateTimeFormat.dateTime());
    }

    void queryLinkedConnections(DateTime startTime, final QueryResponseListener.LinkedConnectionsQuery query, Object tag) {
        QueryResponseListener responseListener = new QueryResponseListener(this, query);
        getLinkedConnectionsByDate(startTime, responseListener, responseListener, tag);
    }

    private void queryLinkedConnections(String startUrl, final QueryResponseListener.LinkedConnectionsQuery query, Object tag) {
        QueryResponseListener responseListener = new QueryResponseListener(this, query);
        getLinkedConnectionsByUrl(startUrl, responseListener, responseListener, tag);
    }

    void getLinkedConnectionsByDateForTimeSpan(DateTime startTime, final DateTime endTime, final TransportDataSuccessResponseListener<LinkedConnections> successListener, final TransportDataErrorResponseListener errorListener, Object tag) {
        TimespanQueryResponseListener listener = new TimespanQueryResponseListener(endTime, TimespanQueryResponseListener.DIRECTION_FORWARD, successListener, errorListener, tag);
        queryLinkedConnections(startTime, listener, tag);
    }

    void getLinkedConnectionsByUrlForTimeSpanBackwards(String startUrl, final DateTime endTime, final TransportDataSuccessResponseListener<LinkedConnections> successListener, final TransportDataErrorResponseListener errorListener, Object tag) {
        TimespanQueryResponseListener listener = new TimespanQueryResponseListener(endTime, TimespanQueryResponseListener.DIRECTION_BACKWARD, successListener, errorListener, tag);
        queryLinkedConnections(startUrl, listener, tag);
    }

    public void getLinkedConnectionsByUrlSpan(String start, final String end, final TransportDataSuccessResponseListener<LinkedConnections> successListener, final TransportDataErrorResponseListener errorListener, Object tag) {
        UrlSpanQueryResponseListener listener = new UrlSpanQueryResponseListener(end, successListener, errorListener, tag);
        queryLinkedConnections(start, listener, tag);
    }


    void getLinkedConnectionsByUrl(final String url, final TransportDataSuccessResponseListener<LinkedConnections> successListener, final TransportDataErrorResponseListener errorListener, final Object tag) {
        // https://graph.irail.be/sncb/connections?departureTime={ISO8601}
        if (BuildConfig.DEBUG) {
            Log.i("LCProvider", "Loading " + url);
        }
        // TODO: prevent loading the same URL twice when two requests are made short after each other (locking based on URL)

        final Trace tracing = FirebasePerformance.getInstance().newTrace("LinkedConnectionsProvider.getByUrl");
        tracing.start();

        Response.Listener<String> volleySuccessListener = buildSuccessListener(url, successListener, errorListener, tag, tracing);

        Response.ErrorListener volleyErrorListener = buildErrorListener(url, successListener, errorListener, tag, tracing);

        StringRequest jsObjRequest = new StringRequest(Request.Method.GET, url,
                volleySuccessListener,
                volleyErrorListener) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-agent", UA);
                return headers;
            }
        };
        LinkedConnectionsOfflineCache.CachedLinkedConnections cache = null;
        if (mCacheEnabled) {
            cache = mLinkedConnectionsOfflineCache.load(url);
        }
        if (cache != null && mCacheEnabled && cache.createdAt.isAfter(DateTime.now().minusSeconds(60))) {
            try {
                ((MeteredDataSource.MeteredRequest) tag).setResponseType(MeteredDataSource.RESPONSE_CACHED);
                if (BuildConfig.DEBUG) {
                    Log.w("LCProvider", "Fulfilled without network");
                }
                volleySuccessListener.onResponse(cache.data);
                return;
            } catch (Exception e) {
                e.printStackTrace();
                tracing.stop();
            }
        } else {
            if (cache == null) {
                if (BuildConfig.DEBUG) {
                    Log.w("LCProvider", "Not in cache");
                }
            } else {
                if (BuildConfig.DEBUG) {
                    Log.w("LCProvider", "Cache is " + (new Duration(cache.createdAt, DateTime.now()).getStandardSeconds()) + "sec old, getting new");
                }
            }
        }

        if (isInternetAvailable()) {
            ((MeteredDataSource.MeteredRequest) tag).setResponseType(MeteredDataSource.RESPONSE_ONLINE);
            jsObjRequest.setShouldCache(mCacheEnabled);
            jsObjRequest.setRetryPolicy(requestPolicy);
            //Log.i(LOGTAG, "Cached? " + url + ": " + (requestQueue.getCache().get(url) == null ? "empty" : (requestQueue.getCache().get(url).isExpired() ? "expired" : "valid")));
            requestQueue.add(jsObjRequest);
        } else {
            ((MeteredDataSource.MeteredRequest) tag).setResponseType(MeteredDataSource.RESPONSE_OFFLINE);
            volleyErrorListener.onErrorResponse(new NoConnectionError());
        }
    }

    @NonNull
    private Response.ErrorListener buildErrorListener(String url, TransportDataSuccessResponseListener<LinkedConnections> successListener, TransportDataErrorResponseListener errorListener, Object tag, Trace tracing) {
        return error -> {
            if (BuildConfig.DEBUG) {
                Log.w("LCProvider", "Getting LC page " + url + " failed: " + error.getMessage());
            }
            LinkedConnectionsOfflineCache.CachedLinkedConnections cache = mLinkedConnectionsOfflineCache.load(url);
            if (cache == null) {
                if (BuildConfig.DEBUG) {
                    Log.w("LCProvider", "Getting LC page " + url + " failed: offline cache missed!");
                }
                tracing.stop();
                errorListener.onErrorResponse(error, tag);
            } else {
                try {
                    if (BuildConfig.DEBUG) {
                        Log.w("LCProvider", "Getting LC page " + url + " failed: offline cache hit!");
                    }
                    LinkedConnections result = getLinkedConnectionsFromJson(cache.data);
                    successListener.onSuccessResponse(result, tag);
                } catch (Exception e) {
                    e.printStackTrace();
                    tracing.stop();
                    errorListener.onErrorResponse(error, tag);
                }
            }
        };
    }

    @NonNull
    private Response.Listener<String> buildSuccessListener(String url, TransportDataSuccessResponseListener<LinkedConnections> successListener, TransportDataErrorResponseListener errorListener, Object tag, Trace tracing) {
        return response -> {
            if (BuildConfig.DEBUG) {
                Log.w("LCProvider", "Getting LC page successful: " + url);
            }
            try {
                LinkedConnections result = getLinkedConnectionsFromJson(response);
                mLinkedConnectionsOfflineCache.store(result, response.toString());
                tracing.stop();
                successListener.onSuccessResponse(result, tag);
            } catch (Exception e) {
                e.printStackTrace();
                tracing.stop();
                errorListener.onErrorResponse(e, tag);
            }


        };
    }

    @NonNull
    @AddTrace(name = "LinkedConnectionsProvider.fromJsonLS")
    private LinkedConnections getLinkedConnectionsFromJson(String response) throws
            IOException {
        return LoganSquare.parse(response, LinkedConnections.class);
    }

    void setCacheEnabled(boolean cacheEnabled) {
        mCacheEnabled = cacheEnabled;
    }
}
