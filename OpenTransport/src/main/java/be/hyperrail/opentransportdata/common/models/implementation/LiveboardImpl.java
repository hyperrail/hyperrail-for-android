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

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.common.models.implementation;

import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import be.hyperrail.opentransportdata.common.contracts.NextDataPointer;
import be.hyperrail.opentransportdata.common.contracts.QueryTimeDefinition;
import be.hyperrail.opentransportdata.common.models.Liveboard;
import be.hyperrail.opentransportdata.common.models.LiveboardType;
import be.hyperrail.opentransportdata.common.models.StopLocation;
import be.hyperrail.opentransportdata.common.models.VehicleStop;

/**
 * This class represents a liveboard entity, containing departures or arrivals.
 * This class extends a station with its departures.
 */
public class LiveboardImpl extends StopLocationImpl implements Liveboard, Serializable {

    private VehicleStop[] mStops;
    private DateTime mSearchTime;
    private final QueryTimeDefinition mTimeDefinition;
    LiveboardType mType;

    private NextDataPointer previousPagePointer;
    private NextDataPointer currentPagePointer;
    private NextDataPointer nextPagePointer;

    public LiveboardImpl(StopLocation station, VehicleStop[] stops, DateTime searchTime, LiveboardType type, QueryTimeDefinition timeDefinition) {
        super(station);

        mStops = stops;
        if (mStops == null) {
            mStops = new VehicleStopImpl[0];
        }

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

    public QueryTimeDefinition getTimeDefinition() {
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
    public LiveboardImpl withStopsAppended(LiveboardImpl... other) {
        Set<String> knownUris = new HashSet<>();
        List<VehicleStop> combinedStops = new ArrayList<>();
        for (VehicleStop stop : mStops) {
            knownUris.add(stop.getDepartureUri());
            combinedStops.add(stop);
        }

        for (LiveboardImpl liveboard : other) {
            for (VehicleStop stop : liveboard.getStops()) {
                if (!knownUris.contains(stop.getDepartureUri())) {
                    knownUris.add(stop.getDepartureUri());
                    combinedStops.add(stop);
                }
            }
        }

        VehicleStop[] stops = new VehicleStopImpl[knownUris.size()];
        stops = combinedStops.toArray(stops);

        Arrays.sort(stops, (o1, o2) -> {
            if (LiveboardImpl.this.mType == LiveboardType.DEPARTURES) {
                return o1.getDepartureTime().compareTo(o2.getDepartureTime());
            } else {
                return o1.getArrivalTime().compareTo(o2.getArrivalTime());
            }
        });

        return new LiveboardImpl(this, stops, this.getSearchTime(), this.getLiveboardType(), this.getTimeDefinition());
    }

    public void setPageInfo(NextDataPointer previous,
                            NextDataPointer current,
                            NextDataPointer next) {
        this.previousPagePointer = previous;
        this.currentPagePointer = current;
        this.nextPagePointer = next;
    }

    @Override
    public NextDataPointer getPreviousResultsPointer() {
        return previousPagePointer;
    }

    @Override
    public NextDataPointer getCurrentResultsPointer() {
        return currentPagePointer;
    }

    @Override
    public NextDataPointer getNextResultsPointer() {
        return nextPagePointer;
    }
}
