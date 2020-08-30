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

import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.Objects;

import be.hyperrail.opentransportdata.common.models.StopLocation;
import be.hyperrail.opentransportdata.common.models.VehicleJourney;
import be.hyperrail.opentransportdata.common.models.implementation.VehicleStopImpl;

/**
 * This class represents a train entity.
 * This class extends a VehicleJourneyStub with its stops.
 */
public class IrailVehicleJourney extends IrailVehicleInfo implements VehicleJourney, Serializable {

    private final double longitude;
    private final double latitude;

    private final VehicleStopImpl[] stops;
    private VehicleStopImpl lastHaltedStop;

    public IrailVehicleJourney(IrailVehicleInfo vehicleInfo, double longitude, double latitude, VehicleStopImpl[] stops) {
        super(vehicleInfo);
        this.longitude = longitude;
        this.latitude = latitude;
        this.stops = stops;

        for (VehicleStopImpl stop : stops) {
            if (stop.hasLeft()) {
                lastHaltedStop = stop;
            }
        }
    }

    public StopLocation getFirstStopLocation() {
        return stops[0].getStopLocation();
    }

    public VehicleStopImpl[] getStops() {
        return stops;
    }

    public VehicleStopImpl getLastHaltedStop() {
        return lastHaltedStop;
    }

    public double getCurrentPostionLongitude() {
        return longitude;
    }

    public double getCurrentPositionLatitude() {
        return latitude;
    }

    /**
     * Get zero-based index for this station in the stops list. -1 if this stop doesn't exist.
     *
     * @param station The station to search for.
     * @return Get zero-based index for this station in the stops list. -1 if this stop doesn't exist.
     */
    public int getIndexForStoplocation(StopLocation station) {
        for (int i = 0; i < stops.length; i++) {
            if (Objects.equals(stops[i].getStopLocation().getHafasId(), station.getHafasId())) {
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
    public int getIndexForDepartureTime(DateTime time) {
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

    public StopLocation getLastStopLocation() {
        return stops[stops.length - 1].getStopLocation();
    }
}
