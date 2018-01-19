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
import be.hyperrail.android.irail.implementation.LiveBoard;
import be.hyperrail.android.irail.implementation.OccupancyHelper;
import be.hyperrail.android.irail.implementation.TrainStop;

public class LiveboardStopLayout extends LinearLayout implements ListDataViewGroup<LiveBoard, TrainStop> {

    protected TextView vDestination;
    protected TextView vTrainType;
    protected TextView vTrainNumber;
    protected TextView vDeparture;
    protected TextView vDepartureDelay;
    protected TextView vDelayTime;
    protected TextView vPlatform;
    protected LinearLayout vPlatformContainer;

    protected LinearLayout vStatusContainer;
    protected TextView vStatusText;

    protected ImageView vOccupancy;

    public LiveboardStopLayout(Context context) {
        super(context);
    }

    public LiveboardStopLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public LiveboardStopLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    // TODO: use when API > 21
    /*public LiveboardstopViewGroup(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }*/

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        vDestination = findViewById(be.hyperrail.android.R.id.text_destination);
        vTrainNumber = findViewById(be.hyperrail.android.R.id.text_train_number);
        vTrainType = findViewById(be.hyperrail.android.R.id.text_train_type);
        vDeparture = findViewById(be.hyperrail.android.R.id.text_departure_time);
        vDepartureDelay = findViewById(be.hyperrail.android.R.id.text_departure_delay);
        vDelayTime = findViewById(be.hyperrail.android.R.id.text_delay_time);
        vPlatform = findViewById(be.hyperrail.android.R.id.text_platform);
        vPlatformContainer = findViewById(be.hyperrail.android.R.id.layout_platform_container);

        vStatusContainer = findViewById(be.hyperrail.android.R.id.layout_train_status_container);
        vStatusText = findViewById(be.hyperrail.android.R.id.text_train_status);
        vOccupancy = findViewById(R.id.image_occupancy);
    }

    @Override
    public void bind(final Context context, final TrainStop stop, final LiveBoard liveboard, final int position) {

        vDestination.setText(stop.getDestination().getLocalizedName());

        vTrainNumber.setText(stop.getTrain().getNumber());
        vTrainType.setText(stop.getTrain().getType());

        DateTimeFormatter df = DateTimeFormat.forPattern("HH:mm");

        if (stop.getDepartureTime() != null) {
            vDeparture.setText(df.print(stop.getDepartureTime()));
            if (stop.getDepartureDelay().getStandardSeconds() > 0) {
                vDepartureDelay.setText(context.getString(be.hyperrail.android.R.string.delay, stop.getDepartureDelay().getStandardMinutes()));
                vDelayTime.setText(df.print(stop.getDelayedDepartureTime()));
            } else {
                vDepartureDelay.setText("");
                vDelayTime.setText("");
            }
        } else {
            // support for arrivals
            vDeparture.setText(df.print(stop.getArrivalTime()));
            if (stop.getArrivalDelay().getStandardSeconds() > 0) {
                vDepartureDelay.setText(context.getString(be.hyperrail.android.R.string.delay, stop.getArrivalDelay().getStandardMinutes()));
                vDelayTime.setText(df.print(stop.getDelayedArrivalTime()));
            } else {
                vDepartureDelay.setText("");
                vDelayTime.setText("");
            }
        }

        vPlatform.setText(String.valueOf(stop.getPlatform()));

        if (stop.isDepartureCanceled()) {
            vPlatform.setText("");
            vPlatformContainer.setBackground(ContextCompat.getDrawable(context, be.hyperrail.android.R.drawable.platform_train_canceled));
            vStatusText.setText(be.hyperrail.android.R.string.status_cancelled);
            vStatusContainer.setVisibility(View.VISIBLE);
            vOccupancy.setVisibility(View.GONE);
            setBackgroundColor(ContextCompat.getColor(context, R.color.colorCanceledBackground));
        } else {
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.background_light));
            vOccupancy.setVisibility(View.VISIBLE);
            vStatusContainer.setVisibility(View.GONE);
            vPlatformContainer.setBackground(ContextCompat.getDrawable(context, be.hyperrail.android.R.drawable.platform_train));
            if (!stop.isPlatformNormal()) {
                Drawable drawable = vPlatformContainer.getBackground();
                drawable.mutate();
                drawable.setColorFilter(ContextCompat.getColor(context, be.hyperrail.android.R.color.colorDelay), PorterDuff.Mode.SRC_ATOP);
            }
        }

        vOccupancy.setImageDrawable(ContextCompat.getDrawable(context, OccupancyHelper.getOccupancyDrawable(stop.getOccupancyLevel())));
    }


}
