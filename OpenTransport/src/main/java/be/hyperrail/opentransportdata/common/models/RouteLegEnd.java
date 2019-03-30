/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.common.models;

import android.support.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.io.Serializable;

import be.hyperrail.opentransportdata.common.contracts.TransportOccupancyLevel;

/**
 * The end of a route leg (either a departure or an arrival)
 */

public interface RouteLegEnd extends Serializable {

    DateTime getTime();

    DateTime getDelayedTime();

    StopLocation getStation();

    String getPlatform();

    Duration getDelay();

    boolean isCanceled();

    boolean isPlatformNormal();

    @Nullable
    TransportOccupancyLevel getOccupancy();

    @Nullable
    String getSemanticId();

    boolean isCompletedByVehicle();
}
