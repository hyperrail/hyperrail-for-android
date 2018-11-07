/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package eu.opentransport.lc2Irail;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.util.Log;

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
import com.crashlytics.android.Crashlytics;
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

import eu.opentransport.BuildConfig;
import eu.opentransport.OpenTransportApi;
import eu.opentransport.common.contracts.MeteredDataSource;
import eu.opentransport.common.contracts.QueryTimeDefinition;
import eu.opentransport.common.contracts.TransportDataErrorResponseListener;
import eu.opentransport.common.contracts.TransportDataSource;
import eu.opentransport.common.contracts.TransportDataSuccessResponseListener;
import eu.opentransport.common.contracts.TransportStopsDataSource;
import eu.opentransport.common.models.Route;
import eu.opentransport.common.models.RoutesList;
import eu.opentransport.common.models.Vehicle;
import eu.opentransport.common.requests.ExtendLiveboardRequest;
import eu.opentransport.common.requests.ExtendRoutesRequest;
import eu.opentransport.common.requests.IrailDisturbanceRequest;
import eu.opentransport.common.requests.IrailLiveboardRequest;
import eu.opentransport.common.requests.IrailPostOccupancyRequest;
import eu.opentransport.common.requests.IrailRouteRequest;
import eu.opentransport.common.requests.IrailRoutesRequest;
import eu.opentransport.common.requests.IrailVehicleRequest;
import eu.opentransport.common.requests.VehicleStopRequest;
import eu.opentransport.irail.IrailApi;
import eu.opentransport.irail.IrailLiveboard;
import eu.opentransport.irail.IrailLiveboardAppendHelper;
import eu.opentransport.irail.IrailRouteAppendHelper;
import eu.opentransport.irail.IrailRoutesList;
import eu.opentransport.irail.IrailVehicle;
import eu.opentransport.irail.IrailVehicleStop;

import static java.util.logging.Level.WARNING;

/**
 * Created in be.hyperrail.android.irail.implementation on 13/04/2018.
 */
public class Lc2IrailDataSource implements TransportDataSource, MeteredDataSource {

    private static final String UA = "OpenTransport for Android - " + BuildConfig.VERSION_NAME;
    private static final String LOGTAG = "Lc2IrailDataSource";

    private final Context mContext;
    private final Lc2IrailParser parser;
    private final RequestQueue requestQueue;
    private final DefaultRetryPolicy requestPolicy;
    private final ConnectivityManager mConnectivityManager;
    private final List<MeteredRequest> mMeteredRequests = new ArrayList<>();
    private final static String TAG_IRAIL_API_GET = "LC2IRAIL_GET";

    public Lc2IrailDataSource(Context context) {
        this.mContext = context;
        TransportStopsDataSource stationsProvider = OpenTransportApi.getStationsProviderInstance();
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
    public void getDisturbances(@NonNull IrailDisturbanceRequest... requests) {
        (new IrailApi(mContext)).getDisturbances(requests);
    }

    @Override
    public void getLiveboard(@NonNull IrailLiveboardRequest... requests) {
        for (IrailLiveboardRequest request : requests) {
            getLiveboard(request);
        }
    }

    private void getLiveboard(@NonNull final IrailLiveboardRequest request) {
        // https://api.irail.be/connections/?to=Halle&from=Brussels-south&date={dmy}&time=2359&timeSel=arrive or depart&format=json
        final MeteredRequest mMeteredRequest = new MeteredRequest();
        mMeteredRequest.setMsecStart(DateTime.now().getMillis());

        final Trace tracing = FirebasePerformance.getInstance().newTrace("lc2irail.getLiveboard");
        tracing.start();

        DateTimeFormatter fmt = ISODateTimeFormat.dateTimeNoMillis();
        String url;
        // https://lc2irail.thesis.bertmarcelis.be/liveboard/008841004/after/2018-04-13T13:13:47+00:00
        if (request.getTimeDefinition() == QueryTimeDefinition.ARRIVE_AT) {
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
                IrailLiveboard liveboard;
                try {
                    liveboard = parser.parseLiveboard(request, response);
                    tracing.stop();
                } catch (Exception e) {
                    Crashlytics.log(
                            WARNING.intValue(), "Failed to parse liveboard", e.getMessage());
                    Log.w(LOGTAG, "Failed to parse liveboard", e);
                    tracing.stop();
                    Crashlytics.logException(e);
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
                Crashlytics.log(
                        WARNING.intValue(), "Failed to get liveboard", e.getMessage());
                request.notifyErrorListeners(e);
            }
        };

        tryOnlineOrServerCache(url, successListener, errorListener, mMeteredRequest);
    }

    @Override
    public void extendLiveboard(@NonNull ExtendLiveboardRequest... requests) {
        for (ExtendLiveboardRequest request :
                requests) {
            IrailLiveboardAppendHelper helper = new IrailLiveboardAppendHelper();
            helper.extendLiveboard(request);
        }
    }

    @Override
    public void getRoutes(@NonNull IrailRoutesRequest... requests) {
        for (IrailRoutesRequest request : requests) {
            getRoutes(request);
        }
    }


    public void getRoutes(@NonNull final IrailRoutesRequest request) {
        final MeteredRequest mMeteredRequest = new MeteredRequest();
        mMeteredRequest.setMsecStart(DateTime.now().getMillis());
        final Trace tracing = FirebasePerformance.getInstance().newTrace("lc2irail.getRoutes");
        tracing.start();
        // https://api.irail.be/connections/?to=Halle&from=Brussels-south&date={dmy}&time=2359&timeSel=arrive or depart&format=json
        DateTimeFormatter fmt = ISODateTimeFormat.dateTimeNoMillis();

        //https://lc2irail.thesis.bertmarcelis.be/connections/008841004/008814001/departing/2018-04-13T13:13:47+00:00
        String url = "https://lc2irail.thesis.bertmarcelis.be/connections/"
                + request.getOrigin().getHafasId() + "/"
                + request.getDestination().getHafasId() + "/";
        if (request.getTimeDefinition() == QueryTimeDefinition.DEPART_AT) {
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
                IrailRoutesList routeResult;
                try {
                    routeResult = parser.parseRoutes(request, response);
                } catch (Exception e) {
                    Crashlytics.log(
                            WARNING.intValue(), "Failed to parse routes", e.getMessage());
                    Crashlytics.logException(e);
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
                Crashlytics.log(
                        WARNING.intValue(), "Failed to get routes", e.getMessage());
                tracing.stop();
                request.notifyErrorListeners(e);
            }
        };

        tryOnlineOrServerCache(url, successListener, errorListener, mMeteredRequest);
    }

    @Override
    public void extendRoutes(@NonNull ExtendRoutesRequest... requests) {
        for (ExtendRoutesRequest request :
                requests) {
            IrailRouteAppendHelper helper = new IrailRouteAppendHelper();
            helper.extendRoutesRequest(request);
        }
    }

    @Override
    public void getRoute(@NonNull IrailRouteRequest... requests) {
        for (final IrailRouteRequest request : requests) {
            IrailRoutesRequest routesRequest = new IrailRoutesRequest(
                    request.getOrigin(), request.getDestination(), request.getTimeDefinition(),
                    request.getSearchTime()
            );

            // Create a new routerequest. A successful response will be iterated to find a matching route. An unsuccessful query will cause the original error handler to be called.
            routesRequest.setCallback(new TransportDataSuccessResponseListener<RoutesList>() {
                @Override
                public void onSuccessResponse(@NonNull RoutesList data, Object tag) {
                    for (Route r : data.getRoutes()) {
                        if (r.getTransfers()[0].getDepartureSemanticId() != null &&
                                r.getTransfers()[0].getDepartureSemanticId().equals(request.getDepartureSemanticId())) {
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
        IrailVehicleRequest vehicleRequest = new IrailVehicleRequest(request.getStop().getVehicle().getId(), time);
        vehicleRequest.setCallback(new TransportDataSuccessResponseListener<Vehicle>() {
            @Override
            public void onSuccessResponse(@NonNull Vehicle data, Object tag) {
                for (IrailVehicleStop stop :
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
    public void getVehicle(@NonNull IrailVehicleRequest... requests) {
        for (IrailVehicleRequest request : requests) {
            getVehicle(request);
        }
    }

    public void getVehicle(@NonNull final IrailVehicleRequest request) {
        final MeteredRequest mMeteredRequest = new MeteredRequest();
        mMeteredRequest.setMsecStart(DateTime.now().getMillis());
        DateTimeFormatter fmt = DateTimeFormat.forPattern("YYYYMMdd");

        final Trace tracing = FirebasePerformance.getInstance().newTrace("lc2irail.getVehicle");
        tracing.start();

        // https://lc2irail.thesis.bertmarcelis.be/vehicle/IC538/20180413
        String url = "https://lc2irail.thesis.bertmarcelis.be/vehicle/"
                + request.getVehicleId() + "/"
                + fmt.print(request.getSearchTime());

        mMeteredRequest.setTag(request.toString());

        Response.Listener<JSONObject> successListener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                IrailVehicle vehicle;
                try {
                    vehicle = parser.parseVehicle(request, response);
                } catch (Exception e) {
                    Crashlytics.log(
                            WARNING.intValue(), "Failed to parse vehicle", e.getMessage());
                    Crashlytics.logException(e);
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
                Crashlytics.log(
                        WARNING.intValue(), "Failed to get vehicle", e.getMessage());
                tracing.stop();
                request.notifyErrorListeners(e);
            }
        };

        tryOnlineOrServerCache(url, successListener, errorListener, mMeteredRequest);
    }

    @Override
    public void postOccupancy(@NonNull IrailPostOccupancyRequest... requests) {
        new IrailApi(mContext).postOccupancy(requests);
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
            meteredRequest.setResponseType(RESPONSE_ONLINE);
            if (requestQueue.getCache().get(jsObjRequest.getCacheKey()) != null && !requestQueue.getCache().get(jsObjRequest.getCacheKey()).isExpired()) {
                meteredRequest.setResponseType(RESPONSE_CACHED);
                try {
                    successListener.onResponse(new JSONObject(new String(requestQueue.getCache().get(jsObjRequest.getCacheKey()).data)));
                    return;
                } catch (JSONException e) {
                    Log.e(LOGTAG, "Failed to return result from cache", e);
                    meteredRequest.setResponseType(RESPONSE_ONLINE);
                    requestQueue.add(jsObjRequest);
                }
            }
            requestQueue.add(jsObjRequest);
        } else {
            Log.d(LOGTAG, "Trying to get data without internet");
            if (requestQueue.getCache().get(jsObjRequest.getCacheKey()) != null) {
                try {
                    JSONObject cache;
                    cache = new JSONObject(new String(requestQueue.getCache().get(jsObjRequest.getCacheKey()).data));
                    meteredRequest.setResponseType(RESPONSE_OFFLINE);
                    successListener.onResponse(cache);
                } catch (JSONException e) {
                    Crashlytics.log(
                            WARNING.intValue(), "Failed to get result from cache", e.getMessage());
                    errorListener.onErrorResponse(new NoConnectionError());
                    meteredRequest.setResponseType(RESPONSE_FAILED);
                }

            } else {
                Log.d(LOGTAG, "No cache available");
                errorListener.onErrorResponse(new NoConnectionError());
                meteredRequest.setResponseType(RESPONSE_FAILED);
            }
        }
    }

    @Override
    public MeteredRequest[] getMeteredRequests() {
        MeteredRequest[] meteredRequests = new MeteredRequest[mMeteredRequests.size()];
        return mMeteredRequests.toArray(meteredRequests);
    }
}
