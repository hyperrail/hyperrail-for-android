/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.common.models;

import java.io.Serializable;

/**
 * Basic vehicle information.
 * This data is typically present in APIs without requiring a second request.
 */
public interface VehicleStub extends Serializable {

    /**
     * Get the ID of this vehicle, used by the transport company to identify this vehicle/trip
     *
     * @return
     */
    String getId();

    /**
     * Get the headsign, a string indicating where this vehicle is going.
     *
     * @return
     */
    String getHeadsign();

    /**
     * Get a string containing a human-readable name for this vehicle, typically the same as advertised by the transport company towards travellers.
     *
     * @return Human-readable name
     */
    String getName();


    /**
     * Get the URI, the semantic ID, for this vehicle, as used by APIs, datasets and datasources.
     *
     * @return Semantic ID
     */
    String getSemanticId();

    /**
     * Get the vehicle type as a String
     *
     * @return The type of this train
     */
    String getType();

    /**
     * Get the vehicle number, which is typically only used by transport companies themselves.
     *
     * @return The number of this train
     */
    String getNumber();
}
