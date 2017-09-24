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
import com.google.firebase.perf.metrics.AddTrace;

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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import be.hyperrail.android.BuildConfig;
import be.hyperrail.android.irail.contracts.IRailErrorResponseListener;
import be.hyperrail.android.irail.contracts.IRailSuccessResponseListener;
import be.hyperrail.android.irail.contracts.IrailDataProvider;
import be.hyperrail.android.irail.contracts.IrailParser;
import be.hyperrail.android.irail.contracts.IrailStationProvider;
import be.hyperrail.android.irail.contracts.OccupancyLevel;
import be.hyperrail.android.irail.contracts.RouteTimeDefinition;
import be.hyperrail.android.irail.db.Station;

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

    public IrailApi(Context context, IrailParser parser, IrailStationProvider stationProvider) {
        this.context = context;
        this.parser = parser;
        this.stationProvider = stationProvider;
        this.requestQueue = Volley.newRequestQueue(context);
        this.requestPolicy = new DefaultRetryPolicy(10000,
                3,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
    }

    private final Context context;
    private final IrailParser parser;
    private final IrailStationProvider stationProvider;

    private final int TAG_IRAIL_API_GET = 0;

    @Override
    public void getRoute(String from, String to, DateTime timeFilter, RouteTimeDefinition timeFilterType,
                         IRailSuccessResponseListener<RouteResult> successListener, IRailErrorResponseListener<RouteResult> errorListener,
                         Object tag) {
        getRoute(stationProvider.getStationByName(from), stationProvider.getStationByName(to), timeFilter, timeFilterType, successListener, errorListener, tag);
    }

    @Override
    @AddTrace(name = "iRailGetroute")
    public void getRoute(final Station from, final Station to, DateTime timeFilter, final RouteTimeDefinition timeFilterType,
                         final IRailSuccessResponseListener<RouteResult> successListener, final IRailErrorResponseListener<RouteResult> errorListener,
                         final Object tag) {

        if (timeFilter == null) {
            timeFilter = new DateTime();
        }

        final DateTime finalDateTime = timeFilter;
        // https://api.irail.be/connections/?to=Halle&from=Brussels-south&date={dmy}&time=2359&timeSel=arrive or depart&format=json

        DateTimeFormatter dateformat = DateTimeFormat.forPattern("ddMMyy");
        DateTimeFormatter timeformat = DateTimeFormat.forPattern("HHmm");

        if (from == null || to == null) {
            errorListener.onErrorResponse(new IllegalArgumentException("One or both stations are null"), tag);
            return;
        }

        String url = "https://api.irail.be/connections/?format=json"
                + "&to=" + to.getId()
                + "&from=" + from.getId()
                + "&date=" + dateformat.print(timeFilter)
                + "&time=" + timeformat.print(timeFilter);

        if (timeFilterType == RouteTimeDefinition.DEPART) {
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
                            routeResult = parser.parseRouteResult(response, from, to, finalDateTime, timeFilterType);
                        } catch (JSONException e) {
                            FirebaseCrash.logcat(WARNING.intValue(), "Failed to parse routes", e.getMessage());
                            FirebaseCrash.report(e);
                            errorListener.onErrorResponse(e, tag);
                            return;
                        }
                        if (successListener != null) {
                            successListener.onSuccessResponse(routeResult, tag);
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError e) {
                        FirebaseCrash.logcat(WARNING.intValue(), "Failed to get routes", e.getMessage());
                        errorListener.onErrorResponse(e, tag);
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

    public void getLiveboard(String name, final DateTime timeFilter, final RouteTimeDefinition timeFilterType,
                             final IRailSuccessResponseListener<LiveBoard> successListener, final IRailErrorResponseListener<LiveBoard> errorListener,
                             final Object tag) {
        getLiveboard(stationProvider.getStationByName(name), timeFilter, timeFilterType, successListener, errorListener, tag);
    }

    @Override
    @AddTrace(name = "iRailGetLiveboard")
    public void getLiveboard(Station station, DateTime timeFilter, final RouteTimeDefinition timeFilterType,
                             final IRailSuccessResponseListener<LiveBoard> successListener, final IRailErrorResponseListener<LiveBoard> errorListener,
                             final Object tag) {
        if (timeFilter == null) {
            timeFilter = new DateTime();
        }
        final DateTime finalDateTime = timeFilter;

        // https://api.irail.be/liveboard/?station=Halle&fast=true

        // suppress errors, this formatting is for an API call
        DateTimeFormatter dateformat = DateTimeFormat.forPattern("ddMMyy");
        DateTimeFormatter timeformat = DateTimeFormat.forPattern("HHmm");

        final String url = "https://api.irail.be/liveboard/?format=json"
                + "&id=" + station.getId()
                + "&date=" + dateformat.print(timeFilter)
                + "&time=" + timeformat.print(timeFilter)
                + "&arrdep=" + ((timeFilterType == RouteTimeDefinition.DEPART) ? "dep" : "arr");

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        LiveBoard result;
                        try {
                            result = parser.parseLiveboard(response, finalDateTime);
                        } catch (JSONException e) {
                            FirebaseCrash.logcat(WARNING.intValue(), "Failed to parse liveboard", e.getMessage());
                            FirebaseCrash.report(e);
                            if (errorListener != null) {
                                errorListener.onErrorResponse(e, tag);
                            }
                            return;
                        }

                        if (successListener != null) {
                            successListener.onSuccessResponse(result, tag);
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError e) {
                        Log.w(LOGTAG, "Tried loading liveboard from " + url + " failed with error " + e);
                        FirebaseCrash.logcat(WARNING.intValue(), "Failed to get liveboard", e.getMessage());
                        if (errorListener != null) {
                            errorListener.onErrorResponse(e, tag);
                        }
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
    public void getTrain(final String id, final DateTime day, final IRailSuccessResponseListener<Train> successListener,
                         final IRailErrorResponseListener<Train> errorListener, final Object tag) {
        DateTimeFormatter dateTimeformat = DateTimeFormat.forPattern("ddMMyy");

        String url = "https://api.irail.be/vehicle/?format=json"
                + "&id=" + id + "&date=" + dateTimeformat.print(day);

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
                            if (errorListener != null) {
                                errorListener.onErrorResponse(e, tag);
                            }
                            return;
                        }
                        if (successListener != null) {
                            successListener.onSuccessResponse(result, tag);
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError e) {
                        FirebaseCrash.logcat(WARNING.intValue(), "Failed to get train", e.getMessage());
                        if (errorListener != null) {
                            errorListener.onErrorResponse(e, tag);
                        }
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
    public void getTrain(String id, final IRailSuccessResponseListener<Train> successListener, final IRailErrorResponseListener<Train> errorListener, final Object tag) {
        getTrain(id, new DateTime(), successListener, errorListener, tag);
    }

    @Override
    public void getDisturbances(final IRailSuccessResponseListener<Disturbance[]> successListener, final IRailErrorResponseListener<Disturbance[]> errorListener, final Object tag) {
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
                            if (errorListener != null) {
                                errorListener.onErrorResponse(e, tag);
                            }
                            return;
                        }
                        successListener.onSuccessResponse(result, tag);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError e) {
                        FirebaseCrash.logcat(WARNING.intValue(), "Failed to get disturbances", e.getMessage());
                        if (errorListener != null) {
                            errorListener.onErrorResponse(e, tag);
                        }
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
    public void postOccupancy(String departureConnection, String stationSemanticId, String vehicleSemanticId, DateTime date,  OccupancyLevel occupancy, IRailSuccessResponseListener<Boolean> successListener,
                              IRailErrorResponseListener<Boolean> errorListener, Object tag) {
        final String url = "https://api.irail.be/feedback/occupancy.php";

        try {
            JSONObject payload = new JSONObject();

            payload.put("connection", departureConnection);
            payload.put("from", vehicleSemanticId);
            payload.put("date", DateTimeFormat.forPattern("Ymd").print(date));
            payload.put("vehicle", vehicleSemanticId);
            payload.put("occupancy", "http://api.irail.be/terms/" + occupancy.name().toLowerCase());

            Log.d(LOGTAG, "Posting feedback: " + url + " : " + payload);

            AsyncTask<String, Void, Void> t = new AsyncTask<String, Void, Void>() {
                @Override
                protected Void doInBackground(String... payload) {
                    postJsonRequest(url, payload[0]);
                    return null;
                }
            };
            t.execute(payload.toString());
        } catch (Exception e) {
            if (errorListener != null) {
                errorListener.onErrorResponse(e, tag);
            }
            return;
        }
        if (successListener != null) {
            successListener.onSuccessResponse(true, tag);
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
        String url;
        String result = null;
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

            String line = null;
            StringBuilder sb = new StringBuilder();

            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }

            bufferedReader.close();
            result = sb.toString();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
}
