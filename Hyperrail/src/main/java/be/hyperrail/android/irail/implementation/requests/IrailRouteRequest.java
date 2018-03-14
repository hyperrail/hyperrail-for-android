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

import be.hyperrail.android.irail.contracts.IrailRequest;
import be.hyperrail.android.irail.contracts.RouteTimeDefinition;
import be.hyperrail.android.irail.db.Station;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.irail.implementation.Route;

/**
 * A request for a route between two or more stations
 */
public class IrailRouteRequest extends IrailBaseRequest<Route> implements IrailRequest<Route> {

    @NonNull
    private final Station origin;

    @NonNull
    private final Station destination;

    @NonNull
    private final RouteTimeDefinition timeDefinition;

    @NonNull
    private final DateTime searchTime;

    /**
     * The (semantic) id for this route
     */
    @NonNull
    private final String departureSemanticId;

    /**
     * Create a request to get a specific between two stations
     */
    // TODO: support vias
    public IrailRouteRequest(@NonNull String departureSemanticId, @NonNull Station origin, @NonNull Station destination, @NonNull RouteTimeDefinition timeDefinition, @NonNull DateTime searchTime) {
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
    public IrailRouteRequest(@NonNull Route route) {
        this.origin = route.getDepartureStation();
        this.destination = route.getArrivalStation();
        this.timeDefinition = RouteTimeDefinition.DEPART;
        this.searchTime = route.getDepartureTime();
        if (route.getDeparture().getDepartureSemanticId() == null) {
            throw new IllegalStateException("Cannot create a route request when no departure semantic id is provided");
        }
        this.departureSemanticId = route.getDeparture().getDepartureSemanticId();
    }

    public IrailRouteRequest(@NonNull JSONObject jsonObject) throws JSONException {
        super(jsonObject);
        this.departureSemanticId = jsonObject.getString("departure_semantic_id");
        this.origin = IrailFactory.getStationsProviderInstance().getStationById(jsonObject.getString("from"));
        this.destination = IrailFactory.getStationsProviderInstance().getStationById(jsonObject.getString("to"));

        timeDefinition = RouteTimeDefinition.DEPART;
        searchTime = new DateTime(jsonObject.getLong("time"));
    }

    @NonNull
    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject json = super.toJson();
        json.put("departure_semantic_id", getDepartureSemanticId());
        json.put("from", getOrigin().getId());
        json.put("to", getDestination().getId());
        json.put("time", searchTime.getMillis());
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
        return searchTime;
    }

    @NonNull
    public String getDepartureSemanticId() {
        return departureSemanticId;
    }


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IrailRouteRequest)) {
            return false;
        }

        IrailRouteRequest other = (IrailRouteRequest) o;
        return (getDepartureSemanticId().equals(other.getDepartureSemanticId()) && getOrigin().equals(other.getOrigin()) && getDestination().equals(other.getDestination()) && getTimeDefinition().equals(other.getTimeDefinition()) && getSearchTime().equals(other.getSearchTime()));
    }

    @Override
    public int compareTo(@NonNull IrailRequest o) {
        if (!(o instanceof IrailRouteRequest)) {
            return -1;
        }

        IrailRouteRequest other = (IrailRouteRequest) o;
        return getOrigin().equals(other.getOrigin()) ?
                getDestination().compareTo(other.getDestination()) :
                getOrigin().compareTo(other.getOrigin());
    }

    @Override
    public boolean equalsIgnoringTime(IrailRequest other) {
        if (!(other instanceof IrailRouteRequest)) {
            return false;
        }
        // Not really meaningful for this request type
        IrailRouteRequest o = (IrailRouteRequest) other;
        return getDepartureSemanticId().equals(o.getDepartureSemanticId()) && getOrigin().equals(o.getOrigin()) && getDestination().equals(o.getDestination());
    }
}
