/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.irail.implementation;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.io.Serializable;

import be.hyperrail.android.irail.contracts.OccupancyLevel;
import be.hyperrail.android.irail.db.Station;

/**
 * The end of a route leg (either a departure or an arrival)
 */

public class RouteLegEnd implements Serializable {

    @NonNull
    private DateTime time;

    @NonNull
    private final Station station;

    @NonNull
    private String platform;
    private boolean isPlatformNormal;

    @NonNull
    private Duration delay = Duration.ZERO;
    private boolean canceled;

    private boolean passed;

    @Nullable
    private OccupancyLevel Occupancy = OccupancyLevel.UNKNOWN;

    @Nullable
    private String uri;

    protected RouteLegEnd(@NonNull Station station,
                       @NonNull DateTime time, @NonNull String platform, boolean normal, @NonNull Duration delay,
                       boolean canceled, boolean passed, @Nullable String semanticId,
                       @Nullable OccupancyLevel occupancy) {
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

    @NonNull
    public DateTime getTime() {
        return time;
    }

    public DateTime getDelayedTime() {
        return time.plus(delay);
    }

    @NonNull
    public Station getStation() {
        return station;
    }

    @NonNull
    public String getPlatform() {
        return platform;
    }

    @NonNull
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
    public OccupancyLevel getOccupancy() {
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
