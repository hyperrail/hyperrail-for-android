/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.be.irail;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import be.hyperrail.opentransportdata.be.irail.util.AsyncJsonPostRequest;
import be.hyperrail.opentransportdata.be.irail.util.IrailHttpException;
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
import be.hyperrail.opentransportdata.common.models.implementation.VehicleCompositionImpl;
import be.hyperrail.opentransportdata.common.models.implementation.VehicleStopImpl;
import be.hyperrail.opentransportdata.common.requests.ActualDisturbancesRequest;
import be.hyperrail.opentransportdata.common.requests.ExtendLiveboardRequest;
import be.hyperrail.opentransportdata.common.requests.ExtendRoutePlanningRequest;
import be.hyperrail.opentransportdata.common.requests.LiveboardRequest;
import be.hyperrail.opentransportdata.common.requests.OccupancyPostRequest;
import be.hyperrail.opentransportdata.common.requests.RequestType;
import be.hyperrail.opentransportdata.common.requests.RoutePlanningRequest;
import be.hyperrail.opentransportdata.common.requests.RouteRefreshRequest;
import be.hyperrail.opentransportdata.common.requests.VehicleCompositionRequest;
import be.hyperrail.opentransportdata.common.requests.VehicleRequest;
import be.hyperrail.opentransportdata.common.requests.VehicleStopRequest;
import be.hyperrail.opentransportdata.logging.OpenTransportLog;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Synchronous API for api.irail.be
 *
 * @inheritDoc
 */
public class IrailApi implements TransportDataSource {

    private static final String BASE_URL = "https://api.irail.be";
    private static final String USER_AGENT = "OpenTransportData for Android";
    private static final OpenTransportLog log = OpenTransportLog.getLogger(IrailApi.class);
    private final OkHttpClient client;
    private final Context context;
    private final IrailApiParser parser;
    private final VehicleCompositionParser vehicleCompositionParser = new VehicleCompositionParser();

    public IrailApi(Context context, TransportStopsDataSource stationProviderInstance) {
        this.context = context;
        this.parser = new IrailApiParser(stationProviderInstance);
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.retryOnConnectionFailure(true)
                .addInterceptor(new ForceCacheInterceptor((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)))
                .connectTimeout(3, TimeUnit.SECONDS)
                .callTimeout(20, TimeUnit.SECONDS)
                .followRedirects(true);
        this.client = new OkHttpClient(builder);
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
            IrailRouteExtendHelper helper = new IrailRouteExtendHelper();
            helper.extendRoutesRequest(request);
        }
    }

    public void getRoutes(RoutePlanningRequest request) {
        // https://api.irail.be/connections/?to=Halle&from=Brussels-south&date={dmy}&time=2359&timeSel=arrive or depart&format=json

        DateTimeFormatter dateformat = DateTimeFormat.forPattern("ddMMyy");
        DateTimeFormatter timeformat = DateTimeFormat.forPattern("HHmm");

        String locale = PreferenceManager.getDefaultSharedPreferences(context).getString(
                "pref_stations_language", "");
        if (locale.isEmpty()) {
            // Only get locale when needed
            locale = Locale.getDefault().getISO3Language();
        }

        String url = BASE_URL + "/connections/?format=json"
                + "&to=" + request.getDestination().getHafasId()
                + "&from=" + request.getOrigin().getHafasId()
                + "&date=" + dateformat.print(request.getSearchTime())
                + "&time=" + timeformat.print(request.getSearchTime().withZone(DateTimeZone.forID("Europe/Brussels")))
                + "&lang=" + locale.substring(0, 2);

        if (request.getTimeDefinition() == QueryTimeDefinition.EQUAL_OR_LATER) {
            url += "&timeSel=depart";
        } else {
            url += "&timeSel=arrive";
        }
        log.info("Fetching connections from " + url);


        Callback callback = new MainthreadCallback() {
            public void onResponse(Call call, Response response) throws IOException {
                RoutesListImpl routeResult;
                try {
                    String body = response.body().string();
                    JSONObject json = new JSONObject(body);
                    if (response.networkResponse().code() >= 400) {
                        IrailHttpException irailHttpException = new IrailHttpException(response.networkResponse().code(), json);
                        onUiThread(context, () -> request.notifyErrorListeners(irailHttpException));
                        return;
                    }
                    routeResult = parser.parseRouteResult(
                            json, request.getOrigin(), request.getDestination(),
                            request.getSearchTime(), request.getTimeDefinition()
                    );
                } catch (JSONException e) {
                    log.warning("Failed to parse routes: " + e.getMessage(), e);
                    onUiThread(context, () -> request.notifyErrorListeners(e));
                    return;
                }
                onUiThread(context, () -> request.notifySuccessListeners(routeResult));
            }

            public void onFailure(Call call, IOException e) {
                log.warning("Failed to get routes: " + e.getMessage());
                onUiThread(context, () -> request.notifyErrorListeners(e));
            }
        };
        tryOnlineOrServerCache(url, callback, request.getRequestTypeTag());
    }

    @Override
    public void getLiveboard(LiveboardRequest... requests) {
        for (LiveboardRequest request : requests) {
            if (request.getTimeDefinition() == QueryTimeDefinition.EQUAL_OR_LATER) {
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
            IrailLiveboardExtendHelper helper = new IrailLiveboardExtendHelper();
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
                            data.getSearchTime(), data.getLiveboardType(), QueryTimeDefinition.EQUAL_OR_EARLIER
                    ));
        }, (e, tag) -> request.notifyErrorListeners(e), actualRequest.getTag());
        getLiveboardAfter(request);
    }

    private void getLiveboardAfter(LiveboardRequest request) {
        // https://api.irail.be/liveboard/?station=Halle&fast=true

        // suppress errors, this formatting is for an API call
        DateTimeFormatter dateformat = DateTimeFormat.forPattern("ddMMyy");
        DateTimeFormatter timeformat = DateTimeFormat.forPattern("HHmm");

        String url = BASE_URL + "/liveboard/?format=json"
                + "&id=" + request.getStation().getHafasId()
                + "&date=" + dateformat.print(request.getSearchTime())
                + "&time=" + timeformat.print(request.getSearchTime().withZone(DateTimeZone.forID("Europe/Brussels")))
                + "&arrdep=" + ((request.getType() == LiveboardType.DEPARTURES) ? "dep" : "arr");
        log.info("Fetching liveboard from " + url);


        Callback callback = new MainthreadCallback() {
            public void onResponse(Call call, Response response) throws IOException {
                LiveboardImpl result;
                try {
                    String body = response.body().string();
                    JSONObject json = new JSONObject(body);
                    if (response.networkResponse().code() >= 400) {
                        IrailHttpException irailHttpException = new IrailHttpException(response.networkResponse().code(), json);
                        onUiThread(context, () -> request.notifyErrorListeners(irailHttpException));
                        return;
                    }
                    result = parser.parseLiveboard(json, request.getSearchTime(), request.getType(), request.getTimeDefinition());
                } catch (JSONException | StopLocationNotResolvedException e) {
                    log.warning("Failed to parse liveboard: " + e.getMessage(), e);
                    onUiThread(context, () -> request.notifyErrorListeners(e));
                    return;
                }

                onUiThread(context, () -> request.notifySuccessListeners(result));
            }

            public void onFailure(Call call, IOException e) {
                log.warning("Tried loading liveboard from " + url + " failed with error " + e, e);
                onUiThread(context, () -> request.notifyErrorListeners(e));
            }
        };

        tryOnlineOrServerCache(url, callback, request.getRequestTypeTag());
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

        String url = BASE_URL + "/vehicle/?format=json"
                + "&id=" + request.getVehicleId() + "&date=" + dateTimeformat.print(
                request.getSearchTime());
        log.info("Fetching vehicle route from " + url);

        Callback callback = new MainthreadCallback() {
            public void onResponse(Call call, Response response) throws IOException {
                IrailVehicleJourney result;
                try {
                    String body = response.body().string();
                    JSONObject json = new JSONObject(body);
                    if (response.networkResponse().code() >= 400) {
                        IrailHttpException irailHttpException = new IrailHttpException(response.networkResponse().code(), json);
                        onUiThread(context, () -> request.notifyErrorListeners(irailHttpException));
                        return;
                    }
                    result = parser.parseVehicleJourney(json);
                } catch (JSONException | StopLocationNotResolvedException e) {
                    log.warning("Failed to parse vehicle: " + e.getMessage(), e);
                    onUiThread(context, () -> request.notifyErrorListeners(e));
                    return;
                }
                onUiThread(context, () -> request.notifySuccessListeners(result));
            }

            public void onFailure(Call call, IOException e) {
                log.warning("Failed to get vehicle:" + e.getMessage());
                onUiThread(context, () -> request.notifyErrorListeners(e));
            }
        };

        tryOnlineOrServerCache(url, callback, request.getRequestTypeTag());
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

        if (locale.isEmpty()) {
            // Only get locale when needed
            locale = Locale.getDefault().getISO3Language();
        }

        String url = BASE_URL + "/disturbances/?format=json&lineBreakCharacter=<br>&lang=" + locale.substring(0, 2);
        log.info("Fetching disturbances from " + url);

        Callback callback = new MainthreadCallback() {
            public void onResponse(Call call, Response response) throws IOException {
                Disturbance[] result;
                try {
                    String body = response.body().string();
                    JSONObject json = new JSONObject(body);
                    if (response.networkResponse().code() >= 400) {
                        IrailHttpException irailHttpException = new IrailHttpException(response.networkResponse().code(), json);
                        onUiThread(context, () -> request.notifyErrorListeners(irailHttpException));
                        return;
                    }
                    result = parser.parseDisturbances(json);
                } catch (JSONException e) {
                    log.warning("Failed to parse disturbances: " + e.getMessage(), e);
                    onUiThread(context, () -> request.notifyErrorListeners(e));
                    return;
                }
                onUiThread(context, () -> request.notifySuccessListeners(result));
            }

            public void onFailure(Call call, IOException e) {
                log.warning("Failed to get disturbances: " + e.getMessage());
                onUiThread(context, () -> request.notifyErrorListeners(e));
            }
        };

        tryOnlineOrServerCache(url, callback, request.getRequestTypeTag());
    }


    @Override
    public void getVehicleComposition(VehicleCompositionRequest... requests) {
        for (VehicleCompositionRequest request :
                requests) {
            getVehicleComposition(request);
        }
    }

    public void getVehicleComposition(VehicleCompositionRequest request) {
        String url = BASE_URL + "/composition/?format=json"
                + "&id=" + request.getVehicleId();
        log.info("Fetching vehicle composition from " + url);

        Callback callback = new MainthreadCallback() {
            public void onResponse(Call call, Response response) {
                VehicleCompositionImpl result;
                try {
                    String body = response.body().string();
                    JSONObject json = new JSONObject(body);
                    if (response.networkResponse().code() >= 400) {
                        IrailHttpException irailHttpException = new IrailHttpException(response.networkResponse().code(), json);
                        onUiThread(context, () -> request.notifyErrorListeners(irailHttpException));
                        return;
                    }
                    result = vehicleCompositionParser.parseVehicleComposition(context, json, request.getVehicleId());
                } catch (JSONException | IOException e) {
                    log.warning("Failed to parse vehicle composition: " + e.getMessage(), e);
                    onUiThread(context, () -> request.notifyErrorListeners(e));
                    return;
                }

                onUiThread(context, () -> request.notifySuccessListeners(result));
            }

            public void onFailure(Call call, IOException e) {
                log.warning("Tried loading vehicle composition from " + url + " failed with error " + e, e);
                onUiThread(context, () -> request.notifyErrorListeners(e));
            }
        };

        tryOnlineOrServerCache(url, callback, request.getRequestTypeTag());
    }

    private void tryOnlineOrServerCache(String url, Callback callback, int tag) {
        log.info("Making request to iRail API at " + url);
        Request httpRequest = new Request.Builder()
                .url(url)
                .tag(tag)
                .headers(Headers.of("User-agent", USER_AGENT))
                .build();


        Call call = client.newCall(httpRequest);
        call.enqueue(callback);
    }

    @Override
    public void postOccupancy(OccupancyPostRequest... requests) {
        for (OccupancyPostRequest request :
                requests) {
            postOccupancy(request);
        }
    }


    private void postOccupancy(OccupancyPostRequest request) {

        String url = BASE_URL + "/feedback/occupancy.php";

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

            AsyncJsonPostRequest.postRequestAsync(url, request, payload);
        } catch (Exception e) {
            request.notifyErrorListeners(e);
        }
    }

    @Override
    public void abortQueries(RequestType type) {
        log.info("Aborting all queries for type " + type);
        for (Call call : this.client.dispatcher().queuedCalls()) {
            if (call.request().tag() == type) {
                call.cancel();
            }
        }
        for (Call call : this.client.dispatcher().runningCalls()) {
            if (call.request().tag() == type) {
                call.cancel();
            }
        }
    }

    private static class ForceCacheInterceptor implements Interceptor {
        private final ConnectivityManager connectivityManager;

        public ForceCacheInterceptor(ConnectivityManager connectivityManager) {
            this.connectivityManager = connectivityManager;
        }

        private boolean isInternetAvailable() {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null &&
                    activeNetwork.isConnectedOrConnecting();
        }

        public Response intercept(Interceptor.Chain chain) throws IOException {
            Request.Builder builder = chain.request().newBuilder();
            if (!isInternetAvailable()) {
                builder.cacheControl(CacheControl.FORCE_CACHE);
            }
            return chain.proceed(builder.build());
        }
    }

    private abstract static class MainthreadCallback implements Callback {

        @Override
        public abstract void onFailure(@NonNull Call call, @NonNull IOException e);

        @Override
        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

        }

        public void onUiThread(Context context, Runnable mtRunnable) {
            Handler mainHandler = new Handler(context.getMainLooper());
            mainHandler.post(mtRunnable);
        }
    }
}
