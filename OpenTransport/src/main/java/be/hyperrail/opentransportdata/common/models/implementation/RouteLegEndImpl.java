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

import android.support.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.io.Serializable;

import be.hyperrail.opentransportdata.common.contracts.TransportOccupancyLevel;
import be.hyperrail.opentransportdata.common.models.RouteLegEnd;
import be.hyperrail.opentransportdata.common.models.StopLocation;

/**
 * The end of a route leg (either a departure or an arrival)
 */

public class RouteLegEndImpl implements RouteLegEnd, Serializable {


    private DateTime time;

    private final StopLocation station;

    private String platform;
    private boolean isPlatformNormal;


    private Duration delay = Duration.ZERO;
    private boolean canceled;

    private boolean passed;

    @Nullable
    private TransportOccupancyLevel Occupancy = TransportOccupancyLevel.UNKNOWN;

    @Nullable
    private String uri;

    public RouteLegEndImpl(StopLocation station,
                           DateTime time, String platform, boolean normal, Duration delay,
                           boolean canceled, boolean passed, @Nullable String semanticId,
                           @Nullable TransportOccupancyLevel occupancy) {
        this.station = station;
        this.passed = passed;

        this.time = time;
        this.platform = platform;
        this.delay = delay;

        this.canceled = canceled;
        this.isPlatformNormal = normal;

        this.uri = semanticId;
        this.Occupancy = occupancy;
    }


    public DateTime getTime() {
        return time;
    }

    public DateTime getDelayedTime() {
        return time.plus(delay);
    }


    public StopLocation getStation() {
        return station;
    }


    public String getPlatform() {
        return platform;
    }


    public Duration getDelay() {
        return delay;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public boolean isPlatformNormal() {
        return isPlatformNormal;
    }

    @Nullable
    public TransportOccupancyLevel getOccupancy() {
        return Occupancy;
    }

    @Nullable
    public String getUri() {
        return uri;
    }

    public boolean hasPassed() {
        return passed;
    }

}
