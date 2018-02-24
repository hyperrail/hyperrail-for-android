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
import org.joda.time.Duration;

import java.io.Serializable;

import be.hyperrail.android.irail.contracts.OccupancyLevel;
import be.hyperrail.android.irail.db.Station;

/**
 * A transfer between two trains
 */
public class Transfer implements Serializable {

    @Nullable
    private final TrainStub arrivingTrain;
    @Nullable
    private final TrainStub departingTrain;
    @Nullable
    private final DateTime arrivalTime;
    @Nullable
    private final DateTime departureTime;

    @NonNull
    private final Station station;

    @Nullable
    private final String departurePlatform;
    private final boolean isDeparturePlatformNormal;
    @Nullable
    private final String arrivalPlatform;
    private final boolean isArrivalPlatformNormal;
    @Nullable
    private final Duration arrivalDelay;
    private final boolean arrivalCanceled;
    @Nullable
    private final Duration departureDelay;
    private final boolean departureCanceled;

    private final boolean left;
    private final boolean arrived;

    @Nullable
    private final OccupancyLevel departureOccupancy;

    @Nullable
    private final String departureSemanticId;

    private final TransferType type;

    public Transfer(@NonNull Station station, @Nullable TrainStub arrivingTrain, @Nullable TrainStub departingTrain, @Nullable String arrivalPlatform, boolean arrivalNormal, boolean arrived, boolean departureNormal, boolean left, @Nullable String departurePlatform, @Nullable Duration arrivalDelay, boolean arrivalCanceled, @Nullable Duration departureDelay, boolean departureCanceled, @Nullable DateTime arrivalTime, @Nullable DateTime departureTime, @Nullable String departureSemanticId, @Nullable OccupancyLevel departureOccupancy, TransferType type) {
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
        this.departureSemanticId = departureSemanticId;
        this.departureOccupancy = departureOccupancy;
        this.type = type;
    }

    @Nullable
    public TrainStub getArrivingTrain() {
        return arrivingTrain;
    }

    @Nullable
    public TrainStub getDepartingTrain() {
        return departingTrain;
    }

    @Nullable
    public DateTime getArrivalTime() {
        return arrivalTime;
    }

    @Nullable
    public DateTime getDepartureTime() {
        return departureTime;
    }

    public DateTime getDelayedDepartureTime() {
        if (departureTime == null) {
            return null;
        }
        return departureTime.plus(departureDelay);
    }

    public DateTime getDelayedArrivalTime() {
        if (arrivalTime == null) {
            return null;
        }
        return arrivalTime.plus(arrivalDelay);
    }

    @NonNull
    public Station getStation() {
        return station;
    }

    @Nullable
    public String getDeparturePlatform() {
        return departurePlatform;
    }

    @Nullable
    public String getArrivalPlatform() {
        return arrivalPlatform;
    }

    @Nullable
    public Duration getArrivalDelay() {
        return arrivalDelay;
    }

    public boolean isArrivalCanceled() {
        return arrivalCanceled;
    }

    @Nullable
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

    @Nullable
    public OccupancyLevel getDepartureOccupancy() {
        return departureOccupancy;
    }

    @Nullable
    public String getDepartureSemanticId() {
        return departureSemanticId;
    }

    public boolean hasLeft() {
        return left;
    }

    public boolean hasArrived() {
        return arrived;
    }

    public TransferType getType() {
        return type;
    }
}
