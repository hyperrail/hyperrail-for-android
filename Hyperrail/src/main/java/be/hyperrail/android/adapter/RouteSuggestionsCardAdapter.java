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

package be.hyperrail.android.adapter;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import be.hyperrail.android.persistence.Suggestion;
import be.hyperrail.opentransportdata.common.requests.RoutePlanningRequest;

/**
 * An adapter to show RouteQueries (recent/favorite routes) in a recyclerview
 */
public class RouteSuggestionsCardAdapter extends RecyclerView.Adapter<RouteSuggestionsCardAdapter.RouteHistoryViewHolder> {

    private final Context context;
    private List<Suggestion<RoutePlanningRequest>> queries;

    private OnRecyclerItemClickListener<Suggestion<RoutePlanningRequest>> listener;
    private OnRecyclerItemLongClickListener<Suggestion<RoutePlanningRequest>> longClickListener;

    public RouteSuggestionsCardAdapter(Context context, List<Suggestion<RoutePlanningRequest>> queries) {
        this.queries = queries;
        this.context = context;
    }

    public void setSuggestedRoutes(List<Suggestion<RoutePlanningRequest>> queries) {
        this.queries = queries;
        this.notifyDataSetChanged();
    }

    @Override
    public RouteHistoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView;

        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("use_card_layout", false)) {
            itemView = LayoutInflater.from(parent.getContext()).inflate(be.hyperrail.android.R.layout.listview_route_history, parent, false);
        } else {
            itemView = LayoutInflater.from(parent.getContext()).inflate(be.hyperrail.android.R.layout.cardview_route_history, parent, false);
        }
        return new RouteSuggestionsCardAdapter.RouteHistoryViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(RouteHistoryViewHolder holder, int position) {
        final Suggestion<RoutePlanningRequest> query = queries.get(position);

        holder.vFrom.setText(query.getData().getOrigin().getLocalizedName());
        holder.vTo.setText(query.getData().getDestination().getLocalizedName());

        switch (query.getType()) {
            case HISTORY:
                holder.vIcon.setImageDrawable(ContextCompat.getDrawable(context, be.hyperrail.android.R.drawable.ic_history));
                break;
            case FAVORITE:
                holder.vIcon.setImageDrawable(ContextCompat.getDrawable(context, be.hyperrail.android.R.drawable.ic_star));
                break;
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onRecyclerItemClick(RouteSuggestionsCardAdapter.this, query);
                }
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (longClickListener != null) {
                    longClickListener.onRecyclerItemLongClick(RouteSuggestionsCardAdapter.this, query);
                }
                return false;
            }
        });
    }

    public void setOnItemClickListener(OnRecyclerItemClickListener<Suggestion<RoutePlanningRequest>> listener) {
        this.listener = listener;
    }

    public void setOnLongItemClickListener(OnRecyclerItemLongClickListener<Suggestion<RoutePlanningRequest>> listener) {
        this.longClickListener = listener;
    }

    @Override
    public int getItemCount() {
        if (queries == null){
            return 0;
        }
        return queries.size();
    }

    class RouteHistoryViewHolder extends RecyclerView.ViewHolder {

        protected final TextView vFrom;
        protected final TextView vTo;
        protected final ImageView vIcon;

        public RouteHistoryViewHolder(View v) {
            super(v);

            vFrom = v.findViewById(be.hyperrail.android.R.id.text_from);
            vTo = v.findViewById(be.hyperrail.android.R.id.text_to);
            vIcon = v.findViewById(be.hyperrail.android.R.id.image_right);
        }
    }
}
