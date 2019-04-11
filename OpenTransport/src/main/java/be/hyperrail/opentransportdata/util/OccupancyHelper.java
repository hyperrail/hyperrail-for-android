/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.util;

import android.support.annotation.DrawableRes;

import be.hyperrail.opentransportdata.R;
import be.hyperrail.opentransportdata.common.contracts.TransportOccupancyLevel;

/**
 * Helper functions for occupancy/spitsgids.
 */
public class OccupancyHelper {

    /**
     * Get an image visualising a certain occupancy level.
     *
     * @param occupancyLevel The occupancy level to visualise.
     * @return An image visualising the requested occupancy level.
     */
    public static
    @DrawableRes
    int getOccupancyDrawable(TransportOccupancyLevel occupancyLevel) {
        switch (occupancyLevel) {
            case LOW:
                return R.drawable.ic_occupancy_low;
            case MEDIUM:
                return R.drawable.ic_occupancy_medium;
            case HIGH:
                return R.drawable.ic_occupancy_high;
            case UNKNOWN:
            default:
                return R.drawable.ic_occupancy_unknown;
        }
    }
}
