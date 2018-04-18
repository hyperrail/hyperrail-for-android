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
 * A vehicle stop, belonging to a certain vehicle.
 * A vehicle stop can either be shown in a liveboard (grouped by station) or a vehicle (grouped by vehicle)
 */
public class VehicleStop implements Serializable {
    @NonNull
    private final VehicleStub vehicle;
    @NonNull
    private final Station station;

    @NonNull
    private final String platform;
    private final boolean isPlatformNormal;
    private boolean hasLeft;

    private final DateTime departureTime;
    @NonNull
    private final Duration departureDelay;
    private final boolean departureCanceled;

    private DateTime arrivalTime;
    @NonNull
    private Duration arrivalDelay;
    private boolean arrivalCanceled;
    @NonNull
    private final String departureUri;
    private final OccupancyLevel occupancyLevel;
    @NonNull
    private final VehicleStopType type;

    protected VehicleStop(@NonNull Station station, @NonNull VehicleStub vehicle, @NonNull String platform, boolean isPlatformNormal, @Nullable DateTime departureTime, @Nullable DateTime arrivalTime, @NonNull Duration departureDelay, @NonNull Duration arrivalDelay, boolean departureCanceled, boolean arrivalCanceled, boolean hasLeft, @NonNull String departureUri, @NonNull OccupancyLevel occupancyLevel, @NonNull VehicleStopType type) {
        this.station = station;
        this.isPlatformNormal = isPlatformNormal;
        this.departureTime = departureTime;
        this.platform = platform;
        this.departureDelay = departureDelay;
        this.departureCanceled = departureCanceled;
        this.arrivalCanceled = departureCanceled;
        this.vehicle = vehicle;
        this.hasLeft = hasLeft;
        this.departureUri = departureUri;
        this.occupancyLevel = occupancyLevel;
        this.arrivalTime = arrivalTime;
        this.arrivalDelay = arrivalDelay;
        this.arrivalCanceled = arrivalCanceled;
        this.type = type;
    }

    public VehicleStop(RouteLeg departureLeg) {
        this.station = departureLeg.getDeparture().getStation();
        this.isPlatformNormal = departureLeg.getDeparture().isPlatformNormal();
        this.departureTime = departureLeg.getDeparture().getTime();
        this.platform = departureLeg.getDeparture().getPlatform();
        this.departureDelay = departureLeg.getDeparture().getDelay();
        this.departureCanceled = departureLeg.getDeparture().isCanceled();
        this.arrivalCanceled = departureCanceled;
        this.vehicle = departureLeg.getVehicleInformation();
        this.hasLeft = departureLeg.getDeparture().hasPassed();
        this.departureUri = departureLeg.getDeparture().getUri();
        this.occupancyLevel = departureLeg.getDeparture().getOccupancy();
        this.arrivalTime = null;
        this.arrivalDelay = new Duration(0);
        this.arrivalCanceled = false;
        this.type = VehicleStopType.DEPARTURE;
    }


    protected static VehicleStop buildDepartureVehicleStop(Station station, VehicleStub train, String platform, boolean isPlatformNormal, DateTime departureTime, Duration departureDelay, boolean departureCanceled, boolean hasLeft, String semanticDepartureConnection, OccupancyLevel occupancyLevel) {
        return new VehicleStop(station, train, platform, isPlatformNormal,
                               departureTime, null, departureDelay, new Duration(0),
                               departureCanceled, departureCanceled, hasLeft, semanticDepartureConnection, occupancyLevel, VehicleStopType.DEPARTURE);
    }

    protected static VehicleStop buildArrivalVehicleStop(Station station, VehicleStub train, String platform, boolean isPlatformNormal, DateTime arrivalTime, Duration arrivalDelay, boolean arrivalCanceled, boolean hasLeft, String semanticDepartureConnection, OccupancyLevel occupancyLevel) {
        return new VehicleStop(station, train, platform, isPlatformNormal,
                               null, arrivalTime, new Duration(0), arrivalDelay,
                               arrivalCanceled, arrivalCanceled, hasLeft, semanticDepartureConnection, occupancyLevel, VehicleStopType.ARRIVAL);
    }


    @NonNull
    public VehicleStub getVehicle() {
        return vehicle;
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

    @NonNull
    public String getPlatform() {
        return platform;
    }

    @NonNull
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
        return vehicle.headsign;
    }

    @NonNull
    public Station getStation() {
        return station;
    }

    public boolean isArrivalCanceled() {
        return arrivalCanceled;
    }

    @NonNull
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

    @NonNull
    public String getDepartureUri() {
        return departureUri;
    }

    public OccupancyLevel getOccupancyLevel() {
        return occupancyLevel;
    }

    @NonNull
    public VehicleStopType getType() {
        return type;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof VehicleStop && (
                (this.getDepartureUri() != null && this.getDepartureUri().equals(((VehicleStop) obj).getDepartureUri())) ||
                        (this.getStation().equals(((VehicleStop) obj).getStation()) && this.getVehicle().equals(((VehicleStop) obj).getVehicle()))
        );
    }
}

