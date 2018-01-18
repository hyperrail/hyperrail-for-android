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
import org.json.JSONException;
import org.json.JSONObject;

import be.hyperrail.android.irail.db.Station;
import be.hyperrail.android.irail.implementation.Disturbance;
import be.hyperrail.android.irail.implementation.LiveBoard;
import be.hyperrail.android.irail.implementation.RouteResult;
import be.hyperrail.android.irail.implementation.Train;

/**
 * A train, as returned from an API call to an Irail16 compliant api.
 * See http://docs.hyperrail.be/spec/16
 */
public interface IrailParser {

    Train parseTrain(JSONObject jsonData, DateTime searchDate) throws JSONException;

    LiveBoard parseLiveboard(JSONObject jsonData, DateTime searchDate) throws JSONException;

    RouteResult parseRouteResult(JSONObject json, Station origin, Station destination, DateTime lastSearchTime, RouteTimeDefinition timeDefinition) throws JSONException;

    Disturbance[] parseDisturbances(JSONObject jsonData) throws JSONException;
}
