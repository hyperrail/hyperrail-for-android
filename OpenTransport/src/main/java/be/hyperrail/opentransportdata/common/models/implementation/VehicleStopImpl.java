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

package be.hyperrail.opentransportdata.common.models.implementation;

import androidx.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.io.Serializable;

import be.hyperrail.opentransportdata.common.contracts.TransportOccupancyLevel;
import be.hyperrail.opentransportdata.common.models.StopLocation;
import be.hyperrail.opentransportdata.common.models.VehicleJourneyStub;
import be.hyperrail.opentransportdata.common.models.VehicleStop;
import be.hyperrail.opentransportdata.common.models.VehicleStopType;

/**
 * A vehicle stop, belonging to a certain vehicle.
 * A vehicle stop can either be shown in a liveboard (grouped by station) or a vehicle (grouped by vehicle)
 */
public class VehicleStopImpl implements VehicleStop, Serializable {

    private final VehicleJourneyStub vehicle;

    private final StopLocation station;

    private final String platform;
    private final boolean isPlatformNormal;
    private boolean hasLeft;

    private final DateTime departureTime;

    private final Duration departureDelay;
    private final boolean departureCanceled;

    private DateTime arrivalTime;

    private Duration arrivalDelay;
    private boolean arrivalCanceled;

    private final String departureUri;
    private final TransportOccupancyLevel occupancyLevel;

    private final VehicleStopType type;

    public VehicleStopImpl(StopLocation station, VehicleJourneyStub vehicle, String platform, boolean isPlatformNormal, @Nullable DateTime departureTime, @Nullable DateTime arrivalTime, Duration departureDelay, Duration arrivalDelay, boolean departureCanceled, boolean arrivalCanceled, boolean hasLeft, String departureUri, TransportOccupancyLevel occupancyLevel, VehicleStopType type) {
        this.station = station;
        this.isPlatformNormal = isPlatformNormal;
        this.departureTime = departureTime;
        this.platform = platform;

        if (departureDelay == null) {
            this.departureDelay = Duration.ZERO;
        } else {
            this.departureDelay = departureDelay;
        }

        this.departureCanceled = departureCanceled;
        this.arrivalCanceled = departureCanceled;
        this.vehicle = vehicle;
        this.hasLeft = hasLeft;
        this.departureUri = departureUri;
        this.occupancyLevel = occupancyLevel;
        this.arrivalTime = arrivalTime;

        if (arrivalDelay == null) {
            this.arrivalDelay = Duration.ZERO;
        } else {
            this.arrivalDelay = arrivalDelay;
        }

        this.arrivalCanceled = arrivalCanceled;
        this.type = type;
    }

    public static VehicleStopImpl buildDepartureVehicleStop(StopLocation station, VehicleJourneyStub train, String platform, boolean isPlatformNormal, DateTime departureTime, Duration departureDelay, boolean departureCanceled, boolean hasLeft, String semanticDepartureConnection, TransportOccupancyLevel occupancyLevel) {
        return new VehicleStopImpl(station, train, platform, isPlatformNormal,
                departureTime, null, departureDelay, Duration.ZERO,
                departureCanceled, departureCanceled, hasLeft, semanticDepartureConnection, occupancyLevel, VehicleStopType.DEPARTURE);
    }

    public static VehicleStopImpl buildArrivalVehicleStop(StopLocation station, VehicleJourneyStub train, String platform, boolean isPlatformNormal, DateTime arrivalTime, Duration arrivalDelay, boolean arrivalCanceled, boolean hasLeft, String semanticDepartureConnection, TransportOccupancyLevel occupancyLevel) {
        return new VehicleStopImpl(station, train, platform, isPlatformNormal,
                null, arrivalTime, Duration.ZERO, arrivalDelay,
                arrivalCanceled, arrivalCanceled, hasLeft, semanticDepartureConnection, occupancyLevel, VehicleStopType.ARRIVAL);
    }


    public VehicleJourneyStub getVehicle() {
        return vehicle;
    }

    public boolean hasLeft() {
        return hasLeft;
    }

    @Override
    public boolean hasArrived() {
        return false;
    }

    public boolean isPlatformNormal() {
        return isPlatformNormal;
    }

    public DateTime getDepartureTime() {
        return departureTime;
    }


    public String getPlatform() {
        return platform;
    }


    public Duration getDepartureDelay() {
        return departureDelay;
    }

    public DateTime getDelayedDepartureTime() {
        if (departureTime == null) {
            return null;
        }
        return departureTime.plus(departureDelay);
    }

    public boolean isDepartureCanceled() {
        return departureCanceled;
    }

    public String getHeadsign() {
        return vehicle.getHeadsign();
    }

    public StopLocation getStopLocation() {
        return station;
    }

    public boolean isArrivalCanceled() {
        return arrivalCanceled;
    }

    public Duration getArrivalDelay() {
        return arrivalDelay;
    }

    public DateTime getArrivalTime() {
        return arrivalTime;
    }

    public DateTime getDelayedArrivalTime() {
        if (arrivalTime == null) {
            return null;
        }
        return arrivalTime.plus(arrivalDelay);
    }

    public void setHasLeft(boolean hasLeft) {
        this.hasLeft = hasLeft;
    }


    public String getDepartureUri() {
        return departureUri;
    }

    public TransportOccupancyLevel getOccupancyLevel() {
        return occupancyLevel;
    }


    public VehicleStopType getType() {
        return type;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof VehicleStopImpl && (
                (this.getDepartureUri() != null && this.getDepartureUri().equals(((VehicleStopImpl) obj).getDepartureUri())) ||
                        (this.getStopLocation().equals(((VehicleStopImpl) obj).getStopLocation()) && this.getVehicle().equals(((VehicleStopImpl) obj).getVehicle()))
        );
    }
}

