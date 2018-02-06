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
    private final String mTrainId;

    @Nullable
    private DateTime mSearchTime;

    // Train IDs aren't always clear to end users, in order to be able to show users meaningful information on trains, some extra information is stored

    /**
     * The departure station of this train. Additional information for request history/favorites.
     */
    @Nullable
    private Station mTrainOriginStation;

    /**
     * The departure time at the departure station for this train. Additional information for request history/favorites.
     */
    @Nullable
    private DateTime mTrainDepartureTime;

    @Nullable
    private Station mTrainDirection;


    /**
     * Create a request for train departures or arrivals in a given station
     *
     * @param trainId    The train for which data should be retrieved
     * @param searchTime The time for which should be searched
     */
    // TODO: support between stations, target scroll station as optional (display) parameters
    public IrailTrainRequest(@NonNull String trainId, @Nullable DateTime searchTime) {
        super();
        this.mTrainId = trainId;
        this.mSearchTime = searchTime;
    }

    public IrailTrainRequest(@NonNull JSONObject jsonObject) throws JSONException {
        super(jsonObject);

        if (jsonObject.has("direction")) {
            this.mTrainDirection = IrailFactory.getStationsProviderInstance().getStationById(jsonObject.getString("direction"));
        } else {
            this.mTrainDirection = null;
        }

        // TODO: ids should not be tightly coupled to irail
        this.mTrainId = jsonObject.getString("id");
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject json = super.toJson();

        json.put("id", getTrainId());
        if (getDirection() != null) {
            json.put("direction", getDirection().getId());
        }
        if (this.getDepartureTime() != null) {
            json.put("departure_time", getDepartureTime().getMillis());
        }
        if (this.getOrigin() != null) {
            json.put("origin", getOrigin().getId());
        }
        mSearchTime = null;
        return json;
    }

    @NonNull
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
        return mTrainOriginStation;
    }

    public void setOrigin(@Nullable Station origin) {
        this.mTrainOriginStation = origin;
    }

    @Nullable
    public DateTime getDepartureTime() {
        return mTrainDepartureTime;
    }

    public void setDepartureTime(@Nullable DateTime departure) {
        this.mTrainDepartureTime = departure;
    }

    @NonNull
    public String getTrainId() {
        return mTrainId;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IrailTrainRequest)) {
            return false;
        }

        IrailTrainRequest other = (IrailTrainRequest) o;
        return (getTrainId().equals(other.getTrainId()) && getSearchTime().equals(other.getSearchTime()));
    }

    @Override
    public int compareTo(@NonNull IrailRequest o) {
        if (!(o instanceof IrailTrainRequest)) {
            return -1;
        }

        IrailTrainRequest other = (IrailTrainRequest) o;
        return getTrainId().compareTo(other.getTrainId());
    }

    /**
     * The direction of this train. Additional information for request history/favorites.
     */
    @Nullable
    public Station getDirection() {
        return mTrainDirection;
    }
}
