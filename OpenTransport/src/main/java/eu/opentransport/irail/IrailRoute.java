/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package eu.opentransport.irail;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;

import java.io.Serializable;

import eu.opentransport.common.models.Message;
import eu.opentransport.common.models.Route;
import eu.opentransport.common.models.RouteLeg;
import eu.opentransport.common.models.Transfer;

/**
 * A route between 2 stations, which might consist of multiple vehicles with transfers in between
 */
public class IrailRoute implements Route, Serializable {
    private final RouteLeg[] legs;
    private final Transfer[] transfers;
    private Message[] alerts;
    private Message[][] trainalerts;
    private Message[] remarks;

    public IrailRoute(RouteLeg[] legs) {
        this.legs = legs;

        // Calculate transfers for easier access to the right data
        this.transfers = new IrailTransfer[legs.length + 1];
        this.transfers[0] = new IrailTransfer(null, legs[0]);
        this.transfers[legs.length] = new IrailTransfer(legs[legs.length - 1], null);

        for (int i = 1; i < legs.length; i++) {
            this.transfers[i] = new IrailTransfer(legs[i - 1], legs[i]);
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

    public int getStationCount() {
        return legs.length + 1;
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

    public IrailStation getDepartureStation() {
        return getDeparture().getStation();
    }

    public IrailStation getArrivalStation() {
        return getArrival().getStation();
    }

    public Message[] getRemarks() {
        return remarks;
    }

    public Message[] getAlerts() {
        return alerts;
    }

    public Message[][] getVehicleAlerts() {
        return trainalerts;
    }

    public boolean isPartiallyCanceled() {
        for (RouteLeg l :
                getLegs()) {
            if (l.getDeparture().isCanceled()) {
                return true;
            }
        }
        return false;
    }

    public Transfer[] getTransfers() {
        return transfers;
    }

    public void setTrainalerts(Message[][] trainalerts) {
        this.trainalerts = trainalerts;
    }

    public void setAlerts(Message[] alerts) {
        this.alerts = alerts;
    }

    public void setRemarks(Message[] remarks) {
        this.remarks = remarks;
    }
}
