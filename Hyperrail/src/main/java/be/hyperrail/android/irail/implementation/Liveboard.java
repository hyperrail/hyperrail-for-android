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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import be.hyperrail.android.irail.contracts.PagedResource;
import be.hyperrail.android.irail.contracts.PagedResourceDescriptor;
import be.hyperrail.android.irail.contracts.RouteTimeDefinition;
import be.hyperrail.android.irail.db.Station;

/**
 * This class represents a liveboard entity, containing departures or arrivals.
 * This class extends a station with its departures.
 */
public class Liveboard extends Station implements Serializable, PagedResource {

    private VehicleStop[] mStops;
    private DateTime mSearchTime;
    private final RouteTimeDefinition mTimeDefinition;
    LiveboardType mType;
    private PagedResourceDescriptor mDescriptor;

    public Liveboard(Station station, VehicleStop[] stops, DateTime searchTime, LiveboardType type, RouteTimeDefinition timeDefinition) {
        super(
                station.getHafasId(),
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
        mType = type;
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

    public LiveboardType getLiveboardType() {
        return mType;
    }

    /**
     * Append this liveboard with stops from another liveboard
     *
     * @param other the other liveboards to merge into this one
     */
    public Liveboard withStopsAppended(Liveboard... other) {
        HashMap<String, VehicleStop> stopsByUri = new HashMap<>();
        for (VehicleStop stop :
                mStops) {
            stopsByUri.put(stop.getDepartureUri(), stop);
        }

        for (Liveboard liveboard : other
                ) {
            for (VehicleStop stop :
                    liveboard.getStops()) {
                stopsByUri.put(stop.getDepartureUri(), stop);
            }
        }

        VehicleStop[] stops = new VehicleStop[stopsByUri.size()];
        stops = stopsByUri.values().toArray(stops);

        Arrays.sort(stops, new Comparator<VehicleStop>() {
            @Override
            public int compare(VehicleStop o1, VehicleStop o2) {
                if (Liveboard.this.mType == LiveboardType.DEPARTURES) {
                    return o1.getDepartureTime().compareTo(o2.getDepartureTime());
                } else {
                    return o1.getArrivalTime().compareTo(o2.getArrivalTime());
                }
            }
        });

        return new Liveboard(this, stops, this.getSearchTime(), this.getLiveboardType(), this.getTimeDefinition());
    }

    @Override
    public PagedResourceDescriptor getPagedResourceDescriptor() {
        return mDescriptor;
    }

    @Override
    public void setPageInfo(PagedResourceDescriptor descriptor) {
        mDescriptor = descriptor;
    }

    public enum LiveboardType {
        DEPARTURES,
        ARRIVALS
    }
}
