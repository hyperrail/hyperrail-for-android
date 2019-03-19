/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.common.models;


import java.io.Serializable;

import be.hyperrail.opentransportdata.irail.IrailVehicleStop;
import be.hyperrail.opentransportdata.irail.IrailVehicleStub;

/**
 * A leg of a route
 */
public interface RouteLeg extends Serializable {
 
    /**
     * The type of transport used for this leg of the route
     *
     * @return the type of transport used for this leg of the route
     */
    RouteLegType getType();

    IrailVehicleStub getVehicleInformation();

    /**
     * The departure end for this leg
     *
     * @return The departure end for this leg
     */
    RouteLegEnd getDeparture();

    /**
     * The arrival end for this leg
     *
     * @return The arrival end for this leg
     */
    RouteLegEnd getArrival();

    /**
     * Get the stops whether this vehicle will halt, but the traveller stays on the train.
     * <p>
     * Null: unknown/unavailable
     * Empty array: no stops
     * Array: stops
     */
    IrailVehicleStop[] getIntermediaryStops();
}
