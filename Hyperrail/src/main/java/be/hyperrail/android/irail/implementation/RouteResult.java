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

import java.io.Serializable;
import java.util.Date;

import be.hyperrail.android.irail.contracts.IrailDataProvider;
import be.hyperrail.android.irail.contracts.IrailDataResponse;
import be.hyperrail.android.irail.contracts.RouteTimeDefinition;
import be.hyperrail.android.irail.db.Station;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.util.ArrayUtils;

/**
 * Result of a route query. Includes the query, as parsed server-side.
 * This query information can be used to display which question the server replied to,
 * and can be used to detect incorrect parsed stations server-side (e.g. when searching a station)
 */
public class RouteResult implements Serializable {

    private final Station origin;
    private final Station destination;
    private final RouteTimeDefinition timeDefinition;
    private final Date lastSearchTime;
    private Route[] routes;

    public RouteResult(Station origin, Station destination, Date lastSearchTime, RouteTimeDefinition timeDefinition, Route[] routes) {
        this.destination = destination;
        this.lastSearchTime = lastSearchTime;
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

    public Date getLastSearchTime() {
        return lastSearchTime;
    }

    public Route[] getRoutes() {
        return routes;
    }

    public IrailDataResponse<Route[]> getNextResults() {
        IrailDataProvider api = IrailFactory.getDataProviderInstance();

        // get last time
        Date lastsearch;

        if (this.routes != null) {
            lastsearch = (Date) this.routes[this.routes.length - 1].getDepartureTime().clone();
        } else {
            lastsearch = lastSearchTime;
        }
        // move one minute further
        lastsearch.setTime(lastsearch.getTime() + 1000 * 60);
        // load
        Route[] newSearch;

        IrailDataResponse<RouteResult> apiResponse = api.getRoute(origin.getName(), destination.getName(), lastsearch);

        if (!apiResponse.isSuccess()) {
            return new ApiResponse<>(null, apiResponse.getException());
        }

        newSearch = apiResponse.getData().getRoutes();

        while (newSearch == null || newSearch.length == 0) {
            // add an hour
            lastsearch.setTime(lastsearch.getTime() + 1000 * 60 * 60);
            // load

            apiResponse = api.getRoute(origin.getName(), destination.getName(), lastsearch);

            if (!apiResponse.isSuccess()) {
                return new ApiResponse<>(null, apiResponse.getException());
            }

            newSearch = apiResponse.getData().getRoutes();

        }

        // add new stops
        this.routes = ArrayUtils.concatenate(this.getRoutes(), newSearch);

        return new ApiResponse<>(newSearch);
    }
}
