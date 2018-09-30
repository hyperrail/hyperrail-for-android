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

import android.support.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.io.Serializable;

import eu.opentransport.common.contracts.TransportOccupancyLevel;
import eu.opentransport.common.models.RouteLegEnd;

/**
 * The end of a route leg (either a departure or an arrival)
 */

public class IrailRouteLegEnd implements RouteLegEnd, Serializable {


    private DateTime time;


    private final IrailStation station;


    private String platform;
    private boolean isPlatformNormal;


    private Duration delay = Duration.ZERO;
    private boolean canceled;

    private boolean passed;

    @Nullable
    private TransportOccupancyLevel Occupancy = TransportOccupancyLevel.UNKNOWN;

    @Nullable
    private String uri;

    public IrailRouteLegEnd(IrailStation station,
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


    public IrailStation getStation() {
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
