/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.common.models;

import org.joda.time.DateTime;

import java.io.Serializable;

import be.hyperrail.opentransportdata.common.contracts.PagedDataResource;
import be.hyperrail.opentransportdata.common.contracts.QueryTimeDefinition;

/**
 * This class represents a liveboard entity, containing departures or arrivals.
 * This class extends a station with its departures.
 */
public interface Liveboard extends StopLocation, PagedDataResource, Serializable {

    /**
     * Get the stops in this station at the requested time.
     * @return The stops in this station at the requested time.
     */
    VehicleStop[] getStops();

    /**
     * Get the time for which the query was made.
     * @return The time which was being searched.
     */
    DateTime getSearchTime();

    /**
     * Get the time definition for the query made to retrieve this data.
     * @return The time definition for the query.
     */
    QueryTimeDefinition getTimeDefinition();

    /**
     * Get the type of liveboard, which indicates if the stops described are departures or arrivals.
     * @return The type of liveboard which is being represented.
     */
    LiveboardType getLiveboardType();
}
