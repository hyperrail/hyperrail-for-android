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

import java.io.Serializable;
import java.util.Date;

import be.hyperrail.android.irail.db.Station;

/**
 * A trainstop, belonging to a certain train.
 * A trainstrop can either be shown in a liveboard (grouped by station) or a train (grouped by vehicle)
 */
public class TrainStop implements Serializable {

    private final TrainStub train;
    private final Station destination;
    private final Station station;

    private final boolean isPlatformNormal;
    private final Date departureTime;
    private final String platform;
    private boolean hasLeft;
    private final int departureDelay;
    private final boolean departureCanceled;

    private Date arrivalTime;
    private int arrivalDelay;
    private boolean arrivalCanceled;

    protected TrainStop(Station station, Station destination, TrainStub train, String platform, boolean isPlatformNormal, Date departureTime, Date arrivalTime, int departureDelay, int arrivalDelay, boolean departureCanceled, boolean arrivalCanceled, boolean hasLeft) {
        this(station, destination, train, platform, isPlatformNormal, departureTime, departureDelay, departureCanceled, hasLeft);

        this.arrivalTime = arrivalTime;
        this.arrivalDelay = arrivalDelay;
        this.arrivalCanceled = arrivalCanceled;
    }

    protected TrainStop(Station station, Station destination, TrainStub train, String platform, boolean isPlatformNormal, Date departureTime, int departureDelay, boolean departureCanceled, boolean hasLeft) {
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
    }

    public TrainStub getTrain() {
        return train;
    }

    public boolean hasLeft() {
        return hasLeft;
    }

    public boolean isPlatformNormal() {
        return isPlatformNormal;
    }

    public Date getDepartureTime() {
        return departureTime;
    }

    public String getPlatform() {
        return platform;
    }

    public int getDepartureDelay() {
        return departureDelay;
    }

    public Date getDelayedDepartureTime() {
        return new Date(departureTime.getTime() + departureDelay * 1000);
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

    public int getArrivalDelay() {
        return arrivalDelay;
    }

    public Date getArrivalTime() {
        return arrivalTime;
    }

    public Date getDelayedArrivalTime() {
        return new Date(arrivalTime.getTime() + departureDelay * 1000);
    }

    public void setHasLeft(boolean hasLeft) {
        this.hasLeft = hasLeft;
    }
}

