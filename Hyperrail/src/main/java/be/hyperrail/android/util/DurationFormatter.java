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

package be.hyperrail.android.util;

import android.annotation.SuppressLint;

import java.util.Date;

/**
 * Utility class to format durations
 */
public class DurationFormatter {

    /**
     * Format a duration (in seconds) as hh:mm
     *
     * @param duration The duration in seconds
     * @return The duration formatted as hh:mm string
     */
    @SuppressLint("DefaultLocale")
    public static String formatDuration(long duration) {
        // to minutes
        duration = duration / (1000 * 60);

        int hours = (int) (duration / 60);
        int minutes = (int) (duration % 60);
        return String.format("%01d:%02d", hours, minutes);
    }

    /**
     * Calculate the duration between t1 (start time) and t2 (end time)
     *
     * @param t1     time 1 (start)
     * @param delay1 delay in minutes
     * @param t2     time 2 (stop)
     * @param delay2 delay in minutes
     * @return h:mm formatted duration
     */
    @SuppressLint("DefaultLocale")
    public static String formatDuration(Date t1, int delay1, Date t2, int delay2) {

        // duration in ms
        long duration = t2.getTime() - t1.getTime();

        // to minutes
        duration = duration / (1000 * 60);

        // later start reduces, later stop increases duration
        duration = duration - delay1 + delay2;

        int hours = (int) (duration / 60);
        int minutes = (int) (duration % 60);
        return String.format("%01d:%02d", hours, minutes);
    }
}
