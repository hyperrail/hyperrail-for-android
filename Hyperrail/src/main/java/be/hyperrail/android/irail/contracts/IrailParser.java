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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import be.hyperrail.android.irail.db.Station;
import be.hyperrail.android.irail.implementation.Disturbance;
import be.hyperrail.android.irail.implementation.LiveBoard;
import be.hyperrail.android.irail.implementation.Route;
import be.hyperrail.android.irail.implementation.RouteResult;
import be.hyperrail.android.irail.implementation.Train;

/**
 * A train, as returned from an API call to an Irail16 compliant api.
 * See http://docs.hyperrail.be/spec/16
 */
public interface IrailParser {

    Train parseTrain(JSONObject jsonData, Date searchDate) throws JSONException;

    LiveBoard parseLiveboard(JSONObject jsonData, Date searchDate) throws JSONException;

    RouteResult parseRouteResult(JSONObject json, Station origin, Station destination, Date lastSearchTime, RouteTimeDefinition timeDefinition) throws JSONException;

    Route parseRoute(JSONObject json) throws JSONException;

    Disturbance[] parseDisturbances(JSONObject jsonData) throws JSONException;
}
