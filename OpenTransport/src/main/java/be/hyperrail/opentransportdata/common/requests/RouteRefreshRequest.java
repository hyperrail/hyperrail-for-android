/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */


package be.hyperrail.opentransportdata.common.requests;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import be.hyperrail.opentransportdata.OpenTransportApi;
import be.hyperrail.opentransportdata.common.contracts.QueryTimeDefinition;
import be.hyperrail.opentransportdata.common.contracts.TransportDataRequest;
import be.hyperrail.opentransportdata.common.exceptions.StopLocationNotResolvedException;
import be.hyperrail.opentransportdata.common.models.Route;
import be.hyperrail.opentransportdata.common.models.StopLocation;

/**
 * A request for a route between two or more stations
 */
public class RouteRefreshRequest extends OpenTransportBaseRequest<Route> implements TransportDataRequest<Route> {


    private static final String JSON_KEY_FROM_URI = "from";
    private static final String JSON_KEY_DEPARTURE_URI = "departure_semantic_id";
    private static final String JSON_KEY_TO_URI = "to";
    private static final String JSON_KEY_SEARCHTIME_MILLIS = "time";
    private final StopLocation origin;


    private final StopLocation destination;


    private final QueryTimeDefinition timeDefinition;


    private final DateTime searchTime;

    /**
     * The (semantic) id for this route
     */

    private final String departureSemanticId;

    /**
     * Create a request to get a specific between two stations
     */
    // TODO: support vias
    public RouteRefreshRequest(String departureSemanticId, StopLocation origin, StopLocation destination, QueryTimeDefinition timeDefinition, DateTime searchTime) {
        this.origin = origin;
        this.destination = destination;
        this.timeDefinition = timeDefinition;
        this.searchTime = searchTime;
        this.departureSemanticId = departureSemanticId;
    }

    /**
     * Create a request to load a specific route
     *
     * @param route The route for which fresh data should be retrieved
     */
    public RouteRefreshRequest(Route route) {
        this.origin = route.getDepartureStation();
        this.destination = route.getArrivalStation();
        this.timeDefinition = QueryTimeDefinition.EQUAL_OR_LATER;
        this.searchTime = route.getDepartureTime();
        if (route.getDeparture().getDepartureSemanticId() == null) {
            throw new IllegalStateException("Cannot create a route request when no departure semantic id is provided");
        }
        this.departureSemanticId = route.getDeparture().getDepartureSemanticId();
    }

    private RouteRefreshRequest(JSONObject jsonObject) throws JSONException, StopLocationNotResolvedException {
        super(jsonObject);
        this.departureSemanticId = jsonObject.getString(JSON_KEY_DEPARTURE_URI);
        this.origin = OpenTransportApi.getStopLocationProviderInstance().getStoplocationBySemanticId(jsonObject.getString(JSON_KEY_FROM_URI));
        this.destination = OpenTransportApi.getStopLocationProviderInstance().getStoplocationBySemanticId(jsonObject.getString(JSON_KEY_TO_URI));

        timeDefinition = QueryTimeDefinition.EQUAL_OR_LATER;
        searchTime = new DateTime(jsonObject.getLong(JSON_KEY_SEARCHTIME_MILLIS));
    }


    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject json = super.toJson();
        json.put(JSON_KEY_DEPARTURE_URI, getDepartureSemanticId());
        json.put(JSON_KEY_FROM_URI, getOrigin().getSemanticId());
        json.put(JSON_KEY_TO_URI, getDestination().getSemanticId());
        json.put(JSON_KEY_SEARCHTIME_MILLIS, searchTime.getMillis());
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
        return searchTime;
    }


    public String getDepartureSemanticId() {
        return departureSemanticId;
    }


    @Override
    public boolean equals(Object o) {
        if (o instanceof JSONObject){
            try {
                o = new RouteRefreshRequest((JSONObject) o);
            } catch (JSONException | StopLocationNotResolvedException e) {
                return false;
            }
        }

        if (!(o instanceof RouteRefreshRequest)) {
            return false;
        }

        RouteRefreshRequest other = (RouteRefreshRequest) o;
        return (getDepartureSemanticId().equals(other.getDepartureSemanticId()) && getOrigin().equals(other.getOrigin()) && getDestination().equals(other.getDestination()) && getTimeDefinition().equals(other.getTimeDefinition()) && getSearchTime().equals(other.getSearchTime()));
    }

    @Override
    public int compareTo( TransportDataRequest o) {
        if (!(o instanceof RouteRefreshRequest)) {
            return -1;
        }

        RouteRefreshRequest other = (RouteRefreshRequest) o;
        return getOrigin().equals(other.getOrigin()) ?
                getDestination().compareTo(other.getDestination()) :
                getOrigin().compareTo(other.getOrigin());
    }

    @Override
    public boolean equalsIgnoringTime(TransportDataRequest other) {
        if (!(other instanceof RouteRefreshRequest)) {
            return false;
        }
        // Not really meaningful for this request type
        RouteRefreshRequest o = (RouteRefreshRequest) other;
        return getDepartureSemanticId().equals(o.getDepartureSemanticId()) && getOrigin().equals(o.getOrigin()) && getDestination().equals(o.getDestination());
    }

    @Override
    public int getRequestTypeTag() {
        return RequestType.ROUTEDETAIL.getRequestTypeTag();
    }
}
