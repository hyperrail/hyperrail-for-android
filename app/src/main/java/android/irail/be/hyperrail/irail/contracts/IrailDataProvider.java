/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package android.irail.be.hyperrail.irail.contracts;

import android.irail.be.hyperrail.irail.db.Station;
import android.irail.be.hyperrail.irail.implementation.Disturbance;
import android.irail.be.hyperrail.irail.implementation.LiveBoard;
import android.irail.be.hyperrail.irail.implementation.RouteResult;
import android.irail.be.hyperrail.irail.implementation.Train;

import java.util.Date;

/**
 * Retrieve (realtime) data according from the iRail API, or any API which has identical endpoints.
 * See http://docs.irail.be/
 */
public interface IrailDataProvider {

    IrailDataResponse<RouteResult> getRoute(Station from, Station to);

    IrailDataResponse<RouteResult> getRoute(Station from, Station to, Date timefilter);

    IrailDataResponse<RouteResult> getRoute(Station from, Station to, Date timefilter, RouteTimeDefinition timeFilterType);

    IrailDataResponse<RouteResult> getRoute(String from, String to);

    IrailDataResponse<RouteResult> getRoute(String from, String to, Date timefilter);

    IrailDataResponse<RouteResult> getRoute(String from, String to, Date timefilter, RouteTimeDefinition timeFilterType);

    IrailDataResponse<LiveBoard> getLiveboard(String name);

    IrailDataResponse<LiveBoard> getLiveboard(String name, Date timefilter);

    IrailDataResponse<LiveBoard> getLiveboard(String name, Date timefilter, RouteTimeDefinition timeFilterType);

    IrailDataResponse<Train> getTrain(String id);

    IrailDataResponse<Train> getTrain(String id, Date day);

    IrailDataResponse<Disturbance[]> getDisturbances();
}
