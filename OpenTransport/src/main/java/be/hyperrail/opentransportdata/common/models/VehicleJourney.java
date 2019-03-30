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

package be.hyperrail.opentransportdata.common.models;

import org.joda.time.DateTime;

import be.hyperrail.opentransportdata.common.models.implementation.VehicleStopImpl;

/**
 * This class represents a vehicle entity.
 * This class extends a VehicleJourneyStub with its stops.
 */
public interface VehicleJourney extends VehicleJourneyStub {

    /**
     * Get the origin station of this vehicle.
     * @return The station where the vehicle departed
     */
    StopLocation getFirstStopLocation();

    /**
     * Get the final stop of this train
     *
     * @return The station where the vehicle has reached its destination
     */
    StopLocation getLastStopLocation();

    /**
     * Get the list of all stops this vehicle will make
     *
     * @return List of stop events
     */
    VehicleStopImpl[] getStops();

    /**
     * Get the last stop where this vehicle halted
     * @return The stop event where the vehicle last halted.
     */
    VehicleStopImpl getLastHaltedStop();

    /**
     * Get the current longitude of this vehicle
     * @return The current longitude in degrees.
     */
    double getCurrentPostionLongitude();

    /**
     * Get the current latitude of this vehicle
     * @return The current latitude in degrees.
     */
    double getCurrentPositionLatitude();

    /**
     * Get zero-based index for this station in the stops list.
     *
     * @param station The station to search for.
     * @return Get zero-based index for this station in the stops list. -1 if this stop doesn't exist.
     */
    int getIndexForStoplocation(StopLocation station);

    /**
     * Get zero-based index for this departure time in the stops list.
     *
     * @param time The datetime to search for
     * @return Get zero-based index for this station in the stops list. -1 if this stop doesn't exist.
     */
    int getIndexForDepartureTime(DateTime time);

}
