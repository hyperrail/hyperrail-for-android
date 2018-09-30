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

package eu.opentransport.irail;

import android.support.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.io.Serializable;

import eu.opentransport.common.contracts.TransportOccupancyLevel;
import eu.opentransport.common.models.RouteLeg;
import eu.opentransport.common.models.VehicleStop;
import eu.opentransport.common.models.VehicleStopType;

/**
 * A vehicle stop, belonging to a certain vehicle.
 * A vehicle stop can either be shown in a liveboard (grouped by station) or a vehicle (grouped by vehicle)
 */
public class IrailVehicleStop implements VehicleStop, Serializable {

    private final IrailVehicleStub vehicle;

    private final IrailStation station;


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

    public IrailVehicleStop(IrailStation station, IrailVehicleStub vehicle, String platform, boolean isPlatformNormal, @Nullable DateTime departureTime, @Nullable DateTime arrivalTime, Duration departureDelay, Duration arrivalDelay, boolean departureCanceled, boolean arrivalCanceled, boolean hasLeft, String departureUri, TransportOccupancyLevel occupancyLevel, VehicleStopType type) {
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

    public IrailVehicleStop(RouteLeg departureLeg) {
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


    public static IrailVehicleStop buildDepartureVehicleStop(IrailStation station, IrailVehicleStub train, String platform, boolean isPlatformNormal, DateTime departureTime, Duration departureDelay, boolean departureCanceled, boolean hasLeft, String semanticDepartureConnection, TransportOccupancyLevel occupancyLevel) {
        return new IrailVehicleStop(station, train, platform, isPlatformNormal,
                                    departureTime, null, departureDelay, new Duration(0),
                                    departureCanceled, departureCanceled, hasLeft, semanticDepartureConnection, occupancyLevel, VehicleStopType.DEPARTURE);
    }

    public static IrailVehicleStop buildArrivalVehicleStop(IrailStation station, IrailVehicleStub train, String platform, boolean isPlatformNormal, DateTime arrivalTime, Duration arrivalDelay, boolean arrivalCanceled, boolean hasLeft, String semanticDepartureConnection, TransportOccupancyLevel occupancyLevel) {
        return new IrailVehicleStop(station, train, platform, isPlatformNormal,
                                    null, arrivalTime, new Duration(0), arrivalDelay,
                                    arrivalCanceled, arrivalCanceled, hasLeft, semanticDepartureConnection, occupancyLevel, VehicleStopType.ARRIVAL);
    }


    public IrailVehicleStub getVehicle() {
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
        return vehicle.headsign;
    }

    public IrailStation getStation() {
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
        return obj instanceof IrailVehicleStop && (
                (this.getDepartureUri() != null && this.getDepartureUri().equals(((IrailVehicleStop) obj).getDepartureUri())) ||
                        (this.getStation().equals(((IrailVehicleStop) obj).getStation()) && this.getVehicle().equals(((IrailVehicleStop) obj).getVehicle()))
        );
    }
}

