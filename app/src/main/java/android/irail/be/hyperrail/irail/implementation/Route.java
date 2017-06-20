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
 * Created by Bert on 18-1-2017.
 */

public class Route implements Serializable {


    private Station departureStation;
    private Station arrivalStation;
    private Date departureTime;
    private Date arrivalTime;

    private int departureDelay;
    private int arrivalDelay;

    private String departurePlatform;
    private boolean isDeparturePlatformNormal;
    private String arrivalPlatform;
    private boolean isArrivalDeparturePlatformNormal;

    private TrainStub[] trains;
    private Transfer[] transfers;

    Route(Station departureStation, Station arrivalStation, Date departureTime, int departureDelay, String departurePlatform, boolean isDeparturePlatformNormal, Date arrivalTime, int arrivalDelay, String arrivalPlatform, boolean isArrivalDeparturePlatformNormal, TrainStub[] trains, Transfer[] transfers) {
        this.departureStation = departureStation;
        this.arrivalStation = arrivalStation;

        this.departureTime = departureTime;
        this.departureDelay = departureDelay;
        this.isDeparturePlatformNormal = isDeparturePlatformNormal;
        this.arrivalTime = arrivalTime;
        this.arrivalDelay = arrivalDelay;

        this.departurePlatform = departurePlatform;
        this.arrivalPlatform = arrivalPlatform;

        this.isArrivalDeparturePlatformNormal = isArrivalDeparturePlatformNormal;
        this.trains = trains;
        this.transfers = transfers;
    }

    public long getDuration() {
        return getArrivalTime().getTime() - getDepartureTime().getTime();
    }


    public Date getDepartureTime() {
        return departureTime;
    }

    public Date getArrivalTime() {
        return arrivalTime;
    }

    public Transfer getOrigin() {
        return transfers[0];
    }

    public Transfer getDestination() {
        return transfers[transfers.length - 1];
    }

    public int getTransferCount() {
        // minus origin and destination
        return transfers.length - 2;
    }

    public int getStationCount() {
        return transfers.length;
    }

    public TrainStub[] getTrains() {
        return trains;
    }

    public Transfer[] getTransfers() {
        return transfers;
    }

    public int getArrivalDelay() {
        return arrivalDelay;
    }

    public int getDepartureDelay() {
        return departureDelay;
    }

    public String getDeparturePlatform() {
        return departurePlatform;
    }

    public String getArrivalPlatform() {
        return arrivalPlatform;
    }

    public boolean isArrivalDeparturePlatformNormal() {
        return isArrivalDeparturePlatformNormal;
    }

    public boolean isDeparturePlatformNormal() {
        return isDeparturePlatformNormal;
    }

    public Station getDepartureStation() {
        return departureStation;
    }

    public Station getArrivalStation() {
        return arrivalStation;
    }
}
