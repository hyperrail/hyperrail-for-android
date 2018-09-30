/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */


package eu.opentransport.irail;

import org.joda.time.DateTime;

import java.io.Serializable;

import eu.opentransport.common.contracts.PagedDataResource;
import eu.opentransport.common.contracts.PagedDataResourceDescriptor;
import eu.opentransport.common.contracts.QueryTimeDefinition;
import eu.opentransport.common.models.Route;
import eu.opentransport.common.models.RoutesList;

/**
 * Result of a route query. Includes the query, as parsed server-side.
 * This query information can be used to display which question the server replied to,
 * and can be used to detect incorrect parsed stations server-side (e.g. when searching a station)
 */
public class IrailRoutesList implements RoutesList, Serializable, PagedDataResource {

    private final IrailStation origin;
    private final IrailStation destination;
    private final QueryTimeDefinition timeDefinition;
    private final DateTime mLastSearchTime;
    private Route[] routes;
    private PagedDataResourceDescriptor mDescriptor;

    public IrailRoutesList(IrailStation origin, IrailStation destination, DateTime searchTime, QueryTimeDefinition timeDefinition, Route[] routes) {
        this.destination = destination;
        this.mLastSearchTime = searchTime;
        this.origin = origin;
        this.routes = routes;
        this.timeDefinition = timeDefinition;
    }

    public IrailStation getOrigin() {
        return origin;
    }

    public IrailStation getDestination() {
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
