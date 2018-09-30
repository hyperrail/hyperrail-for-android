/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package eu.opentransport.linkedconnections;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.firebase.perf.metrics.AddTrace;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import eu.opentransport.common.contracts.MeteredDataSource;
import eu.opentransport.common.contracts.PagedDataResourceDescriptor;
import eu.opentransport.common.contracts.TransportDataErrorResponseListener;
import eu.opentransport.common.contracts.TransportDataSuccessResponseListener;
import eu.opentransport.common.contracts.TransportOccupancyLevel;
import eu.opentransport.common.contracts.TransportStopsDataSource;
import eu.opentransport.common.exceptions.StopLocationNotResolvedException;
import eu.opentransport.common.models.Route;
import eu.opentransport.common.models.RouteLeg;
import eu.opentransport.common.models.RouteLegEnd;
import eu.opentransport.common.models.RouteLegType;
import eu.opentransport.common.requests.IrailRoutesRequest;
import eu.opentransport.irail.IrailRoutesList;
import eu.opentransport.irail.IrailVehicleStub;

import static eu.opentransport.linkedconnections.LinkedConnectionsDataSource.basename;

/**
 * Created in be.hyperrail.android.irail.implementation.linkedconnections on 15/03/2018.
 */
public class RouteResponseListener implements TransportDataSuccessResponseListener<LinkedConnections>, TransportDataErrorResponseListener {

    private final LinkedConnectionsProvider mLinkedConnectionsProvider;
    private final TransportStopsDataSource mStationProvider;
    private IrailRoutesRequest mRoutesRequest;

    @Nullable
    private DateTime mDepartureLimit;
    private int maxMinutes;

    private int maxTransfers = 4;

    // This makes a lot of checks easiers
    private DateTime infinite = new DateTime(3000, 1, 1, 0, 0);

    // For each stop, keep an array of (departuretime, arrivaltime) pairs
    // After execution, this array will contain the xt profile for index x
    // Size n, where n is the number of stations
    // Each entry in this array is an array of  (departuretime, arrivaltime) pairs, sorted by DESCENDING departuretime
    // A DESCENDING departurtime will ensure we always add to the back of the array, thus saving O(n) operations every time!
    // Note: for journey extraction, 2 data fields will be added. These fields can be ignored for the original Profile Connection Scan Algorithm
    HashMap<String, List<StationStopProfile>> S = new HashMap<>();

    // For every trip, keep the earliest possible arrival time
    // The earliest arrival time for the partial journey departing in the earliest scanned connection of the corresponding trip
    // Size m, where m is the number of trips
    HashMap<String, TrainProfile> T = new HashMap<>();
    private Object mTag;
    private String mNext;
    private String mPrevious;
    private String mCurrent;

    public RouteResponseListener(@NonNull LinkedConnectionsProvider linkedConnectionsProvider, @NonNull TransportStopsDataSource stationProvider, @NonNull IrailRoutesRequest request, @Nullable DateTime departureLimit) {
        mLinkedConnectionsProvider = linkedConnectionsProvider;
        mStationProvider = stationProvider;
        mRoutesRequest = request;
        mDepartureLimit = departureLimit;
    }

    public RouteResponseListener(LinkedConnectionsProvider linkedConnectionsProvider, TransportStopsDataSource stationProvider, IrailRoutesRequest routesRequest, DateTime departureLimit, int i) {
        this(linkedConnectionsProvider, stationProvider, routesRequest, departureLimit);
        maxMinutes = i;
    }

    @AddTrace(name = "RouteResponseListener.process")
    private void process(LinkedConnections data) throws StopLocationNotResolvedException {
        // Keep searching
        // - while no results have been found
        // - until we have the number of results we'd like (in case no departure time is given)
        // - but stop when we're passing the departe time limit
        // - when we're searching with a departuretime, we need to continue until we're at the front. This might result in more results, which we'll all pass to the client

        mPrevious = data.previous;
        mCurrent = data.current;
        if (mNext == null) {
            mNext = data.next;
        }

        if (data.connections.length == 0) {
            mLinkedConnectionsProvider.getLinkedConnectionsByUrl(data.previous, this, this, null);
        }

        if (maxMinutes > 0) {
            DateTime limitByMinutes = data.connections[data.connections.length - 1].getDepartureTime().minusMinutes(maxMinutes);
            if (limitByMinutes.isAfter(mDepartureLimit)) {
                mDepartureLimit = limitByMinutes;
            }
            maxMinutes = 0;
        }

        boolean hasPassedDepartureLimit = false;
        for (int i = data.connections.length - 1; i >= 0; i--) {
            LinkedConnection connection = data.connections[i];
            if (!connection.isNormal()) {
                continue;
            }

            if (mDepartureLimit != null && connection.getDepartureTime().isBefore(mDepartureLimit)) {
                hasPassedDepartureLimit = true;
                continue;
            }

            // ====================================================== //
            // START GET EARLIEST ARRIVAL TIME
            // ====================================================== //

            DateTime T1_walkingArrivalTime, T2_stayOnTripArrivalTime, T3_transferArrivalTime;
            int T1_transfers, T2_transfers, T3_transfers;

            // Log::info((new Station($connection->getDepartureStopUri()))->getDefaultName() .' - '.(new Station($connection->getArrivalStopUri()))->getDefaultName() .' - '. $connection->getRoute());
            // Determine T1, the time when walking from here to the destination
            if (Objects.equals(connection.getArrivalStationUri(), mRoutesRequest.getDestination().getUri())) {
                // If this connection ends at the destination, we can walk from here to tthe station exit.
                // Our implementation does not add a footpath at the end
                // Therefore, we arrive at our destination at the time this connection arrives
                T1_walkingArrivalTime = connection.getArrivalTime();
                // We're walking, so this connections has no transfers between it and the destination
                T1_transfers = 0;
                // Log::info("[{$connection->getId()}] Walking possible with arrival time  $T1_walkingArrivalTime.");
            } else {
                // When this isn't the destination stop, we would arrive somewhere far, far in the future.
                // We're walking infinitly slow: we prefer a train
                // For stops which are close to each other, we could walk to another stop to take a train there
                // This is to be supported later on, but requires a list of footpaths.
                // TODO: support walking to a nearby stop, e.g. haren/haren-zuid
                T1_walkingArrivalTime = infinite;
                // Default value to prevent errors due to undefined variables.
                // Will never be used: when an infinitely late arrival is to earliest available, the for loop will skip to the next connection.
                T1_transfers = 999;
                // Log::info("[{$connection->getId()}] Walking not possible.");
            }
            // Determine T2, the first possible time of arrival when remaining seated
            if (T.containsKey(connection.getTrip())) {
                // When we remain seated on this train, we will arrive at the fastest arrival time possible for this vehicle
                T2_stayOnTripArrivalTime = T.get(connection.getTrip()).arrivalTime;
                // Remaining seated will have the same number of transfers between this connection and the destination, as from the best exit stop and the destination
                T2_transfers = T.get(connection.getTrip()).transfers;
                // Log::info("[{$connection->getId()}] Remaining seated possible with arrival time $T2_stayOnTripArrivalTime and $T2_transfers transfers.");
            } else {
                // When there isn't a fastest arrival time for this stop yet, it means we haven't found a connection
                // - To arrive in the destination using this vehicle, or
                // - To transfer to another vehicle in another station
                T2_stayOnTripArrivalTime = infinite;
                // Default value to prevent errors due to undefined variables.
                // Will never be used: when an infinitely late arrival is to earliest available, the for loop will skip to the next connection.
                T2_transfers = 999;
                // Log::info("[{$connection->getId()}] Remaining seated not possible");
            }
            // Determine T3, the time of arrival when taking the best possible transfer in this station
            if (S.containsKey(connection.getArrivalStationUri())) {
                // If there are connections leaving from the arrival station, determine the one which departs after we arrive,
                // but arrives as soon as possible
                // The earliest departure is in the back of the array. This int will keep track of which pair we're evaluating.
                int position = S.get(connection.getArrivalStationUri()).size() - 1;
                StationStopProfile stopProfile = S.get(connection.getArrivalStationUri()).get(position);

                // TODO: replace hard-coded transfer time
                // As long as we're arriving AFTER the pair departure, move forward in the list until we find a departure which is reachable
                // The list is sorted by descending departure time, so the earliest departures are in the back (so we move back to front)

                while ((stopProfile.departureTime.getMillis() - 300 * 1000 <= connection.getArrivalTime().getMillis() ||
                        stopProfile.transfers >= maxTransfers) && position > 0) {
                    position--;
                    stopProfile = S.get(connection.getArrivalStationUri()).get(position);
                }
                if (stopProfile.departureTime.getMillis() - 300 * 1000 > connection.getArrivalTime().getMillis() && stopProfile.transfers <= maxTransfers) {
                    // If a result was found in the list, this is the earliest arrival time when transferring here
                    // Optional: Adding one second to the arrival time will ensure that the route with the smallest number of legs is chosen.
                    // This would not affect journey extaction, but would prefer routes with less legs when arrival times are identical (as their arrival time will be one second earlier)
                    // It would prefer remaining seated over transferring when both would result in the same arrival time
                    // TODO: increase to 240 -> this way we prefer one less transfer in exchange for 10 minutes longer trip
                    // See http://lc2irail.dev/connections/008822160/008895257/departing/1519924311
                    T3_transferArrivalTime = new DateTime(stopProfile.arrivalTime.getMillis() + 240 * 1000);
                    // Using this transfer will increase the number of transfers with 1
                    T3_transfers = stopProfile.transfers + 1;
                } else {
                    // When there isn't a reachable connection, transferring isn't an option
                    T3_transferArrivalTime = infinite;
                    // Default value to prevent errors due to undefined variables.
                    // Will never be used: when an infinitely late arrival is to earliest available, the for loop will skip to the next connection.
                    T3_transfers = 999;
                    // Log::info("[{$connection->getId()}] Transferring not possible: No transfers reachable");
                }
            } else {
                // When there isn't a reachable connection, transferring isn't an option
                T3_transferArrivalTime = infinite;
                // Default value to prevent errors due to undefined variables.
                // Will never be used: when an infinitely late arrival is to earliest available, the for loop will skip to the next connection.
                T3_transfers = 999;
                // Log::info("[{$connection->getId()}] Transferring not possible: No transfers exist");
            }

            // Tmin = Tc in the paper
            // This is the earliest arrival time over the 3 possibilities
            DateTime Tmin;
            LinkedConnection exitTrainConnection;
            int numberOfTransfers;
            // Where do we need to get off the train?
            // The following if-else structure does not follow the JourneyLeg Extraction algorithm as described in the CSA (march 2017) paper.
            // Not only do we want to reconstruct the journey (the vehicles used), but we want departure and arrival times for every single leg.
            // In order to also have the arrival times, we will always save the arrival connection for the next hop, instead of the arrival connection for the entire journey.
            // If T3 < T2, prefer a transfer.
            // If T3 == T2, prefer T3. This ensures we don't go A - (B) - (C) - D - (C) - (B) - E when searching A-E but A - B - E instead
            // Here we force the least amount of transfers for the same arrival time since T3 is already incremented with some seconds
            if (T3_transferArrivalTime.getMillis() <= T2_stayOnTripArrivalTime.getMillis()) {
                // Log::info("Transfer time!");
                Tmin = T3_transferArrivalTime;
                // We're transferring here, so get off the train in this station
                exitTrainConnection = connection;
                // We already incremented this transfer counter when determining the train
                numberOfTransfers = T3_transfers;
            } else {
                // Log::info("Train time!");
                Tmin = T2_stayOnTripArrivalTime;
                // We're staying on this trip. This also implicates a key in T exists for this trip. We're getting off at the previous exit for this vehicle.
                if (T2_stayOnTripArrivalTime.isBefore(infinite)) {
                    exitTrainConnection = T.get(connection.getTrip()).arrivalConnection;
                } else {
                    exitTrainConnection = null;
                }
                numberOfTransfers = T2_transfers;
            }
            // For equal times, we prefer just arriving.
            if (T1_walkingArrivalTime.getMillis() <= Tmin.getMillis()) {
                // Log::info("Nvm, walking time!");
                Tmin = T1_walkingArrivalTime;
                // We're walking from here, so get off here
                exitTrainConnection = connection;
                numberOfTransfers = T1_transfers;
            }
            // ====================================================== //
            // END GET EARLIEST ARRIVAL TIME
            // ====================================================== //

            // The exitTrainConnection condition is unnecessary, but will prevent warnings about possible nullReferenceErrors
            if (Tmin.isEqual(infinite) || exitTrainConnection == null) {
                continue;
            }

            // We now have the minimal arrival time for this connection
            // Update T and S with this new data
            // ====================================================== //
            // START UPDATE T
            // ====================================================== //
            // Set the fastest arrival time for this vehicle, and set the connection at which we have to hop off
            if (T.containsKey(connection.getTrip())) {

                // When there is a faster way for this trip, it's by getting of at this connection's arrival station and transferring (or having arrived)

                // Can also be equal for a transfer with the best transfer (don't do bru south - central - north - transfer - north - central - south
                // We're updating an existing connection, with a way to get off earlier (iterating using descending departure times).
                // This only modifies the transfer stop, nothing else in the journey
                if (Tmin.isEqual(T.get(connection.getTrip()).arrivalTime)
                        && !T.get(connection.getTrip()).arrivalConnection.getArrivalStationUri().equals(mRoutesRequest.getDestination().getUri())
                        && T3_transferArrivalTime.isEqual(T2_stayOnTripArrivalTime)
                        && S.containsKey(T.get(connection.getTrip()).arrivalConnection.getArrivalStationUri())
                        && S.containsKey(connection.getArrivalStationUri())
                        ) {
                    // When the arrival time is the same, the number of transfers should also be the same
                    // We prefer the exit connection with the largest transfer time
                    // Suppose we exit the train here: connection. Does this improve on the transfer time?
                    LinkedConnection currentTrainExit = T.get(connection.getTrip()).arrivalConnection;
                    // Now we need the departure in the next station!
                    // Create a quadruple to lookup the first reachable connection in S
                    // Create one, because we don't know where we'd get on this train

                    StationStopProfile stationStopProfile = new StationStopProfile();
                    stationStopProfile.departureTime = connection.getDepartureTime();
                    stationStopProfile.departureConnection = connection;
                    // Current situation
                    stationStopProfile.arrivalTime = Tmin;
                    stationStopProfile.arrivalConnection = currentTrainExit;

                    Duration currentTransfer = new Duration(currentTrainExit.getArrivalTime(), getFirstReachableConnection(stationStopProfile).departureTime);

                    // New situation
                    stationStopProfile.arrivalTime = Tmin;
                    stationStopProfile.arrivalConnection = exitTrainConnection;
                    Duration newTransfer = new Duration(exitTrainConnection.getArrivalTime(), getFirstReachableConnection(stationStopProfile).departureTime);

                    // If the new situation is better
                    if (newTransfer.isLongerThan(currentTransfer)) {
                        TrainProfile trainProfile = new TrainProfile();
                        trainProfile.arrivalTime = Tmin;
                        trainProfile.arrivalConnection = exitTrainConnection;
                        trainProfile.transfers = numberOfTransfers;

                        T.put(connection.getTrip(), trainProfile);
                    }
                }

                // Faster way
                if (Tmin.isBefore(T.get(connection.getTrip()).arrivalTime)) {
                    // exit = (new Station(exitTrainConnection->getArrivalStopUri()))->getDefaultName();
                    // Log::info("[{connection->getId()}] Updating T: Arrive at Tmin using {connection->getRoute()} with numberOfTransfers transfers. Get off at {exit}.");
                    TrainProfile trainProfile = new TrainProfile();
                    trainProfile.arrivalTime = Tmin;
                    trainProfile.arrivalConnection = exitTrainConnection;
                    trainProfile.transfers = numberOfTransfers;

                    T.put(connection.getTrip(), trainProfile);
                }
            } else {
                // exit = (new Station(exitTrainConnection->getArrivalStopUri()))->getDefaultName();
                // Log::info("[{connection->getId()}] Updating T: New: Arrive at Tmin using {connection->getRoute()} with numberOfTransfers transfers. Get off at {exit}.");
                // To travel towards the destination, get off at the current arrival station (followed by a transfer or walk/arriving)
                TrainProfile trainProfile = new TrainProfile();
                trainProfile.arrivalTime = Tmin;
                trainProfile.arrivalConnection = exitTrainConnection;
                trainProfile.transfers = numberOfTransfers;
                T.put(connection.getTrip(), trainProfile);
            }
            // ====================================================== //
            // END UPDATE T
            // ====================================================== //

            // ====================================================== //
            // START UPDATE S
            // ====================================================== //

            // Create a stopProfile to update S
            StationStopProfile newProfile = new StationStopProfile();
            newProfile.departureTime = connection.getDepartureTime();
            newProfile.arrivalTime = Tmin;
            // Additional data for journey extraction
            newProfile.departureConnection = connection;
            newProfile.arrivalConnection = T.get(connection.getTrip()).arrivalConnection;
            newProfile.transfers = numberOfTransfers;
            if (S.containsKey(connection.getDepartureStationUri())) {
                int numberOfPairs = S.get(connection.getDepartureStationUri()).size();
                StationStopProfile existingProfile = S.get(connection.getDepartureStationUri()).get(numberOfPairs - 1);
                // If existingQuad does not dominate quad
                // The new departure time is always less or equal than an already stored one
                if (newProfile.arrivalTime.isBefore(existingProfile.arrivalTime)) {
                    // // Log::info("[{connection->getId()}] Updating S: Reach destination from departureStop departing at {quad[self::KEY_DEPARTURE_TIME]} arriving at {quad[self::KEY_ARRIVAL_TIME]}");
                    if (newProfile.departureTime.isEqual(existingProfile.departureTime)) {
                        // Replace existingQuad at the back
                        S.get(connection.getDepartureStationUri()).remove(numberOfPairs - 1);
                        S.get(connection.getDepartureStationUri()).add(numberOfPairs - 1, newProfile);
                    } else {
                        // We're iterating over descending departure times, therefore the departure
                        // Insert at the back
                        S.get(connection.getDepartureStationUri()).add(newProfile);
                    }
                }
            } else {
                // Log::info("[{connection->getId()}] Updating S: New: Reach destination from departureStop departing at {quad[self::KEY_DEPARTURE_TIME]} arriving at {quad[self::KEY_ARRIVAL_TIME]}");
                S.put(connection.getDepartureStationUri(), new ArrayList<StationStopProfile>());
                S.get(connection.getDepartureStationUri()).add(newProfile);
            }
            // ====================================================== //
            // END UPDATE S
            // ====================================================== //
        }

        // No results? load more data or stop if we passed the departure time limit
        if (!S.containsKey(mRoutesRequest.getOrigin().getUri())) {
            if (hasPassedDepartureLimit) {
                IrailRoutesList result = new IrailRoutesList(mRoutesRequest.getOrigin(), mRoutesRequest.getDestination(), mRoutesRequest.getSearchTime(), mRoutesRequest.getTimeDefinition(), new Route[0]);
                result.setPageInfo(new PagedDataResourceDescriptor(mPrevious, mCurrent, mNext));
                ((MeteredDataSource.MeteredRequest) mTag).setMsecParsed(DateTime.now().getMillis());
                Log.d("RouteResponseListener", "Found 0 results");
                mRoutesRequest.notifySuccessListeners(result);
            } else {
                mLinkedConnectionsProvider.getLinkedConnectionsByUrl(data.previous, this, this, mTag);
            }
            return;
        }

        // Results? Return data
        Route[] routes = new Route[S.get(mRoutesRequest.getOrigin().getUri()).size()];

        int i = 0;
        for (StationStopProfile profile : S.get(mRoutesRequest.getOrigin().getUri())
                ) {
            // it will iterate over all legs
            StationStopProfile it = profile;
            List<RouteLeg> legs = new ArrayList<>();

            while (!Objects.equals(it.arrivalConnection.getArrivalStationUri(), mRoutesRequest.getDestination().getUri())) {
                RouteLegEnd departure = new RouteLegEnd(mStationProvider.getStationByUri(it.departureConnection.getDepartureStationUri()),
                                                        it.departureConnection.getDepartureTime(), "?", true, Duration.standardSeconds(it.departureConnection.getDepartureDelay()), false, it.departureConnection.getDelayedDepartureTime().isBeforeNow(),
                                                        it.departureConnection.getUri(), TransportOccupancyLevel.UNSUPPORTED);
                RouteLegEnd arrival = new RouteLegEnd(mStationProvider.getStationByUri(it.arrivalConnection.getArrivalStationUri()),
                                                      it.arrivalConnection.getArrivalTime(), "?", true, Duration.standardSeconds(it.arrivalConnection.getArrivalDelay()), false, it.arrivalConnection.getDelayedArrivalTime().isBeforeNow(),
                                                      it.arrivalConnection.getArrivalStationUri(), TransportOccupancyLevel.UNSUPPORTED);
                RouteLeg r = new RouteLeg(RouteLegType.TRAIN, new IrailVehicleStub(basename(it.departureConnection.getRoute()), it.departureConnection.getDirection(), it.departureConnection.getTrip()), departure, arrival);
                legs.add(r);

                it = getFirstReachableConnection(it);
            }

            RouteLegEnd departure = new RouteLegEnd(mStationProvider.getStationByUri(it.departureConnection.getDepartureStationUri()),
                                                    it.departureConnection.getDepartureTime(), "?", true, Duration.standardSeconds(it.departureConnection.getDepartureDelay()), false, it.departureConnection.getDelayedDepartureTime().isBeforeNow(),
                                                    it.departureConnection.getUri(), TransportOccupancyLevel.UNSUPPORTED);
            RouteLegEnd arrival = new RouteLegEnd(mStationProvider.getStationByUri(it.arrivalConnection.getArrivalStationUri()),
                                                  it.arrivalConnection.getArrivalTime(), "?", true, Duration.standardSeconds(it.arrivalConnection.getArrivalDelay()), false, it.arrivalConnection.getDelayedArrivalTime().isBeforeNow(),
                                                  it.arrivalConnection.getArrivalStationUri(), TransportOccupancyLevel.UNSUPPORTED);
            RouteLeg r = new RouteLeg(RouteLegType.TRAIN, new IrailVehicleStub(basename(it.departureConnection.getRoute()), it.departureConnection.getDirection(), it.departureConnection.getTrip()), departure, arrival);
            legs.add(r);

            RouteLeg[] legsArray = new RouteLeg[legs.size()];
            legsArray = legs.toArray(legsArray);
            routes[i++] = new Route(legsArray);
        }

        Arrays.sort(routes, new Comparator<Route>() {
            @Override
            public int compare(Route o1, Route o2) {
                return o1.getDepartureTime().compareTo(o2.getDepartureTime());
            }
        });

        IrailRoutesList result = new IrailRoutesList(mRoutesRequest.getOrigin(), mRoutesRequest.getDestination(), mRoutesRequest.getSearchTime(), mRoutesRequest.getTimeDefinition(), routes);
        result.setPageInfo(new PagedDataResourceDescriptor(mPrevious, mCurrent, mNext));
        ((MeteredDataSource.MeteredRequest) mTag).setMsecParsed(DateTime.now().getMillis());
        Log.d("RouteResponseListener", "Found " + result.getRoutes().length + " results");
        mRoutesRequest.notifySuccessListeners(result);
    }

    private StationStopProfile getFirstReachableConnection(StationStopProfile arrivalQuad) {
        List<StationStopProfile> it_options = S.get(arrivalQuad.arrivalConnection.getArrivalStationUri());
        int i = it_options.size() - 1;
        // Find the next hop. This is the first reachable hop,
        // or even stricter defined: the hop which will get us to the destination at the same arrival time.
        // There will be a one second difference between the arrival times, as a result of the leg optimization
        while (i >= 0 && it_options.get(i).arrivalTime.getMillis() != arrivalQuad.arrivalTime.getMillis() - 240 * 1000) {
            i--;
        }
        return it_options.get(i);
    }

    @Override
    public void onSuccessResponse(@NonNull LinkedConnections data, Object tag) {
        mTag = tag;
        try {
            ((MeteredDataSource.MeteredRequest) tag).setMsecUsableNetworkResponse(DateTime.now().getMillis());
            process(data);
        } catch (StopLocationNotResolvedException e) {
            ((MeteredDataSource.MeteredRequest) tag).setMsecParsed(DateTime.now().getMillis());
            ((MeteredDataSource.MeteredRequest) tag).setResponseType(MeteredDataSource.RESPONSE_FAILED);
            mRoutesRequest.notifyErrorListeners(e);
        }
    }

    @Override
    public void onErrorResponse(@NonNull Exception e, Object tag) {
        mRoutesRequest.notifyErrorListeners(e);
    }


    class StationStopProfile {
        /**
         * The departure time in this stop
         */
        DateTime departureTime;

        /**
         * The arrival time at the final destination
         */
        DateTime arrivalTime;

        /**
         * The departure connection in this stop
         */
        LinkedConnection departureConnection;

        /**
         * The arrival connection for the next transfer or arrival
         */
        LinkedConnection arrivalConnection;

        /**
         * The number of transfers between standing in this station and the destination
         */
        int transfers;
    }

    class TrainProfile {
        /**
         * The arrival time at the final destination
         */
        DateTime arrivalTime;

        /**
         * The number of transfers until the destination when hopping on to this train
         */
        int transfers;

        /**
         * The arrival connection for the next transfer or arrival
         */
        LinkedConnection arrivalConnection;
    }

}
