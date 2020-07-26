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


package be.hyperrail.opentransportdata.common.models.implementation;


import java.io.Serializable;

import be.hyperrail.opentransportdata.common.models.RouteLeg;
import be.hyperrail.opentransportdata.common.models.RouteLegEnd;
import be.hyperrail.opentransportdata.common.models.RouteLegType;
import be.hyperrail.opentransportdata.common.models.VehicleJourneyStub;
import be.hyperrail.opentransportdata.common.models.VehicleStop;

/**
 * A leg of a route
 */
public class RouteLegImpl implements RouteLeg, Serializable {
    private final RouteLegType type;
    private final VehicleJourneyStub vehicleInformation;

    private final RouteLegEnd departure;
    private final RouteLegEnd arrival;

    /**
     * The stops whether this vehicle will halt, but the traveller stays on the train.
     * <p>
     * Null: unknown/unavailable
     * Empty array: no stops
     * Array: stops
     */
    private VehicleStop[] mintermediateStops;

    /**
     * Create a leg (a part) of a route. A route consists of one or more legs,
     * where each leg can use a single means of transport to connect its departure location to its arrival location
     *
     * @param type               The type of transport for this leg
     * @param vehicleInformation Information on the vehicle used in this leg
     * @param departure          The departure location and time of this leg
     * @param arrival            The arrival location and time of this leg
     * @param intermediateStops  The intermediate stops on this leg
     */
    public RouteLegImpl(RouteLegType type, VehicleJourneyStub vehicleInformation, RouteLegEnd departure, RouteLegEnd arrival, VehicleStop[] intermediateStops) {
        this.type = type;
        this.vehicleInformation = vehicleInformation;
        this.departure = departure;
        this.arrival = arrival;
        this.mintermediateStops = intermediateStops;
    }

    /**
     * The type of transport used for this leg of the route
     *
     * @return the type of transport used for this leg of the route
     */
    public RouteLegType getType() {
        return type;
    }

    public VehicleJourneyStub getVehicleInformation() {
        return vehicleInformation;
    }

    /**
     * The departure end for this leg
     *
     * @return The departure end for this leg
     */
    public RouteLegEnd getDeparture() {
        return departure;
    }

    /**
     * The arrival end for this leg
     *
     * @return The arrival end for this leg
     */
    public RouteLegEnd getArrival() {
        return arrival;
    }

    /**
     * Get the stops whether this vehicle will halt, but the traveller stays on the train.
     * <p>
     * Null: unknown/unavailable
     * Empty array: no stops
     * Array: stops
     */
    public VehicleStop[] getintermediateStops() {
        return mintermediateStops;
    }

}
