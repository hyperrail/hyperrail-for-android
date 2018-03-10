/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.viewgroup;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.RemoteViews;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import be.hyperrail.android.R;
import be.hyperrail.android.irail.implementation.Transfer;
import be.hyperrail.android.irail.implementation.VehicleStop;
import be.hyperrail.android.util.DurationFormatter;

public class NotificationLayoutBuilder {

    public static RemoteViews createNotificationLayout(Context context, VehicleStop stop) {
        DateTimeFormatter df = DateTimeFormat.forPattern("HH:mm");

        boolean hasArrivalInfo = (stop.getArrivalTime() != null);
        boolean hasDepartureInfo = (stop.getDepartureTime() != null);

        RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.notification_vehiclestop);

        if (!stop.isDepartureCanceled() && !stop.isArrivalCanceled()) {
            contentView.setViewVisibility(R.id.layout_train_status_container, View.INVISIBLE);
        } else {
            contentView.setViewVisibility(R.id.layout_train_status_container, View.VISIBLE);
        }

        contentView.setViewVisibility(R.id.container_occupancy, View.INVISIBLE);

        if (hasArrivalInfo && hasDepartureInfo) {
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

        } else if (hasDepartureInfo) {
            // only departure info
            contentView.setTextViewText(R.id.text_time1, df.print(stop.getDepartureTime()));
            contentView.setTextViewText(R.id.text_delay1, String.valueOf(stop.getDepartureDelay().getStandardMinutes()));

            contentView.setViewVisibility(R.id.text_time1, View.VISIBLE);
            contentView.setViewVisibility(R.id.text_delay1, View.INVISIBLE);
            contentView.setViewVisibility(R.id.text_time2, View.INVISIBLE);
            contentView.setViewVisibility(R.id.text_delay2, View.INVISIBLE);

            if (stop.getDepartureDelay().getStandardMinutes() > 0) {
                contentView.setViewVisibility(R.id.text_delay1, View.VISIBLE);
                contentView.setTextViewText(R.id.text_time2, df.print(stop.getDelayedDepartureTime()));
                contentView.setViewVisibility(R.id.text_time2, View.VISIBLE);
                contentView.setTextColor(R.id.text_time2, ContextCompat.getColor(context, R.color.colorDelay));
            }

        } else if (hasArrivalInfo) {
            // only arrival info
            contentView.setTextViewText(R.id.text_time1, df.print(stop.getArrivalTime()));
            contentView.setTextViewText(R.id.text_delay1, String.valueOf(stop.getArrivalDelay().getStandardMinutes()));

            contentView.setViewVisibility(R.id.text_time1, View.VISIBLE);
            contentView.setViewVisibility(R.id.text_delay1, View.INVISIBLE);
            contentView.setViewVisibility(R.id.text_time2, View.INVISIBLE);
            contentView.setViewVisibility(R.id.text_delay2, View.INVISIBLE);

            if (stop.getArrivalDelay().getStandardMinutes() > 0) {
                contentView.setViewVisibility(R.id.text_delay1, View.VISIBLE);
                contentView.setTextViewText(R.id.text_time2, df.print(stop.getDelayedArrivalTime()));
                contentView.setViewVisibility(R.id.text_time2, View.VISIBLE);
                contentView.setTextColor(R.id.text_time2, ContextCompat.getColor(context, R.color.colorDelay));
            }

        }

        contentView.setTextViewText(R.id.text_station, stop.getStation().getLocalizedName());
        contentView.setViewVisibility(R.id.text_station, View.VISIBLE);
        contentView.setTextViewText(R.id.text_platform, stop.getPlatform());
        contentView.setViewVisibility(R.id.layout_platform_container, View.VISIBLE);

        return contentView;
    }

    public static RemoteViews createNotificationLayout(Context context, Transfer transfer) {
        DateTimeFormatter df = DateTimeFormat.forPattern("HH:mm");
        boolean hasDepartureInfo = transfer.getDepartureLeg() != null;
        boolean hasArrivalInfo = transfer.getArrivalLeg() != null;

        RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.notification_transfer);

        if (!transfer.isDepartureCanceled() && !transfer.isArrivalCanceled()) {
            contentView.setViewVisibility(R.id.layout_train_status_container, View.INVISIBLE);
        } else {
            contentView.setViewVisibility(R.id.layout_train_status_container, View.VISIBLE);
        }
        contentView.setViewVisibility(R.id.container_occupancy, View.INVISIBLE);

        if (hasArrivalInfo && hasDepartureInfo) {
            contentView.setTextViewText(R.id.text_time1, df.print(transfer.getArrivalTime()));
            contentView.setTextViewText(R.id.text_delay1, String.valueOf(transfer.getArrivalDelay().toStandardMinutes()));

            contentView.setViewVisibility(R.id.text_time1, View.VISIBLE);
            if (transfer.getArrivalDelay().getStandardMinutes() > 0) {
                contentView.setViewVisibility(R.id.text_delay1, View.VISIBLE);
            } else {
                contentView.setViewVisibility(R.id.text_delay1, View.INVISIBLE);
            }

            contentView.setTextViewText(R.id.text_time2, df.print(transfer.getDepartureTime()));
            contentView.setTextViewText(R.id.text_delay2, String.valueOf(transfer.getDepartureDelay().getStandardMinutes()));

            contentView.setViewVisibility(R.id.text_time2, View.VISIBLE);
            if (transfer.getDepartureDelay().getStandardMinutes() > 0) {
                contentView.setViewVisibility(R.id.text_delay2, View.VISIBLE);
            } else {
                contentView.setViewVisibility(R.id.text_delay2, View.INVISIBLE);
            }

            //TODO: cleanup
            contentView.setTextViewText(R.id.text_waiting_time, DurationFormatter.formatDuration(
                    transfer.getArrivalTime(), transfer.getArrivalDelay(),
                    transfer.getDepartureTime(), transfer.getDepartureDelay()));
        } else if (hasDepartureInfo) {
            // only departure info
            contentView.setTextViewText(R.id.text_time1, df.print(transfer.getDepartureTime()));
            contentView.setTextViewText(R.id.text_delay1, String.valueOf(transfer.getDepartureDelay().getStandardMinutes()));

            contentView.setViewVisibility(R.id.text_time1, View.VISIBLE);
            contentView.setViewVisibility(R.id.text_delay1, View.INVISIBLE);
            contentView.setViewVisibility(R.id.text_time2, View.INVISIBLE);
            contentView.setViewVisibility(R.id.text_delay2, View.INVISIBLE);

            if (transfer.getDepartureDelay().getStandardMinutes() > 0) {
                contentView.setViewVisibility(R.id.text_delay1, View.VISIBLE);
                contentView.setTextViewText(R.id.text_time2, df.print(transfer.getDepartureTime().withDurationAdded(transfer.getDepartureDelay(), 1)));
                contentView.setViewVisibility(R.id.text_time2, View.VISIBLE);
                contentView.setTextColor(R.id.text_time2, ContextCompat.getColor(context, R.color.colorDelay));
            }

        } else if (hasArrivalInfo) {
            // only arrival info
            contentView.setTextViewText(R.id.text_time1, df.print(transfer.getArrivalTime()));
            contentView.setTextViewText(R.id.text_delay1, String.valueOf(transfer.getArrivalDelay().getStandardMinutes()));

            contentView.setViewVisibility(R.id.text_time1, View.VISIBLE);
            contentView.setViewVisibility(R.id.text_delay1, View.INVISIBLE);
            contentView.setViewVisibility(R.id.text_time2, View.INVISIBLE);
            contentView.setViewVisibility(R.id.text_delay2, View.INVISIBLE);

            if (transfer.getArrivalDelay().getStandardMinutes() > 0) {
                contentView.setViewVisibility(R.id.text_delay1, View.VISIBLE);
                contentView.setTextViewText(R.id.text_time2, df.print(transfer.getArrivalTime().withDurationAdded(transfer.getArrivalDelay(), 1)));
                contentView.setViewVisibility(R.id.text_time2, View.VISIBLE);
                contentView.setTextColor(R.id.text_time2, ContextCompat.getColor(context, R.color.colorDelay));
            }
        }

        contentView.setTextViewText(R.id.text_station, transfer.getStation().getLocalizedName());
        contentView.setViewVisibility(R.id.text_station, View.VISIBLE);

        if (hasArrivalInfo) {
            contentView.setTextViewText(R.id.text_platform_arrival, transfer.getArrivalPlatform());
            contentView.setViewVisibility(R.id.layout_platform_arrival_container, View.VISIBLE);
        } else {
            contentView.setViewVisibility(R.id.layout_platform_arrival_container, View.INVISIBLE);
        }
        if (hasDepartureInfo) {
            contentView.setTextViewText(R.id.text_platform_departure, transfer.getDeparturePlatform());
            contentView.setViewVisibility(R.id.layout_platform_departure_container, View.VISIBLE);
        } else {
            contentView.setViewVisibility(R.id.layout_platform_departure_container, View.INVISIBLE);
        }

        return contentView;
    }

}
