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

package be.hyperrail.android.irail.implementation;

import org.joda.time.DateTime;

import java.io.Serializable;

import be.hyperrail.android.irail.contracts.PagedResource;
import be.hyperrail.android.irail.contracts.PagedResourceDescriptor;
import be.hyperrail.android.irail.contracts.RouteTimeDefinition;
import be.hyperrail.android.irail.db.Station;

/**
 * Result of a route query. Includes the query, as parsed server-side.
 * This query information can be used to display which question the server replied to,
 * and can be used to detect incorrect parsed stations server-side (e.g. when searching a station)
 */
public class RouteResult implements Serializable, PagedResource {

    private final Station origin;
    private final Station destination;
    private final RouteTimeDefinition timeDefinition;
    private final DateTime mLastSearchTime;
    private Route[] routes;
    private PagedResourceDescriptor mDescriptor;

    public RouteResult(Station origin, Station destination, DateTime searchTime, RouteTimeDefinition timeDefinition, Route[] routes) {
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

    public RouteTimeDefinition getTimeDefinition() {
        return timeDefinition;
    }

    public DateTime getSearchTime() {
        return mLastSearchTime;
    }

    public Route[] getRoutes() {
        return routes;
    }

    @Override
    public PagedResourceDescriptor getPagedResourceDescriptor() {
        return mDescriptor;
    }

    @Override
    public void setPageInfo(PagedResourceDescriptor descriptor) {
        mDescriptor = descriptor;
    }
}
