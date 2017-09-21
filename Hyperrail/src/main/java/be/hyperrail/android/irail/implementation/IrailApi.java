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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
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
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Locale;

import be.hyperrail.android.irail.contracts.IrailDataProvider;
import be.hyperrail.android.irail.contracts.IrailParser;
import be.hyperrail.android.irail.contracts.IrailResponseListener;
import be.hyperrail.android.irail.contracts.IrailStationProvider;
import be.hyperrail.android.irail.contracts.OccupancyLevel;
import be.hyperrail.android.irail.contracts.RouteTimeDefinition;
import be.hyperrail.android.irail.db.Station;
import be.hyperrail.android.irail.exception.NotFoundException;

import static java.util.logging.Level.WARNING;

/**
 * Synchronous API for api.irail.be
 *
 * @inheritDoc
 */
public class IrailApi implements IrailDataProvider {

    private static final String LOGTAG = "iRailApi";
    private final RequestQueue requestQueue;

    public IrailApi(Context context, IrailParser parser, IrailStationProvider stationProvider) {
        this.context = context;
        this.parser = parser;
        this.stationProvider = stationProvider;
        this.requestQueue = Volley.newRequestQueue(context);
    }

    private final Context context;
    private final IrailParser parser;
    private final IrailStationProvider stationProvider;

    private final int TAG_IRAIL_API_GET = 0;

    @Override
    public void getRoute(IrailResponseListener<RouteResult> callback, int tag, String from, String to) {
        getRoute(callback, tag, from, to, new DateTime());
    }

    @Override
    public void getRoute(IrailResponseListener<RouteResult> callback, int tag, String from, String to, DateTime timeFilter) {
        getRoute(callback, tag, to, timeFilter, RouteTimeDefinition.DEPART, from);
    }

    @Override
    public void getRoute(IrailResponseListener<RouteResult> callback, int tag, String to, DateTime timeFilter, RouteTimeDefinition timeFilterType, String from) {
        getRoute(callback, tag, stationProvider.getStationByName(from), stationProvider.getStationByName(to), timeFilter, timeFilterType);
    }

    @Override
    public void getRoute(IrailResponseListener<RouteResult> callback, int tag, Station from, Station to) {
        getRoute(callback, tag, from, to, new DateTime());
    }

    @Override
    public void getRoute(IrailResponseListener<RouteResult> callback, int tag, Station from, Station to, DateTime timeFilter) {
        getRoute(callback, tag, from, to, timeFilter, RouteTimeDefinition.DEPART);
    }

    @Override
    @AddTrace(name = "iRailGetroute")
    public void getRoute(final IrailResponseListener<RouteResult> callback, final int tag, final Station from, final Station to, final DateTime timeFilter, final RouteTimeDefinition timeFilterType) {
        if (callback == null) {
            return;
        }
        // https://api.irail.be/connections/?to=Halle&from=Brussels-south&date={dmy}&time=2359&timeSel=arrive or depart&format=json

        // suppress errors, this formatting is for an API call
        DateTimeFormatter dateformat = DateTimeFormat.forPattern("ddMMyy");
        DateTimeFormatter timeformat = DateTimeFormat.forPattern("HHmm");

        if (from == null || to == null) {
            callback.onIrailErrorResponse(new NotFoundException("", "One or both stations are null"), tag);
            return;
        }

        String from_name;
        String to_name;
        try {
            from_name = URLEncoder.encode(from.getName(), "UTF-8");
            to_name = URLEncoder.encode(to.getName(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            from_name = from.getName();
            to_name = to.getName();
            e.printStackTrace();
        }

        String url = "https://api.irail.be/connections/?format=json"
                + "&to=" + to_name
                + "&from=" + from_name
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
                        try {
                            callback.onIrailSuccessResponse(parser.parseRouteResult(response, from, to, timeFilter, timeFilterType), tag);
                        } catch (JSONException e) {
                            FirebaseCrash.logcat(WARNING.intValue(), "Failed to parse routes", e.getMessage());
                            FirebaseCrash.report(e);
                            callback.onIrailErrorResponse(e, tag);
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError e) {
                        FirebaseCrash.logcat(WARNING.intValue(), "Failed to get routes", e.getMessage());
                        callback.onIrailErrorResponse(e, tag);
                        callback.onIrailErrorResponse(e, tag);
                    }
                });
        jsObjRequest.setTag(TAG_IRAIL_API_GET);
        requestQueue.add(jsObjRequest);
    }

    @Override
    public void getLiveboard(IrailResponseListener<LiveBoard> callback, int tag, String name) {
        getLiveboard(callback, tag, stationProvider.getStationByName(name));
    }

    @Override
    public void getLiveboard(IrailResponseListener<LiveBoard> callback, int tag, Station station) {
        getLiveboard(callback, tag, station, new DateTime());
    }

    @Override
    public void getLiveboard(IrailResponseListener<LiveBoard> callback, int tag, String name, DateTime timeFilter) {
        getLiveboard(callback, tag, stationProvider.getStationByName(name), timeFilter, RouteTimeDefinition.DEPART);
    }

    @Override
    public void getLiveboard(IrailResponseListener<LiveBoard> callback, int tag, Station station, DateTime timeFilter) {
        if (timeFilter == null) {
            timeFilter = new DateTime();
        }
        getLiveboard(callback, tag, station, timeFilter, RouteTimeDefinition.DEPART);
    }

    public void getLiveboard(final IrailResponseListener<LiveBoard> callback, final int tag, String name, final DateTime timeFilter, final RouteTimeDefinition timeFilterType) {
        getLiveboard(callback, tag, stationProvider.getStationByName(name), timeFilter, timeFilterType);
    }

    @Override
    @AddTrace(name = "iRailGetLiveboard")
    public void getLiveboard(final IrailResponseListener<LiveBoard> callback, final int tag, Station station, final DateTime timeFilter, final RouteTimeDefinition timeFilterType) {
        if (callback == null) {
            return;
        }
        // https://api.irail.be/liveboard/?station=Halle&fast=true

        // suppress errors, this formatting is for an API call
        DateTimeFormatter dateformat = DateTimeFormat.forPattern("ddMMyy");
        DateTimeFormatter timeformat = DateTimeFormat.forPattern("HHmm");

        String url = "https://api.irail.be/liveboard/?format=json"
                // TODO: use id here instead of name, supported by API but slow ATM
                + "&station=" + station.getId()
                + "&date=" + dateformat.print(timeFilter)
                + "&time=" + timeformat.print(timeFilter)
                + "&arrdep=" + ((timeFilterType == RouteTimeDefinition.DEPART) ? "dep" : "arr");

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            callback.onIrailSuccessResponse(parser.parseLiveboard(response, timeFilter), tag);
                        } catch (JSONException e) {
                            FirebaseCrash.logcat(WARNING.intValue(), "Failed to parse liveboard", e.getMessage());
                            FirebaseCrash.report(e);
                            callback.onIrailErrorResponse(e, tag);
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError e) {
                        FirebaseCrash.logcat(WARNING.intValue(), "Failed to get liveboard", e.getMessage());
                        callback.onIrailErrorResponse(e, tag);
                        callback.onIrailErrorResponse(e, tag);
                    }
                });
        jsObjRequest.setTag(TAG_IRAIL_API_GET);
        requestQueue.add(jsObjRequest);
    }

    @Override
    public void getTrain(final IrailResponseListener<Train> callback, final int tag, final String id, final DateTime day) {
        if (callback == null) {
            return;
        }
        DateTimeFormatter dateTimeformat = DateTimeFormat.forPattern("ddMMyy");

        String url = "https://api.irail.be/vehicle/?format=json"
                + "&id=" + id + "&date=" + dateTimeformat.print(day);

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            callback.onIrailSuccessResponse(parser.parseTrain(response, new DateTime()), tag);
                        } catch (JSONException e) {
                            FirebaseCrash.logcat(WARNING.intValue(), "Failed to parse train", e.getMessage());
                            FirebaseCrash.report(e);
                            callback.onIrailErrorResponse(e, tag);
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError e) {
                        FirebaseCrash.logcat(WARNING.intValue(), "Failed to get train", e.getMessage());
                        callback.onIrailErrorResponse(e, tag);
                        callback.onIrailErrorResponse(e, tag);
                    }
                });
        jsObjRequest.setTag(TAG_IRAIL_API_GET);
        requestQueue.add(jsObjRequest);
    }

    @Override
    public void getTrain(IrailResponseListener<Train> callback, int tag, String id) {
        getTrain(callback, tag, id, new DateTime());
    }

    @Override
    public void getDisturbances(final IrailResponseListener<Disturbance[]> callback, final int tag) {
        if (callback == null) {
            return;
        }

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
                        try {
                            callback.onIrailSuccessResponse(parser.parseDisturbances(response), tag);
                        } catch (JSONException e) {
                            FirebaseCrash.logcat(WARNING.intValue(), "Failed to parse disturbances", e.getMessage());
                            FirebaseCrash.report(e);
                            callback.onIrailErrorResponse(e, tag);
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError e) {
                        FirebaseCrash.logcat(WARNING.intValue(), "Failed to get disturbances", e.getMessage());
                        callback.onIrailErrorResponse(e, tag);
                        callback.onIrailErrorResponse(e, tag);
                    }
                });
        jsObjRequest.setTag(TAG_IRAIL_API_GET);
        requestQueue.add(jsObjRequest);

    }

    @Override
    public void postOccupancy(IrailResponseListener<Boolean> callback, int tag, TrainStub train, TrainStop stop, OccupancyLevel occupancy) {
        final String url = "https://api.irail.be/feedback/occupancy.php";
        final String payload =
                "{\"connection\": \"" + stop.getSemanticDepartureConnection() + "\"," +
                        "\"from\": \"" + stop.getStation().getSemanticId() + "\"," +
                        "\"date\": \"" + DateTimeFormat.forPattern("Ymd").print(stop.getDepartureTime()) + "\"," +
                        "\"vehicle\": \"" + train.getSemanticId() + "\"," +
                        "\"occupancy\":\"http://api.irail.be/terms/" + occupancy.name().toLowerCase() + "\"}";
        try {

            AsyncTask<String, Void, Void> t = new AsyncTask<String, Void, Void>() {
                @Override
                protected Void doInBackground(String... payload) {
                    postJsonRequest(url, payload[0]);
                    return null;
                }
            };
            t.execute(payload);
        } catch (Exception e) {
            callback.onIrailErrorResponse(e, tag);
        }
        callback.onIrailSuccessResponse(true, tag);
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
        String data = json;
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
            writer.write(data);
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
