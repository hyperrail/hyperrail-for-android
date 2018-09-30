/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */


package eu.opentransport.common.requests;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import eu.opentransport.OpenTransport;
import eu.opentransport.common.contracts.QueryTimeDefinition;
import eu.opentransport.common.contracts.TransportDataRequest;
import eu.opentransport.common.exceptions.StopLocationNotResolvedException;
import eu.opentransport.common.models.Route;
import eu.opentransport.common.models.Station;

/**
 * A request for a route between two or more stations
 */
public class IrailRouteRequest extends IrailBaseRequest<Route> implements TransportDataRequest<Route> {


    private final Station origin;


    private final Station destination;


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
    public IrailRouteRequest( String departureSemanticId,  Station origin,  Station destination,  QueryTimeDefinition timeDefinition,  DateTime searchTime) {
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
    public IrailRouteRequest( Route route) {
        this.origin = route.getDepartureStation();
        this.destination = route.getArrivalStation();
        this.timeDefinition = QueryTimeDefinition.DEPART_AT;
        this.searchTime = route.getDepartureTime();
        if (route.getDeparture().getDepartureSemanticId() == null) {
            throw new IllegalStateException("Cannot create a route request when no departure semantic id is provided");
        }
        this.departureSemanticId = route.getDeparture().getDepartureSemanticId();
    }

    public IrailRouteRequest( JSONObject jsonObject) throws JSONException, StopLocationNotResolvedException {
        super(jsonObject);
        this.departureSemanticId = jsonObject.getString("departure_semantic_id");
        this.origin = OpenTransport.getStationsProviderInstance().getStationByIrailApiId(jsonObject.getString("from"));
        this.destination = OpenTransport.getStationsProviderInstance().getStationByIrailApiId(jsonObject.getString("to"));

        timeDefinition = QueryTimeDefinition.DEPART_AT;
        searchTime = new DateTime(jsonObject.getLong("time"));
    }


    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject json = super.toJson();
        json.put("departure_semantic_id", getDepartureSemanticId());
        json.put("from", getOrigin().getHafasId());
        json.put("to", getDestination().getHafasId());
        json.put("time", searchTime.getMillis());
        return json;
    }


    public Station getOrigin() {
        return origin;
    }


    public Station getDestination() {
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
                o = new IrailRouteRequest((JSONObject) o);
            } catch (JSONException | StopLocationNotResolvedException e) {
                return false;
            }
        }

        if (!(o instanceof IrailRouteRequest)) {
            return false;
        }

        IrailRouteRequest other = (IrailRouteRequest) o;
        return (getDepartureSemanticId().equals(other.getDepartureSemanticId()) && getOrigin().equals(other.getOrigin()) && getDestination().equals(other.getDestination()) && getTimeDefinition().equals(other.getTimeDefinition()) && getSearchTime().equals(other.getSearchTime()));
    }

    @Override
    public int compareTo( TransportDataRequest o) {
        if (!(o instanceof IrailRouteRequest)) {
            return -1;
        }

        IrailRouteRequest other = (IrailRouteRequest) o;
        return getOrigin().equals(other.getOrigin()) ?
                getDestination().compareTo(other.getDestination()) :
                getOrigin().compareTo(other.getOrigin());
    }

    @Override
    public boolean equalsIgnoringTime(TransportDataRequest other) {
        if (!(other instanceof IrailRouteRequest)) {
            return false;
        }
        // Not really meaningful for this request type
        IrailRouteRequest o = (IrailRouteRequest) other;
        return getDepartureSemanticId().equals(o.getDepartureSemanticId()) && getOrigin().equals(o.getOrigin()) && getDestination().equals(o.getDestination());
    }
}
