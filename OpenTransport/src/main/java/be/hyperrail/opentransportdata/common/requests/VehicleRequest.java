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
import be.hyperrail.opentransportdata.common.contracts.TransportDataRequest;
import be.hyperrail.opentransportdata.common.exceptions.StopLocationNotResolvedException;
import be.hyperrail.opentransportdata.common.models.StopLocation;
import be.hyperrail.opentransportdata.common.models.VehicleJourney;

/**
 * A request for train data
 */
public class VehicleRequest extends OpenTransportBaseRequest<VehicleJourney> implements TransportDataRequest<VehicleJourney> {


    private static final String JSON_KEY_DIRECTION_URI = "direction";
    private static final String JSON_KEY_DEPARTURETIME_MILLIS = "departure_time";
    private static final String JSON_KEY_ORIGIN_URI = "origin";
    private static final String JSON_KEY_VEHICLE_ID = "id";
    private final String mVehicleId;

    @Nullable
    private DateTime mSearchTime;

    // VehicleJourney IDs aren't always clear to end users, in order to be able to show users meaningful information on trains, some extra information is stored

    /**
     * The departure station of this train. Additional information for request history/favorites.
     */
    @Nullable
    private StopLocation mVehicleOriginStation;

    /**
     * The departure time at the departure station for this train. Additional information for request history/favorites.
     */
    @Nullable
    private DateTime mVehicleDepartureTime;

    @Nullable
    private StopLocation mVehicleDirection;

    /**
     * Create a request for train departures or arrivals in a given station
     *
     * @param vehicleId  The train for which data should be retrieved
     * @param searchTime The time for which should be searched
     */
    // TODO: support between stations, target scroll station as optional (display) parameters
    public VehicleRequest(String vehicleId, @Nullable DateTime searchTime) {
        this.mVehicleId = vehicleId;
        this.mSearchTime = searchTime;
    }

    public VehicleRequest(JSONObject jsonObject) throws JSONException, StopLocationNotResolvedException {
        super(jsonObject);

        if (jsonObject.has(JSON_KEY_DIRECTION_URI)) {
            this.mVehicleDirection = OpenTransportApi.getStopLocationProviderInstance().getStoplocationBySemanticId(jsonObject.getString(JSON_KEY_DIRECTION_URI));
        } else {
            this.mVehicleDirection = null;
        }
        this.mVehicleId = jsonObject.getString(JSON_KEY_VEHICLE_ID);
    }


    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject json = super.toJson();

        json.put(JSON_KEY_VEHICLE_ID, getVehicleId());
        if (getDirection() != null) {
            json.put(JSON_KEY_DIRECTION_URI, getDirection().getSemanticId());
        }
        if (this.getDepartureTime() != null) {
            json.put(JSON_KEY_DEPARTURETIME_MILLIS, getDepartureTime().getMillis());
        }
        if (this.getOrigin() != null) {
            json.put(JSON_KEY_ORIGIN_URI, getOrigin().getSemanticId());
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
    public StopLocation getOrigin() {
        return mVehicleOriginStation;
    }

    public void setOrigin(@Nullable StopLocation origin) {
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
        if (o instanceof JSONObject) {
            try {
                o = new VehicleRequest((JSONObject) o);
            } catch (JSONException | StopLocationNotResolvedException e) {
                return false;
            }
        }

        if (!(o instanceof VehicleRequest)) {
            return false;
        }

        VehicleRequest other = (VehicleRequest) o;
        return (getVehicleId().equals(other.getVehicleId()) && getSearchTime().equals(other.getSearchTime()));
    }

    @Override
    public int compareTo(TransportDataRequest o) {
        if (!(o instanceof VehicleRequest)) {
            return -1;
        }

        VehicleRequest other = (VehicleRequest) o;
        return getVehicleId().compareTo(other.getVehicleId());
    }

    /**
     * The direction of this train. Additional information for request history/favorites.
     */
    @Nullable
    public StopLocation getDirection() {
        return mVehicleDirection;
    }

    public void setDirection(StopLocation direction) {
        mVehicleDirection = direction;
    }

    @Override
    public boolean equalsIgnoringTime(TransportDataRequest other) {
        return other instanceof VehicleRequest && getVehicleId().equals(((VehicleRequest) other).getVehicleId());
    }

    @Override
    public int getRequestTypeTag() {
        return RequestType.VEHICLEJOURNEY.getRequestTypeTag();
    }
}
