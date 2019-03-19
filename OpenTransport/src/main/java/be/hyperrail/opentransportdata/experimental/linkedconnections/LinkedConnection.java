/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.experimental.linkedconnections;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

import org.joda.time.DateTime;

/**
 * A LinkedConnection entry, which can be parsed using the LoganSquare parser.
 */
@JsonObject
class LinkedConnection {

    @JsonField(name = "@id")
    protected String uri;
    @JsonField(name = "departureStop")
    String departureStationUri;
    @JsonField(name = "arrivalStop")
    String arrivalStationUri;
    @JsonField(name = "departureTime", typeConverter = DateTimeConverter.class)
    DateTime departureTime;
    @JsonField(name = "arrivalTime", typeConverter = DateTimeConverter.class)
    private DateTime arrivalTime;
    @JsonField(name = "departureDelay")
    int departureDelay = 0;
    @JsonField(name = "arrivalDelay")
    int arrivalDelay = 0;
    @JsonField(name = "direction")
    protected String direction;
    @JsonField(name = "gtfs:route")
    protected String route;
    @JsonField(name = "gtfs:trip")
    private String trip;
    @JsonField(name = "gtfs:pickupType")
    String pickupType;
    @JsonField(name = "gtfs:dropOffType")
    String dropoffType;


    DateTime getDelayedDepartureTime() {
        return getDepartureTime().plusSeconds(getDepartureDelay());
    }

    DateTime getDelayedArrivalTime() {
        return getArrivalTime().plusSeconds(getArrivalDelay());
    }

    public String getUri() {
        return uri;
    }

    String getDepartureStationUri() {
        return departureStationUri;
    }

    String getArrivalStationUri() {
        return arrivalStationUri;
    }

    public DateTime getDepartureTime() {
        return departureTime;
    }

    public DateTime getArrivalTime() {
        return arrivalTime;
    }

    void setArrivalTime(DateTime arrivalTime) {
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

    String getTrip() {
        return trip;
    }

    void setTrip(String trip) {
        this.trip = trip;
    }

    boolean isNormal() {
        return pickupType != null && dropoffType != null && pickupType.equals("gtfs:Regular") && dropoffType.equals("gtfs:Regular");
    }
}

