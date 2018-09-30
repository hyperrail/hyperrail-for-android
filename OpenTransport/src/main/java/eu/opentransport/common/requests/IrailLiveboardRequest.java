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

package eu.opentransport.common.requests;

import android.support.annotation.Nullable;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import eu.opentransport.OpenTransport;
import eu.opentransport.common.contracts.QueryTimeDefinition;
import eu.opentransport.common.contracts.TransportDataRequest;
import eu.opentransport.common.exceptions.StopLocationNotResolvedException;
import eu.opentransport.common.models.Liveboard;
import eu.opentransport.common.models.Station;

/**
 * A request for a station liveboard
 */
public class IrailLiveboardRequest extends IrailBaseRequest<Liveboard> implements TransportDataRequest<Liveboard> {


    private final Station station;


    private QueryTimeDefinition timeDefinition;


    private Liveboard.LiveboardType type;

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
    public IrailLiveboardRequest( Station station,  QueryTimeDefinition timeDefinition,  Liveboard.LiveboardType type, @Nullable DateTime searchTime) {
        this.station = station;
        this.timeDefinition = timeDefinition;
        this.type = type;
        this.searchTime = searchTime;
    }

    public IrailLiveboardRequest( JSONObject jsonObject) throws JSONException, StopLocationNotResolvedException {
        super(jsonObject);
        String id = jsonObject.getString("id");
        if (id.startsWith("BE.NMBS.")) {
            id = id.substring(8);
        }
        this.station = OpenTransport.getStationsProviderInstance().getStationByHID(id);
        timeDefinition = QueryTimeDefinition.DEPART_AT;
        type = Liveboard.LiveboardType.DEPARTURES;
        searchTime = null;
    }

    public IrailLiveboardRequest(IrailLiveboardRequest copy) {
        this.searchTime = copy.searchTime;
        this.timeDefinition = copy.timeDefinition;
        this.station = copy.station;
        this.type = copy.type;
    }


    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject json = super.toJson();
        json.put("id", station.getHafasId());
        return json;
    }

    @Override
    public boolean equalsIgnoringTime(TransportDataRequest other) {
        return other instanceof IrailLiveboardRequest && this.getStation().equals(((IrailLiveboardRequest) other).getStation());
    }

    public boolean equals(String json) {
        try {
            IrailLiveboardRequest other = new IrailLiveboardRequest(new JSONObject(json));
            return this.equals(other);
        } catch (JSONException | StopLocationNotResolvedException e) {
            return false;
        }
    }


    public Station getStation() {
        return station;
    }


    public QueryTimeDefinition getTimeDefinition() {
        return timeDefinition;
    }


    public DateTime getSearchTime() {
        if (this.searchTime == null) {
            return new DateTime(); // return now;
        }
        return searchTime; // return the actual query time
    }


    public Liveboard.LiveboardType getType() {
        return type;
    }

    public void setSearchTime(@Nullable DateTime searchTime) {
        this.searchTime = searchTime;
    }

    public boolean isNow() {
        return (this.searchTime == null);
    }

    public IrailLiveboardRequest withTimeDefinition(QueryTimeDefinition timeDefinition) {
        IrailLiveboardRequest clone = new IrailLiveboardRequest(this);
        clone.timeDefinition = timeDefinition;
        return clone;
    }

    public IrailLiveboardRequest withLiveboardType(Liveboard.LiveboardType type) {
        IrailLiveboardRequest clone = new IrailLiveboardRequest(this);
        clone.type = type;
        return clone;
    }

    public IrailLiveboardRequest withSearchTime(DateTime searchTime) {
        IrailLiveboardRequest clone = new IrailLiveboardRequest(this);
        clone.searchTime = searchTime;
        return clone;
    }

    @Override
    public boolean equals(Object o) {

        if (o instanceof JSONObject){
            try {
                o = new IrailLiveboardRequest((JSONObject) o);
            } catch (JSONException | StopLocationNotResolvedException e) {
                return false;
            }
        }

        if (!(o instanceof IrailLiveboardRequest)) {
            return false;
        }

        IrailLiveboardRequest other = (IrailLiveboardRequest) o;
        return (getStation().equals(other.getStation()) && searchTime == other.searchTime);
    }

    @Override
    public int compareTo( TransportDataRequest o) {
        if (!(o instanceof IrailLiveboardRequest)) {
            return -1;
        }

        IrailLiveboardRequest other = (IrailLiveboardRequest) o;
        return getStation().compareTo(other.getStation());
    }

}
