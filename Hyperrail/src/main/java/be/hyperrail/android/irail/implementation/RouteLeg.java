/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.irail.implementation;


import java.io.Serializable;

/**
 * A leg of a route
 */
public class RouteLeg implements Serializable {
    private final RouteLegType type;
    private final VehicleStub vehicleInformation;

    private final RouteLegEnd departure;
    private final RouteLegEnd arrival;

    public RouteLeg(RouteLegType type, VehicleStub vehicleInformation, RouteLegEnd departure, RouteLegEnd arrival) {
        this.type = type;
        this.vehicleInformation = vehicleInformation;
        this.departure = departure;
        this.arrival = arrival;
    }

    public RouteLegType getType() {
        return type;
    }

    public VehicleStub getVehicleInformation() {
        return vehicleInformation;
    }

    public RouteLegEnd getDeparture() {
        return departure;
    }

    public RouteLegEnd getArrival() {
        return arrival;
    }
}
