/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.irail.implementation;

import android.content.Context;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
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
import be.hyperrail.android.irail.contracts.IrailParser;
import be.hyperrail.android.irail.contracts.RouteTimeDefinition;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.irail.implementation.requests.IrailDisturbanceRequest;
import be.hyperrail.android.irail.implementation.requests.IrailLiveboardRequest;
import be.hyperrail.android.irail.implementation.requests.IrailPostOccupancyRequest;
import be.hyperrail.android.irail.implementation.requests.IrailRouteRequest;
import be.hyperrail.android.irail.implementation.requests.IrailRoutesRequest;
import be.hyperrail.android.irail.implementation.requests.IrailTrainRequest;

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

    public IrailApi(Context context) {
        this.context = context;
        this.parser = new IrailApiParser(IrailFactory.getStationsProviderInstance());
        this.requestQueue = Volley.newRequestQueue(context);
        this.requestPolicy = new DefaultRetryPolicy(3000,
                4,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
    }

    private final Context context;
    private final IrailParser parser;

    private final int TAG_IRAIL_API_GET = 0;


    @Override
    public void getRoute(@NonNull IrailRouteRequest... requests) {
        for (final IrailRouteRequest request : requests
                ) {
            IrailRoutesRequest routesRequest = new IrailRoutesRequest(request.getOrigin(), request.getDestination(), request.getTimeDefinition(), request.getSearchTime());

            // Create a new routerequest. A successful response will be iterated to find a matching route. An unsuccessful query will cause the original error handler to be called.
            routesRequest.setCallback(new IRailSuccessResponseListener<RouteResult>() {
                @Override
                public void onSuccessResponse(RouteResult data, Object tag) {
                    for (Route r : data.getRoutes()) {
                        if (r.getTransfers()[0].getDepartureSemanticId().equals(request.getDepartureSemanticId())) {
                            request.notifySuccessListeners(r);
                        }
                    }
                }
            }, new IRailErrorResponseListener() {
                @Override
                public void onErrorResponse(Exception e, Object tag) {
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

    public void getRoutes(final IrailRoutesRequest request) {

        // https://api.irail.be/connections/?to=Halle&from=Brussels-south&date={dmy}&time=2359&timeSel=arrive or depart&format=json

        DateTimeFormatter dateformat = DateTimeFormat.forPattern("ddMMyy");
        DateTimeFormatter timeformat = DateTimeFormat.forPattern("HHmm");

        String locale = PreferenceManager.getDefaultSharedPreferences(context).getString("pref_stations_language", "");
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

        if (request.getTimeDefinition() == RouteTimeDefinition.DEPART) {
            url += "&timeSel=depart";
        } else {
            url += "&timeSel=arrive";
        }

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        RouteResult routeResult;
                        try {
                            routeResult = parser.parseRouteResult(response, request.getOrigin(), request.getDestination(), request.getSearchTime(), request.getTimeDefinition());
                        } catch (JSONException e) {
                            FirebaseCrash.logcat(WARNING.intValue(), "Failed to parse routes", e.getMessage());
                            FirebaseCrash.report(e);
                            request.notifyErrorListeners(e);
                            return;
                        }
                        request.notifySuccessListeners(routeResult);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError e) {
                        FirebaseCrash.logcat(WARNING.intValue(), "Failed to get routes", e.getMessage());
                        request.notifyErrorListeners(e);
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-agent", UA);
                return headers;
            }
        };
        jsObjRequest.setRetryPolicy(requestPolicy);
        jsObjRequest.setTag(TAG_IRAIL_API_GET);
        requestQueue.add(jsObjRequest);
    }

    @Override
    public void getLiveboard(@NonNull IrailLiveboardRequest... requests) {
        for (IrailLiveboardRequest request : requests) {
            getLiveboard(request);
        }
    }

    public void getLiveboard(final IrailLiveboardRequest request) {
        // https://api.irail.be/liveboard/?station=Halle&fast=true

        // suppress errors, this formatting is for an API call
        DateTimeFormatter dateformat = DateTimeFormat.forPattern("ddMMyy");
        DateTimeFormatter timeformat = DateTimeFormat.forPattern("HHmm");

        final String url = "https://api.irail.be/liveboard/?format=json"
                + "&id=" + request.getStation().getId()
                + "&date=" + dateformat.print(request.getSearchTime())
                + "&time=" + timeformat.print(request.getSearchTime())
                + "&arrdep=" + ((request.getTimeDefinition() == RouteTimeDefinition.DEPART) ? "dep" : "arr");

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        LiveBoard result;
                        try {
                            result = parser.parseLiveboard(response, request.getSearchTime());
                        } catch (JSONException e) {
                            FirebaseCrash.logcat(WARNING.intValue(), "Failed to parse liveboard", e.getMessage());
                            FirebaseCrash.report(e);
                            request.notifyErrorListeners(e);
                            return;
                        }

                        request.notifySuccessListeners(result);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError e) {
                        Log.w(LOGTAG, "Tried loading liveboard from " + url + " failed with error " + e);
                        FirebaseCrash.logcat(WARNING.intValue(), "Failed to get liveboard", e.getMessage());
                        request.notifyErrorListeners(e);
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-agent", UA);
                return headers;
            }
        };

        jsObjRequest.setRetryPolicy(requestPolicy);
        jsObjRequest.setTag(TAG_IRAIL_API_GET);
        requestQueue.add(jsObjRequest);
    }

    public void getLiveboardBefore(@NonNull IrailLiveboardRequest... requests) {
        for (IrailLiveboardRequest request :
                requests) {
            getLiveboardBefore(request);
        }
    }

    public void getLiveboardBefore(final IrailLiveboardRequest request) {
        final IrailLiveboardRequest actualRequest = new IrailLiveboardRequest(request.getStation(), request.getTimeDefinition(), request.getSearchTime().minusHours(1));
        actualRequest.setCallback(new IRailSuccessResponseListener<LiveBoard>() {
            @Override
            public void onSuccessResponse(LiveBoard data, Object tag) {
                List<TrainStop> stops = new ArrayList<>();
                for (TrainStop s : data.getStops()) {
                    if (s.getDepartureTime().isBefore(actualRequest.getSearchTime())) {
                        stops.add(s);
                    }
                }
                request.notifySuccessListeners(new LiveBoard(data, stops.toArray(new TrainStop[]{}), data.getSearchTime()));
            }
        }, new IRailErrorResponseListener() {
            @Override
            public void onErrorResponse(Exception e, Object tag) {
                request.notifyErrorListeners(e);
            }
        }, actualRequest.getTag());
        getLiveboard(request);
    }

    @Override
    public void getTrain(@NonNull final IrailTrainRequest... requests) {
        for (IrailTrainRequest request :
                requests) {
            getTrain(request);
        }
    }

    public void getTrain(final IrailTrainRequest request) {
        DateTimeFormatter dateTimeformat = DateTimeFormat.forPattern("ddMMyy");

        String url = "https://api.irail.be/vehicle/?format=json"
                + "&id=" + request.getTrainId() + "&date=" + dateTimeformat.print(request.getSearchTime());

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Train result;
                        try {
                            result = parser.parseTrain(response, new DateTime());
                        } catch (JSONException e) {
                            FirebaseCrash.logcat(WARNING.intValue(), "Failed to parse train", e.getMessage());
                            FirebaseCrash.report(e);
                            request.notifyErrorListeners(e);
                            return;
                        }
                        request.notifySuccessListeners(result);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError e) {
                        FirebaseCrash.logcat(WARNING.intValue(), "Failed to get train", e.getMessage());
                        request.notifyErrorListeners(e);
                    }
                })

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
        requestQueue.add(jsObjRequest);
    }

    @Override
    public void getDisturbances(@NonNull final IrailDisturbanceRequest... requests) {
        for (IrailDisturbanceRequest request :
                requests) {
            getDisturbances(request);
        }
    }

    public void getDisturbances(final IrailDisturbanceRequest request) {

        String locale = PreferenceManager.getDefaultSharedPreferences(context).getString("pref_stations_language", "");
        if (locale.isEmpty()) {
            // Only get locale when needed
            locale = Locale.getDefault().getISO3Language();
        }

        String url = "https://api.irail.be/disturbances/?format=json&lang=" + locale.substring(0, 2);

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
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
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError e) {
                        FirebaseCrash.logcat(WARNING.intValue(), "Failed to get disturbances", e.getMessage());
                        request.notifyErrorListeners(e);
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-agent", UA);
                return headers;
            }
        };

        jsObjRequest.setRetryPolicy(requestPolicy);
        jsObjRequest.setTag(TAG_IRAIL_API_GET);
        requestQueue.add(jsObjRequest);
    }

    @Override
    public void postOccupancy(@NonNull IrailPostOccupancyRequest... requests) {
        for (IrailPostOccupancyRequest request :
                requests) {
            postOccupancy(request);
        }
    }

    public void postOccupancy(IrailPostOccupancyRequest request) {

        final String url = "https://api.irail.be/feedback/occupancy.php";

        try {
            final JSONObject payload = new JSONObject();

            payload.put("connection", request.getDepartureSemanticId());
            payload.put("from", request.getStationSemanticId());
            payload.put("date", DateTimeFormat.forPattern("YYYYMMdd").print(request.getDate()));
            payload.put("vehicle", request.getVehicleSemanticId());
            payload.put("occupancy", "http://api.irail.be/terms/" + request.getOccupancy().name().toLowerCase());

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
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
            writer.write(json);
            writer.close();
            outputStream.close();

            //Read
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));

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
