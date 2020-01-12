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
import be.hyperrail.android.viewgroup.RouteintermediateStopLayout;
import be.hyperrail.opentransportdata.common.models.RouteLeg;

/**
 * RecyclerViewAdapter which shows intermediate stops for a route leg.
 */
public class RouteintermediateStopCardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    /**
     * The routeleg for which the intermediate stops should be shown.
     */
    private final RouteLeg routeLeg;
    // We need to keep the context as an activity in order to be able to open the contextmenu here
    // TODO: check if this "deep nesting" can be prevented
    private final Activity context;
    private OnRecyclerItemClickListener<Object> listener;

    public RouteintermediateStopCardAdapter(Activity context, RouteLeg routeLeg) {
        this.context = context;
        this.routeLeg = routeLeg;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_route_detail_intermediate_stop, parent, false);
        return new RouteintermediateStopViewHolder(itemView);
    }

    /**
     * Bind a ViewHolder instance
     * Note: since recyclerview recyclers, either set or unset fields, but don't leave them as-is!
     */
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        RouteintermediateStopViewHolder routeTransferViewHolder = (RouteintermediateStopViewHolder) holder;
        routeTransferViewHolder.routeintermediateStopLayout.bind(context, routeLeg.getintermediateStops()[position], routeLeg.getintermediateStops(), position);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecyclerItemClick(RouteintermediateStopCardAdapter.this, routeLeg.getintermediateStops()[position]);
            }
        });
    }

    @Override
    public int getItemCount() {
        if (routeLeg == null) {
            return 0;
        }

        return routeLeg.getintermediateStops().length;
    }

    public void setOnItemClickListener(OnRecyclerItemClickListener<Object> listener) {
        this.listener = listener;
    }


    /**
     * Transfer ViewHolder, showing station, waiting time, arrival, departure, delay, platforms, timeline
     */
    private class RouteintermediateStopViewHolder extends RecyclerView.ViewHolder {

        RouteintermediateStopLayout routeintermediateStopLayout;

        RouteintermediateStopViewHolder(View view) {
            super(view);
            routeintermediateStopLayout = (RouteintermediateStopLayout) view.getRootView();
        }
    }
}

