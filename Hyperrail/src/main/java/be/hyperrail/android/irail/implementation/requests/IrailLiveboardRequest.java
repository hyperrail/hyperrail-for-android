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
import android.support.annotation.Nullable;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import be.hyperrail.android.irail.contracts.IrailRequest;
import be.hyperrail.android.irail.contracts.RouteTimeDefinition;
import be.hyperrail.android.irail.contracts.StationNotResolvedException;
import be.hyperrail.android.irail.db.Station;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.irail.implementation.Liveboard;

/**
 * A request for a station liveboard
 */
public class IrailLiveboardRequest extends IrailBaseRequest<Liveboard> implements IrailRequest<Liveboard> {

    @NonNull
    private final Station station;

    @NonNull
    private RouteTimeDefinition timeDefinition;

    @NonNull
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
    public IrailLiveboardRequest(@NonNull Station station, @NonNull RouteTimeDefinition timeDefinition, @NonNull Liveboard.LiveboardType type, @Nullable DateTime searchTime) {
        this.station = station;
        this.timeDefinition = timeDefinition;
        this.type = type;
        this.searchTime = searchTime;
    }

    public IrailLiveboardRequest(@NonNull JSONObject jsonObject) throws JSONException, StationNotResolvedException {
        super(jsonObject);
        String id = jsonObject.getString("id");
        if (id.startsWith("BE.NMBS.")) {
            id = id.substring(5);
        }
        this.station = IrailFactory.getStationsProviderInstance().getStationByHID(id);
        timeDefinition = RouteTimeDefinition.DEPART_AT;
        type = Liveboard.LiveboardType.DEPARTURES;
        searchTime = null;
    }

    public IrailLiveboardRequest(IrailLiveboardRequest copy) {
        this.searchTime = copy.searchTime;
        this.timeDefinition = copy.timeDefinition;
        this.station = copy.station;
        this.type = copy.type;
    }

    @NonNull
    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject json = super.toJson();
        json.put("id", station.getHafasId());
        return json;
    }

    @Override
    public boolean equalsIgnoringTime(IrailRequest other) {
        return other instanceof IrailLiveboardRequest && this.getStation().equals(((IrailLiveboardRequest) other).getStation());
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
        if (this.searchTime == null) {
            return new DateTime(); // return now;
        }
        return searchTime; // return the actual query time
    }

    @NonNull
    public Liveboard.LiveboardType getType() {
        return type;
    }

    public void setSearchTime(@Nullable DateTime searchTime) {
        this.searchTime = searchTime;
    }

    public boolean isNow() {
        return (this.searchTime == null);
    }

    public IrailLiveboardRequest withTimeDefinition(RouteTimeDefinition timeDefinition) {
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
        if (!(o instanceof IrailLiveboardRequest)) {
            return false;
        }

        IrailLiveboardRequest other = (IrailLiveboardRequest) o;
        return (getStation().equals(other.getStation()) && searchTime == other.searchTime);
    }

    @Override
    public int compareTo(@NonNull IrailRequest o) {
        if (!(o instanceof IrailLiveboardRequest)) {
            return -1;
        }

        IrailLiveboardRequest other = (IrailLiveboardRequest) o;
        return getStation().compareTo(other.getStation());
    }

}
