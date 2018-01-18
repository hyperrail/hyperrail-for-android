/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.viewgroup;

import android.content.Context;
import android.view.View;
import android.widget.RemoteViews;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import be.hyperrail.android.R;
import be.hyperrail.android.irail.implementation.TrainStop;

public class NotificationLayoutBuilder {

    public static RemoteViews createNotificationLayout(Context context, TrainStop stop) {
        DateTimeFormatter df = DateTimeFormat.forPattern("HH:mm");

        RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.notif_h64);
        contentView.setTextViewText(R.id.text_time1, df.print(stop.getDepartureTime()));
        contentView.setTextViewText(R.id.text_delay1, String.valueOf(stop.getDepartureDelay().getStandardMinutes()));
        contentView.setTextViewText(R.id.text_time2, df.print(stop.getArrivalTime()));
        contentView.setViewVisibility(R.id.text_time2, View.VISIBLE);
        contentView.setTextViewText(R.id.text_delay2, String.valueOf(stop.getArrivalDelay().getStandardMinutes()));
        contentView.setViewVisibility(R.id.text_delay2, View.VISIBLE);
        contentView.setTextViewText(R.id.text_station, stop.getStation().getLocalizedName());
        contentView.setViewVisibility(R.id.text_station, View.VISIBLE);
        contentView.setTextViewText(R.id.text_platform, stop.getPlatform());
        contentView.setViewVisibility(R.id.layout_platform_container, View.VISIBLE);

        return contentView;
    }

}
