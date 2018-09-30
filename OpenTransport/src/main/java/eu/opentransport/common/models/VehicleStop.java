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

package eu.opentransport.common.models;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.io.Serializable;

import eu.opentransport.common.contracts.TransportOccupancyLevel;
import eu.opentransport.irail.IrailStation;

/**
 * A vehicle stop, belonging to a certain vehicle.
 * A vehicle stop can either be shown in a liveboard (grouped by station) or a vehicle (grouped by vehicle)
 */
public interface VehicleStop extends Serializable {

    /**
     * Get the arrival delay.
     *
     * @return The amount of time by which the arrival was delayed.
     */
    Duration getArrivalDelay();

    /**
     * Get the scheduled arrival time.
     *
     * @return The scheduled arrival time.
     */
    DateTime getArrivalTime();

    /**
     * Get the delayed arrival time.
     *
     * @return The sum of the scheduled arrival time and the arrival delay.
     */
    DateTime getDelayedArrivalTime();

    /**
     * Get the delayed departure time.
     *
     * @return The sum of the scheduled departure time and the departure delay.
     */
    DateTime getDelayedDepartureTime();

    /**
     * Get the departure delay.
     *
     * @return The amount of time by which the departure was delayed.
     */
    Duration getDepartureDelay();

    /**
     * Get the scheduled departure time.
     *
     * @return The scheduled departure time.
     */
    DateTime getDepartureTime();

    /**
     * Get the URI which identifies the departure at this stop
     *
     * @return
     */
    String getDepartureUri();

    /**
     * The headsign of the vehicle when arriving at this stop.
     *
     * @return
     */
    String getHeadsign();

    /**
     * Get the occupancy level of the vehicle when leaving this stop
     *
     * @return
     */
    TransportOccupancyLevel getOccupancyLevel();

    /**
     * Get the platform where this stop takes place.
     *
     * @return The name of the platform.
     */
    String getPlatform();

    /**
     * Get the stop at which this stop event takes place
     *
     * @return
     */
    IrailStation getStation();

    /**
     * Get the type of stop the vehicle makes
     *
     * @return
     */
    VehicleStopType getType();

    /**
     * Get the vehicle which makes this stop.
     *
     * @return The vehicleStub describing the vehicle which makes this stop.
     */
    VehicleStub getVehicle();

    /**
     * Whether or not the vehicle has already left this halting point.
     *
     * @return True in case the vehicle has departed from this halting point.
     */
    boolean hasLeft();

    /**
     * Whether or not the vehicle has already arrived at this halting point.
     *
     * @return True in case the vehicle has arrived at this halting point.
     */
    boolean hasArrived();

    /**
     * Whether or not the arrival of the vehicle is canceled
     *
     * @return
     */
    boolean isArrivalCanceled();

    /**
     * Whether or not the departure at this stop was canceled.
     *
     * @return
     */
    boolean isDepartureCanceled();

    /**
     * Whether or not the vehicle will stop or has stopped at the scheduled platform.
     *
     * @return true in case the platform was not changed. False in case the platform was changed..
     */
    boolean isPlatformNormal();

}

