/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.linkedconnections;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

import org.joda.time.DateTime;

/**
 * Created in be.hyperrail.android.irail.implementation.linkedconnections on 15/03/2018.
 */
@JsonObject
class LinkedConnection {

    @JsonField(name = "@id")
    protected String uri;
    @JsonField(name = "departureStop")
    protected String departureStationUri;
    @JsonField(name = "arrivalStop")
    protected String arrivalStationUri;
    @JsonField(name = "departureTime", typeConverter = DateTimeConverter.class)
    protected DateTime departureTime;
    @JsonField(name = "arrivalTime", typeConverter = DateTimeConverter.class)
    protected DateTime arrivalTime;
    @JsonField(name = "departureDelay")
    protected int departureDelay = 0;
    @JsonField(name = "arrivalDelay")
    protected int arrivalDelay = 0;
    @JsonField(name = "direction")
    protected String direction;
    @JsonField(name = "gtfs:route")
    protected String route;
    @JsonField(name = "gtfs:trip")
    protected String trip;
    @JsonField(name = "gtfs:pickupType")
    protected String pickupType;
    @JsonField(name = "gtfs:dropOffType")
    protected String dropoffType;


    DateTime getDelayedDepartureTime() {
        return getDepartureTime().plusSeconds(getDepartureDelay());
    }

    DateTime getDelayedArrivalTime() {
        return getArrivalTime().plusSeconds(getArrivalDelay());
    }

    public String getUri() {
        return uri;
    }

    public String getDepartureStationUri() {
        return departureStationUri;
    }

    public String getArrivalStationUri() {
        return arrivalStationUri;
    }

    public DateTime getDepartureTime() {
        return departureTime;
    }


    public DateTime getArrivalTime() {
        return arrivalTime;
    }

    protected void setArrivalTime(DateTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public int getDepartureDelay() {
        return departureDelay;
    }

    public int getArrivalDelay() {
        return arrivalDelay;
    }

    public String getDirection() {
        return direction;
    }

    public String getRoute() {
        return route;
    }

    public String getTrip() {
        return trip;
    }

    protected void setTrip(String trip) {
        this.trip = trip;
    }

    public boolean isNormal(){
        return pickupType != null && dropoffType != null && pickupType.equals("gtfs:Regular") && dropoffType.equals("gtfs:Regular");
    }
}

