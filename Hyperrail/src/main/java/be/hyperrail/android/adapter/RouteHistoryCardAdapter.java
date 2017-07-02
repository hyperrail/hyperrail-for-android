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

import be.hyperrail.android.persistence.RouteQuery;

/**
 * An adapter to show RouteQueries (recent/favorite routes) in a recyclerview
 */
public class RouteHistoryCardAdapter extends RecyclerView.Adapter<RouteHistoryCardAdapter.RouteHistoryViewHolder> {

    private final Context context;
    private RouteQuery[] queries;

    private onRecyclerItemClickListener<RouteQuery> listener;
    private onLongRecyclerItemClickListener<RouteQuery> longClickListener;

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
            itemView = LayoutInflater.from(parent.getContext()).inflate(be.hyperrail.android.R.layout.listview_route_history, parent, false);
        } else {
            itemView = LayoutInflater.from(parent.getContext()).inflate(be.hyperrail.android.R.layout.cardview_route_history, parent, false);
        }
        return new RouteHistoryCardAdapter.RouteHistoryViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(RouteHistoryViewHolder holder, int position) {
        final RouteQuery query = queries[position];

        holder.vFrom.setText(query.fromName);
        holder.vTo.setText(query.toName);

        switch (query.type) {
            case RECENT_ROUTE:
                holder.vIcon.setImageDrawable(ContextCompat.getDrawable(context, be.hyperrail.android.R.drawable.ic_history));
                break;
            case FAVORITE_ROUTE:
                holder.vIcon.setImageDrawable(ContextCompat.getDrawable(context, be.hyperrail.android.R.drawable.ic_star));
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

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (longClickListener != null) {
                    longClickListener.onLongRecyclerItemClick(RouteHistoryCardAdapter.this, query);
                }
                return false;
            }
        });
    }

    public void setOnItemClickListener(onRecyclerItemClickListener<RouteQuery> listener) {
        this.listener = listener;
    }

    public void setOnLongItemClickListener(onLongRecyclerItemClickListener<RouteQuery> listener) {
        this.longClickListener = listener;
    }

    @Override
    public int getItemCount() {
        return queries.length;
    }

    class RouteHistoryViewHolder extends RecyclerView.ViewHolder {

        protected final TextView vFrom;
        protected final TextView vTo;
        protected final ImageView vIcon;

        public RouteHistoryViewHolder(View v) {
            super(v);

            vFrom = (TextView) v.findViewById(be.hyperrail.android.R.id.text_from);
            vTo = (TextView) v.findViewById(be.hyperrail.android.R.id.text_to);
            vIcon = (ImageView) v.findViewById(be.hyperrail.android.R.id.image_right);
        }
    }
}
