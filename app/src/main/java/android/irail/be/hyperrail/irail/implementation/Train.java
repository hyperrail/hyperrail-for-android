/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package android.irail.be.hyperrail.irail.implementation;

import android.irail.be.hyperrail.irail.contracts.IrailDataResponse;
import android.irail.be.hyperrail.irail.db.Station;

import java.io.Serializable;
import java.util.Objects;

/**
 * This class represents a train entity.
 * This class extends a TrainStub with its stops.
 */
public class Train extends TrainStub implements Serializable {

    private final double longitude;
    private final double latitude;
    private final Station origin;

    private final TrainStop[] stops;
    private final TrainStop lastHaltedStop;

    Train(String id, Station destination, Station origin, double longitude, double latitude, TrainStop[] stops, TrainStop lastHaltedStop) {
        super(id, destination);
        this.origin = origin;
        this.longitude = longitude;
        this.latitude = latitude;
        this.stops = stops;
        this.lastHaltedStop = lastHaltedStop;
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

    public int getStopNumberForStation(Station station) {
        for (int i = 0; i < stops.length; i++) {
            if (Objects.equals(stops[i].getStation().getId(), station.getId())) {
                return i;
            }
        }
        return -1;
    }

}
