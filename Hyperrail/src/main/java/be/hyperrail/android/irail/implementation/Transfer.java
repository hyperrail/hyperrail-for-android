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
 * A transfer between two route legs.
 * This is a helper class, to make it easier displaying and using route information. It's constructed based on routeLegs.
 */
public class Transfer implements Serializable {
    @Nullable
    private final RouteLeg mDepartureLeg;
    @Nullable
    private final RouteLeg mArrivalLeg;

    @Nullable
    private final RouteLegEnd mDeparture;
    @Nullable
    private final RouteLegEnd mArrival;

    private final TransferType type;

    protected Transfer(@Nullable RouteLeg arrival, @Nullable RouteLeg departure) {

        this.mDepartureLeg = departure;
        this.mArrivalLeg = arrival;

        if (departure != null && arrival != null) {
            type = TransferType.TRANSFER;
            this.mDeparture = departure.getDeparture();
            this.mArrival = arrival.getArrival();
        } else if (departure != null) {
            type = TransferType.DEPARTURE;
            this.mDeparture = departure.getDeparture();
            this.mArrival = null;
        } else if (arrival != null) {
            type = TransferType.ARRIVAL;
            this.mArrival = arrival.getArrival();
            this.mDeparture = null;
        } else {
            throw new IllegalStateException("A transfer needs at least a departure or arrival!");
        }
    }

    @Nullable
    public DateTime getArrivalTime() {
        return (mArrival != null) ? mArrival.getTime() : null;
    }

    @Nullable
    public DateTime getDepartureTime() {
        return (mDeparture != null) ? mDeparture.getTime() : null;
    }

    public DateTime getDelayedDepartureTime() {
        if (mDeparture == null) {
            return null;
        }
        return mDeparture.getTime().plus(mDeparture.getDelay());
    }

    public DateTime getDelayedArrivalTime() {
        if (mArrival == null) {
            return null;
        }
        return mArrival.getTime().plus(mArrival.getDelay());
    }

    @NonNull
    public Station getStation() {
        if (mDeparture != null) {
            return mDeparture.getStation();
        } else if (mArrival != null) {
            return mArrival.getStation();
        } else {
            throw new IllegalStateException("A transfer needs at least a departure or arrival!");
        }
    }

    @Nullable
    public String getDeparturePlatform() {
        return (mDeparture != null) ? mDeparture.getPlatform() : null;
    }

    @Nullable
    public String getArrivalPlatform() {
        return (mArrival != null) ? mArrival.getPlatform() : null;
    }

    @NonNull
    public Duration getArrivalDelay() {
        return (mArrival != null) ? mArrival.getDelay() : Duration.ZERO;
    }

    public boolean isArrivalCanceled() {
        return (mArrival != null) && mArrival.isCanceled();
    }

    @NonNull
    public Duration getDepartureDelay() {
        return (mDeparture != null) ? mDeparture.getDelay() : Duration.ZERO;
    }

    public boolean isDepartureCanceled() {
        return (mDeparture != null) && mDeparture.isCanceled();
    }

    public boolean isArrivalPlatformNormal() {
        return (mArrival != null) && mArrival.isPlatformNormal();
    }

    public boolean isDeparturePlatformNormal() {
        return (mDeparture != null) && mDeparture.isPlatformNormal();
    }

    @Nullable
    public OccupancyLevel getDepartureOccupancy() {
        return (mDeparture != null) ? mDeparture.getOccupancy() : null;
    }

    @Nullable
    public String getDepartureSemanticId() {
        return (mDeparture != null) ? mDeparture.getUri() : null;
    }

    public boolean hasLeft() {
        return (mDeparture != null) && mDeparture.hasPassed();
    }

    public boolean hasArrived() {
        return (mArrival != null) && mArrival.hasPassed();
    }

    public TransferType getType() {
        return type;
    }

    @Nullable
    public RouteLeg getArrivalLeg() {
        return mArrivalLeg;
    }

    @Nullable
    public RouteLeg getDepartureLeg() {
        return mDepartureLeg;
    }

    @Nullable
    public VehicleStop toDepartureVehicleStop() {
        if (mDepartureLeg != null) {
            return new VehicleStop(mDepartureLeg);
        } else {
            return null;
        }
    }
}
