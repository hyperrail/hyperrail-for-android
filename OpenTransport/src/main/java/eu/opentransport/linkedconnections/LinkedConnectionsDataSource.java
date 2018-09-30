package eu.opentransport.linkedconnections;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import eu.opentransport.OpenTransportApi;
import eu.opentransport.R;
import eu.opentransport.common.contracts.MeteredDataSource;
import eu.opentransport.common.contracts.QueryTimeDefinition;
import eu.opentransport.common.contracts.TransportDataErrorResponseListener;
import eu.opentransport.common.contracts.TransportDataSource;
import eu.opentransport.common.contracts.TransportDataSuccessResponseListener;
import eu.opentransport.common.contracts.TransportStopsDataSource;
import eu.opentransport.common.models.Route;
import eu.opentransport.common.models.VehicleStopType;
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
import eu.opentransport.irail.IrailRoutesList;
import eu.opentransport.irail.IrailVehicleStop;

import static eu.opentransport.irail.IrailLiveboard.LiveboardType.ARRIVALS;
import static eu.opentransport.irail.IrailLiveboard.LiveboardType.DEPARTURES;

/**
 * This API loads linkedConnection data and builds responses based on this data
 */
public class LinkedConnectionsDataSource implements TransportDataSource, MeteredDataSource {

    private final TransportStopsDataSource mStationsProvider;
    private final LinkedConnectionsProvider mLinkedConnectionsProvider;
    private final ConnectivityManager mConnectivityManager;
    private Context mContext;
    private static final String LOGTAG = "LinkedConnectionsDataSource";
    List<MeteredRequest> mMeteredRequests = new ArrayList<>();

    public LinkedConnectionsDataSource(Context context) {
        this.mContext = context;
        this.mStationsProvider = OpenTransportApi.getStationsProviderInstance();
        this.mLinkedConnectionsProvider = new LinkedConnectionsProvider(context);
        mConnectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        new PreloadPagesTask(this).execute();
    }


    @Override
    public void getDisturbances(@NonNull IrailDisturbanceRequest... request) {
        // Fallback to the legacy API
        IrailApi api = new IrailApi(mContext);
        api.getDisturbances(request);
    }

    @Override
    public void getLiveboard(@NonNull IrailLiveboardRequest... requests) {
        for (IrailLiveboardRequest request :
                requests) {
            getLiveboard(request);
        }
    }

    private void getLiveboard(@NonNull final IrailLiveboardRequest request) {
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
    public void getRoutes(@NonNull IrailRoutesRequest... requests) {
        // TODO: switch to API specific code
        for (IrailRoutesRequest request :
                requests) {
            getRoutes(request);
        }
    }

    @Override
    public void extendRoutes(@NonNull ExtendRoutesRequest... requests) {
        for (ExtendRoutesRequest request :
                requests) {
            new ExtendRoutesTask(this).execute(request);
        }
    }

    private void getRoutes(@NonNull final IrailRoutesRequest request) {
        new StartRouteRequestTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, request);
    }

    @Override
    public void getRoute(@NonNull IrailRouteRequest... requests) {
        for (IrailRouteRequest request :
                requests) {
            getRoute(request);
        }
    }

    private void getRoute(@NonNull final IrailRouteRequest request) {
        IrailRoutesRequest routesRequest = new IrailRoutesRequest(
                request.getOrigin(), request.getDestination(), request.getTimeDefinition(),
                request.getSearchTime()
        );

        // Create a new routerequest. A successful response will be iterated to find a matching route. An unsuccessful query will cause the original error handler to be called.
        routesRequest.setCallback(new TransportDataSuccessResponseListener<IrailRoutesList>() {
            @Override
            public void onSuccessResponse(@NonNull IrailRoutesList data, Object tag) {
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

    @Override
    public void getStop(@NonNull VehicleStopRequest... requests) {
        for (VehicleStopRequest request :
                requests) {
            getStop(request);
        }
    }

    private void getStop(@NonNull final VehicleStopRequest request) {
        IrailLiveboardRequest liveboardRequest;
        if (request.getStop().getType() == VehicleStopType.DEPARTURE || request.getStop().getType() == VehicleStopType.STOP) {
            liveboardRequest = new IrailLiveboardRequest(request.getStop().getStation(), QueryTimeDefinition.DEPART_AT, DEPARTURES, request.getStop().getDepartureTime());
        } else {
            liveboardRequest = new IrailLiveboardRequest(request.getStop().getStation(), QueryTimeDefinition.ARRIVE_AT, ARRIVALS, request.getStop().getArrivalTime());
        }
        liveboardRequest.setCallback(new TransportDataSuccessResponseListener<IrailLiveboard>() {
            @Override
            public void onSuccessResponse(@NonNull IrailLiveboard data, Object tag) {
                for (IrailVehicleStop stop :
                        data.getStops()) {
                    if (stop.getDepartureUri().equals(request.getStop().getDepartureUri())) {
                        request.notifySuccessListeners(stop);
                        return;
                    }
                }
            }
        }, request.getOnErrorListener(), null);
        getLiveboard(liveboardRequest);
    }

    @Override
    public void getVehicle(@NonNull IrailVehicleRequest... requests) {
        for (IrailVehicleRequest request :
                requests) {
            getVehicle(request);
        }
    }

    private void getVehicle(@NonNull final IrailVehicleRequest request) {
        StartVehicleRequestTask StartVehicleRequestTask = new StartVehicleRequestTask(this);
        StartVehicleRequestTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, request);
    }

    @Override
    public void postOccupancy(@NonNull IrailPostOccupancyRequest... request) {
        // Fallback to the legacy API
        IrailApi api = new IrailApi(mContext);
        api.postOccupancy(request);
    }

    @Override
    public void abortAllQueries() {

    }

    public static String basename(String url) {
        return url.substring(url.lastIndexOf('/') + 1);
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


    static class StartVehicleRequestTask extends AsyncTask<IrailVehicleRequest, Void, Void> {

        private final WeakReference<LinkedConnectionsDataSource> mApi;

        StartVehicleRequestTask(LinkedConnectionsDataSource api) {
            mApi = new WeakReference<>(api);
        }


        ArrayList<String> trainDeparturesIndex;

        @Override
        protected Void doInBackground(IrailVehicleRequest... requests) {

            if (mApi.get() == null) {
                return null;
            }
            LinkedConnectionsDataSource api = mApi.get();

            IrailVehicleRequest request = requests[0];
            MeteredRequest meteredRequest = new MeteredRequest();
            meteredRequest.setTag(request.toString());
            meteredRequest.setMsecStart(DateTime.now().getMillis());
            api.mMeteredRequests.add(meteredRequest);

            VehicleResponseListener listener = new VehicleResponseListener(request, api.mStationsProvider);
            VehicleQueryResponseListener query = new VehicleQueryResponseListener("http://irail.be/vehicle/" + request.getVehicleId(), listener, listener, meteredRequest);

            DateTime departureTime = request.getSearchTime().withTimeAtStartOfDay().withHourOfDay(3);
            if (trainDeparturesIndex == null) {
                trainDeparturesIndex = new ArrayList<>();
                try (InputStream in = api.mContext.getResources().openRawResource(R.raw.firstdepartures)) {
                    java.util.Scanner s = new java.util.Scanner(in).useDelimiter("\\A");
                    while (s.hasNextLine()) trainDeparturesIndex.add(s.nextLine());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            for (String departure : trainDeparturesIndex) {
                if (departure.split(" ")[0].equals(request.getVehicleId())) {
                    departureTime = request.getSearchTime().withTimeAtStartOfDay()
                            .withHourOfDay(Integer.valueOf(departure.split(" ")[1]))
                            .withMinuteOfHour(Integer.valueOf(departure.split(" ")[2]));
                    break;
                }
            }

            Log.d("LinkedConnectionsDS", "Departure time from index for " + request.getVehicleId() + " is " + departureTime.toString(ISODateTimeFormat.basicDateTimeNoMillis()));

            api.mLinkedConnectionsProvider.queryLinkedConnections(departureTime, query, meteredRequest);
            return null;
        }
    }

    static class StartLiveboardRequestTask extends AsyncTask<IrailLiveboardRequest, Void, Void> {

        private final WeakReference<LinkedConnectionsDataSource> mApi;

        StartLiveboardRequestTask(LinkedConnectionsDataSource api) {
            mApi = new WeakReference<>(api);
        }

        @Override
        protected Void doInBackground(IrailLiveboardRequest... requests) {

            if (mApi.get() == null) {
                return null;
            }
            LinkedConnectionsDataSource api = mApi.get();
            IrailLiveboardRequest request = requests[0];
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

    static class StartRouteRequestTask extends AsyncTask<IrailRoutesRequest, Void, Void> {

        private final WeakReference<LinkedConnectionsDataSource> mApi;

        StartRouteRequestTask(LinkedConnectionsDataSource api) {
            mApi = new WeakReference<>(api);
        }

        @Override
        protected Void doInBackground(IrailRoutesRequest... requests) {

            if (mApi.get() == null) {
                return null;
            }

            final LinkedConnectionsDataSource api = mApi.get();
            final IrailRoutesRequest request = requests[0];
            final MeteredRequest meteredRequest = new MeteredRequest();
            meteredRequest.setTag(request.toString());
            meteredRequest.setMsecStart(DateTime.now().getMillis());
            api.mMeteredRequests.add(meteredRequest);

            DateTime departureLimit;

            if (request.getTimeDefinition() == QueryTimeDefinition.DEPART_AT) {
                departureLimit = request.getSearchTime();
            } else {
                departureLimit = request.getSearchTime().minusHours(24);
            }

            final RouteResponseListener listener = new RouteResponseListener(api.mLinkedConnectionsProvider, api.mStationsProvider, request, departureLimit);

            if (request.getTimeDefinition() == QueryTimeDefinition.DEPART_AT) {
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

    static class ExtendRoutesTask extends AsyncTask<ExtendRoutesRequest, Void, Void> {

        private final WeakReference<LinkedConnectionsDataSource> mApi;

        ExtendRoutesTask(LinkedConnectionsDataSource api) {
            mApi = new WeakReference<>(api);
        }

        @Override
        protected Void doInBackground(ExtendRoutesRequest... requests) {

            if (mApi.get() == null) {
                return null;
            }

            final LinkedConnectionsDataSource api = mApi.get();
            final ExtendRoutesRequest request = requests[0];
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
