/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.be.experimental.linkedconnections;

import androidx.annotation.NonNull;
import android.util.Log;

import com.google.firebase.perf.metrics.AddTrace;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import be.hyperrail.opentransportdata.be.irail.IrailVehicleJourney;
import be.hyperrail.opentransportdata.be.irail.IrailVehicleInfo;
import be.hyperrail.opentransportdata.common.contracts.MeteredDataSource;
import be.hyperrail.opentransportdata.common.contracts.TransportDataErrorResponseListener;
import be.hyperrail.opentransportdata.common.contracts.TransportDataSuccessResponseListener;
import be.hyperrail.opentransportdata.common.contracts.TransportOccupancyLevel;
import be.hyperrail.opentransportdata.common.contracts.TransportStopsDataSource;
import be.hyperrail.opentransportdata.common.exceptions.StopLocationNotResolvedException;
import be.hyperrail.opentransportdata.common.models.StopLocation;
import be.hyperrail.opentransportdata.common.models.VehicleStopType;
import be.hyperrail.opentransportdata.common.models.implementation.VehicleStopImpl;
import be.hyperrail.opentransportdata.common.requests.VehicleRequest;

import static be.hyperrail.opentransportdata.be.experimental.linkedconnections.LinkedConnectionsDataSource.basename;

/**
 * Created in be.hyperrail.android.irail.implementation.linkedconnections on 15/03/2018.
 */

public class VehicleResponseListener implements TransportDataSuccessResponseListener<LinkedConnections>, TransportDataErrorResponseListener {

    private VehicleRequest mRequest;
    private final TransportStopsDataSource mStationProvider;

    public VehicleResponseListener(VehicleRequest request, TransportStopsDataSource stationProvider) {
        mRequest = request;
        mStationProvider = stationProvider;
    }

    @Override
    @AddTrace(name = "VehicleResponseListener.sucess")
    public void onSuccessResponse(@NonNull LinkedConnections data, Object tag) {
        ((MeteredDataSource.MeteredRequest) tag).setMsecUsableNetworkResponse(DateTime.now().getMillis());
        List<VehicleStopImpl> stops = new ArrayList<>();
        Log.i("VehicleResponseListener", "Parsing train...");
        LinkedConnection lastConnection = null;
        for (int i = 0; i < data.connections.length; i++) {
            LinkedConnection connection = data.connections[i];
            if (!connection.isNormal() || !Objects.equals(connection.getRoute(), "http://irail.be/vehicle/" + mRequest.getVehicleId())) {
                continue;
            }

            StopLocation departure;
            try {
                departure = mStationProvider.getStoplocationBySemanticId(connection.getDepartureStationUri());
            } catch (StopLocationNotResolvedException e) {
                mRequest.notifyErrorListeners(e);
                return;
            }

            String headsign = parseHeadsign(connection);
            if (stops.isEmpty()) {
                // First stop
                stops.add(VehicleStopImpl.buildDepartureVehicleStop(departure, new IrailVehicleInfo(basename(connection.getRoute()), headsign, connection.getRoute()), "?", true,
                        connection.getDepartureTime(),
                        Duration.standardSeconds(connection.getDepartureDelay()),
                        false, connection.getDelayedDepartureTime().isBeforeNow(),
                        connection.getSemanticId(), TransportOccupancyLevel.UNSUPPORTED));
            } else {
                // Some stop during the journey
                stops.add(new VehicleStopImpl(departure, new IrailVehicleInfo(basename(connection.getRoute()), headsign, connection.getRoute()), "?", true,
                        connection.getDepartureTime(), lastConnection.getArrivalTime(),
                        Duration.standardSeconds(connection.getDepartureDelay()),
                        Duration.standardSeconds(lastConnection.getArrivalDelay()),
                        false, false, lastConnection.getDelayedArrivalTime().isBeforeNow(),
                        connection.getSemanticId(), TransportOccupancyLevel.UNSUPPORTED, VehicleStopType.STOP));
            }

            lastConnection = connection;
        }

        if (!stops.isEmpty()) {
            StopLocation arrival;
            try {
                arrival = mStationProvider.getStoplocationBySemanticId(lastConnection.getArrivalStationUri());
            } catch (StopLocationNotResolvedException e) {
                mRequest.notifyErrorListeners(e);
                return;
            }

            String headsign = parseHeadsign(lastConnection);
            // Arrival stop
            stops.add(VehicleStopImpl.buildArrivalVehicleStop(arrival, new IrailVehicleInfo(basename(lastConnection.getRoute()), headsign, lastConnection.getRoute()),
                    "?", true,
                    lastConnection.getArrivalTime(),
                    Duration.standardSeconds(lastConnection.getArrivalDelay()),
                    false, lastConnection.getDelayedArrivalTime().isBeforeNow(),
                    lastConnection.getSemanticId(), TransportOccupancyLevel.UNSUPPORTED));

            VehicleStopImpl[] stopsArray = new VehicleStopImpl[stops.size()];
            ((MeteredDataSource.MeteredRequest) tag).setMsecParsed(DateTime.now().getMillis());
            mRequest.notifySuccessListeners(new IrailVehicleJourney(stops.get(0).getVehicle().getId(), lastConnection.getRoute(), 0, 0, stops.toArray(stopsArray)));
        }
    }

    private String parseHeadsign(LinkedConnection connection) {
        StopLocation direction = mStationProvider.getStoplocationByExactName(connection.getDirection());
        String headsign;
        if (direction != null) {
            headsign = direction.getLocalizedName();
        } else {
            headsign = connection.getDirection();
        }
        return headsign;
    }

    @Override
    public void onErrorResponse(@NonNull Exception e, Object tag) {
        Log.w("VehicleResponseListener", "Failed to load page! " + e.getMessage());
        mRequest.notifyErrorListeners(e);
    }
}

