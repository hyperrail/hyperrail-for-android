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

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.be.irail;

import android.support.annotation.Nullable;

import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.Objects;

import be.hyperrail.opentransportdata.common.models.StopLocation;
import be.hyperrail.opentransportdata.common.models.Vehicle;
import be.hyperrail.opentransportdata.common.models.implementation.VehicleStopImpl;

/**
 * This class represents a train entity.
 * This class extends a VehicleStub with its stops.
 */
public class IrailVehicle extends IrailVehicleStub implements Vehicle, Serializable {

    private final double longitude;
    private final double latitude;

    private final VehicleStopImpl[] stops;
    private VehicleStopImpl lastHaltedStop;

    public IrailVehicle(String id, @Nullable String uri, double longitude, double latitude, VehicleStopImpl[] stops) {
        super(id, stops[stops.length - 1].getStation().getLocalizedName(), uri);
        this.longitude = longitude;
        this.latitude = latitude;
        this.stops = stops;

        for (VehicleStopImpl stop : stops) {
            if (stop.hasLeft()) {
                lastHaltedStop = stop;
            }
        }
    }

    public StopLocation getOrigin() {
        return stops[0].getStation();
    }

    public VehicleStopImpl[] getStops() {
        return stops;
    }

    public VehicleStopImpl getLastHaltedStop() {
        return lastHaltedStop;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    /**
     * Get zero-based index for this station in the stops list. -1 if this stop doesn't exist.
     *
     * @param station The station to search for.
     * @return Get zero-based index for this station in the stops list. -1 if this stop doesn't exist.
     */
    public int getStopNumberForStation(StopLocation station) {
        for (int i = 0; i < stops.length; i++) {
            if (Objects.equals(stops[i].getStation().getHafasId(), station.getHafasId())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Get zero-based index for this departure time in the stops list. -1 if this stop doesn't exist.
     *
     * @param time The datetime to search for
     * @return Get zero-based index for this station in the stops list. -1 if this stop doesn't exist.
     */
    public int getStopnumberForDepartureTime(DateTime time) {
        for (int i = 0; i < stops.length; i++) {
            if (Objects.equals(stops[i].getDepartureTime(), time)) {
                return i;
            }
        }
        if (Objects.equals(stops[stops.length - 1].getArrivalTime(), time)) {
            return stops.length - 1;
        }
        return -1;
    }


    /**
     * The direction (final stop) of this train
     *
     * @return direction (final stop) of this train
     */

    public StopLocation getDirection() {
        return stops[stops.length - 1].getStation();
    }
}
