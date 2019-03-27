/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.common.models.implementation;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;

import java.io.Serializable;

import be.hyperrail.opentransportdata.common.models.Message;
import be.hyperrail.opentransportdata.common.models.Route;
import be.hyperrail.opentransportdata.common.models.RouteLeg;
import be.hyperrail.opentransportdata.common.models.StopLocation;
import be.hyperrail.opentransportdata.common.models.Transfer;

/**
 * A route between 2 stations, which might consist of multiple vehicles with transfers in between
 */
public class RouteImpl implements Route, Serializable {
    private final RouteLeg[] legs;
    private final Transfer[] transfers;
    private Message[] alerts;
    private Message[][] vehicleAlerts;
    private Message[] remarks;

    public RouteImpl(RouteLeg[] legs) {
        this.legs = legs;

        // Calculate transfers for easier access to the right data
        this.transfers = new TransferImpl[legs.length + 1];
        this.transfers[0] = new TransferImpl(null, legs[0]);
        this.transfers[legs.length] = new TransferImpl(legs[legs.length - 1], null);

        for (int i = 1; i < legs.length; i++) {
            this.transfers[i] = new TransferImpl(legs[i - 1], legs[i]);
        }
    }

    public Duration getDuration() {
        return new Period(getDepartureTime(), getArrivalTime()).toStandardDuration();
    }

    public Duration getDurationIncludingDelays() {
        return new Period(getDepartureTime().plus(getDepartureDelay()), getArrivalTime().plus(getArrivalDelay())).toStandardDuration();
    }

    public DateTime getDepartureTime() {
        return getDeparture().getDepartureTime();
    }

    public DateTime getArrivalTime() {
        return getArrival().getArrivalTime();
    }

    public int getTransferCount() {
        // minus origin and destination
        return legs.length - 1;
    }

    public RouteLeg[] getLegs() {
        return legs;
    }

    public Transfer getDeparture() {
        return transfers[0];
    }

    public Transfer getArrival() {
        return transfers[transfers.length - 1];
    }

    public Duration getArrivalDelay() {
        return getArrival().getArrivalDelay();
    }

    public Duration getDepartureDelay() {
        return getDeparture().getDepartureDelay();
    }

    public String getDeparturePlatform() {
        return getDeparture().getDeparturePlatform();
    }

    public String getArrivalPlatform() {
        return getArrival().getArrivalPlatform();
    }

    public boolean isArrivalDeparturePlatformNormal() {
        return getArrival().isArrivalPlatformNormal();
    }

    public boolean isDeparturePlatformNormal() {
        return getDeparture().isDeparturePlatformNormal();
    }

    public StopLocation getDepartureStation() {
        return getDeparture().getStation();
    }

    public StopLocation getArrivalStation() {
        return getArrival().getStation();
    }

    public Message[] getRemarks() {
        return remarks;
    }

    public Message[] getAlerts() {
        return alerts;
    }

    public Message[][] getVehicleAlerts() {
        return vehicleAlerts;
    }

    public boolean isPartiallyCanceled() {
        for (RouteLeg l :
                getLegs()) {
            if (l.getDeparture().isCanceled() || l.getArrival().isCanceled()) {
                return true;
            }
        }
        return false;
    }

    public Transfer[] getTransfers() {
        return transfers;
    }

    public void setVehicleAlerts(Message[][] vehicleAlerts) {
        this.vehicleAlerts = vehicleAlerts;
    }

    public void setAlerts(Message[] alerts) {
        this.alerts = alerts;
    }

    public void setRemarks(Message[] remarks) {
        this.remarks = remarks;
    }
}
