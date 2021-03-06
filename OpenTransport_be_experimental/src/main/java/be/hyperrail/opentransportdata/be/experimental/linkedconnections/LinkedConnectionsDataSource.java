package be.hyperrail.opentransportdata.be.experimental.linkedconnections;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import be.hyperrail.opentransportdata.be.irail.IrailApi;
import be.hyperrail.opentransportdata.common.contracts.MeteredDataSource;
import be.hyperrail.opentransportdata.common.contracts.QueryTimeDefinition;
import be.hyperrail.opentransportdata.common.contracts.TransportDataErrorResponseListener;
import be.hyperrail.opentransportdata.common.contracts.TransportDataSource;
import be.hyperrail.opentransportdata.common.contracts.TransportStopsDataSource;
import be.hyperrail.opentransportdata.common.models.LiveboardType;
import be.hyperrail.opentransportdata.common.models.Route;
import be.hyperrail.opentransportdata.common.models.VehicleStop;
import be.hyperrail.opentransportdata.common.models.VehicleStopType;
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

/**
 * This API loads linkedConnection data and builds responses based on this data.
 * This API is experimental and limited/hardcoded for Belgium.
 */
public class LinkedConnectionsDataSource implements TransportDataSource, MeteredDataSource {

    private static final String LOGTAG = "LinkedConnectionsDS";

    private final TransportStopsDataSource mStationsProvider;
    private final LinkedConnectionsProvider mLinkedConnectionsProvider;
    private final ConnectivityManager mConnectivityManager;
    private List<MeteredRequest> mMeteredRequests = new ArrayList<>();

    private Context mContext;

    public LinkedConnectionsDataSource(Context context, TransportStopsDataSource stationsProvider) {
        this.mContext = context;
        this.mStationsProvider = stationsProvider;
        this.mLinkedConnectionsProvider = new LinkedConnectionsProvider(context);
        mConnectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        new PreloadPagesTask(this).execute();
    }

    public static String basename(String url) {
        return url.substring(url.lastIndexOf('/') + 1);
    }

    @Override
    public void getActualDisturbances(@NonNull ActualDisturbancesRequest... request) {
        // Fallback to the legacy API
        IrailApi api = new IrailApi(mContext, mStationsProvider);
        api.getActualDisturbances(request);
    }

    @Override
    public void getLiveboard(@NonNull LiveboardRequest... requests) {
        for (LiveboardRequest request :
                requests) {
            getLiveboard(request);
        }
    }

    private void getLiveboard(@NonNull final LiveboardRequest request) {
        StartLiveboardRequestTask task = new StartLiveboardRequestTask(this);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, request);
    }

    @Override
    public void extendLiveboard(@NonNull ExtendLiveboardRequest... requests) {
        for (final ExtendLiveboardRequest request :
                requests) {
            new ExtendLiveboardTask(this).execute(request);
        }
    }

    @Override
    public void getRoutePlanning(@NonNull RoutePlanningRequest... requests) {
        // TODO: switch to API specific code
        for (RoutePlanningRequest request :
                requests) {
            getRoutes(request);
        }
    }

    @Override
    public void extendRoutePlanning(@NonNull ExtendRoutePlanningRequest... requests) {
        for (ExtendRoutePlanningRequest request :
                requests) {
            new ExtendRoutesTask(this).execute(request);
        }
    }

    private void getRoutes(@NonNull final RoutePlanningRequest request) {
        new StartRouteRequestTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, request);
    }

    @Override
    public void getRoute(@NonNull RouteRefreshRequest... requests) {
        for (RouteRefreshRequest request :
                requests) {
            getRoute(request);
        }
    }

    private void getRoute(@NonNull final RouteRefreshRequest request) {
        RoutePlanningRequest routesRequest = new RoutePlanningRequest(
                request.getOrigin(), request.getDestination(), request.getTimeDefinition(),
                request.getSearchTime()
        );

        // Create a new routerequest. A successful response will be iterated to find a matching route. An unsuccessful query will cause the original error handler to be called.
        routesRequest.setCallback(
                (data, tag) -> {
                    for (Route r : data.getRoutes()) {
                        if (r.getDeparture().getDepartureSemanticId() != null &&
                                r.getDeparture().getDepartureSemanticId().equals(request.getDepartureSemanticId())) {
                            request.notifySuccessListeners(r);
                        }
                    }
                },
                (e, tag) -> request.notifyErrorListeners(e), request.getTag());

        getRoutes(routesRequest);

    }

    @Override
    public void getStop(@NonNull VehicleStopRequest... requests) {
        for (VehicleStopRequest request :
                requests) {
            getStop(request);
        }
    }

    private void getStop(@NonNull final VehicleStopRequest request) {
        LiveboardRequest liveboardRequest;
        if (request.getStop().getType() == VehicleStopType.DEPARTURE || request.getStop().getType() == VehicleStopType.STOP) {
            liveboardRequest = new LiveboardRequest(request.getStop().getStopLocation(), QueryTimeDefinition.EQUAL_OR_LATER, LiveboardType.DEPARTURES, request.getStop().getDepartureTime());
        } else {
            liveboardRequest = new LiveboardRequest(request.getStop().getStopLocation(), QueryTimeDefinition.EQUAL_OR_EARLIER, LiveboardType.ARRIVALS, request.getStop().getArrivalTime());
        }
        liveboardRequest.setCallback((data, tag) -> {
            for (VehicleStop stop :
                    data.getStops()) {
                if (stop.getDepartureUri().equals(request.getStop().getDepartureUri())) {
                    request.notifySuccessListeners(stop);
                    return;
                }
            }
        }, request.getOnErrorListener(), null);
        getLiveboard(liveboardRequest);
    }

    @Override
    public void getVehicleJourney(@NonNull VehicleRequest... requests) {
        for (VehicleRequest request :
                requests) {
            getVehicle(request);
        }
    }

    @Override
    public void getVehicleComposition(VehicleCompositionRequest... requests) {
        throw new UnsupportedOperationException("Not supported");
    }

    private void getVehicle(@NonNull final VehicleRequest request) {
        StartVehicleRequestTask StartVehicleRequestTask = new StartVehicleRequestTask(this);
        StartVehicleRequestTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, request);
    }

    @Override
    public void postOccupancy(@NonNull OccupancyPostRequest... request) {
        // Fallback to the legacy API
        IrailApi api = new IrailApi(mContext, mStationsProvider);
        api.postOccupancy(request);
    }

    @Override
    public void abortQueries(RequestType type) {
        // Nothing to do
    }

    @Override
    public MeteredRequest[] getMeteredRequests() {
        MeteredRequest[] meteredRequests = new MeteredRequest[mMeteredRequests.size()];
        return mMeteredRequests.toArray(meteredRequests);
    }

    public void setCacheEnabled(boolean enabled) {
        mLinkedConnectionsProvider.setCacheEnabled(enabled);
    }

    private boolean isInternetAvailable() {
        NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }


    static class StartVehicleRequestTask extends AsyncTask<VehicleRequest, Void, Void> {

        private final WeakReference<LinkedConnectionsDataSource> mApi;

        StartVehicleRequestTask(LinkedConnectionsDataSource api) {
            mApi = new WeakReference<>(api);
        }

        @Override
        protected Void doInBackground(VehicleRequest... requests) {

            if (mApi.get() == null) {
                return null;
            }
            LinkedConnectionsDataSource api = mApi.get();

            VehicleRequest request = requests[0];
            MeteredRequest meteredRequest = new MeteredRequest();
            meteredRequest.setTag(request.toString());
            meteredRequest.setMsecStart(DateTime.now().getMillis());
            api.mMeteredRequests.add(meteredRequest);

            VehicleResponseListener listener = new VehicleResponseListener(request, api.mStationsProvider);
            VehicleQueryResponseListener query = new VehicleQueryResponseListener("http://irail.be/vehicle/" + request.getVehicleId(), listener, listener, meteredRequest);

            DateTime departureTime = request.getSearchTime().withTimeAtStartOfDay().withHourOfDay(3);

            Log.d(LOGTAG, "Departure time from index for " + request.getVehicleId() + " is " + departureTime.toString(ISODateTimeFormat.basicDateTimeNoMillis()));

            api.mLinkedConnectionsProvider.queryLinkedConnections(departureTime, query, meteredRequest);
            return null;
        }
    }

    static class StartLiveboardRequestTask extends AsyncTask<LiveboardRequest, Void, Void> {

        private final WeakReference<LinkedConnectionsDataSource> mApi;

        StartLiveboardRequestTask(LinkedConnectionsDataSource api) {
            mApi = new WeakReference<>(api);
        }

        @Override
        protected Void doInBackground(LiveboardRequest... requests) {

            if (mApi.get() == null) {
                return null;
            }
            LinkedConnectionsDataSource api = mApi.get();
            LiveboardRequest request = requests[0];
            MeteredRequest meteredRequest = new MeteredRequest();
            meteredRequest.setTag(request.toString());
            meteredRequest.setMsecStart(DateTime.now().getMillis());
            api.mMeteredRequests.add(meteredRequest);

            LiveboardResponseListener listener = new LiveboardResponseListener(api.mLinkedConnectionsProvider, api.mStationsProvider, request);
            api.mLinkedConnectionsProvider.getLinkedConnectionsByDate(request.getSearchTime(),
                    listener,
                    listener,
                    meteredRequest);
            return null;
        }
    }

    static class StartRouteRequestTask extends AsyncTask<RoutePlanningRequest, Void, Void> {

        private final WeakReference<LinkedConnectionsDataSource> mApi;

        StartRouteRequestTask(LinkedConnectionsDataSource api) {
            mApi = new WeakReference<>(api);
        }

        @Override
        protected Void doInBackground(RoutePlanningRequest... requests) {

            if (mApi.get() == null) {
                return null;
            }

            final LinkedConnectionsDataSource api = mApi.get();
            final RoutePlanningRequest request = requests[0];
            final MeteredRequest meteredRequest = new MeteredRequest();
            meteredRequest.setTag(request.toString());
            meteredRequest.setMsecStart(DateTime.now().getMillis());
            api.mMeteredRequests.add(meteredRequest);

            DateTime departureLimit;

            if (request.getTimeDefinition() == QueryTimeDefinition.EQUAL_OR_LATER) {
                departureLimit = request.getSearchTime();
            } else {
                departureLimit = request.getSearchTime().minusHours(24);
            }

            final RouteResponseListener listener = new RouteResponseListener(api.mLinkedConnectionsProvider, api.mStationsProvider, request, departureLimit);

            if (request.getTimeDefinition() == QueryTimeDefinition.EQUAL_OR_LATER) {
                DateTime end = request.getSearchTime();
                if (end.getHourOfDay() < 18 && end.getHourOfDay() >= 6) {
                    end = request.getSearchTime().plusHours(4);
                } else {
                    end = request.getSearchTime().plusHours(6);
                }

                api.mLinkedConnectionsProvider.getLinkedConnectionsByDateForTimeSpan(request.getSearchTime(), end, listener, new TransportDataErrorResponseListener() {
                    @Override
                    public void onErrorResponse(@NonNull Exception e, Object tag) {
                        DateTime newEnd = request.getSearchTime().plusHours(3);
                        api.mLinkedConnectionsProvider.getLinkedConnectionsByDateForTimeSpan(request.getSearchTime(), newEnd, listener, new TransportDataErrorResponseListener() {
                            @Override
                            public void onErrorResponse(@NonNull Exception e, Object tag) {
                                DateTime newEnd = request.getSearchTime().plusHours(2);
                                api.mLinkedConnectionsProvider.getLinkedConnectionsByDateForTimeSpan(request.getSearchTime(), newEnd, listener, listener, meteredRequest);
                            }
                        }, meteredRequest);
                    }
                }, meteredRequest);
            } else {
                api.mLinkedConnectionsProvider.getLinkedConnectionsByDateForTimeSpan(request.getSearchTime().minusHours(4), request.getSearchTime(), listener, listener, meteredRequest);
            }
            return null;
        }
    }

    static class ExtendLiveboardTask extends AsyncTask<ExtendLiveboardRequest, Void, Void> {

        private final WeakReference<LinkedConnectionsDataSource> mApi;

        ExtendLiveboardTask(LinkedConnectionsDataSource api) {
            mApi = new WeakReference<>(api);
        }

        @Override
        protected Void doInBackground(ExtendLiveboardRequest... requests) {

            if (mApi.get() == null) {
                return null;
            }

            final LinkedConnectionsDataSource api = mApi.get();
            final ExtendLiveboardRequest request = requests[0];
            MeteredRequest meteredRequest = new MeteredRequest();
            meteredRequest.setTag(request.toString());
            meteredRequest.setMsecStart(DateTime.now().getMillis());
            api.mMeteredRequests.add(meteredRequest);

            LiveboardExtendHelper helper = new LiveboardExtendHelper(api.mLinkedConnectionsProvider, api.mStationsProvider, request, meteredRequest);
            helper.extend();
            return null;
        }
    }

    static class ExtendRoutesTask extends AsyncTask<ExtendRoutePlanningRequest, Void, Void> {

        private final WeakReference<LinkedConnectionsDataSource> mApi;

        ExtendRoutesTask(LinkedConnectionsDataSource api) {
            mApi = new WeakReference<>(api);
        }

        @Override
        protected Void doInBackground(ExtendRoutePlanningRequest... requests) {

            if (mApi.get() == null) {
                return null;
            }

            final LinkedConnectionsDataSource api = mApi.get();
            final ExtendRoutePlanningRequest request = requests[0];
            MeteredRequest meteredRequest = new MeteredRequest();
            meteredRequest.setTag(request.toString());
            meteredRequest.setMsecStart(DateTime.now().getMillis());
            api.mMeteredRequests.add(meteredRequest);

            RouteExtendHelper helper = new RouteExtendHelper(api.mLinkedConnectionsProvider, api.mStationsProvider, request, meteredRequest);
            helper.extend();
            return null;
        }
    }

    static class PreloadPagesTask extends AsyncTask<Void, Void, Void> {

        private final WeakReference<LinkedConnectionsDataSource> mApi;

        PreloadPagesTask(LinkedConnectionsDataSource api) {
            mApi = new WeakReference<>(api);
        }

        @Override
        protected Void doInBackground(Void... requests) {

            if (mApi.get() == null) {
                return null;
            }
            LinkedConnectionsDataSource api = mApi.get();
            MeteredRequest meteredRequest = new MeteredRequest();
            meteredRequest.setTag("Pre-load 60");
            meteredRequest.setMsecStart(DateTime.now().getMillis());
            api.mMeteredRequests.add(meteredRequest);

            api.mLinkedConnectionsProvider.getLinkedConnectionsByDateForTimeSpan(DateTime.now(), DateTime.now().plusMinutes(60), null, null, meteredRequest);
            return null;
        }
    }

}
