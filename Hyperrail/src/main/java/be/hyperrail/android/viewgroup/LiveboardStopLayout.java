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
import be.hyperrail.android.irail.implementation.Liveboard;
import be.hyperrail.android.irail.implementation.OccupancyHelper;
import be.hyperrail.android.irail.implementation.VehicleStop;
import be.hyperrail.android.irail.implementation.VehicleStopType;

public class LiveboardStopLayout extends LinearLayout implements RecyclerViewItemViewGroup<Liveboard, VehicleStop> {

    protected TextView vDestination;
    protected TextView vTrainType;
    protected TextView vTrainNumber;
    protected TextView vTime;
    protected TextView vDelay;
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
        vTime = findViewById(R.id.text_time1);
        vDelay = findViewById(R.id.text_delay1);
        vDelayTime = findViewById(be.hyperrail.android.R.id.text_delay_time);
        vPlatform = findViewById(be.hyperrail.android.R.id.text_platform);
        vPlatformContainer = findViewById(be.hyperrail.android.R.id.layout_platform_container);

        vStatusContainer = findViewById(be.hyperrail.android.R.id.layout_train_status_container);
        vStatusText = findViewById(be.hyperrail.android.R.id.text_train_status);
        vOccupancy = findViewById(R.id.image_occupancy);
    }

    @Override
    public void bind(Context context, VehicleStop stop, Liveboard liveboard, int position) {
        vDestination.setText(stop.getHeadsign());

        vTrainNumber.setText(stop.getVehicle().getNumber());
        vTrainType.setText(stop.getVehicle().getType());

        bindTimeAndDelay(context, stop);

        vPlatform.setText(stop.getPlatform());

        bindDetails(context, stop);

        vOccupancy.setImageDrawable(ContextCompat.getDrawable(context, OccupancyHelper.getOccupancyDrawable(stop.getOccupancyLevel())));
    }

    private void bindTimeAndDelay(Context context, VehicleStop stop) {
        DateTimeFormatter df = DateTimeFormat.forPattern("HH:mm");

        if (stop.getType() == VehicleStopType.DEPARTURE) {
            vTime.setText(df.print(stop.getDepartureTime()));
            if (stop.getDepartureDelay().getStandardSeconds() > 0) {
                vDelay.setText(context.getString(R.string.delay, stop.getDepartureDelay().getStandardMinutes()));
                vDelayTime.setText(df.print(stop.getDelayedDepartureTime()));
            } else {
                vDelay.setText("");
                vDelayTime.setText("");
            }
        } else {
            // support for arrivals
            vTime.setText(df.print(stop.getArrivalTime()));
            if (stop.getArrivalDelay().getStandardSeconds() > 0) {
                vDelay.setText(context.getString(R.string.delay, stop.getArrivalDelay().getStandardMinutes()));
                vDelayTime.setText(df.print(stop.getDelayedArrivalTime()));
            } else {
                vDelay.setText("");
                vDelayTime.setText("");
            }
        }
    }

    private void bindDetails(Context context, VehicleStop stop) {
        if (stop.isDepartureCanceled()) {
            vPlatform.setText("");
            vPlatformContainer.setBackground(ContextCompat.getDrawable(context, R.drawable.platform_train_canceled));
            vStatusText.setText(R.string.status_cancelled);
            vStatusContainer.setVisibility(View.VISIBLE);
            vOccupancy.setVisibility(View.GONE);
            setBackgroundColor(ContextCompat.getColor(context, R.color.colorCanceledBackground));
        } else {
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.background_light));
            vOccupancy.setVisibility(View.VISIBLE);
            vStatusContainer.setVisibility(View.GONE);
            vPlatformContainer.setBackground(ContextCompat.getDrawable(context, R.drawable.platform_train));
            if (!stop.isPlatformNormal()) {
                Drawable drawable = vPlatformContainer.getBackground();
                drawable.mutate();
                drawable.setColorFilter(ContextCompat.getColor(context, R.color.colorDelay), PorterDuff.Mode.SRC_ATOP);
            }
        }
    }

}
