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

import java.io.Serializable;

import eu.opentransport.common.contracts.PagedDataResource;
import eu.opentransport.common.contracts.PagedDataResourceDescriptor;
import eu.opentransport.common.contracts.QueryTimeDefinition;

/**
 * Result of a route query. Includes the query, as parsed server-side.
 * This query information can be used to display which question the server replied to,
 * and can be used to detect incorrect parsed stations server-side (e.g. when searching a station)
 */
public class RouteResult implements Serializable, PagedDataResource {

    private final Station origin;
    private final Station destination;
    private final QueryTimeDefinition timeDefinition;
    private final DateTime mLastSearchTime;
    private Route[] routes;
    private PagedDataResourceDescriptor mDescriptor;

    public RouteResult(Station origin, Station destination, DateTime searchTime, QueryTimeDefinition timeDefinition, Route[] routes) {
        this.destination = destination;
        this.mLastSearchTime = searchTime;
        this.origin = origin;
        this.routes = routes;
        this.timeDefinition = timeDefinition;
    }

    public Station getOrigin() {
        return origin;
    }

    public Station getDestination() {
        return destination;
    }

    public QueryTimeDefinition getTimeDefinition() {
        return timeDefinition;
    }

    public DateTime getSearchTime() {
        return mLastSearchTime;
    }

    public Route[] getRoutes() {
        return routes;
    }

    @Override
    public PagedDataResourceDescriptor getPagedResourceDescriptor() {
        return mDescriptor;
    }

    @Override
    public void setPageInfo(PagedDataResourceDescriptor descriptor) {
        mDescriptor = descriptor;
    }
}
