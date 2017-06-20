/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package android.irail.be.hyperrail.irail.implementation;

import android.irail.be.hyperrail.irail.db.Station;

import java.io.Serializable;
import java.util.Date;

/**
 * A transfer between two trains
 */
public class Transfer implements Serializable {

    private TrainStub arrivingTrain;
    private TrainStub departingTrain;
    private Date arrivalTime;
    private Date departureTime;

    private Station station;
    private String departurePlatform;
    private boolean isDeparturePlatformNormal;
    private String arrivalPlatform;
    private boolean isArrivalPlatformNormal;
    private int arrivalDelay;
    private boolean arrivalCanceled;
    private int departureDelay;
    private boolean departureCanceled;

    public Transfer(Station station, TrainStub arrivingTrain, TrainStub departingTrain, String arrivalPlatform, boolean arrivalNormal, String departurePlatform, boolean departureNormal, Date arrivalTime, Date departureTime, int arrivalDelay, boolean arrivalCanceled, int departureDelay, boolean departureCanceled) {
        this.station = station;
        this.arrivingTrain = arrivingTrain;
        this.departingTrain = departingTrain;
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
    }

    public TrainStub getArrivingTrain() {
        return arrivingTrain;
    }

    public TrainStub getDepartingTrain() {
        return departingTrain;
    }

    public Date getArrivalTime() {
        return arrivalTime;
    }

    public Date getDepartureTime() {
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

    public int getArrivalDelay() {
        return arrivalDelay;
    }

    public boolean isArrivalCanceled() {
        return arrivalCanceled;
    }

    public int getDepartureDelay() {
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
}
