/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package android.irail.be.hyperrail.irail.contracts;

import android.irail.be.hyperrail.irail.db.Station;
import android.irail.be.hyperrail.irail.implementation.Disturbance;
import android.irail.be.hyperrail.irail.implementation.LiveBoard;
import android.irail.be.hyperrail.irail.implementation.Route;
import android.irail.be.hyperrail.irail.implementation.RouteResult;
import android.irail.be.hyperrail.irail.implementation.Train;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

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
