/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.irail.implementation;

import android.support.annotation.DrawableRes;

import be.hyperrail.android.R;
import be.hyperrail.android.irail.contracts.OccupancyLevel;

/**
 * Helper functions for occupancy/spitsgids
 */
public class OccupancyHelper {

    public static
    @DrawableRes
    int getOccupancyDrawable(OccupancyLevel occupancyLevel) {
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
