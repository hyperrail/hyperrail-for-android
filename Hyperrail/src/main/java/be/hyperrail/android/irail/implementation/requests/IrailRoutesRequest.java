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
import be.hyperrail.android.irail.contracts.RouteTimeDefinition;
import be.hyperrail.android.irail.db.Station;
import be.hyperrail.android.irail.factories.IrailFactory;

/**
 * A request for a routes between two or more stations
 */
public class IrailRoutesRequest extends IrailBaseRequest implements IrailRequest {

    private final Station origin;
    private final Station destination;
    private final RouteTimeDefinition timeDefinition;
    private final DateTime searchTime;

    /**
     * Create a request to search routes between two stations
     */
    // TODO: support vias
    public IrailRoutesRequest(Station origin, Station destination, RouteTimeDefinition timeDefinition, DateTime searchTime) {
        super();
        this.origin = origin;
        this.destination = destination;
        this.timeDefinition = timeDefinition;
        this.searchTime = searchTime;
    }

    public IrailRoutesRequest(JSONObject jsonObject) throws JSONException {
        this.origin = IrailFactory.getStationsProviderInstance().getStationById(jsonObject.getString("from"));
        this.destination = IrailFactory.getStationsProviderInstance().getStationById(jsonObject.getString("to"));

        if (jsonObject.has("time_definition")) {
            this.timeDefinition = RouteTimeDefinition.valueOf(jsonObject.getString("time_definition"));
        } else {
            this.timeDefinition = RouteTimeDefinition.DEPART;
        }

        if (jsonObject.has("time")) {
            this.searchTime = new DateTime(jsonObject.getLong("time"));
        } else {
            this.searchTime = new DateTime();
        }
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject json = super.toJson();
        json.put("time_definition", timeDefinition.name());
        json.put("time", searchTime.getMillis());
        json.put("from", origin.getId());
        json.put("to", destination.getId());
        return json;
    }

    public Station getOrigin() {
        return origin;
    }

    public Station getDestination() {
        return destination;
    }

    public RouteTimeDefinition getTimeDefinition() {
        return timeDefinition;
    }

    public DateTime getSearchTime() {
        return searchTime;
    }
}
