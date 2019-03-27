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

package be.hyperrail.opentransportdata.common.models;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.io.Serializable;

/**
 * A route between 2 stations, which might consist of multiple vehicles with transfers in between
 */
public interface Route extends Serializable {

    Duration getDuration();

    Duration getDurationIncludingDelays();

    DateTime getDepartureTime();

    DateTime getArrivalTime();

    int getTransferCount();

    RouteLeg[] getLegs();

    Transfer getDeparture();

    Transfer getArrival();

    Duration getArrivalDelay();

    Duration getDepartureDelay();

    String getDeparturePlatform();

    String getArrivalPlatform();

    boolean isArrivalDeparturePlatformNormal();

    boolean isDeparturePlatformNormal();

    StopLocation getDepartureStation();

    StopLocation getArrivalStation();

    Message[] getRemarks();

    Message[] getAlerts();

    Message[][] getVehicleAlerts();

    boolean isPartiallyCanceled();

    /**
     * Get the transfers made in this journey.
     * Transfers including boarding the vehicle at the first stop and leaving it at the last.
     *
     * @return The transfers of this vehicle, including departure and arrival.
     */
    @Deprecated
    Transfer[] getTransfers();

    void setVehicleAlerts(Message[][] vehicleAlerts);

    void setAlerts(Message[] alerts);

    void setRemarks(Message[] remarks);
}
