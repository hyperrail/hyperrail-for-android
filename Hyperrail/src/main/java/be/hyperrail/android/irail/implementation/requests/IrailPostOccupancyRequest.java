/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.irail.implementation.requests;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import be.hyperrail.android.irail.contracts.IrailRequest;
import be.hyperrail.android.irail.contracts.OccupancyLevel;

/**
 * A request to post occupancy data
 */
public class IrailPostOccupancyRequest extends IrailBaseRequest implements IrailRequest {


    private final String departureSemanticId;
    private final String stationSemanticId;
    private final String vehicleSemanticId;
    private final DateTime date;
    private final OccupancyLevel occupancy;

    /**
     * Create a request  to post occupancy data
     */
    public IrailPostOccupancyRequest(String departureSemanticId, String stationSemanticId, String vehicleSemanticId, DateTime date, OccupancyLevel occupancy) {
        super();

        this.departureSemanticId = departureSemanticId;
        this.stationSemanticId = stationSemanticId;
        this.vehicleSemanticId = vehicleSemanticId;
        this.date = date;
        this.occupancy = occupancy;
    }

    /**
     * Deserialize JSON for a request to post occupancy data
     */
    public IrailPostOccupancyRequest(JSONObject jsonObject) throws JSONException {
        super(jsonObject);

        this.departureSemanticId = jsonObject.getString("departure_semantic_id");
        this.stationSemanticId = jsonObject.getString("station_semantic_id");
        this.vehicleSemanticId = jsonObject.getString("vehicle_semantic_id");
        this.date = new DateTime(jsonObject.getLong("date"));
        this.occupancy = OccupancyLevel.valueOf(jsonObject.getString("occupancy"));
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject json = super.toJson();
        json.put("departure_semantic_id", departureSemanticId);
        json.put("station_semantic_id", stationSemanticId);
        json.put("vehicle_semantic_id", vehicleSemanticId);
        json.put("date", date.getMillis());
        json.put("occupancy", occupancy.name());
        return json;
    }


}
