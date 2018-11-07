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
 * Result of a route query. Includes the query, as parsed server-side.
 * This query information can be used to display which question the server replied to,
 * and can be used to detect incorrect parsed stations server-side (e.g. when searching a station)
 */
public interface RoutesList extends Serializable, PagedDataResource {

    StopLocation getOrigin();

    StopLocation getDestination();

    QueryTimeDefinition getTimeDefinition();

    DateTime getSearchTime();

    Route[] getRoutes();

}
