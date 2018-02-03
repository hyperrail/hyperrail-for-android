/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.irail.implementation.requests;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import be.hyperrail.android.irail.contracts.IrailRequest;
import be.hyperrail.android.irail.contracts.RouteTimeDefinition;
import be.hyperrail.android.irail.db.Station;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.irail.implementation.TrainStub;

/**
 * A request for train data
 */
public class IrailTrainRequest extends IrailBaseRequest implements IrailRequest {

    private final TrainStub trainStub;
    private final DateTime searchTime;

    // Train IDs arent always clear to end users, in order to be able to show users meaningful information on trains, some extra information is stored

    /**
     * The departure station of this train
     */
    private Station origin;

    /**
     * The departure time at the departure station for this train
     */
    private DateTime departureTime;

    /**
     * Create a request for train departures or arrivals in a given station
     *
     * @param train      The train for which data should be retrieved
     * @param searchTime The time for which should be searched
     */
    public IrailTrainRequest(TrainStub train, RouteTimeDefinition timeDefinition, DateTime searchTime) {
        super();
        this.trainStub = train;
        this.searchTime = searchTime;
    }

    public IrailTrainRequest(JSONObject jsonObject) throws JSONException {
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
            this.departureTime = new DateTime();
        }
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject json = super.toJson();

        json.put("id", trainStub.getId());
        if (trainStub.getDirection() != null) {
            json.put("direction", trainStub.getDirection().getId());
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

    public DateTime getSearchTime() {
        return searchTime;
    }

    public Station getOrigin() {
        return origin;
    }

    public void setOrigin(Station origin) {
        this.origin = origin;
    }

    public DateTime getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(DateTime departure) {
        this.departureTime = departure;
    }
}
