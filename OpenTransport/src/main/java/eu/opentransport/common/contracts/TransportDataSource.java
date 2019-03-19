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

package eu.opentransport.common.contracts;

import eu.opentransport.common.requests.ActualDisturbancesRequest;
import eu.opentransport.common.requests.ExtendLiveboardRequest;
import eu.opentransport.common.requests.ExtendRoutePlanningRequest;
import eu.opentransport.common.requests.LiveboardRequest;
import eu.opentransport.common.requests.OccupancyPostRequest;
import eu.opentransport.common.requests.RoutePlanningRequest;
import eu.opentransport.common.requests.RouteRefreshRequest;
import eu.opentransport.common.requests.VehicleRequest;
import eu.opentransport.common.requests.VehicleStopRequest;

/**
 * Retrieve (realtime) data according from the iRail API, or any API which provides similar data.
 * Requests can contain additional data fields which are not supported by all supported data sources. Data fields should be ignored when they are not supported by the API.
 * See http://docs.irail.be/
 */
public interface TransportDataSource {
    /**
     * Get actual disturbances on the network.
     *
     * @param requests
     */
    void getActualDisturbances(ActualDisturbancesRequest... requests);

    /**
     * Get a liveboard with Departures and Arrivals from a stop.
     *
     * @param requests
     */
    void getLiveboard(LiveboardRequest... requests);

    /**
     * Extend a previous Liveboard response.
     *
     * @param requests
     */
    void extendLiveboard(ExtendLiveboardRequest... requests);

    /**
     * Run routeplanning from A to B.
     *
     * @param requests
     */
    void getRoutePlanning(RoutePlanningRequest... requests);

    /**
     * Extend a previous routeplanning response.
     *
     * @param requests
     */
    void extendRoutePlanning(ExtendRoutePlanningRequest... requests);

    /**
     * Get up-to-date information on a specified route.
     */
    void getRoute(RouteRefreshRequest... requests);

    /**
     * Get up-to-date information on a certain stop made by a certain vehicle.
     *
     * @param requests
     */
    void getStop(VehicleStopRequest... requests);

    /**
     * Get information on a vehicle journey.
     *
     * @param requests
     */
    void getVehicleJourney(VehicleRequest... requests);

    /**
     * Report the occupancy on a certain vehicle.
     *
     * @param requests
     */
    void postOccupancy(OccupancyPostRequest... requests);

    /**
     * Abort all running background jobs.
     */
    void abortAllQueries();

}
