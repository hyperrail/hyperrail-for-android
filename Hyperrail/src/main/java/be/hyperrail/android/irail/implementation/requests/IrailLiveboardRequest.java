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

    @Nullable
    private DateTime searchTime;

    /**
     * Create a request for train departures or arrivals in a given station
     *
     * @param station        The station for which departures or arrivals should be retrieved
     * @param timeDefinition The kind of data which should be retrieved: arrivals or departures
     * @param searchTime     The time for which should be searched
     */
    public IrailLiveboardRequest(@NonNull Station station, @NonNull RouteTimeDefinition timeDefinition, @Nullable DateTime searchTime) {
        super();
        this.station = station;
        this.timeDefinition = timeDefinition;
        this.searchTime = searchTime;
    }

    public IrailLiveboardRequest(@NonNull JSONObject jsonObject) throws JSONException {
        super(jsonObject);

        this.station = IrailFactory.getStationsProviderInstance().getStationById(jsonObject.getString("id"));
        timeDefinition = RouteTimeDefinition.DEPART;
        searchTime = null;
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject json = super.toJson();
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
        if (this.searchTime == null) {
            return new DateTime(); // return now;
        }
        return searchTime; // return the actual query time
    }

    public void setSearchTime(@Nullable DateTime searchTime) {
        this.searchTime = searchTime;
    }

    public boolean isNow() {
        return (this.searchTime == null);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IrailLiveboardRequest)) {
            return false;
        }

        IrailLiveboardRequest other = (IrailLiveboardRequest) o;
        return (getStation().equals(other.getStation()) && getSearchTime().equals(other.getSearchTime()));
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
