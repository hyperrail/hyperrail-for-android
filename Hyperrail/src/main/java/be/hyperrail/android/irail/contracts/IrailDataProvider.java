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

package be.hyperrail.android.irail.contracts;

import org.joda.time.DateTime;

import be.hyperrail.android.irail.db.Station;
import be.hyperrail.android.irail.implementation.Disturbance;
import be.hyperrail.android.irail.implementation.LiveBoard;
import be.hyperrail.android.irail.implementation.RouteResult;
import be.hyperrail.android.irail.implementation.Train;
import be.hyperrail.android.irail.implementation.TrainStop;
import be.hyperrail.android.irail.implementation.TrainStub;

/**
 * Retrieve (realtime) data according from the iRail API, or any API which has identical endpoints.
 * See http://docs.irail.be/
 */
public interface IrailDataProvider {

    void getRoute(Station from, Station to, DateTime timeFilter, RouteTimeDefinition timeFilterType, IRailSuccessResponseListener<RouteResult> successListener, IRailErrorResponseListener<RouteResult> errorListener, Object tag);

    void getRoute(String from, String to, DateTime timeFilter, RouteTimeDefinition timeFilterType, IRailSuccessResponseListener<RouteResult> successListener, IRailErrorResponseListener<RouteResult> errorListener, Object tag);

    void getLiveboard(String name, DateTime timeFilter, RouteTimeDefinition timeFilterType, IRailSuccessResponseListener<LiveBoard> successListener, IRailErrorResponseListener<LiveBoard> errorListener, Object tag);

    void getLiveboard(Station station, DateTime timeFilter, RouteTimeDefinition timeFilterType, IRailSuccessResponseListener<LiveBoard> successListener, IRailErrorResponseListener<LiveBoard> errorListener, Object tag);

    void getTrain(String id, IRailSuccessResponseListener<Train> successListener, IRailErrorResponseListener<Train> errorListener, Object tag);

    void getTrain(String id, DateTime day, IRailSuccessResponseListener<Train> successListener, IRailErrorResponseListener<Train> errorListener, Object tag);

    void getDisturbances(IRailSuccessResponseListener<Disturbance[]> successListener, IRailErrorResponseListener<Disturbance[]> errorListener, Object tag);

    void postOccupancy(TrainStub train, TrainStop stop, OccupancyLevel occupancy, IRailSuccessResponseListener<Boolean> successListener, IRailErrorResponseListener<Boolean> errorListener, Object tag);

    void abortAllQueries();
}
