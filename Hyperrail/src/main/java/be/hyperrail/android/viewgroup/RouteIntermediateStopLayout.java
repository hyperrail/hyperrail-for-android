/*
 *  This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 *  If a copy of the MPL was not distributed with this file,
 *  You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.viewgroup;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import be.hyperrail.android.R;
import be.hyperrail.opentransportdata.common.models.VehicleStop;

public class RouteIntermediateStopLayout extends ConstraintLayout implements RecyclerViewItemViewGroup<VehicleStop[], VehicleStop> {

    protected ImageView vTimeline;
    protected TextView vDepartureTime;
    protected TextView vDepartureDelay;
    protected TextView vStation;

    DateTimeFormatter mHhmmFormatter = DateTimeFormat.forPattern("HH:mm");

    public RouteIntermediateStopLayout(Context context) {
        super(context);
    }

    public RouteIntermediateStopLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RouteIntermediateStopLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        vDepartureTime = findViewById(R.id.text_departure_time);
        vDepartureDelay = findViewById(R.id.text_departure_delay);
        vStation = findViewById(R.id.text_station);
        vTimeline = findViewById(R.id.image_timeline);
    }

    @Override
    public void bind(Context context, VehicleStop intermediateStop, VehicleStop[] allintermediateStops, int position) {
        vStation.setText(intermediateStop.getStopLocation().getLocalizedName());
        bindTimeAndDelay(context, intermediateStop);
        bindTimelineDrawable(context, intermediateStop);
    }

    private void bindTimeAndDelay(Context context, VehicleStop intermediateStop) {
        // if we have a departure delay, set the departure delay
        if (intermediateStop.getDepartureDelay().getStandardSeconds() > 0) {
            vDepartureDelay.setText(context.getString(R.string.delay,
                    intermediateStop.getDepartureDelay().getStandardMinutes()));
        } else {
            vDepartureDelay.setText("");
        }

        // if we have an arrival time, set the arrival time
        if (intermediateStop.getDepartureTime() != null) {
            vDepartureTime.setText(mHhmmFormatter.print(intermediateStop.getArrivalTime()));
        } else {
            vDepartureTime.setText("xx:xx");
        }
    }

    private void bindTimelineDrawable(Context context, VehicleStop intermediateStop) {
        int drawable;
        if (intermediateStop.hasLeft()){
            drawable= R.drawable.timeline_intermediatestop_filled;
        } else if (intermediateStop.hasArrived()) {
            drawable= R.drawable.timeline_intermediatestop_inprogress;
        } else {
            drawable = R.drawable.timeline_intermediatestop_hollow;
        }
        vTimeline.setImageDrawable(ContextCompat.getDrawable(context, drawable));
    }
}