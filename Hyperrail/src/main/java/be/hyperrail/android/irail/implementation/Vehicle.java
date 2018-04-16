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

package be.hyperrail.android.irail.implementation;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.Objects;

import be.hyperrail.android.irail.db.Station;

/**
 * This class represents a train entity.
 * This class extends a VehicleStub with its stops.
 */
public class Vehicle extends VehicleStub implements Serializable {

    private final double longitude;
    private final double latitude;

    private final VehicleStop[] stops;
    private VehicleStop lastHaltedStop;

    public Vehicle(@NonNull String id, @Nullable String uri, double longitude, double latitude, VehicleStop[] stops) {
        super(id, stops[stops.length - 1].getStation().getLocalizedName(), uri);
        this.longitude = longitude;
        this.latitude = latitude;
        this.stops = stops;

        for (VehicleStop stop : stops) {
            if (stop.hasLeft()) {
                lastHaltedStop = stop;
            }
        }
    }

    public Station getOrigin() {
        return stops[0].getStation();
    }

    public VehicleStop[] getStops() {
        return stops;
    }

    public VehicleStop getLastHaltedStop() {
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
    public int getStopNumberForStation(Station station) {
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
    @NonNull
    public Station getDirection() {
        return stops[stops.length - 1].getStation();
    }
}
