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

package be.hyperrail.opentransportdata.common.requests;

import androidx.annotation.Nullable;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import be.hyperrail.opentransportdata.OpenTransportApi;
import be.hyperrail.opentransportdata.common.contracts.QueryTimeDefinition;
import be.hyperrail.opentransportdata.common.contracts.TransportDataRequest;
import be.hyperrail.opentransportdata.common.exceptions.StopLocationNotResolvedException;
import be.hyperrail.opentransportdata.common.models.Liveboard;
import be.hyperrail.opentransportdata.common.models.LiveboardType;
import be.hyperrail.opentransportdata.common.models.StopLocation;

/**
 * A request for a station liveboard
 */
public class LiveboardRequest extends OpenTransportBaseRequest<Liveboard> implements TransportDataRequest<Liveboard> {


    private static final String JSON_KEY_STOPLOCATION_URI = "id";
    private final StopLocation station;


    private QueryTimeDefinition timeDefinition;


    private LiveboardType type;

    @Nullable
    private DateTime searchTime;

    /**
     * Create a request for train departures or arrivals in a given station
     *
     * @param station        The station for which departures or arrivals should be retrieved
     * @param timeDefinition The time which timeDefinition implies: arriving at or departing at
     * @param type           The type of data which should be retrieved: arrivals or departures
     * @param searchTime     The time for which should be searched
     */
    public LiveboardRequest(StopLocation station, QueryTimeDefinition timeDefinition, LiveboardType type, @Nullable DateTime searchTime) {
        this.station = station;
        this.timeDefinition = timeDefinition;
        this.type = type;
        this.searchTime = searchTime;
    }

    public LiveboardRequest(JSONObject jsonObject) throws JSONException, StopLocationNotResolvedException {
        super(jsonObject);

        String id = jsonObject.getString(JSON_KEY_STOPLOCATION_URI);
        this.station = OpenTransportApi.getStopLocationProviderInstance().getStoplocationBySemanticId(id);
        timeDefinition = QueryTimeDefinition.EQUAL_OR_LATER;
        type = LiveboardType.DEPARTURES;
        searchTime = null;
    }

    public LiveboardRequest(LiveboardRequest copy) {
        this.searchTime = copy.searchTime;
        this.timeDefinition = copy.timeDefinition;
        this.station = copy.station;
        this.type = copy.type;
    }


    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject json = super.toJson();
        json.put(JSON_KEY_STOPLOCATION_URI, station.getSemanticId());
        return json;
    }

    @Override
    public boolean equalsIgnoringTime(TransportDataRequest other) {
        return other instanceof LiveboardRequest && this.getStation().equals(((LiveboardRequest) other).getStation());
    }

    public boolean equals(String json) {
        try {
            LiveboardRequest other = new LiveboardRequest(new JSONObject(json));
            return this.equals(other);
        } catch (JSONException | StopLocationNotResolvedException e) {
            return false;
        }
    }

    public StopLocation getStation() {
        return station;
    }

    public QueryTimeDefinition getTimeDefinition() {
        return timeDefinition;
    }


    public DateTime getSearchTime() {
        if (this.searchTime == null) {
            return new DateTime(); // the current time as default;
        }
        return searchTime; // the actual query time
    }


    public LiveboardType getType() {
        return type;
    }

    public void setSearchTime(@Nullable DateTime searchTime) {
        this.searchTime = searchTime;
    }

    public boolean isNow() {
        return (this.searchTime == null);
    }

    public LiveboardRequest withTimeDefinition(QueryTimeDefinition timeDefinition) {
        LiveboardRequest clone = new LiveboardRequest(this);
        clone.timeDefinition = timeDefinition;
        return clone;
    }

    public LiveboardRequest withLiveboardType(LiveboardType type) {
        LiveboardRequest clone = new LiveboardRequest(this);
        clone.type = type;
        return clone;
    }

    public LiveboardRequest withSearchTime(DateTime searchTime) {
        LiveboardRequest clone = new LiveboardRequest(this);
        clone.searchTime = searchTime;
        return clone;
    }

    @Override
    public boolean equals(Object o) {

        if (o instanceof JSONObject) {
            try {
                o = new LiveboardRequest((JSONObject) o);
            } catch (JSONException | StopLocationNotResolvedException e) {
                return false;
            }
        }

        if (!(o instanceof LiveboardRequest)) {
            return false;
        }

        LiveboardRequest other = (LiveboardRequest) o;
        return (getStation().equals(other.getStation()) && searchTime == other.searchTime);
    }

    @Override
    public int compareTo(TransportDataRequest o) {
        if (!(o instanceof LiveboardRequest)) {
            return -1;
        }

        LiveboardRequest other = (LiveboardRequest) o;
        return getStation().compareTo(other.getStation());
    }

    @Override
    public int getRequestTypeTag() {
        return RequestType.LIVEBOARD.getRequestTypeTag();
    }

}
