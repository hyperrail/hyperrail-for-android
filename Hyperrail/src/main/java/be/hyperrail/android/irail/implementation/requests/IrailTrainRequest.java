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
import be.hyperrail.android.irail.implementation.TrainStub;

/**
 * A request for train data
 */
public class IrailTrainRequest extends IrailBaseRequest<Train> implements IrailRequest<Train> {

    @NonNull
    private final TrainStub trainStub;

    @NonNull
    private final DateTime searchTime;

    // Train IDs arent always clear to end users, in order to be able to show users meaningful information on trains, some extra information is stored

    /**
     * The departure station of this train
     */
    @Nullable
    private Station origin;

    /**
     * The departure time at the departure station for this train
     */
    @Nullable
    private DateTime departureTime;

    /**
     * Create a request for train departures or arrivals in a given station
     *
     * @param train      The train for which data should be retrieved
     * @param searchTime The time for which should be searched
     */
    public IrailTrainRequest(@NonNull TrainStub train, @NonNull DateTime searchTime) {
        super();
        this.trainStub = train;

        this.searchTime = searchTime;
    }

    public IrailTrainRequest(@NonNull JSONObject jsonObject) throws JSONException {
        super(jsonObject);
        Station direction = null;
        String id = jsonObject.getString("id");

        if (jsonObject.has("direction")) {
            direction = IrailFactory.getStationsProviderInstance().getStationById(jsonObject.getString("direction"));
        }

        // TODO: ids should not be tightly coupled to irail
        this.trainStub = new TrainStub(id, direction, "http://irail.be/vehicle/" + id);

        if (jsonObject.has("time")) {
            this.searchTime = new DateTime(jsonObject.getLong("time"));
        } else {
            this.searchTime = new DateTime();
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

        json.put("id", getTrainStub().getId());
        if (getTrainStub().getDirection() != null) {
            json.put("direction", getTrainStub().getDirection().getId());
        }
        if (this.getDepartureTime() != null) {
            json.put("departure_time", getDepartureTime().getMillis());
        }
        if (this.getOrigin() != null) {
            json.put("origin", getOrigin().getId());
        }
        json.put("time", searchTime.getMillis());
        return json;
    }

    @NonNull
    public DateTime getSearchTime() {
        return searchTime;
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
    public TrainStub getTrainStub() {
        return trainStub;
    }
}
