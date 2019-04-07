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

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.RemoteViews;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import be.hyperrail.android.R;
import be.hyperrail.opentransportdata.common.models.VehicleStop;

public class NotificationLayoutBuilder {

    private NotificationLayoutBuilder(){
        // No public constructor
    }

    public static RemoteViews createNotificationLayout(Context context, VehicleStop stop) {
        DateTimeFormatter df = DateTimeFormat.forPattern("HH:mm");

        boolean hasArrivalInfo = (stop.getArrivalTime() != null);
        boolean hasDepartureInfo = (stop.getDepartureTime() != null);

        RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.notification_vehiclestop);

        setTrain(stop, contentView);

        contentView.setViewVisibility(R.id.container_occupancy, View.INVISIBLE);

        if (hasArrivalInfo && hasDepartureInfo) {
            setDepartureAndArrivalTime(stop, df, contentView);
        } else if (hasDepartureInfo) {
            setDepartureOrArrivalTime(context, df, contentView, stop.getDepartureTime(), stop.getDepartureDelay(), stop.getDelayedDepartureTime());
        } else if (hasArrivalInfo) {
            setDepartureOrArrivalTime(context, df, contentView, stop.getArrivalTime(), stop.getArrivalDelay(), stop.getDelayedArrivalTime());
        }

        contentView.setTextViewText(R.id.text_train_type, stop.getVehicle().getType());
        contentView.setTextViewText(R.id.text_train_number, stop.getVehicle().getNumber());

        setStoplocationAndPlatform(stop, contentView);

        return contentView;
    }

    private static void setTrain(VehicleStop stop, RemoteViews contentView) {
        if (!stop.isDepartureCanceled() && !stop.isArrivalCanceled()) {
            contentView.setViewVisibility(R.id.layout_train_status_container, View.INVISIBLE);
        } else {
            contentView.setViewVisibility(R.id.layout_train_status_container, View.VISIBLE);
        }
    }

    private static void setStoplocationAndPlatform(VehicleStop stop, RemoteViews contentView) {
        contentView.setTextViewText(R.id.text_station, stop.getStopLocation().getLocalizedName());
        contentView.setViewVisibility(R.id.text_station, View.VISIBLE);
        contentView.setTextViewText(R.id.text_platform, stop.getPlatform());
        contentView.setViewVisibility(R.id.layout_platform_container, View.VISIBLE);
    }

    private static void setDepartureAndArrivalTime(VehicleStop stop, DateTimeFormatter df, RemoteViews contentView) {
        contentView.setTextViewText(R.id.text_time1, df.print(stop.getArrivalTime()));
        contentView.setTextViewText(R.id.text_delay1, String.valueOf(stop.getArrivalDelay().getStandardMinutes()));
        contentView.setViewVisibility(R.id.text_time1, View.VISIBLE);
        if (stop.getArrivalDelay().getStandardMinutes() > 0) {
            contentView.setViewVisibility(R.id.text_delay1, View.VISIBLE);
        } else {
            contentView.setViewVisibility(R.id.text_delay1, View.INVISIBLE);
        }

        contentView.setTextViewText(R.id.text_time2, df.print(stop.getDepartureTime()));
        contentView.setTextViewText(R.id.text_delay2, String.valueOf(stop.getDepartureDelay().getStandardMinutes()));
        contentView.setViewVisibility(R.id.text_time2, View.VISIBLE);
        if (stop.getDepartureDelay().getStandardMinutes() > 0) {
            contentView.setViewVisibility(R.id.text_delay2, View.VISIBLE);
        } else {
            contentView.setViewVisibility(R.id.text_delay2, View.INVISIBLE);
        }
    }

    private static void setDepartureOrArrivalTime(Context context, DateTimeFormatter df, RemoteViews contentView, DateTime time, Duration delay, DateTime delayedTime) {
        contentView.setTextViewText(R.id.text_time1, df.print(time));
        contentView.setTextViewText(R.id.text_delay1, String.valueOf(delay.getStandardMinutes()));

        contentView.setViewVisibility(R.id.text_time1, View.VISIBLE);
        contentView.setViewVisibility(R.id.text_delay1, View.INVISIBLE);
        contentView.setViewVisibility(R.id.text_time2, View.INVISIBLE);
        contentView.setViewVisibility(R.id.text_delay2, View.INVISIBLE);

        if (delay.getStandardMinutes() > 0) {
            contentView.setViewVisibility(R.id.text_delay1, View.VISIBLE);
            contentView.setTextViewText(R.id.text_time2, df.print(delayedTime));
            contentView.setViewVisibility(R.id.text_time2, View.VISIBLE);
            contentView.setTextColor(R.id.text_time2, ContextCompat.getColor(context, R.color.colorDelay));
        }
    }

}
