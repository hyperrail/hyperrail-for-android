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

import java.io.Serializable;
import java.util.Date;

import be.hyperrail.android.irail.contracts.IrailDataResponse;
import be.hyperrail.android.irail.db.Station;
import be.hyperrail.android.util.ArrayUtils;

/**
 * This class represents a liveboard entity, containing departures or arrivals.
 * This class extends a station with its departures.
 */
public class LiveBoard extends Station implements Serializable {
    private TrainStop[] stops;
    private Date searchDate;

    LiveBoard(Station station, TrainStop[] stops, Date searchDate) {
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
        Date lastSearch;

        if (this.stops.length > 0) {
            lastSearch = (Date) this.stops[this.stops.length - 1].getDepartureTime().clone();
            // move one minute further
            lastSearch.setTime(lastSearch.getTime() + 1000 * 60);
        } else {
            // if it was empty (caused by e.g. night or weekend
            lastSearch = (Date) searchDate.clone();
            // move one hour further
            lastSearch.setTime(lastSearch.getTime() + 1000 * 60 * 60);
        }

        // load
        IrailDataResponse<LiveBoard> apiResponse =  getLiveBoard(lastSearch);

        if (!apiResponse.isSuccess()) {
            return new ApiResponse<>(null, apiResponse.getException());
        }

        LiveBoard newSearch = apiResponse.getData();

        int i = 0;
        while (newSearch.getStops().length == 0 && i < 12) {
            // add an hour
            lastSearch.setTime(lastSearch.getTime() + 1000 * 60 * 60);

            // load
            apiResponse =  getLiveBoard(lastSearch);

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

    public Date getSearchDate() {
        return searchDate;
    }
}
