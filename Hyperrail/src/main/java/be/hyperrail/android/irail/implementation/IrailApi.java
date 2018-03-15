/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.irail.implementation;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.crash.FirebaseCrash;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import be.hyperrail.android.BuildConfig;
import be.hyperrail.android.irail.contracts.IRailErrorResponseListener;
import be.hyperrail.android.irail.contracts.IRailSuccessResponseListener;
import be.hyperrail.android.irail.contracts.IrailDataProvider;
import be.hyperrail.android.irail.contracts.RouteTimeDefinition;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.irail.implementation.irailapi.LiveboardAppendHelper;
import be.hyperrail.android.irail.implementation.irailapi.RouteAppendHelper;
import be.hyperrail.android.irail.implementation.requests.ExtendLiveboardRequest;
import be.hyperrail.android.irail.implementation.requests.ExtendRoutesRequest;
import be.hyperrail.android.irail.implementation.requests.IrailDisturbanceRequest;
import be.hyperrail.android.irail.implementation.requests.IrailLiveboardRequest;
import be.hyperrail.android.irail.implementation.requests.IrailPostOccupancyRequest;
import be.hyperrail.android.irail.implementation.requests.IrailRouteRequest;
import be.hyperrail.android.irail.implementation.requests.IrailRoutesRequest;
import be.hyperrail.android.irail.implementation.requests.IrailVehicleRequest;
import be.hyperrail.android.irail.implementation.requests.VehicleStopRequest;

import static java.util.logging.Level.WARNING;

/**
 * Synchronous API for api.irail.be
 *
 * @inheritDoc
 */
public class IrailApi implements IrailDataProvider {

    private static final String LOGTAG = "iRailApi";
    private final RequestQueue requestQueue;
    private static final String UA = "HyperRail for Android - " + BuildConfig.VERSION_NAME;
    private final RetryPolicy requestPolicy;

    private final Context context;
    private final IrailApiParser parser;
    private final ConnectivityManager mConnectivityManager;
    private final int TAG_IRAIL_API_GET = 0;

    public IrailApi(Context context) {
        this.context = context;
        this.parser = new IrailApiParser(IrailFactory.getStationsProviderInstance());
        this.requestQueue = Volley.newRequestQueue(context);
        this.requestPolicy = new DefaultRetryPolicy(
                1500,
                4,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        );
        mConnectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);


    }

    private boolean isInternetAvailable() {
        NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }


    @Override
    public void getRoute(@NonNull IrailRouteRequest... requests) {
        for (final IrailRouteRequest request : requests
                ) {
            IrailRoutesRequest routesRequest = new IrailRoutesRequest(
                    request.getOrigin(), request.getDestination(), request.getTimeDefinition(),
                    request.getSearchTime()
            );

            // Create a new routerequest. A successful response will be iterated to find a matching route. An unsuccessful query will cause the original error handler to be called.
            routesRequest.setCallback(new IRailSuccessResponseListener<RouteResult>() {
                @Override
                public void onSuccessResponse(@NonNull RouteResult data, Object tag) {
                    for (Route r : data.getRoutes()) {
                        if (r.getTransfers()[0].getDepartureSemanticId() != null && r.getTransfers()[0].getDepartureSemanticId().equals(
                                request.getDepartureSemanticId())) {
                            request.notifySuccessListeners(r);
                        }
                    }
                }
            }, new IRailErrorResponseListener() {
                @Override
                public void onErrorResponse(@NonNull Exception e, Object tag) {
                    request.notifyErrorListeners(e);
                }
            }, request.getTag());

            getRoutes(routesRequest);
        }
    }

    @Override
    public void getRoutes(@NonNull IrailRoutesRequest... requests) {
        for (IrailRoutesRequest request :
                requests) {
            getRoutes(request);
        }
    }

    @Override
    public void extendRoutes(@NonNull ExtendRoutesRequest... requests) {
        for (ExtendRoutesRequest request :
                requests) {
            RouteAppendHelper helper = new RouteAppendHelper();
            helper.extendRoutesRequest(request);
        }
    }

    public void getRoutes(final IrailRoutesRequest request) {

        // https://api.irail.be/connections/?to=Halle&from=Brussels-south&date={dmy}&time=2359&timeSel=arrive or depart&format=json

        DateTimeFormatter dateformat = DateTimeFormat.forPattern("ddMMyy");
        DateTimeFormatter timeformat = DateTimeFormat.forPattern("HHmm");

        String locale = PreferenceManager.getDefaultSharedPreferences(context).getString(
                "pref_stations_language", "");
        if (locale.isEmpty()) {
            // Only get locale when needed
            locale = Locale.getDefault().getISO3Language();
        }

        String url = "https://api.irail.be/connections/?format=json"
                + "&to=" + request.getDestination().getId()
                + "&from=" + request.getOrigin().getId()
                + "&date=" + dateformat.print(request.getSearchTime())
                + "&time=" + timeformat.print(request.getSearchTime())
                + "&lang=" + locale.substring(0, 2);

        if (request.getTimeDefinition() == RouteTimeDefinition.DEPART_AT) {
            url += "&timeSel=depart";
        } else {
            url += "&timeSel=arrive";
        }

        Response.Listener<JSONObject> successListener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                RouteResult routeResult;
                try {
                    routeResult = parser.parseRouteResult(
                            response, request.getOrigin(), request.getDestination(),
                            request.getSearchTime(), request.getTimeDefinition()
                    );
                } catch (JSONException e) {
                    FirebaseCrash.logcat(
                            WARNING.intValue(), "Failed to parse routes", e.getMessage());
                    FirebaseCrash.report(e);
                    request.notifyErrorListeners(e);
                    return;
                }
                request.notifySuccessListeners(routeResult);
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError e) {
                FirebaseCrash.logcat(
                        WARNING.intValue(), "Failed to get routes", e.getMessage());
                request.notifyErrorListeners(e);
            }
        };

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, successListener, errorListener) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-agent", UA);
                return headers;
            }
        };
        jsObjRequest.setRetryPolicy(requestPolicy);
        jsObjRequest.setTag(TAG_IRAIL_API_GET);

        tryOnlineOrServerCache(jsObjRequest, successListener, errorListener);
    }

    @Override
    public void getLiveboard(@NonNull IrailLiveboardRequest... requests) {
        for (IrailLiveboardRequest request : requests) {
            if (request.getTimeDefinition() == RouteTimeDefinition.DEPART_AT) {
                getLiveboardAfter(request);
            } else {
                getLiveboardBefore(request);
            }
        }
    }

    @Override
    public void extendLiveboard(@NonNull ExtendLiveboardRequest... requests) {
        for (ExtendLiveboardRequest request :
                requests) {
            LiveboardAppendHelper helper = new LiveboardAppendHelper();
            helper.extendLiveboard(request);
        }
    }

    private void getLiveboardBefore(final IrailLiveboardRequest request) {
        final IrailLiveboardRequest actualRequest = request.withSearchTime(
                request.getSearchTime().minusHours(1));

        actualRequest.setCallback(new IRailSuccessResponseListener<Liveboard>() {
            @Override
            public void onSuccessResponse(@NonNull Liveboard data, Object tag) {
                List<VehicleStop> stops = new ArrayList<>();
                for (VehicleStop s : data.getStops()) {
                    if (s.getDepartureTime().isBefore(actualRequest.getSearchTime())) {
                        stops.add(s);
                    }
                }
                request.notifySuccessListeners(
                        new Liveboard(data, stops.toArray(new VehicleStop[]{}),
                                      data.getSearchTime(), data.getLiveboardType(), RouteTimeDefinition.ARRIVE_AT
                        ));
            }
        }, new IRailErrorResponseListener() {
            @Override
            public void onErrorResponse(@NonNull Exception e, Object tag) {
                request.notifyErrorListeners(e);
            }
        }, actualRequest.getTag());
        getLiveboardAfter(request);
    }

    private void getLiveboardAfter(final IrailLiveboardRequest request) {
        // https://api.irail.be/liveboard/?station=Halle&fast=true

        // suppress errors, this formatting is for an API call
        DateTimeFormatter dateformat = DateTimeFormat.forPattern("ddMMyy");
        DateTimeFormatter timeformat = DateTimeFormat.forPattern("HHmm");

        final String url = "https://api.irail.be/liveboard/?format=json"
                + "&id=" + request.getStation().getId()
                + "&date=" + dateformat.print(request.getSearchTime())
                + "&time=" + timeformat.print(request.getSearchTime())
                + "&arrdep=" + ((request.getType() == Liveboard.LiveboardType.DEPARTURES) ? "dep" : "arr");

        Response.Listener<JSONObject> successListener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Liveboard result;
                try {
                    result = parser.parseLiveboard(response, request.getSearchTime(), request.getType(), request.getTimeDefinition());
                } catch (JSONException e) {
                    FirebaseCrash.logcat(WARNING.intValue(), "Failed to parse liveboard", e.getMessage());
                    FirebaseCrash.report(e);
                    request.notifyErrorListeners(e);
                    return;
                }

                request.notifySuccessListeners(result);
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError e) {
                Log.w(LOGTAG, "Tried loading liveboard from " + url + " failed with error " + e);
                FirebaseCrash.logcat(
                        WARNING.intValue(), "Failed to get liveboard", e.getMessage());
                request.notifyErrorListeners(e);
            }
        };

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, successListener, errorListener) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-agent", UA);
                return headers;
            }
        };

        jsObjRequest.setRetryPolicy(requestPolicy);
        jsObjRequest.setTag(TAG_IRAIL_API_GET);

        tryOnlineOrServerCache(jsObjRequest, successListener, errorListener);
    }

    @Override
    public void getVehicle(@NonNull IrailVehicleRequest... requests) {
        for (IrailVehicleRequest request :
                requests) {
            getVehicle(request);
        }
    }

    public void getVehicle(final IrailVehicleRequest request) {
        DateTimeFormatter dateTimeformat = DateTimeFormat.forPattern("ddMMyy");

        String url = "https://api.irail.be/vehicle/?format=json"
                + "&id=" + request.getVehicleId() + "&date=" + dateTimeformat.print(
                request.getSearchTime());

        Response.Listener<JSONObject> successListener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Vehicle result;
                try {
                    result = parser.parseTrain(response, request.getSearchTime());
                } catch (JSONException e) {
                    FirebaseCrash.logcat(
                            WARNING.intValue(), "Failed to parse vehicle", e.getMessage());
                    FirebaseCrash.report(e);
                    request.notifyErrorListeners(e);
                    return;
                }
                request.notifySuccessListeners(result);
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError e) {
                FirebaseCrash.logcat(
                        WARNING.intValue(), "Failed to get vehicle", e.getMessage());
                request.notifyErrorListeners(e);
            }
        };
        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, successListener, errorListener)

        {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-agent", UA);
                return headers;
            }
        };

        jsObjRequest.setRetryPolicy(requestPolicy);
        jsObjRequest.setTag(TAG_IRAIL_API_GET);

        tryOnlineOrServerCache(jsObjRequest, successListener, errorListener);
    }

    @Override
    public void getStop(@NonNull VehicleStopRequest... requests) {
        for (VehicleStopRequest request :
                requests) {
            getStop(request);
        }
    }

    private void getStop(@NonNull final VehicleStopRequest request) {
        DateTime time = request.getStop().getDepartureTime();
        if (time == null) {
            time = request.getStop().getArrivalTime();
        }
        IrailVehicleRequest vehicleRequest = new IrailVehicleRequest(request.getStop().getVehicle().getId(), time);
        vehicleRequest.setCallback(new IRailSuccessResponseListener<Vehicle>() {
            @Override
            public void onSuccessResponse(@NonNull Vehicle data, Object tag) {
                for (VehicleStop stop :
                        data.getStops()) {
                    if (stop.getDepartureSemanticId().equals(request.getStop().getDepartureSemanticId())) {
                        request.notifySuccessListeners(stop);
                        return;
                    }
                }
            }
        }, request.getOnErrorListener(), null);
        getVehicle(vehicleRequest);
    }

    @Override
    public void getDisturbances(@NonNull IrailDisturbanceRequest... requests) {
        for (IrailDisturbanceRequest request :
                requests) {
            getDisturbances(request);
        }
    }

    public void getDisturbances(final IrailDisturbanceRequest request) {

        String locale = PreferenceManager.getDefaultSharedPreferences(context).getString(
                "pref_stations_language", "");
        if (locale.isEmpty()) {
            // Only get locale when needed
            locale = Locale.getDefault().getISO3Language();
        }

        String url = "https://api.irail.be/disturbances/?format=json&lang=" + locale.substring(
                0, 2);


        Response.Listener<JSONObject> successListener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Disturbance[] result;
                try {
                    result = parser.parseDisturbances(response);
                } catch (JSONException e) {
                    FirebaseCrash.logcat(WARNING.intValue(), "Failed to parse disturbances", e.getMessage());
                    FirebaseCrash.report(e);
                    request.notifyErrorListeners(e);
                    return;
                }
                request.notifySuccessListeners(result);
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError e) {
                FirebaseCrash.logcat(WARNING.intValue(), "Failed to get disturbances", e.getMessage());
                request.notifyErrorListeners(e);
            }
        };

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, successListener, errorListener) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-agent", UA);
                return headers;
            }
        };

        jsObjRequest.setRetryPolicy(requestPolicy);
        jsObjRequest.setTag(TAG_IRAIL_API_GET);
        tryOnlineOrServerCache(jsObjRequest, successListener, errorListener);
    }

    /**
     * If internet is available, make a request. Otherwise, check the cache
     *
     * @param jsObjRequest    The request which should be made to the server
     * @param successListener The listener for successful responses, which will be used by the cache
     * @param errorListener   The listener for unsuccessful responses
     */
    private void tryOnlineOrServerCache(JsonObjectRequest jsObjRequest, Response.Listener<JSONObject> successListener, Response.ErrorListener errorListener) {
        if (isInternetAvailable()) {
            requestQueue.add(jsObjRequest);
        } else {
            if (requestQueue.getCache().get(jsObjRequest.getCacheKey()) != null) {
                try {
                    JSONObject cache;
                    cache = new JSONObject(new String(requestQueue.getCache().get(jsObjRequest.getCacheKey()).data));
                    successListener.onResponse(cache);
                } catch (JSONException e) {
                    FirebaseCrash.logcat(
                            WARNING.intValue(), "Failed to get result from cache", e.getMessage());
                    errorListener.onErrorResponse(new NoConnectionError());
                }

            } else {
                errorListener.onErrorResponse(new NoConnectionError());
            }
        }
    }

    @Override
    public void postOccupancy(@NonNull IrailPostOccupancyRequest... requests) {
        for (IrailPostOccupancyRequest request :
                requests) {
            postOccupancy(request);
        }
    }

    public void postOccupancy(IrailPostOccupancyRequest request) {

        String url = "https://api.irail.be/feedback/occupancy.php";

        try {
            JSONObject payload = new JSONObject();

            payload.put("connection", request.getDepartureSemanticId());
            payload.put("from", request.getStationSemanticId());
            payload.put("date", DateTimeFormat.forPattern("YYYYMMdd").print(request.getDate()));
            payload.put("vehicle", request.getVehicleSemanticId());
            payload.put(
                    "occupancy",
                    "http://api.irail.be/terms/" + request.getOccupancy().name().toLowerCase()
            );

            Log.d(LOGTAG, "Posting feedback: " + url + " : " + payload);

            PostOccupancyTask t = new PostOccupancyTask(url, request);
            t.execute(payload.toString());
        } catch (Exception e) {
            request.notifyErrorListeners(e);
        }
    }

    @Override
    public void abortAllQueries() {
        this.requestQueue.cancelAll(TAG_IRAIL_API_GET);
    }

    /**
     * Make a synchronous POST request with a JSON body.
     *
     * @param uri  The URI to make the request to
     * @param json The request body
     * @return The return text from the server
     */

    private static String postJsonRequest(String uri, String json) {
        HttpURLConnection urlConnection;
        String result;
        try {
            //Connect
            urlConnection = (HttpURLConnection) ((new URL(uri).openConnection()));
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setRequestMethod("POST");
            urlConnection.connect();

            //Write
            OutputStream outputStream = urlConnection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(outputStream, "UTF-8"));
            writer.write(json);
            writer.close();
            outputStream.close();

            //Read
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));

            String line;
            StringBuilder sb = new StringBuilder();

            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }

            bufferedReader.close();
            result = sb.toString();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return result;
    }

    private static class PostOccupancyTask extends AsyncTask<String, Void, String> {

        private final String url;
        private final IrailPostOccupancyRequest request;

        public PostOccupancyTask(@NonNull String url, IrailPostOccupancyRequest request) {
            this.url = url;
            this.request = request;
        }

        @Override
        protected String doInBackground(String... payload) {
            return postJsonRequest(this.url, payload[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (result != null) {
                request.notifySuccessListeners(true);
            } else {
                // TODO: better exception handling
                request.notifyErrorListeners(new Exception("Failed to submit occupancy data"));
            }
        }
    }

}
