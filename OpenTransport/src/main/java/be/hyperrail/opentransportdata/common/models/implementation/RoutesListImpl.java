/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */


package be.hyperrail.opentransportdata.common.models.implementation;

import org.joda.time.DateTime;

import java.io.Serializable;

import be.hyperrail.opentransportdata.common.contracts.NextDataPointer;
import be.hyperrail.opentransportdata.common.contracts.PagedDataResource;
import be.hyperrail.opentransportdata.common.contracts.QueryTimeDefinition;
import be.hyperrail.opentransportdata.common.models.Route;
import be.hyperrail.opentransportdata.common.models.RoutesList;
import be.hyperrail.opentransportdata.common.models.StopLocation;

/**
 * Result of a route query. Includes the query, as parsed server-side.
 * This query information can be used to display which question the server replied to,
 * and can be used to detect incorrect parsed stations server-side (e.g. when searching a station)
 */
public class RoutesListImpl implements RoutesList, Serializable, PagedDataResource {

    private final StopLocation origin;
    private final StopLocation destination;
    private final QueryTimeDefinition timeDefinition;
    private final DateTime mLastSearchTime;
    private Route[] routes;

    private NextDataPointer previousPagePointer;
    private NextDataPointer currentPagePointer;
    private NextDataPointer nextPagePointer;

    public RoutesListImpl(StopLocation origin, StopLocation destination, DateTime searchTime, QueryTimeDefinition timeDefinition, Route[] routes) {
        this.destination = destination;
        this.mLastSearchTime = searchTime;
        this.origin = origin;
        this.routes = routes;
        this.timeDefinition = timeDefinition;
    }

    public StopLocation getOrigin() {
        return origin;
    }

    public StopLocation getDestination() {
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

    public void setPageInfo(NextDataPointer previous,
                            NextDataPointer current,
                            NextDataPointer next) {
        this.previousPagePointer = previous;
        this.currentPagePointer = current;
        this.nextPagePointer = next;
    }

    @Override
    public NextDataPointer getPreviousResultsPointer() {
        return previousPagePointer;
    }

    @Override
    public NextDataPointer getCurrentResultsPointer() {
        return currentPagePointer;
    }

    @Override
    public NextDataPointer getNextResultsPointer() {
        return nextPagePointer;
    }
}
