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
import be.hyperrail.android.irail.implementation.Route;
import be.hyperrail.android.irail.implementation.Transfer;
import be.hyperrail.android.util.DurationFormatter;

public class RouteTransferItemLayout extends LinearLayout implements ListDataViewGroup<Route, Transfer> {
    protected TextView vDepartureTime;
    protected LinearLayout vDepartureContainer;
    protected TextView vDepartureDelay;

    protected TextView vArrivalTime;
    protected LinearLayout vArrivalContainer;
    protected TextView vArrivalDelay;

    protected TextView vArrivalPlatform;
    protected LinearLayout vArrivalPlatformContainer;
    protected TextView vDeparturePlatform;
    protected LinearLayout vDeparturePlatformContainer;

    protected TextView vStation;
    protected TextView vWaitingTime;
    protected LinearLayout vWaitingTimeContainer;

    protected ImageView vTimeline;

    DateTimeFormatter mHhmmFormatter = DateTimeFormat.forPattern("HH:mm");

    public RouteTransferItemLayout(Context context) {
        super(context);
    }

    public RouteTransferItemLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RouteTransferItemLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    // TODO: use when API > 21
    /*public RouteTransferItemLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }*/

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        vDepartureTime = findViewById(R.id.text_departure_time);
        vDepartureContainer = findViewById(R.id.text_departure_container);
        vDepartureDelay = findViewById(R.id.text_departure_delay);

        vArrivalTime = findViewById(R.id.text_arrival_time);
        vArrivalContainer = findViewById(R.id.text_arrival_container);
        vArrivalDelay = findViewById(R.id.text_arrival_delay);

        vArrivalPlatform = findViewById(R.id.text_platform_arrival);
        vArrivalPlatformContainer = findViewById(R.id.layout_platform_arrival_container);
        vDeparturePlatform = findViewById(R.id.text_platform_departure);
        vDeparturePlatformContainer = findViewById(R.id.layout_platform_departure_container);

        vStation = findViewById(R.id.text_station);
        vWaitingTime = findViewById(R.id.text_waiting_time);
        vWaitingTimeContainer = findViewById(R.id.layout_transfer_duration);

        vTimeline = findViewById(R.id.image_timeline);
    }

    @Override
    public void bind(final Context context, final Transfer transfer, final Route route, final int position) {

        vStation.setText(transfer.getStation().getLocalizedName());

        // If we have have both and arrival and a departure, set the duration
        if (transfer.getArrivalTime() != null && transfer.getDepartureTime() != null) {
            //TODO: cleanup
            vWaitingTime.setText(
                    DurationFormatter.formatDuration(
                            transfer.getArrivalTime(), transfer.getArrivalDelay(),
                            transfer.getDepartureTime(), transfer.getDepartureDelay()
                    ));

        } else {
            vWaitingTime.setText("");
            vWaitingTimeContainer.setVisibility(View.GONE);
        }

        // if we have a departure, set the departure
        if (transfer.getDepartureTime() != null) {
            vDepartureTime.setText(mHhmmFormatter.print(transfer.getDepartureTime()));
        } else {
            vDepartureTime.setText("");
            vDepartureContainer.setVisibility(View.GONE);
        }

        // if we have a departure delay, set the departure delay
        if (transfer.getDepartureDelay().getStandardSeconds() > 0) {
            vDepartureDelay.setText(context.getString(R.string.delay, transfer.getDepartureDelay().getStandardMinutes()));
        } else {
            vDepartureDelay.setText("");
        }

        // if we have an arrival time, set the arrival time
        if (transfer.getArrivalTime() != null) {
            vArrivalTime.setText(mHhmmFormatter.print(transfer.getArrivalTime()));
        } else {
            vArrivalTime.setText("");
            vArrivalContainer.setVisibility(View.GONE);
        }

        // if we have an arrival delay, set the arrival delay
        if (transfer.getArrivalDelay().getStandardSeconds() > 0) {
            vArrivalDelay.setText(context.getString(R.string.delay, transfer.getArrivalDelay().getStandardMinutes()));
        } else {
            vArrivalDelay.setText("");
        }

        // If we have a departure platform
        if (transfer.getDeparturePlatform() != null) {
            vDeparturePlatform.setText(transfer.getDeparturePlatform());

            // if cancelled, show icon
            if (transfer.isDepartureCanceled()) {
                vDeparturePlatform.setText("");
                vDeparturePlatformContainer.setBackground(ContextCompat.getDrawable(context, R.drawable.platform_train_canceled));
            } else {
                vDeparturePlatformContainer.setBackground(ContextCompat.getDrawable(context, R.drawable.platform_train));

                if (!transfer.isArrivalPlatformNormal()) {
                    // if platform changed, mark in red
                    Drawable drawable = vDeparturePlatformContainer.getBackground();
                    drawable.mutate();
                    drawable.setColorFilter(ContextCompat.getColor(context, R.color.colorDelay), PorterDuff.Mode.SRC_ATOP);
                }
            }

        } else {
            // no departure platform
            vDeparturePlatform.setText("");
            vDeparturePlatformContainer.setVisibility(View.GONE);
        }

        if (transfer.getArrivalPlatform() != null) {
            vArrivalPlatform.setText(transfer.getArrivalPlatform());

            // if cancelled, show icon
            if (transfer.isArrivalCanceled()) {
                vArrivalPlatform.setText("");
                vArrivalPlatformContainer.setBackground(ContextCompat.getDrawable(context, R.drawable.platform_train_canceled));
            } else {
                vArrivalPlatformContainer.setBackground(ContextCompat.getDrawable(context, R.drawable.platform_train));
                if (!transfer.isArrivalPlatformNormal()) {
                    // if platform changed, mark in red
                    Drawable drawable = vArrivalPlatformContainer.getBackground();
                    drawable.mutate();
                    drawable.setColorFilter(ContextCompat.getColor(context, R.color.colorDelay), PorterDuff.Mode.SRC_ATOP);
                }
            }

        } else {
            // no arrival platform
            vArrivalPlatform.setText("");
            vArrivalPlatformContainer.setVisibility(View.GONE);
        }

        if (position == 0) {
            if (transfer.hasLeft()) {
                vTimeline.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_departure_filled));
            } else {
                vTimeline.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_departure_hollow));
            }
        } else if (position == route.getTransfers().length - 1) {
            if (transfer.hasArrived()) {
                vTimeline.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_arrival_filled));
            } else {
                vTimeline.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_arrival_hollow));
            }
        } else {
            if (transfer.hasArrived()) {
                if (transfer.hasLeft()) {
                    vTimeline.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_transfer_filled));
                } else {
                    vTimeline.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_transfer_inprogress));
                }
            } else {
                vTimeline.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_transfer_hollow));
            }
        }


    }
}