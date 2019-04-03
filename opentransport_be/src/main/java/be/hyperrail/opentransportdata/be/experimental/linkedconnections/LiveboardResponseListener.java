/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.be.experimental.linkedconnections;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.perf.metrics.AddTrace;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;

import be.hyperrail.opentransportdata.OpenTransportApi;
import be.hyperrail.opentransportdata.be.irail.IrailVehicleJourneyStub;
import be.hyperrail.opentransportdata.common.contracts.MeteredDataSource;
import be.hyperrail.opentransportdata.common.contracts.MeteredDataSource.MeteredRequest;
import be.hyperrail.opentransportdata.common.contracts.QueryTimeDefinition;
import be.hyperrail.opentransportdata.common.contracts.TransportDataErrorResponseListener;
import be.hyperrail.opentransportdata.common.contracts.TransportDataSuccessResponseListener;
import be.hyperrail.opentransportdata.common.contracts.TransportOccupancyLevel;
import be.hyperrail.opentransportdata.common.contracts.TransportStopsDataSource;
import be.hyperrail.opentransportdata.common.models.LiveboardType;
import be.hyperrail.opentransportdata.common.models.StopLocation;
import be.hyperrail.opentransportdata.common.models.VehicleStopType;
import be.hyperrail.opentransportdata.common.models.implementation.LiveboardImpl;
import be.hyperrail.opentransportdata.common.models.implementation.StringPagePointer;
import be.hyperrail.opentransportdata.common.models.implementation.VehicleStopImpl;
import be.hyperrail.opentransportdata.common.requests.LiveboardRequest;

import static be.hyperrail.opentransportdata.be.experimental.linkedconnections.LinkedConnectionsDataSource.basename;

/**
 * A listener which receives graph.irail.be data and builds a liveboard for 2 hours.
 */
public class LiveboardResponseListener implements TransportDataSuccessResponseListener<LinkedConnections>, TransportDataErrorResponseListener {
    private final ArrayList<LinkedConnection> arrivals = new ArrayList<>();
    private final ArrayList<LinkedConnection> departures = new ArrayList<>();
    private final ArrayList<VehicleStopImpl> stops = new ArrayList<>();

    // Both departures and arrivals are in chronological order. We'll search to see if we can find a departure which matches an arrival, but only start looking AFTER this arrival.
    private final ArrayList<Integer> departureIndexForArrivals = new ArrayList<>();
    private final LinkedConnectionsProvider mLinkedConnectionsProvider;
    private final TransportStopsDataSource mStationProvider;
    private LiveboardRequest request;

    private String previous;
    private String current;
    private String next;

    private int pages = 0;

    public LiveboardResponseListener(LinkedConnectionsProvider linkedConnectionsProvider, TransportStopsDataSource stationProvider, LiveboardRequest request) {
        mLinkedConnectionsProvider = linkedConnectionsProvider;
        mStationProvider = stationProvider;
        this.request = request;
    }

    @Override
    @AddTrace(name = "LiveboardResponseListener.onSuccess")
    public void onSuccessResponse(@NonNull LinkedConnections data, Object tag) {

        ((MeteredRequest) tag).setMsecUsableNetworkResponse(DateTime.now().getMillis());

        if (current == null) {
            previous = data.previous;
            current = data.current;
            next = data.next;
        }

        if (request.getTimeDefinition() == QueryTimeDefinition.EQUAL_OR_LATER) {
            // Moving forward through pages
            next = data.next;
        } else {
            // Moving backward through pages
            previous = data.previous;
        }

        for (LinkedConnection connection : data.connections) {
            if (!connection.isNormal()) {
                continue;
            }

            if (connection.getDepartureStationUri().equals(request.getStation().getSemanticId())) {
                departures.add(connection);
            }
            if (connection.getArrivalStationUri().equals(request.getStation().getSemanticId())) {
                arrivals.add(connection);
                departureIndexForArrivals.add(departures.size());
            }
        }
        pages++;

        if ((request.getType() == LiveboardType.DEPARTURES && departures.size() > 0) || (request.getType() == LiveboardType.ARRIVALS && arrivals.size() > 0)) {
            VehicleStopImpl[] stoparray = generateStopArray();
            LiveboardImpl liveboard = new LiveboardImpl(request.getStation(), stoparray, request.getSearchTime(), request.getType(), request.getTimeDefinition());
            liveboard.setPageInfo(
                    new StringPagePointer(previous),
                    new StringPagePointer(current),
                    new StringPagePointer(next)
            );
            Log.i("LiveboardResponse", "Found " + stoparray.length + " results after searching " + pages + " pages");
            request.notifySuccessListeners(liveboard);
            ((MeteredRequest) tag).setMsecParsed(DateTime.now().getMillis());
        } else {
            Log.i("LiveboardResponse", "Found no results");
            String link = data.next;
            // When searching for "arrive before", we need to look backwards
            if (request.getTimeDefinition() == QueryTimeDefinition.EQUAL_OR_EARLIER) {
                link = data.previous;
            }

            // TODO: use a better way than comparing searchTime, as searchTime can be unchanged when extending a liveboard
            if (data.connections.length > 0 && data.connections[0].getDepartureTime().isAfter(request.getSearchTime().plusHours(48))) {
                request.notifyErrorListeners(new FileNotFoundException());
                return;
            }

            mLinkedConnectionsProvider.getLinkedConnectionsByUrl(link,
                    this,
                    new TransportDataErrorResponseListener() {
                        @Override
                        public void onErrorResponse(@NonNull Exception e, Object tag) {
                            Log.w("LiveboardResponseLstnr", "Getting next LC page failed");
                        }
                    },
                    tag);
        }

    }

    @Override
    public void onErrorResponse(@NonNull Exception e, Object tag) {
        request.notifyErrorListeners(e);
        ((MeteredRequest) tag).setMsecParsed(DateTime.now().getMillis());
        ((MeteredDataSource.MeteredRequest) tag).setResponseType(MeteredDataSource.RESPONSE_FAILED);
    }

    @AddTrace(name = "LiveboardResponseListener.createStopArray")
    private VehicleStopImpl[] generateStopArray() {
        // Find stops (train arrives and leaves again)
        ArrayList<LinkedConnection> handledConnections = new ArrayList<>();

        for (int i = 0; i < arrivals.size(); i++) {
            boolean foundMatchingDeparture = false;

            for (int j = departureIndexForArrivals.get(i); j < departures.size() && !foundMatchingDeparture; j++) {
                if (Objects.equals(arrivals.get(i).getTrip(), departures.get(j).getTrip())) {
                    foundMatchingDeparture = true;

                    LinkedConnection departure = departures.get(j);
                    LinkedConnection arrival = arrivals.get(i);

                    handledConnections.add(departure);
                    handledConnections.add(arrival);

                    StopLocation direction = OpenTransportApi.getStationsProviderInstance().getStationByExactName(
                            departure.getDirection());

                    String headsign;
                    if (direction == null) {
                        headsign = departure.getDirection();
                    } else {
                        headsign = direction.getLocalizedName();
                    }
                    stops.add(new VehicleStopImpl(request.getStation(),
                            new IrailVehicleJourneyStub(
                                    basename(departure.getRoute()),
                                    headsign,
                                    departure.getRoute()),
                            "?",
                            true,
                            departure.getDepartureTime(),
                            arrival.getArrivalTime(),
                            Duration.standardSeconds(departure.getDepartureDelay()),
                            Duration.standardSeconds(arrival.getArrivalDelay()),
                            false,
                            false,
                            departure.getDelayedDepartureTime().isAfterNow(),
                            departure.getSemanticId(),
                            TransportOccupancyLevel.UNSUPPORTED,
                            VehicleStopType.STOP));
                }
            }
        }

        if (request.getType() == LiveboardType.DEPARTURES) {
            for (int i = 0; i < departures.size(); i++) {
                if (handledConnections.contains(departures.get(i))) {
                    continue;
                }

                LinkedConnection departure = departures.get(i);
                StopLocation direction = mStationProvider.getStationByExactName(
                        departure.getDirection());
                String headsign;
                if (direction == null) {
                    headsign = departure.getDirection();
                } else {
                    headsign = direction.getLocalizedName();
                }
                stops.add(new VehicleStopImpl(request.getStation(), new IrailVehicleJourneyStub(
                        basename(departure.getRoute()),
                        headsign,
                        departure.getRoute()),
                        "?",
                        true,
                        departure.getDepartureTime(),
                        null,
                        Duration.standardSeconds(departure.getDepartureDelay()),
                        new Duration(0),
                        false,
                        false,
                        departure.getDelayedDepartureTime().isBeforeNow(),
                        departure.getSemanticId(),
                        TransportOccupancyLevel.UNSUPPORTED,
                        VehicleStopType.DEPARTURE));

            }

            Collections.sort(stops, (o1, o2) -> o1.getDepartureTime().compareTo(o2.getDepartureTime()));
        } else {
            for (int i = 0; i < arrivals.size(); i++) {
                if (handledConnections.contains(arrivals.get(i))) {
                    continue;
                }
                LinkedConnection arrival = arrivals.get(i);
                StopLocation direction = request.getStation();

                stops.add(new VehicleStopImpl(request.getStation(), new IrailVehicleJourneyStub(
                        basename(arrival.getRoute()),
                        direction.getLocalizedName(),
                        arrival.getRoute()),
                        "?",
                        true,
                        null,
                        arrival.getArrivalTime(),
                        new Duration(0),
                        Duration.standardSeconds(arrival.getArrivalDelay()),
                        false,
                        false,
                        arrival.getDelayedArrivalTime().isBeforeNow(),
                        arrival.getSemanticId(),
                        TransportOccupancyLevel.UNSUPPORTED,
                        VehicleStopType.ARRIVAL));

            }

            Collections.sort(stops, new Comparator<VehicleStopImpl>() {
                @Override
                public int compare(VehicleStopImpl o1, VehicleStopImpl o2) {
                    return o1.getArrivalTime().compareTo(o2.getArrivalTime());
                }
            });
        }


        VehicleStopImpl[] stoparray = new VehicleStopImpl[stops.size()];
        stops.toArray(stoparray);
        return stoparray;
    }
}