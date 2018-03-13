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

import java.io.Serializable;

import be.hyperrail.android.irail.contracts.RouteTimeDefinition;
import be.hyperrail.android.irail.db.Station;

/**
 * This class represents a liveboard entity, containing departures or arrivals.
 * This class extends a station with its departures.
 */
public class LiveBoard extends Station implements Serializable {

    private VehicleStop[] mStops;
    private DateTime mSearchTime;
    private final RouteTimeDefinition mTimeDefinition;

    public LiveBoard(Station station, VehicleStop[] stops, DateTime searchTime, RouteTimeDefinition timeDefinition) {
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
        mStops = stops;
        mSearchTime = searchTime;
        mTimeDefinition = timeDefinition;
    }

    public VehicleStop[] getStops() {
        return mStops;
    }

    public DateTime getSearchTime() {
        return mSearchTime;
    }

    public RouteTimeDefinition getTimeDefinition() {
        return mTimeDefinition;
    }
}
