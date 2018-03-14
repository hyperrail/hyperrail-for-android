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
 * A trainstop, belonging to a certain train.
 * A trainstrop can either be shown in a liveboard (grouped by station) or a train (grouped by vehicle)
 */
public class VehicleStop implements Serializable {

    private final VehicleStub train;
    private final Station destination;
    private final Station station;

    private final boolean isPlatformNormal;
    private final DateTime departureTime;
    private final String platform;
    private boolean hasLeft;
    private final Duration departureDelay;
    private final boolean departureCanceled;

    private DateTime arrivalTime;
    private Duration arrivalDelay;
    private boolean arrivalCanceled;

    private final String departureSemanticId;
    private final OccupancyLevel occupancyLevel;

    private final VehicleStopType type;

    protected VehicleStop(Station station, Station destination, VehicleStub train, String platform, boolean isPlatformNormal, DateTime departureTime, DateTime arrivalTime, Duration departureDelay, Duration arrivalDelay, boolean departureCanceled, boolean arrivalCanceled, boolean hasLeft, String departureSemanticId, OccupancyLevel occupancyLevel, VehicleStopType type) {
        this.station = station;
        this.destination = destination;
        this.isPlatformNormal = isPlatformNormal;
        this.departureTime = departureTime;
        this.platform = platform;
        this.departureDelay = departureDelay;
        this.departureCanceled = departureCanceled;
        this.arrivalCanceled = departureCanceled;
        this.train = train;
        this.hasLeft = hasLeft;
        this.departureSemanticId = departureSemanticId;
        this.occupancyLevel = occupancyLevel;
        this.arrivalTime = arrivalTime;
        this.arrivalDelay = arrivalDelay;
        this.arrivalCanceled = arrivalCanceled;
        this.type = type;
    }


    protected static VehicleStop buildDepartureVehicleStop(Station station, Station destination, VehicleStub train, String platform, boolean isPlatformNormal, DateTime departureTime, Duration departureDelay, boolean departureCanceled, boolean hasLeft, String semanticDepartureConnection, OccupancyLevel occupancyLevel) {
        return new VehicleStop(station, destination, train, platform, isPlatformNormal,
                               departureTime, null, departureDelay, null,
                               departureCanceled, departureCanceled, hasLeft, semanticDepartureConnection, occupancyLevel, VehicleStopType.DEPARTURE);
    }

    protected static VehicleStop buildArrivalVehicleStop(Station station, Station destination, VehicleStub train, String platform, boolean isPlatformNormal, DateTime arrivalTime, Duration arrivalDelay, boolean arrivalCanceled, boolean hasLeft, String semanticDepartureConnection, OccupancyLevel occupancyLevel) {
        return new VehicleStop(station, destination, train, platform, isPlatformNormal,
                               null, arrivalTime, null, arrivalDelay,
                               arrivalCanceled, arrivalCanceled, hasLeft, semanticDepartureConnection, occupancyLevel, VehicleStopType.ARRIVAL);
    }


    public VehicleStub getVehicle() {
        return train;
    }

    public boolean hasLeft() {
        return hasLeft;
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

    public Station getDestination() {
        return destination;
    }

    public Station getStation() {
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

    public String getDepartureSemanticId() {
        return departureSemanticId;
    }

    public OccupancyLevel getOccupancyLevel() {
        return occupancyLevel;
    }

    public VehicleStopType getType() {
        return type;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof VehicleStop && (
                (this.getDepartureSemanticId() != null && this.getDepartureSemanticId().equals(((VehicleStop) obj).getDepartureSemanticId())) ||
                        (this.getStation().equals(((VehicleStop) obj).getStation()) && this.getVehicle().equals(((VehicleStop) obj).getVehicle()))
        );
    }
}

