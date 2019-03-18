/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */


package eu.opentransport.common.models;

import org.joda.time.DateTime;

import java.io.Serializable;

import eu.opentransport.common.contracts.PagedDataResource;
import eu.opentransport.common.contracts.QueryTimeDefinition;

/**
 * This class represents a liveboard entity, containing departures or arrivals.
 * This class extends a station with its departures.
 */
public interface Liveboard extends StopLocation, Serializable, PagedDataResource {

    VehicleStop[] getStops();

    DateTime getSearchTime();

    QueryTimeDefinition getTimeDefinition();

    LiveboardType getLiveboardType();
}
