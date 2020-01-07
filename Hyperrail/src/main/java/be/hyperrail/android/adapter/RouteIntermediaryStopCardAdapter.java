/*
 *  This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 *  If a copy of the MPL was not distributed with this file,
 *  You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.adapter;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import be.hyperrail.android.R;
import be.hyperrail.android.viewgroup.RouteIntermediateStopLayout;
import be.hyperrail.opentransportdata.common.models.RouteLeg;

/**
 * RecyclerViewAdapter which shows intermediary stops for a route leg.
 */
public class RouteIntermediaryStopCardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    /**
     * The routeleg for which the intermediary stops should be shown.
     */
    private final RouteLeg routeLeg;
    // We need to keep the context as an activity in order to be able to open the contextmenu here
    // TODO: check if this "deep nesting" can be prevented
    private final Activity context;
    private OnRecyclerItemClickListener<Object> listener;

    public RouteIntermediaryStopCardAdapter(Activity context, RouteLeg routeLeg) {
        this.context = context;
        this.routeLeg = routeLeg;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_route_detail_intermediate_stop, parent, false);
        return new RouteIntermediaryStopViewHolder(itemView);
    }

    /**
     * Bind a ViewHolder instance
     * Note: since recyclerview recyclers, either set or unset fields, but don't leave them as-is!
     */
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        RouteIntermediaryStopViewHolder routeTransferViewHolder = (RouteIntermediaryStopViewHolder) holder;
        routeTransferViewHolder.routeIntermediaryStopLayout.bind(context, routeLeg.getIntermediaryStops()[position], routeLeg.getIntermediaryStops(), position);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecyclerItemClick(RouteIntermediaryStopCardAdapter.this, routeLeg.getIntermediaryStops()[position]);
            }
        });
    }

    @Override
    public int getItemCount() {
        if (routeLeg == null) {
            return 0;
        }

        return routeLeg.getIntermediaryStops().length;
    }

    public void setOnItemClickListener(OnRecyclerItemClickListener<Object> listener) {
        this.listener = listener;
    }


    /**
     * Transfer ViewHolder, showing station, waiting time, arrival, departure, delay, platforms, timeline
     */
    private class RouteIntermediaryStopViewHolder extends RecyclerView.ViewHolder {

        RouteIntermediateStopLayout routeIntermediaryStopLayout;

        RouteIntermediaryStopViewHolder(View view) {
            super(view);
            routeIntermediaryStopLayout = view.findViewById(R.id.binder);
        }
    }
}

