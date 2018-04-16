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
    private DateTime searchTime;

    /**
     * Create a request to search routes between two stations
     */
    // TODO: support vias
    public IrailRoutesRequest(@NonNull Station origin, @NonNull Station destination, @NonNull RouteTimeDefinition timeDefinition, @Nullable DateTime searchTime) {
        this.origin = origin;
        this.destination = destination;
        this.timeDefinition = timeDefinition;

        this.searchTime = searchTime;
    }

    public IrailRoutesRequest(JSONObject jsonObject) throws JSONException, StationNotResolvedException {
        String from = jsonObject.getString("from");
        if (from.startsWith("BE.NMBS.")) {
            from = from.substring(8);
        }

        String to = jsonObject.getString("to");
        if (to.startsWith("BE.NMBS.")) {
            to = to.substring(8);
        }

        this.origin = IrailFactory.getStationsProviderInstance().getStationByHID(from);
        this.destination = IrailFactory.getStationsProviderInstance().getStationByHID(to);

        timeDefinition = RouteTimeDefinition.DEPART_AT;
        searchTime = null;
    }

    @NonNull
    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject json = super.toJson();
        json.put("from", origin.getHafasId());
        json.put("to", destination.getHafasId());
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

    public void setSearchTime(@Nullable DateTime searchTime) {
        this.searchTime = searchTime;
    }

    public boolean isNow() {
        return (this.searchTime == null);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IrailRoutesRequest)) {
            return false;
        }

        IrailRoutesRequest other = (IrailRoutesRequest) o;
        return (getOrigin().equals(other.getOrigin()) && getDestination().equals(other.getDestination()) && getTimeDefinition().equals(other.getTimeDefinition()) && (searchTime == other.searchTime));
    }

    @Override
    public int compareTo(@NonNull IrailRequest o) {
        if (!(o instanceof IrailRouteRequest)) {
            return -1;
        }

        IrailRouteRequest other = (IrailRouteRequest) o;
        return getOrigin().equals(other.getOrigin()) ?
                getDestination().getLocalizedName().compareTo(other.getDestination().getLocalizedName()) :
                getOrigin().getLocalizedName().compareTo(other.getOrigin().getLocalizedName());
    }

    @Override
    public boolean equalsIgnoringTime(IrailRequest other) {
        if (!(other instanceof IrailRoutesRequest)) {
            return false;
        }

        IrailRoutesRequest o = (IrailRoutesRequest) other;
        return getOrigin().equals(o.getOrigin()) && getDestination().equals(o.getDestination());
    }
}
