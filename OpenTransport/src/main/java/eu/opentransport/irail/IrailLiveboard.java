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

package eu.opentransport.irail;

import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;

import eu.opentransport.common.contracts.PagedDataResource;
import eu.opentransport.common.contracts.PagedDataResourceDescriptor;
import eu.opentransport.common.contracts.QueryTimeDefinition;
import eu.opentransport.common.models.Liveboard;
import eu.opentransport.common.models.LiveboardType;
import eu.opentransport.common.models.StopLocation;

/**
 * This class represents a liveboard entity, containing departures or arrivals.
 * This class extends a station with its departures.
 */
public class IrailLiveboard extends IrailStation implements Liveboard, Serializable, PagedDataResource {

    private IrailVehicleStop[] mStops;
    private DateTime mSearchTime;
    private final QueryTimeDefinition mTimeDefinition;
    LiveboardType mType;
    private PagedDataResourceDescriptor mDescriptor;

    public IrailLiveboard(StopLocation station, IrailVehicleStop[] stops, DateTime searchTime, LiveboardType type, QueryTimeDefinition timeDefinition) {
        super(station);

        mStops = stops;
        if (mStops == null) {
            mStops = new IrailVehicleStop[0];
        }

        mSearchTime = searchTime;
        mTimeDefinition = timeDefinition;
        mType = type;
    }


    public IrailVehicleStop[] getStops() {
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
    IrailLiveboard withStopsAppended(IrailLiveboard... other) {
        HashMap<String, IrailVehicleStop> stopsByUri = new HashMap<>();
        for (IrailVehicleStop stop :
                mStops) {
            stopsByUri.put(stop.getDepartureUri(), stop);
        }

        for (IrailLiveboard liveboard : other
                ) {
            for (IrailVehicleStop stop :
                    liveboard.getStops()) {
                stopsByUri.put(stop.getDepartureUri(), stop);
            }
        }

        IrailVehicleStop[] stops = new IrailVehicleStop[stopsByUri.size()];
        stops = stopsByUri.values().toArray(stops);

        Arrays.sort(stops, (o1, o2) -> {
            if (IrailLiveboard.this.mType == LiveboardType.DEPARTURES) {
                return o1.getDepartureTime().compareTo(o2.getDepartureTime());
            } else {
                return o1.getArrivalTime().compareTo(o2.getArrivalTime());
            }
        });

        return new IrailLiveboard(this, stops, this.getSearchTime(), this.getLiveboardType(), this.getTimeDefinition());
    }

    @Override
    public PagedDataResourceDescriptor getPagedResourceDescriptor() {
        return mDescriptor;
    }

    @Override
    public void setPageInfo(PagedDataResourceDescriptor descriptor) {
        mDescriptor = descriptor;
    }
}
