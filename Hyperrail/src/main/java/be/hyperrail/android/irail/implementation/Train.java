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

import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.Objects;

import be.hyperrail.android.irail.contracts.IrailDataResponse;
import be.hyperrail.android.irail.db.Station;

/**
 * This class represents a train entity.
 * This class extends a TrainStub with its stops.
 */
public class Train extends TrainStub implements Serializable {

    private final double longitude;
    private final double latitude;
    private final Station origin;

    private final TrainStop[] stops;
    private TrainStop lastHaltedStop;

    public Train(String id, String uri, Station destination, Station origin, double longitude, double latitude, TrainStop[] stops) {
        super(id, destination, uri);
        this.origin = origin;
        this.longitude = longitude;
        this.latitude = latitude;
        this.stops = stops;

        for (TrainStop stop : stops) {
            if (stop.hasLeft()) {
                lastHaltedStop = stop;
            }
        }
    }

    public Station getOrigin() {
        return origin;
    }

    public TrainStop[] getStops() {
        return stops;
    }

    public TrainStop getLastHaltedStop() {
        return lastHaltedStop;
    }

    public IrailDataResponse<Train> getTrain() {
        // override stub method
        return new ApiResponse<>(this);
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    /**
     * Get zero-based index for this station in the stops list. -1 if this stop doesn't exist.
     * @param station The station to search for.
     * @return Get zero-based index for this station in the stops list. -1 if this stop doesn't exist.
     */
    public int getStopNumberForStation(Station station) {
        for (int i = 0; i < stops.length; i++) {
            if (Objects.equals(stops[i].getStation().getId(), station.getId())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Get zero-based index for this departure time in the stops list. -1 if this stop doesn't exist.
     * @param time The datetime to search for
     * @return Get zero-based index for this station in the stops list. -1 if this stop doesn't exist.
     */
    public int getStopnumberForDepartureTime(DateTime time) {
        for (int i = 0; i < stops.length; i++) {
            if (Objects.equals(stops[i].getDepartureTime(), time)) {
                return i;
            }
        }
        if (Objects.equals(stops[stops.length-1].getArrivalTime(), time)) {
            return stops.length-1;
        }
        return -1;
    }

}
