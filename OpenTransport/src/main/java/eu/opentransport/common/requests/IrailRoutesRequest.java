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

import eu.opentransport.OpenTransportApi;
import eu.opentransport.common.contracts.QueryTimeDefinition;
import eu.opentransport.common.contracts.TransportDataRequest;
import eu.opentransport.common.exceptions.StopLocationNotResolvedException;
import eu.opentransport.common.models.RoutesList;
import eu.opentransport.common.models.StopLocation;

/**
 * A request for a routes between two or more stations
 */
public class IrailRoutesRequest extends IrailBaseRequest<RoutesList> implements TransportDataRequest<RoutesList> {


    private final StopLocation origin;


    private final StopLocation destination;


    private final QueryTimeDefinition timeDefinition;

    @Nullable
    private DateTime searchTime;

    /**
     * Create a request to search routes between two stations
     */
    // TODO: support vias
    public IrailRoutesRequest(StopLocation origin, StopLocation destination, QueryTimeDefinition timeDefinition, @Nullable DateTime searchTime) {
        this.origin = origin;
        this.destination = destination;
        this.timeDefinition = timeDefinition;

        this.searchTime = searchTime;
    }

    public IrailRoutesRequest(JSONObject jsonObject) throws JSONException, StopLocationNotResolvedException {
        super(jsonObject);
        String from = jsonObject.getString("from");
        if (from.startsWith("BE.NMBS.")) {
            from = from.substring(8);
        }

        String to = jsonObject.getString("to");
        if (to.startsWith("BE.NMBS.")) {
            to = to.substring(8);
        }

        this.origin = OpenTransportApi.getStationsProviderInstance().getStationByHID(from);
        this.destination = OpenTransportApi.getStationsProviderInstance().getStationByHID(to);

        timeDefinition = QueryTimeDefinition.DEPART_AT;
        searchTime = null;
    }


    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject json = super.toJson();
        json.put("from", origin.getHafasId());
        json.put("to", destination.getHafasId());
        return json;
    }


    public StopLocation getOrigin() {
        return origin;
    }


    public StopLocation getDestination() {
        return destination;
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

    public void setSearchTime(@Nullable DateTime searchTime) {
        this.searchTime = searchTime;
    }

    public boolean isNow() {
        return (this.searchTime == null);
    }

    @Override
    public boolean equals(Object o) {

        if (o instanceof JSONObject){
            try {
                o = new IrailRoutesRequest((JSONObject) o);
            } catch (JSONException | StopLocationNotResolvedException e) {
                return false;
            }
        }

        if (!(o instanceof IrailRoutesRequest)) {
            return false;
        }

        IrailRoutesRequest other = (IrailRoutesRequest) o;
        return (getOrigin().equals(other.getOrigin()) && getDestination().equals(other.getDestination()) && getTimeDefinition().equals(other.getTimeDefinition()) && (searchTime == other.searchTime));
    }

    @Override
    public int compareTo( TransportDataRequest o) {
        if (!(o instanceof IrailRouteRequest)) {
            return -1;
        }

        IrailRouteRequest other = (IrailRouteRequest) o;
        return getOrigin().equals(other.getOrigin()) ?
                getDestination().getLocalizedName().compareTo(other.getDestination().getLocalizedName()) :
                getOrigin().getLocalizedName().compareTo(other.getOrigin().getLocalizedName());
    }

    @Override
    public boolean equalsIgnoringTime(TransportDataRequest other) {
        if (!(other instanceof IrailRoutesRequest)) {
            return false;
        }

        IrailRoutesRequest o = (IrailRoutesRequest) other;
        return getOrigin().equals(o.getOrigin()) && getDestination().equals(o.getDestination());
    }
}
