/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */


package be.hyperrail.android.irail.implementation.requests;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import be.hyperrail.android.irail.contracts.IrailRequest;
import be.hyperrail.android.irail.contracts.RouteTimeDefinition;
import be.hyperrail.android.irail.db.Station;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.irail.implementation.RouteResult;

/**
 * A request for a routes between two or more stations
 */
public class IrailRoutesRequest extends IrailBaseRequest<RouteResult> implements IrailRequest<RouteResult> {

    @NonNull
    private final Station origin;

    @NonNull
    private final Station destination;

    @NonNull
    private final RouteTimeDefinition timeDefinition;

    @Nullable
    private final DateTime searchTime;

    /**
     * Create a request to search routes between two stations
     */
    // TODO: support vias
    public IrailRoutesRequest(@NonNull Station origin, @NonNull Station destination, @NonNull RouteTimeDefinition timeDefinition, @Nullable DateTime searchTime) {
        super();
        this.origin = origin;
        this.destination = destination;
        this.timeDefinition = timeDefinition;

        this.searchTime = searchTime;
    }

    public IrailRoutesRequest(JSONObject jsonObject) throws JSONException {
        this.origin = IrailFactory.getStationsProviderInstance().getStationById(jsonObject.getString("from"));
        this.destination = IrailFactory.getStationsProviderInstance().getStationById(jsonObject.getString("to"));

        if (origin == null || destination == null) {
            throw new IllegalArgumentException("Origin or destionation station can't be null");
        }

        if (jsonObject.has("time_definition")) {
            this.timeDefinition = RouteTimeDefinition.valueOf(jsonObject.getString("time_definition"));
        } else {
            this.timeDefinition = RouteTimeDefinition.DEPART;
        }

        if (jsonObject.has("time")) {
            this.searchTime = new DateTime(jsonObject.getLong("time"));
        } else {
            this.searchTime = null;
        }
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject json = super.toJson();
        json.put("time_definition", timeDefinition.name());
        if (searchTime != null) {
            json.put("time", searchTime.getMillis());
        }
        json.put("from", origin.getId());
        json.put("to", destination.getId());
        return json;
    }

    @NonNull
    public Station getOrigin() {
        return origin;
    }

    @NonNull
    public Station getDestination() {
        return destination;
    }

    @NonNull
    public RouteTimeDefinition getTimeDefinition() {
        return timeDefinition;
    }

    @NonNull
    public DateTime getSearchTime() {
        if (this.searchTime == null) {
            return new DateTime(); // return now;
        }
        return searchTime; // return the actual query time
    }

    public boolean isNow(){
        return (this.searchTime == null);
    }
}
