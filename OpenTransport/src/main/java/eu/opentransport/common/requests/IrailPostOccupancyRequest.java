/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package eu.opentransport.common.requests;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import eu.opentransport.common.contracts.TransportDataRequest;
import eu.opentransport.common.contracts.TransportOccupancyLevel;

/**
 * A request to post occupancy data
 */
public class IrailPostOccupancyRequest extends IrailBaseRequest<Boolean> implements TransportDataRequest<Boolean> {


    private final String departureSemanticId;


    private final String stationSemanticId;


    private final String vehicleSemanticId;


    private final DateTime date;


    private final TransportOccupancyLevel occupancy;

    /**
     * Create a request  to post occupancy data
     */
    public IrailPostOccupancyRequest( String departureSemanticId,  String stationSemanticId,  String vehicleSemanticId,  DateTime date,  TransportOccupancyLevel occupancy) {

        this.departureSemanticId = departureSemanticId;
        this.stationSemanticId = stationSemanticId;
        this.vehicleSemanticId = vehicleSemanticId;
        this.date = date;
        this.occupancy = occupancy;
    }

    /**
     * Deserialize JSON for a request to post occupancy data
     */
    public IrailPostOccupancyRequest( JSONObject jsonObject) throws JSONException {
        super(jsonObject);

        this.departureSemanticId = jsonObject.getString("departure_semantic_id");
        this.stationSemanticId = jsonObject.getString("station_semantic_id");
        this.vehicleSemanticId = jsonObject.getString("vehicle_semantic_id");
        this.date = new DateTime(jsonObject.getLong("date"));
        this.occupancy = TransportOccupancyLevel.valueOf(jsonObject.getString("occupancy"));
    }


    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject json = super.toJson();
        json.put("departure_semantic_id", getDepartureSemanticId());
        json.put("station_semantic_id", getStationSemanticId());
        json.put("vehicle_semantic_id", getVehicleSemanticId());
        json.put("date", getDate().getMillis());
        json.put("occupancy", getOccupancy().name());
        return json;
    }

    @Override
    public boolean equalsIgnoringTime(TransportDataRequest other) {
        // Time is essential for this request
        // Not supported
        return false;
    }



    public String getDepartureSemanticId() {
        return departureSemanticId;
    }


    public String getStationSemanticId() {
        return stationSemanticId;
    }


    public String getVehicleSemanticId() {
        return vehicleSemanticId;
    }


    public DateTime getDate() {
        return date;
    }


    public TransportOccupancyLevel getOccupancy() {
        return occupancy;
    }

    @Override
    public int compareTo( TransportDataRequest o) {
        if (!(o instanceof IrailPostOccupancyRequest)) {
            return -1;
        }
        return getDate().compareTo(((IrailPostOccupancyRequest) o).getDate());
    }
}
