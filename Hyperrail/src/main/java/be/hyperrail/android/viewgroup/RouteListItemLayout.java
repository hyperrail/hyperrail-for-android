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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import be.hyperrail.android.R;
import be.hyperrail.android.activities.searchresult.LiveboardActivity;
import be.hyperrail.android.activities.searchresult.VehicleActivity;
import be.hyperrail.android.adapter.RouteDetailCardAdapter;
import be.hyperrail.android.util.DurationFormatter;
import be.hyperrail.opentransportdata.common.contracts.QueryTimeDefinition;
import be.hyperrail.opentransportdata.common.models.LiveboardType;
import be.hyperrail.opentransportdata.common.models.Route;
import be.hyperrail.opentransportdata.common.models.RouteLegType;
import be.hyperrail.opentransportdata.common.models.RoutesList;
import be.hyperrail.opentransportdata.common.models.Transfer;
import be.hyperrail.opentransportdata.common.models.VehicleJourneyStub;
import be.hyperrail.opentransportdata.common.requests.LiveboardRequest;
import be.hyperrail.opentransportdata.common.requests.VehicleRequest;

public class RouteListItemLayout extends LinearLayout implements RecyclerViewItemViewGroup<RoutesList, Route> {

    protected TextView vDepartureTime;
    protected TextView vDepartureDelay;
    protected TextView vArrivalTime;
    protected TextView vArrivalDelay;
    protected TextView vDirection;
    protected TextView vDuration;
    protected ImageView vDurationIcon;
    protected TextView vTrainCount;
    protected TextView vPlatform;
    protected ImageView vPlatformDrawable;
    protected RecyclerView vRecyclerView;
    protected RelativeLayout vHeaderContainer;
    protected LinearLayout vDetailContainer;
    protected TextView vStatusText;
    protected LinearLayout vStatusContainer;
    protected TextView vAlertsText;

    public RouteListItemLayout(Context context) {
        super(context);
    }

    public RouteListItemLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RouteListItemLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        vDepartureTime = findViewById(R.id.text_departure_time);
        vDepartureDelay = findViewById(R.id.text_departure_delay);

        vArrivalTime = findViewById(R.id.text_arrival_time);
        vArrivalDelay = findViewById(R.id.text_arrival_delay);

        vDirection = findViewById(R.id.text_destination);
        vDuration = findViewById(R.id.text_duration);
        vDurationIcon = findViewById(R.id.image_duration);
        vTrainCount = findViewById(R.id.text_train_count);

        vPlatform = findViewById(R.id.text_platform);
        vPlatformDrawable = findViewById(R.id.image_platform);

        vRecyclerView = findViewById(R.id.recyclerview_primary);
        vHeaderContainer = findViewById(R.id.cardview_collapsed);
        vDetailContainer = findViewById(R.id.cardview_expanded);

        vStatusContainer = findViewById(R.id.layout_train_status_container);
        vStatusText = findViewById(R.id.text_train_status);
        vAlertsText = findViewById(R.id.alert_message);
    }

    @Override
    public void bind(final Context context, Route route, RoutesList allRoutes, int position) {

        DateTimeFormatter hhmm = DateTimeFormat.forPattern("HH:mm");

        bindTimeAndDelay(context, route, hhmm);

        if (route.getLegs()[0].getType() == RouteLegType.WALK) {
            vDirection.setText(R.string.walk_heading);
        } else {
            vDirection.setText(route.getLegs()[0].getVehicleInformation().getHeadsign());
        }

        vDuration.setText(DurationFormatter.formatDuration(route.getDurationIncludingDelays().toPeriod()));
        vTrainCount.setText(String.valueOf(route.getLegs().length));

        bindPlatformAndStatus(context, route);

        bindAlerts(route);

        // The initial call from an activity to the adapter responsible for this layout should pass the context in an activity!
        RouteDetailCardAdapter adapter = new RouteDetailCardAdapter((Activity) context, route, true);

        // Launch intents to view details / click through
        adapter.setOnItemClickListener((sender, object) -> {
            Intent i = null;
            if (object instanceof Bundle) {
                i = VehicleActivity.createIntent(context,
                        new VehicleRequest(
                                ((VehicleJourneyStub) ((Bundle) object).getSerializable("train")).getId(),
                                (DateTime) ((Bundle) object).getSerializable("date")
                        ));

            } else if (object instanceof Transfer) {
                i = LiveboardActivity.createIntent(context, new LiveboardRequest(
                        ((Transfer) object).getStopLocation(), QueryTimeDefinition.EQUAL_OR_LATER, LiveboardType.DEPARTURES, null));
            }
            context.startActivity(i);
        });

        vRecyclerView.setAdapter(adapter);
        vRecyclerView.setItemAnimator(new DefaultItemAnimator());
        vRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        vRecyclerView.setNestedScrollingEnabled(false);

        vHeaderContainer.setOnClickListener(v -> {
            if (vDetailContainer.getVisibility() == View.GONE) {
                vDetailContainer.setVisibility(View.VISIBLE);
            } else {
                vDetailContainer.setVisibility(View.GONE);
            }
        });

        vHeaderContainer.setOnLongClickListener(v -> {
            // still handle this in the parent
            return false;
        });
    }

    private void bindAlerts(Route route) {
        if (route.getAlerts() != null && route.getAlerts().length > 0) {
            vAlertsText.setVisibility(View.VISIBLE);

            StringBuilder text = new StringBuilder();
            int n = route.getAlerts().length;
            for (int i = 0; i < n; i++) {
                text.append(route.getAlerts()[i].getHeader());
                if (i < n - 1) {
                    text.append("\n");
                }
            }

            vAlertsText.setText(text.toString());
        } else {
            vAlertsText.setVisibility(View.GONE);
        }
    }

    private void bindPlatformAndStatus(Context context, Route route) {
        vPlatform.setText(route.getDeparturePlatform());

        if (route.getDeparture().isDepartureCanceled()) {
            vPlatform.setText("");
            vPlatformDrawable.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.platform_train_canceled));
            vStatusText.setText(R.string.status_cancelled);
            vStatusContainer.setVisibility(View.VISIBLE);
        } else if (route.isPartiallyCanceled()) {
            vPlatformDrawable.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.platform_train));
            vStatusText.setText(R.string.status_partially_cancelled);
            vStatusContainer.setVisibility(View.VISIBLE);
        } else {
            vStatusContainer.setVisibility(View.GONE);
            vPlatformDrawable.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.platform_train));
        }


        if (!route.isDeparturePlatformNormal()) {
            Drawable drawable = vPlatformDrawable.getDrawable();
            drawable.mutate();
            drawable.setColorFilter(ContextCompat.getColor(context, R.color.colorDelay), PorterDuff.Mode.SRC_ATOP);
            vPlatformDrawable.setImageDrawable(drawable);
        }
    }

    private void bindTimeAndDelay(Context context, Route route, DateTimeFormatter hhmm) {
        vDepartureTime.setText(hhmm.print(route.getDepartureTime()));
        if (route.getDepartureDelay().getStandardSeconds() > 0) {
            vDepartureDelay.setText(context.getString(R.string.delay, route.getDepartureDelay().getStandardMinutes()));
        } else {
            vDepartureDelay.setText("");
        }

        vArrivalTime.setText(hhmm.print(route.getArrivalTime()));
        if (route.getArrivalDelay().getStandardSeconds() > 0) {
            vArrivalDelay.setText(context.getString(R.string.delay, route.getArrivalDelay().getStandardMinutes()));
        } else {
            vArrivalDelay.setText("");
        }

        Duration routeWithDelays = route.getDurationIncludingDelays();
        Duration routeWithoutDelays = route.getDuration();

        if (routeWithDelays.equals(routeWithoutDelays)) {
            vDuration.setTextColor(ContextCompat.getColor(context, R.color.colorMuted));
            vDurationIcon.setColorFilter(ContextCompat.getColor(context, R.color.colorMuted));
        } else if (routeWithDelays.isLongerThan(routeWithoutDelays)) {
            vDuration.setTextColor(ContextCompat.getColor(context, R.color.colorDelay));
            vDurationIcon.setColorFilter(ContextCompat.getColor(context, R.color.colorDelay));
        } else {
            vDuration.setTextColor(ContextCompat.getColor(context, R.color.colorFaster));
            vDurationIcon.setColorFilter(ContextCompat.getColor(context, R.color.colorFaster));
        }
    }
}