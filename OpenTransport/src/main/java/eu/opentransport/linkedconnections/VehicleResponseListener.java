/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package eu.opentransport.linkedconnections;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.perf.metrics.AddTrace;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import eu.opentransport.OpenTransportApi;
import eu.opentransport.common.contracts.MeteredDataSource;
import eu.opentransport.common.contracts.TransportDataErrorResponseListener;
import eu.opentransport.common.contracts.TransportDataSuccessResponseListener;
import eu.opentransport.common.contracts.TransportOccupancyLevel;
import eu.opentransport.common.contracts.TransportStopsDataSource;
import eu.opentransport.common.exceptions.StopLocationNotResolvedException;
import eu.opentransport.common.models.StopLocation;
import eu.opentransport.common.models.VehicleStopType;
import eu.opentransport.common.requests.IrailVehicleRequest;
import eu.opentransport.irail.IrailStation;
import eu.opentransport.irail.IrailVehicle;
import eu.opentransport.irail.IrailVehicleStop;
import eu.opentransport.irail.IrailVehicleStub;

import static eu.opentransport.linkedconnections.LinkedConnectionsDataSource.basename;

/**
 * Created in be.hyperrail.android.irail.implementation.linkedconnections on 15/03/2018.
 */

public class VehicleResponseListener implements TransportDataSuccessResponseListener<LinkedConnections>, TransportDataErrorResponseListener {

    private IrailVehicleRequest mRequest;
    private final TransportStopsDataSource mStationProvider;

    public VehicleResponseListener(IrailVehicleRequest request, TransportStopsDataSource stationProvider) {
        mRequest = request;
        mStationProvider = stationProvider;
    }

    @Override
    @AddTrace(name = "VehicleResponseListener.sucess")
    public void onSuccessResponse(@NonNull LinkedConnections data, Object tag) {
        ((MeteredDataSource.MeteredRequest)tag).setMsecUsableNetworkResponse(DateTime.now().getMillis());
        List<IrailVehicleStop> stops = new ArrayList<>();
        Log.i("VehicleResponseListener", "Parsing train...");
        LinkedConnection lastConnection = null;
        for (int i = 0; i < data.connections.length; i++) {
            LinkedConnection connection = data.connections[i];
            if (!connection.isNormal() ||  !Objects.equals(connection.getRoute(), "http://irail.be/vehicle/" + mRequest.getVehicleId())) {
                continue;
            }

            StopLocation departure;
            try {
                departure = mStationProvider.getStationByUri(connection.getDepartureStationUri());
            } catch (StopLocationNotResolvedException e) {
                mRequest.notifyErrorListeners(e);
                return;
            }

            StopLocation direction = mStationProvider.getStationByExactName(connection.getDirection());
            String headsign;
            if (direction != null) {
                headsign = direction.getLocalizedName();
            } else {
                headsign = connection.getDirection();
            }
            if (stops.size() == 0) {
                // First stop
                stops.add(IrailVehicleStop.buildDepartureVehicleStop(departure, new IrailVehicleStub(basename(connection.getRoute()), headsign, connection.getRoute()), "?", true,
                                                                     connection.getDepartureTime(),
                                                                     Duration.standardSeconds(connection.getDepartureDelay()),
                                                                     false, connection.getDelayedDepartureTime().isBeforeNow(),
                                                                     connection.getUri(), TransportOccupancyLevel.UNSUPPORTED));
            } else {
                // Some stop during the journey
                assert lastConnection != null;
                stops.add(new IrailVehicleStop(departure, new IrailVehicleStub(basename(connection.getRoute()), headsign, connection.getRoute()), "?", true,
                                               connection.getDepartureTime(), lastConnection.getArrivalTime(),
                                               Duration.standardSeconds(connection.getDepartureDelay()),
                                               Duration.standardSeconds(lastConnection.getArrivalDelay()),
                                               false, false, lastConnection.getDelayedArrivalTime().isBeforeNow(),
                                               connection.getUri(), TransportOccupancyLevel.UNSUPPORTED, VehicleStopType.STOP));
            }

            lastConnection = connection;
        }

        if (stops.size() > 0 && lastConnection != null) {
            StopLocation arrival;
            try {
                arrival = OpenTransportApi.getStationsProviderInstance().getStationByUri(lastConnection.getArrivalStationUri());
            } catch (StopLocationNotResolvedException e) {
                mRequest.notifyErrorListeners(e);
                return;
            }

            StopLocation direction = OpenTransportApi.getStationsProviderInstance().getStationByExactName(lastConnection.getDirection());
            String headsign;
            if (direction != null) {
                headsign = direction.getLocalizedName();
            } else {
                headsign = lastConnection.getDirection();
            }
            // Arrival stop
            stops.add(IrailVehicleStop.buildArrivalVehicleStop(arrival, new IrailVehicleStub(basename(lastConnection.getRoute()), headsign, lastConnection.getRoute()),
                                                               "?", true,
                                                               lastConnection.getArrivalTime(),
                                                               Duration.standardSeconds(lastConnection.getArrivalDelay()),
                                                               false, lastConnection.getDelayedArrivalTime().isBeforeNow(),
                                                               lastConnection.getUri(), TransportOccupancyLevel.UNSUPPORTED));

            IrailVehicleStop[] stopsArray = new IrailVehicleStop[stops.size()];
            ((MeteredDataSource.MeteredRequest)tag).setMsecParsed(DateTime.now().getMillis());
            mRequest.notifySuccessListeners(new IrailVehicle(stops.get(0).getVehicle().getId(), lastConnection.getRoute(), 0, 0, stops.toArray(stopsArray)));
        }
    }

    @Override
    public void onErrorResponse(@NonNull Exception e, Object tag) {
        Log.w("VehicleResponseListener", "Failed to load page! " + e.getMessage());
        mRequest.notifyErrorListeners(e);
    }
}

