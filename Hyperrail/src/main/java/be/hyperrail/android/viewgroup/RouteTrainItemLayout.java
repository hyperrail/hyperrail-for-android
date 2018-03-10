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
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import be.hyperrail.android.R;
import be.hyperrail.android.irail.implementation.Message;
import be.hyperrail.android.irail.implementation.OccupancyHelper;
import be.hyperrail.android.irail.implementation.Route;
import be.hyperrail.android.irail.implementation.RouteLeg;
import be.hyperrail.android.irail.implementation.RouteLegType;
import be.hyperrail.android.irail.implementation.Transfer;
import be.hyperrail.android.util.DurationFormatter;

public class RouteTrainItemLayout extends LinearLayout implements RecyclerViewItemViewGroup<Route,RouteLeg> {

    protected TextView vDirection;
    protected TextView vDuration;
    protected TextView vTrainType;
    protected TextView vTrainNumber;

    protected LinearLayout vStatusContainer;
    protected TextView vStatusText;

    protected ImageView vOccupancy;
    protected ImageView vTimeline;
    protected ImageView vTimeline2;
    protected LinearLayout vAlertContainer;
    protected TextView vAlertText;

    public RouteTrainItemLayout(Context context) {
        super(context);
    }

    public RouteTrainItemLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RouteTrainItemLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    // TODO: use when API > 21
    /*public RouteListItemLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }*/

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        vDirection = findViewById(R.id.text_direction);
        vDuration = findViewById(R.id.text_duration);

        vTrainNumber = findViewById(R.id.text_train_number);
        vTrainType = findViewById(R.id.text_train_type);

        vStatusContainer = findViewById(R.id.layout_train_status_container);
        vStatusText = findViewById(R.id.text_train_status);

        vOccupancy = findViewById(R.id.image_occupancy);

        vAlertContainer = findViewById(R.id.alert_container);
        vAlertText = findViewById(R.id.alert_message);

        vTimeline = findViewById(R.id.image_timeline);
        vTimeline2 = findViewById(R.id.image_timeline_2);
    }

    @Override
    public void bind(Context context, RouteLeg routeLeg, Route route, int position) {
        Transfer transferBefore = route.getTransfers()[position];
        Transfer transferAfter = route.getTransfers()[position+1];

        if (routeLeg.getType() == RouteLegType.WALK) {
            bindWalk(context, transferBefore,transferAfter);
        } else {
            bindVehicle(context, routeLeg, transferBefore, transferAfter, route, position);
        }
    }

    private void bindWalk(Context context, Transfer transferBefore, Transfer transferAfter) {

        vDirection.setText(R.string.walk_heading);
        vTrainType.setVisibility(View.GONE);
        vTrainNumber.setText(R.string.walk_description);
        vOccupancy.setVisibility(View.GONE);

        if (transferBefore.hasArrived()) {
            vTimeline.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_walk_filled));
        } else {
            vTimeline.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_walk_hollow));
        }

        bindDuration(transferBefore, transferAfter);
    }

    private void bindVehicle(Context context, RouteLeg routeLeg, Transfer transferBefore, Transfer transferAfter, Route route, int position) {
        vTrainNumber.setText(routeLeg.getVehicleInformation().getNumber());
        vTrainType.setText(routeLeg.getVehicleInformation().getType());
        vOccupancy.setVisibility(View.VISIBLE);
        vTrainType.setVisibility(View.VISIBLE);
        vDirection.setText(routeLeg.getVehicleInformation().getDirection().getLocalizedName());

        if (transferBefore.hasLeft()) {
            if (transferAfter.hasArrived()) {
                vTimeline.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_train_filled));
                vTimeline2.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_continuous_filled));
            } else {
                vTimeline.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_train_inprogress));
                vTimeline2.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_continuous_hollow));
            }
        } else {
            vTimeline.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_train_hollow));
            vTimeline2.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_continuous_hollow));
        }

        bindDuration(transferBefore, transferAfter);

        if (transferBefore.isDepartureCanceled()) {
            vStatusText.setText(R.string.status_cancelled);
            vStatusContainer.setVisibility(View.VISIBLE);
            vOccupancy.setVisibility(View.GONE);
        } else {
            vOccupancy.setVisibility(View.VISIBLE);
            vStatusContainer.setVisibility(View.GONE);
        }

        Message[] trainAlerts = route.getVehicleAlerts()[position];
        if (trainAlerts != null && trainAlerts.length > 0) {
            vAlertContainer.setVisibility(View.VISIBLE);

            StringBuilder text = new StringBuilder();
            int n = trainAlerts.length;
            for (int i = 0; i < n; i++) {
                text.append(trainAlerts[i].getHeader());
                if (i < n - 1) {
                    text.append("\n");
                }
            }

            vAlertText.setText(text.toString());
        } else {
            vAlertContainer.setVisibility(View.GONE);
        }

        vOccupancy.setImageDrawable(ContextCompat.getDrawable(context, OccupancyHelper.getOccupancyDrawable(transferBefore.getDepartureOccupancy())));
    }

    private void bindDuration(Transfer transferBefore, Transfer transferAfter) {
        vDuration.setText(
                DurationFormatter.formatDuration(transferBefore.getDepartureTime(), transferBefore.getDepartureDelay(), transferAfter.getArrivalTime(), transferAfter.getArrivalDelay()));
    }
}