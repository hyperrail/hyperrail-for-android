/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.common.models.implementation;

import android.support.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.io.Serializable;

import be.hyperrail.opentransportdata.common.contracts.TransportOccupancyLevel;
import be.hyperrail.opentransportdata.common.models.RouteLeg;
import be.hyperrail.opentransportdata.common.models.RouteLegEnd;
import be.hyperrail.opentransportdata.common.models.StopLocation;
import be.hyperrail.opentransportdata.common.models.Transfer;
import be.hyperrail.opentransportdata.common.models.TransferType;

/**
 * A transfer between two route legs.
 * This is a helper class, to make it easier displaying and using route information. It's constructed based on routeLegs.
 */
public class TransferImpl implements Transfer, Serializable {
    @Nullable
    private final RouteLeg mDepartureLeg;
    @Nullable
    private final RouteLeg mArrivalLeg;

    @Nullable
    private final RouteLegEnd mDeparture;
    @Nullable
    private final RouteLegEnd mArrival;

    private final TransferType type;

    TransferImpl(@Nullable RouteLeg arrival, @Nullable RouteLeg departure) {

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


    public StopLocation getStopLocation() {
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


    public Duration getArrivalDelay() {
        return (mArrival != null) ? mArrival.getDelay() : Duration.ZERO;
    }

    public boolean isArrivalCanceled() {
        return (mArrival != null) && mArrival.isCanceled();
    }


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
    public TransportOccupancyLevel getDepartureOccupancy() {
        return (mDeparture != null) ? mDeparture.getOccupancyLevel() : null;
    }

    @Nullable
    public String getDepartureSemanticId() {
        return (mDeparture != null) ? mDeparture.getSemanticId() : null;
    }

    public boolean hasLeft() {
        return (mDeparture != null) && mDeparture.isCompletedByVehicle();
    }

    public boolean hasArrived() {
        return (mArrival != null) && mArrival.isCompletedByVehicle();
    }

    public TransferType getType() {
        return type;
    }

    @Nullable
    public RouteLeg getArrivingLeg() {
        return mArrivalLeg;
    }

    @Nullable
    public RouteLeg getDepartingLeg() {
        return mDepartureLeg;
    }

    @Nullable
    public VehicleStopImpl getDepartingLegAsVehicleStop() {
        if (mDepartureLeg != null) {
            RouteLegEnd departure = mDepartureLeg.getDeparture();
            return VehicleStopImpl.buildDepartureVehicleStop(departure.getStation(),
                    mDepartureLeg.getVehicleInformation(), departure.getPlatform(), departure.isPlatformNormal(),
                    departure.getTime(), departure.getDelay(), departure.isCanceled(), departure.isCompletedByVehicle(),
                    departure.getSemanticId(), departure.getOccupancyLevel());
        } else {
            return null;
        }
    }
}
