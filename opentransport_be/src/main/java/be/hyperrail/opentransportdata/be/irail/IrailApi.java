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

package be.hyperrail.opentransportdata.be.irail;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import be.hyperrail.opentransportdata.be.irail.util.AsyncJsonPostRequest;
import be.hyperrail.opentransportdata.common.contracts.QueryTimeDefinition;
import be.hyperrail.opentransportdata.common.contracts.TransportDataSource;
import be.hyperrail.opentransportdata.common.contracts.TransportStopsDataSource;
import be.hyperrail.opentransportdata.common.exceptions.StopLocationNotResolvedException;
import be.hyperrail.opentransportdata.common.models.Disturbance;
import be.hyperrail.opentransportdata.common.models.LiveboardType;
import be.hyperrail.opentransportdata.common.models.Route;
import be.hyperrail.opentransportdata.common.models.VehicleStop;
import be.hyperrail.opentransportdata.common.models.implementation.LiveboardImpl;
import be.hyperrail.opentransportdata.common.models.implementation.RoutesListImpl;
import be.hyperrail.opentransportdata.common.models.implementation.VehicleStopImpl;
import be.hyperrail.opentransportdata.common.requests.ActualDisturbancesRequest;
import be.hyperrail.opentransportdata.common.requests.ExtendLiveboardRequest;
import be.hyperrail.opentransportdata.common.requests.ExtendRoutePlanningRequest;
import be.hyperrail.opentransportdata.common.requests.LiveboardRequest;
import be.hyperrail.opentransportdata.common.requests.OccupancyPostRequest;
import be.hyperrail.opentransportdata.common.requests.RoutePlanningRequest;
import be.hyperrail.opentransportdata.common.requests.RouteRefreshRequest;
import be.hyperrail.opentransportdata.common.requests.VehicleRequest;
import be.hyperrail.opentransportdata.common.requests.VehicleStopRequest;
import be.hyperrail.opentransportdata.logging.OpenTransportLog;
import be.opentransport.BuildConfig;

import static java.util.logging.Level.WARNING;

/**
 * Synchronous API for api.irail.be
 *
 * @inheritDoc
 */
public class IrailApi implements TransportDataSource {

    private static final String LOGTAG = "iRailApi";
    private static final String USER_AGENT = "OpenTransportData for Android - " + BuildConfig.VERSION_NAME;

    private final RequestQueue requestQueue;
    private final RetryPolicy requestPolicy;

    private final Context context;
    private final IrailApiParser parser;
    private final ConnectivityManager connectivityManager;

    private final int REQUEST_TAG_GET_REQUEST = 0;

    public IrailApi(Context context, TransportStopsDataSource stationProviderInstance) {
        this.context = context;
        this.parser = new IrailApiParser(stationProviderInstance);
        this.requestQueue = Volley.newRequestQueue(context);
        this.requestPolicy = new DefaultRetryPolicy(
                750,
                3,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        );
        connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);


    }

    private boolean isInternetAvailable() {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    @Override
    public void getRoute(RouteRefreshRequest... requests) {
        for (RouteRefreshRequest request : requests
        ) {
            RoutePlanningRequest routesRequest = new RoutePlanningRequest(
                    request.getOrigin(), request.getDestination(), request.getTimeDefinition(),
                    request.getSearchTime()
            );

            // Create a new routerequest. A successful response will be iterated to find a matching route. An unsuccessful query will cause the original error handler to be called.
            routesRequest.setCallback((data, tag) -> {
                for (Route r : data.getRoutes()) {
                    if (r.getDeparture().getDepartureSemanticId() != null && r.getDeparture().getDepartureSemanticId().equals(
                            request.getDepartureSemanticId())) {
                        request.notifySuccessListeners(r);
                    }
                }
            }, (e, tag) -> request.notifyErrorListeners(e), request.getTag());

            getRoutes(routesRequest);
        }
    }

    @Override
    public void getRoutePlanning(RoutePlanningRequest... requests) {
        for (RoutePlanningRequest request :
                requests) {
            getRoutes(request);
        }
    }

    @Override
    public void extendRoutePlanning(ExtendRoutePlanningRequest... requests) {
        for (ExtendRoutePlanningRequest request :
                requests) {
            IrailRouteAppendHelper helper = new IrailRouteAppendHelper();
            helper.extendRoutesRequest(request);
        }
    }

    public void getRoutes(RoutePlanningRequest request) {

        // https://api.irail.be/connections/?to=Halle&from=Brussels-south&date={dmy}&time=2359&timeSel=arrive or depart&format=json

        DateTimeFormatter dateformat = DateTimeFormat.forPattern("ddMMyy");
        DateTimeFormatter timeformat = DateTimeFormat.forPattern("HHmm");

        String locale = PreferenceManager.getDefaultSharedPreferences(context).getString(
                "pref_stations_language", "");
        if (locale == null || locale.isEmpty()) {
            // Only get locale when needed
            locale = Locale.getDefault().getISO3Language();
        }

        String url = "https://api.irail.be/connections/?format=json"
                + "&to=" + request.getDestination().getHafasId()
                + "&from=" + request.getOrigin().getHafasId()
                + "&date=" + dateformat.print(request.getSearchTime())
                + "&time=" + timeformat.print(request.getSearchTime().withZone(DateTimeZone.forID("Europe/Brussels")))
                + "&lang=" + locale.substring(0, 2);

        if (request.getTimeDefinition() == QueryTimeDefinition.DEPART_AT) {
            url += "&timeSel=depart";
        } else {
            url += "&timeSel=arrive";
        }

        Response.Listener<JSONObject> successListener = response -> {
            RoutesListImpl routeResult;
            try {
                routeResult = parser.parseRouteResult(
                        response, request.getOrigin(), request.getDestination(),
                        request.getSearchTime(), request.getTimeDefinition()
                );
            } catch (JSONException e) {
                OpenTransportLog.log(
                        WARNING.intValue(), "Failed to parse routes", e.getMessage());
                OpenTransportLog.logException(e);
                request.notifyErrorListeners(e);
                return;
            }
            request.notifySuccessListeners(routeResult);
        };

        Response.ErrorListener errorListener = e -> {
            OpenTransportLog.log(
                    WARNING.intValue(), "Failed to get routes", e.getMessage());
            request.notifyErrorListeners(e);
        };

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, successListener, errorListener) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-agent", USER_AGENT);
                return headers;
            }
        };
        jsObjRequest.setRetryPolicy(requestPolicy);
        jsObjRequest.setTag(REQUEST_TAG_GET_REQUEST);

        tryOnlineOrServerCache(jsObjRequest, successListener, errorListener);
    }

    @Override
    public void getLiveboard(LiveboardRequest... requests) {
        for (LiveboardRequest request : requests) {
            if (request.getTimeDefinition() == QueryTimeDefinition.DEPART_AT) {
                getLiveboardAfter(request);
            } else {
                getLiveboardBefore(request);
            }
        }
    }

    @Override
    public void extendLiveboard(ExtendLiveboardRequest... requests) {
        for (ExtendLiveboardRequest request :
                requests) {
            IrailLiveboardAppendHelper helper = new IrailLiveboardAppendHelper();
            helper.extendLiveboard(request);
        }
    }

    private void getLiveboardBefore(LiveboardRequest request) {
        LiveboardRequest actualRequest = request.withSearchTime(
                request.getSearchTime().minusHours(1));

        actualRequest.setCallback((data, tag) -> {

            if (!(data instanceof LiveboardImpl)) {
                throw new IllegalArgumentException("IrailApi should only handle Irail specific models");
            }

            List<VehicleStop> stops = new ArrayList<>();
            for (VehicleStop s : data.getStops()) {
                if (s.getDepartureTime().isBefore(actualRequest.getSearchTime())) {
                    stops.add(s);
                }
            }

            //noinspection SuspiciousToArrayCall
            request.notifySuccessListeners(
                    new LiveboardImpl(data, stops.toArray(new VehicleStopImpl[]{}),
                            data.getSearchTime(), data.getLiveboardType(), QueryTimeDefinition.ARRIVE_AT
                    ));
        }, (e, tag) -> request.notifyErrorListeners(e), actualRequest.getTag());
        getLiveboardAfter(request);
    }

    private void getLiveboardAfter(LiveboardRequest request) {
        // https://api.irail.be/liveboard/?station=Halle&fast=true

        // suppress errors, this formatting is for an API call
        DateTimeFormatter dateformat = DateTimeFormat.forPattern("ddMMyy");
        DateTimeFormatter timeformat = DateTimeFormat.forPattern("HHmm");

        String url = "https://api.irail.be/liveboard/?format=json"
                + "&id=" + request.getStation().getHafasId()
                + "&date=" + dateformat.print(request.getSearchTime())
                + "&time=" + timeformat.print(request.getSearchTime().withZone(DateTimeZone.forID("Europe/Brussels")))
                + "&arrdep=" + ((request.getType() == LiveboardType.DEPARTURES) ? "dep" : "arr");

        Response.Listener<JSONObject> successListener = response -> {
            LiveboardImpl result;
            try {
                result = parser.parseLiveboard(response, request.getSearchTime(), request.getType(), request.getTimeDefinition());
            } catch (JSONException | StopLocationNotResolvedException e) {
                OpenTransportLog.log(WARNING.intValue(), "Failed to parse liveboard", e.getMessage());
                OpenTransportLog.logException(e);
                request.notifyErrorListeners(e);
                return;
            }

            request.notifySuccessListeners(result);
        };

        Response.ErrorListener errorListener = e -> {
            Log.w(LOGTAG, "Tried loading liveboard from " + url + " failed with error " + e);
            OpenTransportLog.log(
                    WARNING.intValue(), "Failed to get liveboard", e.getMessage());
            request.notifyErrorListeners(e);
        };

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, successListener, errorListener) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-agent", USER_AGENT);
                return headers;
            }
        };

        jsObjRequest.setRetryPolicy(requestPolicy);
        jsObjRequest.setTag(REQUEST_TAG_GET_REQUEST);

        tryOnlineOrServerCache(jsObjRequest, successListener, errorListener);
    }

    @Override
    public void getVehicleJourney(VehicleRequest... requests) {
        for (VehicleRequest request :
                requests) {
            getVehicle(request);
        }
    }

    public void getVehicle(VehicleRequest request) {
        DateTimeFormatter dateTimeformat = DateTimeFormat.forPattern("ddMMyy");

        String url = "https://api.irail.be/vehicle/?format=json"
                + "&id=" + request.getVehicleId() + "&date=" + dateTimeformat.print(
                request.getSearchTime());

        Response.Listener<JSONObject> successListener = response -> {
            IrailVehicle result;
            try {
                result = parser.parseTrain(response, request.getSearchTime());
            } catch (JSONException | StopLocationNotResolvedException e) {
                OpenTransportLog.log(
                        WARNING.intValue(), "Failed to parse vehicle", e.getMessage());
                OpenTransportLog.logException(e);
                request.notifyErrorListeners(e);
                return;
            }
            request.notifySuccessListeners(result);
        };

        Response.ErrorListener errorListener = e -> {
            OpenTransportLog.log(
                    WARNING.intValue(), "Failed to get vehicle", e.getMessage());
            request.notifyErrorListeners(e);
        };
        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, successListener, errorListener) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-agent", USER_AGENT);
                return headers;
            }
        };

        jsObjRequest.setRetryPolicy(requestPolicy);
        jsObjRequest.setTag(REQUEST_TAG_GET_REQUEST);

        tryOnlineOrServerCache(jsObjRequest, successListener, errorListener);
    }

    @Override
    public void getStop(VehicleStopRequest... requests) {
        for (VehicleStopRequest request :
                requests) {
            getStop(request);
        }
    }

    private void getStop(VehicleStopRequest request) {
        DateTime time = request.getStop().getDepartureTime();
        if (time == null) {
            time = request.getStop().getArrivalTime();
        }
        VehicleRequest vehicleRequest = new VehicleRequest(request.getStop().getVehicle().getId(), time);
        vehicleRequest.setCallback((data, tag) -> {
            for (VehicleStopImpl stop :
                    data.getStops()) {
                if (stop.getDepartureUri().equals(request.getStop().getDepartureUri())) {
                    request.notifySuccessListeners(stop);
                    return;
                }
            }
        }, request.getOnErrorListener(), null);
        getVehicle(vehicleRequest);
    }

    @Override
    public void getActualDisturbances(ActualDisturbancesRequest... requests) {
        for (ActualDisturbancesRequest request :
                requests) {
            getDisturbances(request);
        }
    }

    private void getDisturbances(ActualDisturbancesRequest request) {

        String locale = PreferenceManager.getDefaultSharedPreferences(context).getString("pref_stations_language", "");

        if (locale == null || locale.isEmpty()) {
            // Only get locale when needed
            locale = Locale.getDefault().getISO3Language();
        }

        String url = "https://api.irail.be/disturbances/?format=json&lang=" + locale.substring(
                0, 2);

        Response.Listener<JSONObject> successListener = response -> {
            Disturbance[] result;
            try {
                result = parser.parseDisturbances(response);
            } catch (JSONException e) {
                OpenTransportLog.log(WARNING.intValue(), "Failed to parse disturbances", e.getMessage());
                OpenTransportLog.logException(e);
                request.notifyErrorListeners(e);
                return;
            }
            request.notifySuccessListeners(result);
        };

        Response.ErrorListener errorListener = e -> {
            OpenTransportLog.log(WARNING.intValue(), "Failed to get disturbances", e.getMessage());
            request.notifyErrorListeners(e);
        };

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, successListener, errorListener) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-agent", USER_AGENT);
                return headers;
            }
        };

        jsObjRequest.setRetryPolicy(requestPolicy);
        jsObjRequest.setTag(REQUEST_TAG_GET_REQUEST);
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
                    OpenTransportLog.log(
                            WARNING.intValue(), "Failed to get result from cache", e.getMessage());
                    errorListener.onErrorResponse(new NoConnectionError());
                }

            } else {
                errorListener.onErrorResponse(new NoConnectionError());
            }
        }
    }

    @Override
    public void postOccupancy(OccupancyPostRequest... requests) {
        for (OccupancyPostRequest request :
                requests) {
            postOccupancy(request);
        }
    }

    private void postOccupancy(OccupancyPostRequest request) {

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

            AsyncJsonPostRequest.postRequestAsync(url, request, payload);
        } catch (Exception e) {
            request.notifyErrorListeners(e);
        }
    }

    @Override
    public void abortAllQueries() {
        this.requestQueue.cancelAll(REQUEST_TAG_GET_REQUEST);
    }

}
