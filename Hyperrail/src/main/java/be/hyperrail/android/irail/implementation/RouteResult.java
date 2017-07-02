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

import android.util.Log;

import org.joda.time.DateTime;

import java.io.Serializable;

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
    private final DateTime mLastSearchTime;
    private Route[] routes;

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

    public IrailDataResponse<Route[]> getNextResults() {
        IrailDataProvider api = IrailFactory.getDataProviderInstance();

        // get last time
        DateTime lastsearch;

        if (this.routes != null) {
            lastsearch = new DateTime(this.routes[this.routes.length - 1].getDepartureTime());
        } else {
            lastsearch = mLastSearchTime;
        }
        // move one minute further
        lastsearch = lastsearch.plusMinutes(1);
        // load
        Route[] newSearch;

        IrailDataResponse<RouteResult> apiResponse = api.getRoute(origin.getName(), destination.getName(), lastsearch);

        if (!apiResponse.isSuccess()) {
            return new ApiResponse<>(null, apiResponse.getException());
        }

        newSearch = apiResponse.getData().getRoutes();

        while (newSearch == null || newSearch.length == 0) {
            // add an hour
            lastsearch = lastsearch.plusHours(2);
            // load

            apiResponse = api.getRoute(origin.getName(), destination.getName(), lastsearch);

            if (!apiResponse.isSuccess()) {
                Log.d("RouteResult", "Extending route list failed with exception " + apiResponse.getException().getMessage());
                return new ApiResponse<>(null, apiResponse.getException());
            }

            newSearch = apiResponse.getData().getRoutes();
        }

        // add new stops
        this.routes = ArrayUtils.concatenate(this.getRoutes(), newSearch);

        return new ApiResponse<>(newSearch);
    }
}
