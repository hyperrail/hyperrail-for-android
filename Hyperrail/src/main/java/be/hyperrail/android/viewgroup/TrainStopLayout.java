/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.viewgroup;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import be.hyperrail.android.R;
import be.hyperrail.android.irail.implementation.OccupancyHelper;
import be.hyperrail.android.irail.implementation.Train;
import be.hyperrail.android.irail.implementation.TrainStop;


public class TrainStopLayout extends LinearLayout implements RecyclerViewItemViewGroup<Train, TrainStop> {

    protected TextView vDestination;
    protected TextView vDepartureTime;
    protected TextView vDepartureDelay;
    protected TextView vArrivalTime;
    protected TextView vArrivalDelay;
    protected TextView vPlatform;
    protected LinearLayout vPlatformContainer;
    protected ImageView vIcon;

    protected LinearLayout vStatusContainer;
    protected TextView vStatusText;

    protected ImageView vOccupancy;

    public TrainStopLayout(Context context) {
        super(context);
    }

    public TrainStopLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public TrainStopLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    // TODO: use when API > 21
    /*public LiveboardstopViewGroup(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }*/

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        vDestination = findViewById(R.id.text_station);

        vDepartureTime = findViewById(R.id.text_departure_time);
        vDepartureDelay = findViewById(R.id.text_departure_delay);

        vArrivalTime = findViewById(R.id.text_arrival_time);
        vArrivalDelay = findViewById(R.id.text_arrival_delay);

        vPlatform = findViewById(R.id.text_platform);
        vPlatformContainer = findViewById(R.id.layout_platform_container);

        vStatusContainer = findViewById(R.id.layout_train_status_container);
        vStatusText = findViewById(R.id.text_train_status);

        vIcon = findViewById(R.id.image_timeline);
        vOccupancy = findViewById(R.id.image_occupancy);
    }


    @Override
    public void bind(final Context context, final TrainStop stop, final Train train, final int position) {

        vDestination.setText(stop.getStation().getLocalizedName());

        DateTimeFormatter df = DateTimeFormat.forPattern("HH:mm");

        if (stop.getDepartureTime() != null) {
            vDepartureTime.setText(df.print(stop.getDepartureTime()));
            if (stop.getDepartureDelay().getStandardSeconds() > 0) {
                vDepartureDelay.setText(context.getString(R.string.delay, stop.getDepartureDelay().getStandardMinutes()));
            } else {
                vDepartureDelay.setText("");
            }
        } else {
            vDepartureTime.setText("--:--");
            vDepartureDelay.setText("");
        }

        if (stop.getArrivalTime() != null) {
            vArrivalTime.setText(df.print(stop.getArrivalTime()));
            if (stop.getArrivalDelay().getStandardSeconds() > 0) {
                vArrivalDelay.setText(context.getString(R.string.delay, stop.getArrivalDelay().getStandardMinutes()));
            } else {
                vArrivalDelay.setText("");
            }
        } else {
            vArrivalTime.setText("--:--");
            vArrivalDelay.setText("");
        }

        vPlatform.setText(String.valueOf(stop.getPlatform()));

        if (stop.isDepartureCanceled()) {
            vPlatform.setText("");
            vPlatformContainer.setBackground(ContextCompat.getDrawable(context, R.drawable.platform_train_canceled));
            vStatusText.setText(R.string.status_cancelled);
            vStatusContainer.setVisibility(View.VISIBLE);
            vOccupancy.setVisibility(View.GONE);
            setBackgroundColor(ContextCompat.getColor(context, R.color.colorCanceledBackground));
        } else {
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.background_light));
            vStatusContainer.setVisibility(View.GONE);
            vOccupancy.setVisibility(View.VISIBLE);
            vPlatformContainer.setBackground(ContextCompat.getDrawable(context, R.drawable.platform_train));

            if (!stop.isPlatformNormal()) {
                Drawable drawable = vPlatformContainer.getBackground();
                drawable.mutate();
                drawable.setColorFilter(ContextCompat.getColor(context, R.color.colorDelay), PorterDuff.Mode.SRC_ATOP);
            }
        }

        if (stop.hasLeft()) {
            if (position == 0) {
                vIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_departure_filled));
            } else if (position == train.getStops().length - 1) {
                vIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_arrival_filled));
            } else {
                vIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_transfer_filled));
            }
        } else {
            if (position == 0) {
                vIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_departure_hollow));
            } else if (position == train.getStops().length - 1) {
                vIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_arrival_hollow));
            } else {
                vIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_transfer_hollow));
            }
        }

        vOccupancy.setImageDrawable(ContextCompat.getDrawable(context, OccupancyHelper.getOccupancyDrawable(stop.getOccupancyLevel())));
    }
}
