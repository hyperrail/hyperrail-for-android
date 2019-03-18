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

package eu.opentransport.common.models;

import android.support.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.io.Serializable;

import eu.opentransport.common.contracts.TransportOccupancyLevel;
import eu.opentransport.irail.IrailVehicleStop;

/**
 * A transfer between two route legs.
 * This is a helper class, to make it easier displaying and using route information. It's constructed based on routeLegs.
 */
public interface Transfer extends Serializable {

    @Nullable
    DateTime getArrivalTime();

    @Nullable
    DateTime getDepartureTime();

    DateTime getDelayedDepartureTime();

    DateTime getDelayedArrivalTime();


    StopLocation getStation();

    @Nullable
    String getDeparturePlatform();

    @Nullable
    String getArrivalPlatform();


    Duration getArrivalDelay();

    boolean isArrivalCanceled();


    Duration getDepartureDelay();

    boolean isDepartureCanceled();

    boolean isArrivalPlatformNormal();

    boolean isDeparturePlatformNormal();

    @Nullable
    TransportOccupancyLevel getDepartureOccupancy();

    @Nullable
    String getDepartureSemanticId();

    boolean hasLeft();

    boolean hasArrived();

    TransferType getType();

    @Nullable
    RouteLeg getArrivalLeg();

    @Nullable
    RouteLeg getDepartureLeg();

    @Nullable
    IrailVehicleStop toDepartureVehicleStop();

}
