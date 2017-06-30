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

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

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
    public static String formatDuration(Period duration) {
        // to minutes
        PeriodFormatter hhmm = new PeriodFormatterBuilder()
                .printZeroAlways()
                .minimumPrintedDigits(2) // gives the '01'
                .appendHours()
                .appendSeparator(":")
                .appendMinutes()
                .toFormatter();
        return duration.toString(hhmm);
    }

    /**
     * Calculate the duration between t1 (start time) and t2 (end time)
     *
     * @param t1     time 1 (start)
     * @param delay1 delay in seconds
     * @param t2     time 2 (stop)
     * @param delay2 delay in seconds
     * @return h:mm formatted duration
     */
    @SuppressLint("DefaultLocale")
    public static String formatDuration(DateTime t1, Duration delay1, DateTime t2, Duration delay2) {

        // duration in ms
        Period duration = new Period(t1, t2);

        // later start reduces, later stop increases duration
        duration = duration.minus(delay1.toPeriod()).plus(delay2.toPeriod());
        return formatDuration(duration);
    }
}
