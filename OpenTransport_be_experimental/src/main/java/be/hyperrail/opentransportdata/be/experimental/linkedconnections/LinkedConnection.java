/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.be.experimental.linkedconnections;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

import org.joda.time.DateTime;

/**
 * A LinkedConnection entry, which can be parsed using the LoganSquare parser.
 */
@JsonObject
class LinkedConnection {

    @JsonField(name = "@id")
    private String semanticId;
    @JsonField(name = "departureStop")
    private String departureStationUri;
    @JsonField(name = "arrivalStop")
    private String arrivalStationUri;
    @JsonField(name = "departureTime", typeConverter = DateTimeConverter.class)
    private DateTime departureTime;
    @JsonField(name = "arrivalTime", typeConverter = DateTimeConverter.class)
    private DateTime arrivalTime;
    @JsonField(name = "departureDelay")
    private int departureDelay = 0;
    @JsonField(name = "arrivalDelay")
    private int arrivalDelay = 0;
    @JsonField(name = "direction")
    private String direction;
    @JsonField(name = "gtfs:route")
    private String route;
    @JsonField(name = "gtfs:trip")
    private String trip;
    @JsonField(name = "gtfs:pickupType")
    private String pickupType;
    @JsonField(name = "gtfs:dropOffType")
    private String dropoffType;


    DateTime getDelayedDepartureTime() {
        return getDepartureTime().plusSeconds(getDepartureDelay());
    }

    DateTime getDelayedArrivalTime() {
        return getArrivalTime().plusSeconds(getArrivalDelay());
    }

    String getSemanticId() {
        return semanticId;
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

    DateTime getArrivalTime() {
        return arrivalTime;
    }

    void setArrivalTime(DateTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    int getDepartureDelay() {
        return departureDelay;
    }

    int getArrivalDelay() {
        return arrivalDelay;
    }

    String getDirection() {
        return direction;
    }

    String getRoute() {
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

