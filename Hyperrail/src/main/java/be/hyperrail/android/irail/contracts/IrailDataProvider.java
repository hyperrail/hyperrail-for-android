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

    void getRoute(IrailResponseListener<RouteResult> callback, int tag, Station from, Station to);

    void getRoute(IrailResponseListener<RouteResult> callback, int tag, Station from, Station to, DateTime timeFilter);

    void getRoute(IrailResponseListener<RouteResult> callback, int tag, Station from, Station to, DateTime timeFilter, RouteTimeDefinition timeFilterType);

    void getRoute(IrailResponseListener<RouteResult> callback, int tag, String from, String to);

    void getRoute(IrailResponseListener<RouteResult> callback, int tag, String from, String to, DateTime timeFilter);

    void getRoute(IrailResponseListener<RouteResult> callback, int tag, String to, DateTime timeFilter, RouteTimeDefinition timeFilterType, String from);

    void getLiveboard(IrailResponseListener<LiveBoard> callback, int tag, String name);

    void getLiveboard(IrailResponseListener<LiveBoard> callback, int tag, Station station);

    void getLiveboard(IrailResponseListener<LiveBoard> callback, int tag, String name, DateTime timeFilter);

    void getLiveboard(IrailResponseListener<LiveBoard> callback, int tag, Station station, DateTime timeFilter);

    void getLiveboard(IrailResponseListener<LiveBoard> callback, int tag, String name, DateTime timeFilter, RouteTimeDefinition timeFilterType);

    void getLiveboard(IrailResponseListener<LiveBoard> callback, int tag, Station station, DateTime timeFilter, RouteTimeDefinition timeFilterType);

    void getTrain(IrailResponseListener<Train> callback, int tag, String id);

    void getTrain(IrailResponseListener<Train> callback, int tag, String id, DateTime day);

    void getDisturbances(IrailResponseListener<Disturbance[]> callback, int tag);

    void postOccupancy(IrailResponseListener<Boolean> callback, int tag, TrainStub train, TrainStop stop, OccupancyLevel occupancy);

    void abortAllQueries();
}
