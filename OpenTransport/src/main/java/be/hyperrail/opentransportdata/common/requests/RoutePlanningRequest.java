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
import be.hyperrail.opentransportdata.common.models.RoutesList;
import be.hyperrail.opentransportdata.common.models.StopLocation;

/**
 * A request for a routes between two or more stations
 */
public class RoutePlanningRequest extends OpenTransportBaseRequest<RoutesList> implements TransportDataRequest<RoutesList> {


    private static final String JSON_KEY_FROM_URI = "from";
    private static final String JSON_KEY_TO_URI = "to";
    private final StopLocation origin;


    private final StopLocation destination;


    private final QueryTimeDefinition timeDefinition;

    @Nullable
    private DateTime searchTime;

    /**
     * Create a request to search routes between two stations
     */
    // TODO: support vias
    public RoutePlanningRequest(StopLocation origin, StopLocation destination, QueryTimeDefinition timeDefinition, @Nullable DateTime searchTime) {
        this.origin = origin;
        this.destination = destination;
        this.timeDefinition = timeDefinition;

        this.searchTime = searchTime;
    }

    public RoutePlanningRequest(JSONObject jsonObject) throws JSONException, StopLocationNotResolvedException {
        super(jsonObject);

        String from = jsonObject.getString(JSON_KEY_FROM_URI);
        String to = jsonObject.getString(JSON_KEY_TO_URI);

        this.origin = OpenTransportApi.getStopLocationProviderInstance().getStoplocationBySemanticId(from);
        this.destination = OpenTransportApi.getStopLocationProviderInstance().getStoplocationBySemanticId(to);

        timeDefinition = QueryTimeDefinition.EQUAL_OR_LATER;
        searchTime = null;
    }


    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject json = super.toJson();
        json.put(JSON_KEY_FROM_URI, origin.getSemanticId());
        json.put(JSON_KEY_TO_URI, destination.getSemanticId());
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
            // current time as a default;
            return new DateTime();
        }
        // the actual query time
        return searchTime;
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
                o = new RoutePlanningRequest((JSONObject) o);
            } catch (JSONException | StopLocationNotResolvedException e) {
                return false;
            }
        }

        if (!(o instanceof RoutePlanningRequest)) {
            return false;
        }

        RoutePlanningRequest other = (RoutePlanningRequest) o;
        return (getOrigin().equals(other.getOrigin()) && getDestination().equals(other.getDestination()) && getTimeDefinition().equals(other.getTimeDefinition()) && (searchTime == other.searchTime));
    }

    @Override
    public int compareTo( TransportDataRequest o) {
        if (!(o instanceof RouteRefreshRequest)) {
            return -1;
        }

        RouteRefreshRequest other = (RouteRefreshRequest) o;
        return getOrigin().equals(other.getOrigin()) ?
                getDestination().getLocalizedName().compareTo(other.getDestination().getLocalizedName()) :
                getOrigin().getLocalizedName().compareTo(other.getOrigin().getLocalizedName());
    }

    @Override
    public boolean equalsIgnoringTime(TransportDataRequest other) {
        if (!(other instanceof RoutePlanningRequest)) {
            return false;
        }

        RoutePlanningRequest o = (RoutePlanningRequest) other;
        return getOrigin().equals(o.getOrigin()) && getDestination().equals(o.getDestination());
    }

    @Override
    public int getRequestTypeTag() {
        return RequestType.ROUTEPLANNING.getRequestTypeTag();
    }
}
