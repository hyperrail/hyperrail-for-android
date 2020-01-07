/*
 *  This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 *  If a copy of the MPL was not distributed with this file,
 *  You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.viewgroup;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import be.hyperrail.android.R;
import be.hyperrail.android.activities.searchresult.LiveboardActivity;
import be.hyperrail.android.adapter.RouteIntermediaryStopCardAdapter;
import be.hyperrail.opentransportdata.common.contracts.QueryTimeDefinition;
import be.hyperrail.opentransportdata.common.models.LiveboardType;
import be.hyperrail.opentransportdata.common.models.RouteLeg;
import be.hyperrail.opentransportdata.common.models.Transfer;
import be.hyperrail.opentransportdata.common.requests.LiveboardRequest;

public class RouteIntermediateStopsLayout extends ConstraintLayout {

    protected TextView vDescription;
    protected ImageView vTimeline;
    protected ImageView vExpandCollapse;
    protected RecyclerView vRecyclerView;
    protected RecyclerView vLayoutRoot;

    public RouteIntermediateStopsLayout(Context context) {
        super(context);
    }

    public RouteIntermediateStopsLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RouteIntermediateStopsLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        vDescription = findViewById(R.id.text_description);
        vRecyclerView = findViewById(R.id.recyclerview);
        vTimeline = findViewById(R.id.image_timeline);
        vExpandCollapse = findViewById(R.id.img_toggle_collapse);
        vLayoutRoot = findViewById(R.id.binder);

        // start collapsed
        hideIntermediateStopList();
    }

    public void bind(Context context, RouteLeg routeLeg) {
        vDescription.setText(context.getString(R.string.route_intermediate_stops, routeLeg.getIntermediaryStops().length));
        vLayoutRoot.setOnClickListener(v -> toggleIntermediateStopsList());
        bindTimelineDrawable(context, routeLeg);

        // The initial call from an activity to the adapter responsible for this layout should pass the context in an activity!
        RouteIntermediaryStopCardAdapter adapter = new RouteIntermediaryStopCardAdapter((Activity) context, routeLeg);

        // Launch intents to view details / click through
        adapter.setOnItemClickListener((sender, intermediateStop) -> {
            Intent i = LiveboardActivity.createIntent(context, new LiveboardRequest(
                    ((Transfer) intermediateStop).getStopLocation(), QueryTimeDefinition.EQUAL_OR_LATER, LiveboardType.DEPARTURES, null));
            context.startActivity(i);
        });

        vRecyclerView.setAdapter(adapter);
        vRecyclerView.setItemAnimator(new DefaultItemAnimator());
        vRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        vRecyclerView.setNestedScrollingEnabled(false);
    }

    private void toggleIntermediateStopsList() {
        if (vRecyclerView.getVisibility() == GONE) {
            showIntermediateStopList();
        } else {
            hideIntermediateStopList();
        }
    }

    private void hideIntermediateStopList() {
        vRecyclerView.setVisibility(GONE);
        vExpandCollapse.setImageResource(R.drawable.ic_unfold_more);
    }

    private void showIntermediateStopList() {
        vRecyclerView.setVisibility(VISIBLE);
        vExpandCollapse.setImageResource(R.drawable.ic_unfold_less);
    }

    private void bindTimelineDrawable(Context context, RouteLeg routeLeg) {
        if (routeLeg.getDeparture().isCompletedByVehicle()) {
            int drawable = R.drawable.timeline_continuous_filled;
            vTimeline.setImageDrawable(ContextCompat.getDrawable(context, drawable));
        } else {
            int drawable = R.drawable.timeline_continuous_hollow;
            vTimeline.setImageDrawable(ContextCompat.getDrawable(context, drawable));
        }
    }

}