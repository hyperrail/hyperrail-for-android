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

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.irail.implementation.requests;

import android.support.annotation.NonNull;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import be.hyperrail.android.irail.contracts.IRailSuccessResponseListener;
import be.hyperrail.android.irail.contracts.IrailRequest;
import be.hyperrail.android.irail.contracts.RouteTimeDefinition;
import be.hyperrail.android.irail.db.Station;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.irail.implementation.LiveBoard;

/**
 * A request for a station liveboard
 */
public class IrailLiveboardRequest extends IrailBaseRequest<LiveBoard> implements IrailRequest<LiveBoard> {

    @NonNull
    private final Station station;

    @NonNull
    private final RouteTimeDefinition timeDefinition;

    @NonNull
    private final DateTime searchTime;

    /**
     * Create a request for train departures or arrivals in a given station
     *
     * @param station        The station for which departures or arrivals should be retrieved
     * @param timeDefinition The kind of data which should be retrieved: arrivals or departures
     * @param searchTime     The time for which should be searched
     */
    public IrailLiveboardRequest(@NonNull Station station, @NonNull RouteTimeDefinition timeDefinition, @NonNull DateTime searchTime) {
        super();
        this.station = station;
        this.timeDefinition = timeDefinition;
        this.searchTime = searchTime;
    }

    public IrailLiveboardRequest(@NonNull JSONObject jsonObject) throws JSONException {
        super(jsonObject);

        this.station = IrailFactory.getStationsProviderInstance().getStationById(jsonObject.getString("id"));
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
        json.put("id", station.getId());
        return json;
    }

    @NonNull
    public Station getStation() {
        return station;
    }

    @NonNull
    public RouteTimeDefinition getTimeDefinition() {
        return timeDefinition;
    }

    @NonNull
    public DateTime getSearchTime() {
        return searchTime;
    }

    IRailSuccessResponseListener<LiveBoard> successResponseListener;

}
