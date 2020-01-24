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
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import be.hyperrail.android.R;
import be.hyperrail.android.util.DurationFormatter;
import be.hyperrail.opentransportdata.common.models.Message;
import be.hyperrail.opentransportdata.common.models.Route;
import be.hyperrail.opentransportdata.common.models.RouteLeg;
import be.hyperrail.opentransportdata.common.models.RouteLegType;
import be.hyperrail.opentransportdata.common.models.Transfer;
import be.hyperrail.opentransportdata.util.OccupancyHelper;

public class RouteTrainItemLayout extends LinearLayout implements RecyclerViewItemViewGroup<Route, RouteLeg> {

    protected TextView vDirection;
    protected TextView vDuration;
    protected TextView vTrainType;
    protected TextView vTrainNumber;

    protected LinearLayout vStatusContainer;
    protected TextView vStatusText;

    protected ImageView vOccupancy;
    protected ImageView vTimeline;
    protected ImageView vTimelineAlerts;
    protected LinearLayout vAlertContainer;
    protected TextView vAlertText;

    protected RouteIntermediateStopsLayout vintermediateStops;

    public RouteTrainItemLayout(Context context) {
        super(context);
    }

    public RouteTrainItemLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RouteTrainItemLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        vDirection = findViewById(R.id.text_direction);
        vDuration = findViewById(R.id.text_duration);

        vTrainNumber = findViewById(R.id.text_train_number);
        vTrainType = findViewById(R.id.text_train_type);

        vStatusContainer = findViewById(R.id.incl_status);
        vStatusText = findViewById(R.id.text_train_status);

        vOccupancy = findViewById(R.id.incl_occupancy);

        vAlertContainer = findViewById(R.id.alert_container);
        vAlertText = findViewById(R.id.alert_message);

        vTimeline = findViewById(R.id.image_timeline);
        vintermediateStops = findViewById(R.id.incl_intermediary_stops);
        vTimelineAlerts = findViewById(R.id.image_timeline_alerts);
    }

    @Override
    public void bind(Context context, RouteLeg routeLeg, Route route, int position) {
        Transfer transferBefore = route.getTransfers()[position];
        Transfer transferAfter = route.getTransfers()[position + 1];

        if (routeLeg.getType() == RouteLegType.WALK) {
            bindWalk(context, transferBefore, transferAfter);
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
        vDirection.setText(routeLeg.getVehicleInformation().getHeadsign());

        bindTimelineDrawable(context, transferBefore, transferAfter, routeLeg);
        bindDuration(transferBefore, transferAfter);

        if (transferBefore.isDepartureCanceled()) {
            vStatusText.setText(R.string.status_cancelled);
            vStatusContainer.setVisibility(View.VISIBLE);
            vOccupancy.setVisibility(View.GONE);
        } else {
            vOccupancy.setVisibility(View.VISIBLE);
            vStatusContainer.setVisibility(View.GONE);
        }

        bindintermediateStops(context, routeLeg);
        bindAlerts(route, position);

        vOccupancy.setImageDrawable(ContextCompat.getDrawable(context, OccupancyHelper.getOccupancyDrawable(transferBefore.getDepartureOccupancy())));
    }

    private void bindintermediateStops(Context context, RouteLeg routeLeg) {
        if (routeLeg.getintermediateStops().length == 0) {
            vintermediateStops.setVisibility(GONE);
        } else {
            vintermediateStops.setVisibility(VISIBLE);
            vintermediateStops.bind(context, routeLeg);
        }
    }


    private void bindAlerts(Route route, int position) {
        if (route.getVehicleAlerts() != null && route.getVehicleAlerts().length > position) {
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
        } else {
            vAlertContainer.setVisibility(View.GONE);
        }
    }

    private void bindTimelineDrawable(Context context, Transfer transferBefore, Transfer transferAfter, RouteLeg leg) {
        if (transferBefore.hasLeft()) {
            if (transferAfter.hasArrived()) {
                vTimeline.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_train_filled));
                vTimelineAlerts.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_continuous_filled));
            } else {
                vTimelineAlerts.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_continuous_hollow));
                if (leg.getintermediateStops().length > 0 &&leg.getintermediateStops()[0].hasArrived()){
                    // The intermediate stops timeline comes after the train icon timeline
                    vTimeline.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_train_filled));
                } else {
                    vTimeline.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_train_inprogress));
                }
            }
        } else {
            vTimeline.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_train_hollow));
            vTimelineAlerts.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_continuous_hollow));
        }
    }

    private void bindDuration(Transfer transferBefore, Transfer transferAfter) {
        vDuration.setText(
                DurationFormatter.formatDuration(transferBefore.getDepartureTime(), transferBefore.getDepartureDelay(), transferAfter.getArrivalTime(), transferAfter.getArrivalDelay()));
    }
}