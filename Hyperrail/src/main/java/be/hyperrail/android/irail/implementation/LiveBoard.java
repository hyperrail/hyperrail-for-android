/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.irail.implementation;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.Serializable;

import be.hyperrail.android.irail.contracts.IrailDataResponse;
import be.hyperrail.android.irail.db.Station;
import be.hyperrail.android.util.ArrayUtils;

/**
 * This class represents a liveboard entity, containing departures or arrivals.
 * This class extends a station with its departures.
 */
public class LiveBoard extends Station implements Serializable {

    private TrainStop[] stops;
    private DateTime searchDate;

    LiveBoard(Station station, TrainStop[] stops, DateTime searchDate) {
        super(
                station.getId(),
                station.getName(),
                station.getAlternativeNl(),
                station.getAlternativeFr(),
                station.getAlternativeDe(),
                station.getAlternativeEn(),
                station.getLocalizedName(),
                station.getCountryCode(),
                station.getLatitude(),
                station.getLongitude(),
                station.getAvgStopTimes());
        this.stops = stops;
        this.searchDate = searchDate;
    }

    public TrainStop[] getStops() {
        return stops;
    }

    public ApiResponse<TrainStop[]> getNextStops() {
        // get last time
        DateTime lastSearchTime;

        if (this.stops.length > 0) {
            lastSearchTime = new DateTime(this.stops[this.stops.length - 1].getDelayedDepartureTime());
            // move one minute further
            DateTimeFormatter dtf = DateTimeFormat.forPattern("dd-MM-yyyy HH:mm");
            lastSearchTime = lastSearchTime.plusMinutes(1);
        } else {
            // if it was empty (caused by e.g. night or weekend
            lastSearchTime = new DateTime(searchDate);
            // move one hour further
            lastSearchTime = lastSearchTime.plusHours(1);
        }

        // load
        IrailDataResponse<LiveBoard> apiResponse = getLiveBoard(lastSearchTime);

        if (!apiResponse.isSuccess()) {
            return new ApiResponse<>(null, apiResponse.getException());
        }

        LiveBoard newSearch = apiResponse.getData();

        int i = 0;
        while (newSearch.getStops().length == 0 && i < 12) {

            // add an hour when completely empty
            lastSearchTime = lastSearchTime.plusHours(1);

            // load
            apiResponse = getLiveBoard(lastSearchTime);

            if (!apiResponse.isSuccess()) {
                return new ApiResponse<>(null, apiResponse.getException());
            }

            newSearch = apiResponse.getData();
            i++;
        }
        // add new stops
        this.stops = ArrayUtils.concatenate(this.getStops(), newSearch.getStops());

        return new ApiResponse<>(newSearch.getStops());
    }

    public DateTime getSearchDate() {
        return searchDate;
    }
}
