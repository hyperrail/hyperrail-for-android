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
import org.joda.time.Duration;

import java.io.Serializable;

import be.hyperrail.android.irail.contracts.OccupancyLevel;
import be.hyperrail.android.irail.db.Station;

/**
 * A transfer between two trains
 */
public class Transfer implements Serializable {

    private final TrainStub arrivingTrain;
    private final TrainStub departingTrain;
    private final DateTime arrivalTime;
    private final DateTime departureTime;

    private final Station station;
    private final String departurePlatform;
    private final boolean isDeparturePlatformNormal;
    private final String arrivalPlatform;
    private final boolean isArrivalPlatformNormal;
    private final Duration arrivalDelay;
    private final boolean arrivalCanceled;
    private final Duration departureDelay;
    private final boolean departureCanceled;

    private final boolean left;
    private final boolean arrived;

    private final OccupancyLevel departureOccupancy;
    private final String departureConnectionSemanticId;

    public Transfer(Station station, TrainStub arrivingTrain, TrainStub departingTrain, String arrivalPlatform, boolean arrivalNormal, boolean arrived, boolean departureNormal, boolean left, String departurePlatform, Duration arrivalDelay, boolean arrivalCanceled, Duration departureDelay, boolean departureCanceled, DateTime arrivalTime, DateTime departureTime, String departureConnectionSemanticId, OccupancyLevel departureOccupancy) {
        this.station = station;
        this.arrivingTrain = arrivingTrain;
        this.departingTrain = departingTrain;
        this.arrived = arrived;
        this.left = left;
        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
        this.departurePlatform = departurePlatform;
        this.arrivalPlatform = arrivalPlatform;
        this.arrivalDelay = arrivalDelay;
        this.departureDelay = departureDelay;
        this.arrivalCanceled = arrivalCanceled;
        this.departureCanceled = departureCanceled;
        this.isDeparturePlatformNormal = departureNormal;
        this.isArrivalPlatformNormal = arrivalNormal;
        this.departureConnectionSemanticId = departureConnectionSemanticId;
        this.departureOccupancy = departureOccupancy;
    }

    public TrainStub getArrivingTrain() {
        return arrivingTrain;
    }

    public TrainStub getDepartingTrain() {
        return departingTrain;
    }

    public DateTime getArrivalTime() {
        return arrivalTime;
    }

    public DateTime getDepartureTime() {
        return departureTime;
    }

    public Station getStation() {
        return station;
    }

    public String getDeparturePlatform() {
        return departurePlatform;
    }

    public String getArrivalPlatform() {
        return arrivalPlatform;
    }

    public Duration getArrivalDelay() {
        return arrivalDelay;
    }

    public boolean isArrivalCanceled() {
        return arrivalCanceled;
    }

    public Duration getDepartureDelay() {
        return departureDelay;
    }

    public boolean isDepartureCanceled() {
        return departureCanceled;
    }

    public boolean isArrivalPlatformNormal() {
        return isArrivalPlatformNormal;
    }

    public boolean isDeparturePlatformNormal() {
        return isDeparturePlatformNormal;
    }

    public OccupancyLevel getDepartureOccupancy() {
        return departureOccupancy;
    }

    public String getDepartureConnectionSemanticId() {
        return departureConnectionSemanticId;
    }

    public boolean hasLeft() {
        return left;
    }

    public boolean hasArrived() {
        return arrived;
    }
}
