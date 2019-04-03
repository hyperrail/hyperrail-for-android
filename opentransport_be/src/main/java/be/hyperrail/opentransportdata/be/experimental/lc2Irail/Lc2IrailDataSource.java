/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.be.experimental.lc2Irail;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.hyperrail.opentransportdata.be.irail.IrailApi;
import be.hyperrail.opentransportdata.be.irail.IrailLiveboardExtendHelper;
import be.hyperrail.opentransportdata.be.irail.IrailRouteExtendHelper;
import be.hyperrail.opentransportdata.be.irail.IrailVehicleJourney;
import be.hyperrail.opentransportdata.common.contracts.MeteredDataSource;
import be.hyperrail.opentransportdata.common.contracts.QueryTimeDefinition;
import be.hyperrail.opentransportdata.common.contracts.TransportDataErrorResponseListener;
import be.hyperrail.opentransportdata.common.contracts.TransportDataSource;
import be.hyperrail.opentransportdata.common.contracts.TransportDataSuccessResponseListener;
import be.hyperrail.opentransportdata.common.contracts.TransportStopsDataSource;
import be.hyperrail.opentransportdata.common.models.Route;
import be.hyperrail.opentransportdata.common.models.RoutesList;
import be.hyperrail.opentransportdata.common.models.VehicleJourney;
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

/**
 * Created in be.hyperrail.android.irail.implementation on 13/04/2018.
 */
public class Lc2IrailDataSource implements TransportDataSource, MeteredDataSource {

    private static final OpenTransportLog log = OpenTransportLog.getLogger(Lc2IrailDataSource.class);
    private static final String UA = "OpenTransport for Android - " + BuildConfig.VERSION_NAME;

    private final Context mContext;
    private final Lc2IrailParser parser;
    private final TransportStopsDataSource stationsProvider;
    private final RequestQueue requestQueue;
    private final DefaultRetryPolicy requestPolicy;
    private final ConnectivityManager mConnectivityManager;
    private final List<MeteredRequest> mMeteredRequests = new ArrayList<>();
    private final static String TAG_IRAIL_API_GET = "LC2IRAIL_GET";

    public Lc2IrailDataSource(Context context, TransportStopsDataSource stopsProvider) {
        this.mContext = context;
        stationsProvider = stopsProvider;
        this.parser = new Lc2IrailParser(stationsProvider);

        BasicNetwork network;
        network = new BasicNetwork(new HurlStack());
        File cacheDir = new File(mContext.getCacheDir(), "volley");
        this.requestQueue = new RequestQueue(new DiskBasedCache(cacheDir, 48 * 1024 * 1024), network);
        requestQueue.start();

        this.requestPolicy = new DefaultRetryPolicy(
                5000,
                1,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        );
        mConnectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

    }

    @Override
    public void getActualDisturbances(@NonNull ActualDisturbancesRequest... requests) {
        (new IrailApi(mContext, stationsProvider)).getActualDisturbances(requests);
    }

    @Override
    public void getLiveboard(@NonNull LiveboardRequest... requests) {
        for (LiveboardRequest request : requests) {
            getLiveboard(request);
        }
    }

    private void getLiveboard(@NonNull final LiveboardRequest request) {
        // https://api.irail.be/connections/?to=Halle&from=Brussels-south&date={dmy}&time=2359&timeSel=arrive or depart&format=json
        final MeteredRequest mMeteredRequest = new MeteredRequest();
        mMeteredRequest.setMsecStart(DateTime.now().getMillis());

        final Trace tracing = FirebasePerformance.getInstance().newTrace("lc2irail.getLiveboard");
        tracing.start();

        DateTimeFormatter fmt = ISODateTimeFormat.dateTimeNoMillis();
        String url;
        // https://lc2irail.thesis.bertmarcelis.be/liveboard/008841004/after/2018-04-13T13:13:47+00:00
        if (request.getTimeDefinition() == QueryTimeDefinition.EQUAL_OR_EARLIER) {
            url = "https://lc2irail.thesis.bertmarcelis.be/liveboard/"
                    + request.getStation().getHafasId() + "/before/"
                    + fmt.print(request.getSearchTime());
        } else {
            url = "https://lc2irail.thesis.bertmarcelis.be/liveboard/"
                    + request.getStation().getHafasId() + "/after/"
                    + fmt.print(request.getSearchTime());
        }

        mMeteredRequest.setTag(request.toString());

        Response.Listener<JSONObject> successListener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                mMeteredRequest.setMsecUsableNetworkResponse(DateTime.now().getMillis());
                LiveboardImpl liveboard;
                try {
                    liveboard = parser.parseLiveboard(request, response);
                    tracing.stop();
                } catch (Exception e) {
                    log.severe("Failed to parse liveboard", e);
                    tracing.stop();
                    log.logException(e);
                    request.notifyErrorListeners(e);
                    return;
                }
                request.notifySuccessListeners(liveboard);

                mMeteredRequest.setMsecParsed(DateTime.now().getMillis());
                mMeteredRequests.add(mMeteredRequest);
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError e) {
                tracing.stop();
                log.warning("Failed to get liveboard", e);
                request.notifyErrorListeners(e);
            }
        };

        tryOnlineOrServerCache(url, successListener, errorListener, mMeteredRequest);
    }

    @Override
    public void extendLiveboard(@NonNull ExtendLiveboardRequest... requests) {
        for (ExtendLiveboardRequest request :
                requests) {
            IrailLiveboardExtendHelper helper = new IrailLiveboardExtendHelper();
            helper.extendLiveboard(request);
        }
    }

    @Override
    public void getRoutePlanning(@NonNull RoutePlanningRequest... requests) {
        for (RoutePlanningRequest request : requests) {
            getRoutes(request);
        }
    }


    public void getRoutes(@NonNull final RoutePlanningRequest request) {
        final MeteredRequest mMeteredRequest = new MeteredRequest();
        mMeteredRequest.setMsecStart(DateTime.now().getMillis());
        final Trace tracing = FirebasePerformance.getInstance().newTrace("lc2irail.getRoutePlanning");
        tracing.start();
        // https://api.irail.be/connections/?to=Halle&from=Brussels-south&date={dmy}&time=2359&timeSel=arrive or depart&format=json
        DateTimeFormatter fmt = ISODateTimeFormat.dateTimeNoMillis();

        //https://lc2irail.thesis.bertmarcelis.be/connections/008841004/008814001/departing/2018-04-13T13:13:47+00:00
        String url = "https://lc2irail.thesis.bertmarcelis.be/connections/"
                + request.getOrigin().getHafasId() + "/"
                + request.getDestination().getHafasId() + "/";
        if (request.getTimeDefinition() == QueryTimeDefinition.EQUAL_OR_LATER) {
            url += "departing/";
        } else {
            url += "arriving/";
        }
        url += fmt.print(request.getSearchTime());

        mMeteredRequest.setTag(request.toString());

        Response.Listener<JSONObject> successListener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                mMeteredRequest.setMsecUsableNetworkResponse(DateTime.now().getMillis());
                RoutesListImpl routeResult;
                try {
                    routeResult = parser.parseRoutes(request, response);
                } catch (Exception e) {
                    log.warning("Failed to parse routes", e);
                    request.notifyErrorListeners(e);
                    return;
                }
                tracing.stop();
                request.notifySuccessListeners(routeResult);
                mMeteredRequest.setMsecParsed(DateTime.now().getMillis());
                mMeteredRequests.add(mMeteredRequest);
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError e) {
                log.warning("Failed to get routes", e);
                tracing.stop();
                request.notifyErrorListeners(e);
            }
        };

        tryOnlineOrServerCache(url, successListener, errorListener, mMeteredRequest);
    }

    @Override
    public void extendRoutePlanning(@NonNull ExtendRoutePlanningRequest... requests) {
        for (ExtendRoutePlanningRequest request :
                requests) {
            IrailRouteExtendHelper helper = new IrailRouteExtendHelper();
            helper.extendRoutesRequest(request);
        }
    }

    @Override
    public void getRoute(@NonNull RouteRefreshRequest... requests) {
        for (final RouteRefreshRequest request : requests) {
            RoutePlanningRequest routesRequest = new RoutePlanningRequest(
                    request.getOrigin(), request.getDestination(), request.getTimeDefinition(),
                    request.getSearchTime()
            );

            // Create a new routerequest. A successful response will be iterated to find a matching route. An unsuccessful query will cause the original error handler to be called.
            routesRequest.setCallback(new TransportDataSuccessResponseListener<RoutesList>() {
                @Override
                public void onSuccessResponse(@NonNull RoutesList data, Object tag) {
                    for (Route r : data.getRoutes()) {
                        if (r.getDeparture().getDepartureSemanticId() != null &&
                                r.getDeparture().getDepartureSemanticId().equals(request.getDepartureSemanticId())) {
                            request.notifySuccessListeners(r);
                        }
                    }
                }
            }, new TransportDataErrorResponseListener() {
                @Override
                public void onErrorResponse(@NonNull Exception e, Object tag) {
                    request.notifyErrorListeners(e);
                }
            }, request.getTag());

            getRoutes(routesRequest);
        }
    }

    @Override
    public void getStop(@NonNull VehicleStopRequest... requests) {
        for (VehicleStopRequest request : requests) {
            getStop(request);
        }
    }

    private void getStop(@NonNull final VehicleStopRequest request) {
        DateTime time = request.getStop().getDepartureTime();
        if (time == null) {
            time = request.getStop().getArrivalTime();
        }
        VehicleRequest vehicleRequest = new VehicleRequest(request.getStop().getVehicle().getId(), time);
        vehicleRequest.setCallback(new TransportDataSuccessResponseListener<VehicleJourney>() {
            @Override
            public void onSuccessResponse(@NonNull VehicleJourney data, Object tag) {
                for (VehicleStopImpl stop :
                        data.getStops()) {
                    if (stop.getDepartureUri().equals(request.getStop().getDepartureUri())) {
                        request.notifySuccessListeners(stop);
                        return;
                    }
                }
            }
        }, request.getOnErrorListener(), null);
        getVehicle(vehicleRequest);
    }

    @Override
    public void getVehicleJourney(@NonNull VehicleRequest... requests) {
        for (VehicleRequest request : requests) {
            getVehicle(request);
        }
    }

    public void getVehicle(@NonNull final VehicleRequest request) {
        final MeteredRequest mMeteredRequest = new MeteredRequest();
        mMeteredRequest.setMsecStart(DateTime.now().getMillis());
        DateTimeFormatter fmt = DateTimeFormat.forPattern("YYYYMMdd");

        final Trace tracing = FirebasePerformance.getInstance().newTrace("lc2irail.getVehicleJourney");
        tracing.start();

        // https://lc2irail.thesis.bertmarcelis.be/vehicle/IC538/20180413
        String url = "https://lc2irail.thesis.bertmarcelis.be/vehicle/"
                + request.getVehicleId() + "/"
                + fmt.print(request.getSearchTime());

        mMeteredRequest.setTag(request.toString());

        Response.Listener<JSONObject> successListener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                IrailVehicleJourney vehicle;
                try {
                    vehicle = parser.parseVehicle(request, response);
                } catch (Exception e) {
                    log.warning("Failed to parse vehicle", e);
                    request.notifyErrorListeners(e);
                    return;
                }
                tracing.stop();
                request.notifySuccessListeners(vehicle);

                mMeteredRequest.setMsecParsed(DateTime.now().getMillis());
                mMeteredRequests.add(mMeteredRequest);
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError e) {
                log.warning("Failed to get vehicle", e);
                tracing.stop();
                request.notifyErrorListeners(e);
            }
        };

        tryOnlineOrServerCache(url, successListener, errorListener, mMeteredRequest);
    }

    @Override
    public void postOccupancy(@NonNull OccupancyPostRequest... requests) {
        new IrailApi(mContext, stationsProvider).postOccupancy(requests);
    }

    @Override
    public void abortAllQueries() {

    }

    private boolean isInternetAvailable() {
        NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }


    /**
     * If internet is available, make a request. Otherwise, check the cache
     *
     * @param url             The url where the request should be made to
     * @param successListener The listener for successful responses, which will be used by the cache
     * @param errorListener   The listener for unsuccessful responses
     */
    private void tryOnlineOrServerCache(String url, Response.Listener<JSONObject> successListener, Response.ErrorListener errorListener, MeteredRequest meteredRequest) {
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

        if (isInternetAvailable()) {
            meteredRequest.setResponseType(MeteredDataSource.RESPONSE_ONLINE);
            if (requestQueue.getCache().get(jsObjRequest.getCacheKey()) != null && !requestQueue.getCache().get(jsObjRequest.getCacheKey()).isExpired()) {
                meteredRequest.setResponseType(MeteredDataSource.RESPONSE_CACHED);
                try {
                    successListener.onResponse(new JSONObject(new String(requestQueue.getCache().get(jsObjRequest.getCacheKey()).data)));
                    return;
                } catch (JSONException e) {
                    log.debug("Failed to return result from cache", e);
                    meteredRequest.setResponseType(MeteredDataSource.RESPONSE_ONLINE);
                    requestQueue.add(jsObjRequest);
                }
            }
            requestQueue.add(jsObjRequest);
        } else {
            log.debug("Trying to get data without internet");
            if (requestQueue.getCache().get(jsObjRequest.getCacheKey()) != null) {
                try {
                    JSONObject cache;
                    cache = new JSONObject(new String(requestQueue.getCache().get(jsObjRequest.getCacheKey()).data));
                    meteredRequest.setResponseType(MeteredDataSource.RESPONSE_OFFLINE);
                    successListener.onResponse(cache);
                } catch (JSONException e) {
                    log.warning("Failed to get result from cache", e);
                    errorListener.onErrorResponse(new NoConnectionError());
                    meteredRequest.setResponseType(MeteredDataSource.RESPONSE_FAILED);
                }

            } else {
                log.debug("No cache available");
                errorListener.onErrorResponse(new NoConnectionError());
                meteredRequest.setResponseType(MeteredDataSource.RESPONSE_FAILED);
            }
        }
    }

    @Override
    public MeteredRequest[] getMeteredRequests() {
        MeteredRequest[] meteredRequests = new MeteredRequest[mMeteredRequests.size()];
        return mMeteredRequests.toArray(meteredRequests);
    }
}
