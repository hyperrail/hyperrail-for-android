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
import be.hyperrail.android.irail.db.Station;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.irail.implementation.Train;

/**
 * A request for train data
 */
public class IrailTrainRequest extends IrailBaseRequest<Train> implements IrailRequest<Train> {

    @NonNull
    private final String trainId;

    @Nullable
    private DateTime searchTime;

    // Train IDs arent always clear to end users, in order to be able to show users meaningful information on trains, some extra information is stored

    /**
     * The departure station of this train. Additional information for request history/favorites.
     */
    @Nullable
    private Station origin;

    /**
     * The departure time at the departure station for this train. Additional information for request history/favorites.
     */
    @Nullable
    private DateTime departureTime;

    /**
     * The direction of this train. Additional information for request history/favorites.
     */
    @Nullable
    private Station direction;
    private Station targetStation;


    /**
     * Create a request for train departures or arrivals in a given station
     *
     * @param trainId    The train for which data should be retrieved
     * @param searchTime The time for which should be searched
     */
    // TODO: support between stations, target scroll station as optional (display) parameters
    public IrailTrainRequest(@NonNull String trainId, @Nullable DateTime searchTime) {
        super();
        this.trainId = trainId;
        this.searchTime = searchTime;
    }

    public IrailTrainRequest(@NonNull JSONObject jsonObject) throws JSONException {
        super(jsonObject);

        if (jsonObject.has("direction")) {
            this.direction = IrailFactory.getStationsProviderInstance().getStationById(jsonObject.getString("direction"));
        } else {
            this.direction = null;
        }

        // TODO: ids should not be tightly coupled to irail
        this.trainId = jsonObject.getString("id");

        if (jsonObject.has("time")) {
            this.searchTime = new DateTime(jsonObject.getLong("time"));
        } else {
            this.searchTime = null;
        }

        if (jsonObject.has("departure_time")) {
            this.departureTime = new DateTime(jsonObject.getLong("departure_time"));
        } else {
            this.departureTime = null;
        }
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject json = super.toJson();

        json.put("id", getTrainId());
        if (direction != null) {
            json.put("direction", direction.getId());
        }
        if (this.getDepartureTime() != null) {
            json.put("departure_time", getDepartureTime().getMillis());
        }
        if (this.getOrigin() != null) {
            json.put("origin", getOrigin().getId());
        }
        if (this.searchTime != null) {
            json.put("time", searchTime.getMillis());
        }
        return json;
    }

    @NonNull
    public DateTime getSearchTime() {
        if (searchTime == null) {
            return new DateTime();
        }
        return searchTime;
    }

    public boolean isNow() {
        return searchTime == null;
    }

    public void setSearchTime(@Nullable DateTime searchTime) {
        this.searchTime = searchTime;
    }

    @Nullable
    public Station getOrigin() {
        return origin;
    }

    public void setOrigin(@Nullable Station origin) {
        this.origin = origin;
    }

    @Nullable
    public DateTime getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(@Nullable DateTime departure) {
        this.departureTime = departure;
    }

    @NonNull
    public String getTrainId() {
        return trainId;
    }

    public Station getTargetStation() {
        return targetStation;
    }
}
