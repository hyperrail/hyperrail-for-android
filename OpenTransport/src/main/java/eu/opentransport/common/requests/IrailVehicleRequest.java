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
import eu.opentransport.common.contracts.TransportDataRequest;
import eu.opentransport.common.exceptions.StopLocationNotResolvedException;
import eu.opentransport.common.models.Station;
import eu.opentransport.common.models.Vehicle;

/**
 * A request for train data
 */
public class IrailVehicleRequest extends IrailBaseRequest<Vehicle> implements TransportDataRequest<Vehicle> {


    private final String mVehicleId;

    @Nullable
    private DateTime mSearchTime;

    // Vehicle IDs aren't always clear to end users, in order to be able to show users meaningful information on trains, some extra information is stored

    /**
     * The departure station of this train. Additional information for request history/favorites.
     */
    @Nullable
    private Station mVehicleOriginStation;

    /**
     * The departure time at the departure station for this train. Additional information for request history/favorites.
     */
    @Nullable
    private DateTime mVehicleDepartureTime;

    @Nullable
    private Station mVehicleDirection;


    /**
     * Create a request for train departures or arrivals in a given station
     *
     * @param vehicleId    The train for which data should be retrieved
     * @param searchTime The time for which should be searched
     */
    // TODO: support between stations, target scroll station as optional (display) parameters
    public IrailVehicleRequest( String vehicleId, @Nullable DateTime searchTime) {
        this.mVehicleId = vehicleId;
        this.mSearchTime = searchTime;
    }

    public IrailVehicleRequest( JSONObject jsonObject) throws JSONException, StopLocationNotResolvedException {
        super(jsonObject);

        if (jsonObject.has("direction")) {
            this.mVehicleDirection = OpenTransport.getStationsProviderInstance().getStationByIrailApiId(jsonObject.getString("direction"));
        } else {
            this.mVehicleDirection = null;
        }

        // TODO: ids should not be tightly coupled to irail
        this.mVehicleId = jsonObject.getString("id");
    }


    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject json = super.toJson();

        json.put("id", getVehicleId());
        if (getDirection() != null) {
            json.put("direction", getDirection().getHafasId());
        }
        if (this.getDepartureTime() != null) {
            json.put("departure_time", getDepartureTime().getMillis());
        }
        if (this.getOrigin() != null) {
            json.put("origin", getOrigin().getHafasId());
        }
        return json;
    }


    public DateTime getSearchTime() {
        if (mSearchTime == null) {
            return new DateTime();
        }
        return mSearchTime;
    }

    public boolean isNow() {
        return mSearchTime == null;
    }

    public void setSearchTime(@Nullable DateTime searchTime) {
        this.mSearchTime = searchTime;
    }

    @Nullable
    public Station getOrigin() {
        return mVehicleOriginStation;
    }

    public void setOrigin(@Nullable Station origin) {
        this.mVehicleOriginStation = origin;
    }

    @Nullable
    public DateTime getDepartureTime() {
        return mVehicleDepartureTime;
    }

    public void setDepartureTime(@Nullable DateTime departure) {
        this.mVehicleDepartureTime = departure;
    }


    public String getVehicleId() {
        return mVehicleId;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof JSONObject){
            try {
                o = new IrailVehicleRequest((JSONObject) o);
            } catch (JSONException | StopLocationNotResolvedException e) {
                return false;
            }
        }

        if (!(o instanceof IrailVehicleRequest)) {
            return false;
        }

        IrailVehicleRequest other = (IrailVehicleRequest) o;
        return (getVehicleId().equals(other.getVehicleId()) && getSearchTime().equals(other.getSearchTime()));
    }

    @Override
    public int compareTo( TransportDataRequest o) {
        if (!(o instanceof IrailVehicleRequest)) {
            return -1;
        }

        IrailVehicleRequest other = (IrailVehicleRequest) o;
        return getVehicleId().compareTo(other.getVehicleId());
    }

    /**
     * The direction of this train. Additional information for request history/favorites.
     */
    @Nullable
    public Station getDirection() {
        return mVehicleDirection;
    }

    public void setDirection(Station direction) {
        mVehicleDirection = direction;
    }

    @Override
    public boolean equalsIgnoringTime(TransportDataRequest other) {
        return other instanceof IrailVehicleRequest && getVehicleId().equals(((IrailVehicleRequest) other).getVehicleId());
    }
}
