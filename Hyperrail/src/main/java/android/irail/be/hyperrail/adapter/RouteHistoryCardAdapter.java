/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package android.irail.be.hyperrail.adapter;

import android.content.Context;
import android.irail.be.hyperrail.R;
import android.irail.be.hyperrail.persistence.RouteQuery;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * An adapter to show RouteQueries (recent/favorite routes) in a recyclerview
 */
public class RouteHistoryCardAdapter extends RecyclerView.Adapter<RouteHistoryCardAdapter.RouteHistoryViewHolder> {

    private final Context context;
    private RouteQuery[] queries;

    private onRecyclerItemClickListener<RouteQuery> listener;

    public RouteHistoryCardAdapter(Context context, RouteQuery[] queries) {
        this.queries = queries;
        this.context = context;
    }

    public void updateHistory(RouteQuery[] queries) {
        this.queries = queries;
        this.notifyDataSetChanged();
    }

    @Override
    public RouteHistoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView;

        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("use_card_layout", false)) {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.listview_route_history, parent, false);
        } else {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_route_history, parent, false);
        }
        return new RouteHistoryCardAdapter.RouteHistoryViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(RouteHistoryViewHolder holder, int position) {
        final RouteQuery query = queries[position];

        holder.vFrom.setText(query.from);
        holder.vTo.setText(query.to);

        switch (query.type) {
            case RECENT_ROUTE:
                holder.vIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_history));
                break;
            case FAVORITE_ROUTE:
                holder.vIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_star));
                break;
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onRecyclerItemClick(RouteHistoryCardAdapter.this, query);
                }
            }
        });
    }

    public void setOnItemClickListener(onRecyclerItemClickListener<RouteQuery> listener) {
        this.listener = listener;
    }

    public void clearOnItemClickListener() {
        this.listener = null;
    }

    @Override
    public int getItemCount() {
        return queries.length;
    }

    class RouteHistoryViewHolder extends RecyclerView.ViewHolder {

        final TextView vFrom;
        final TextView vTo;
        final ImageView vIcon;

        public RouteHistoryViewHolder(View v) {
            super(v);

            vFrom = (TextView) v.findViewById(R.id.text_from);
            vTo = (TextView) v.findViewById(R.id.text_to);
            vIcon = (ImageView) v.findViewById(R.id.image_right);
        }
    }
}
