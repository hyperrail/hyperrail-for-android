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

import android.app.Activity;
import android.content.Context;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;

import be.hyperrail.android.R;
import be.hyperrail.android.infiniteScrolling.InfiniteScrollingAdapter;
import be.hyperrail.android.infiniteScrolling.InfiniteScrollingDataSource;
import be.hyperrail.android.viewgroup.RouteListItemLayout;
import be.hyperrail.opentransportdata.common.models.Route;
import be.hyperrail.opentransportdata.common.models.RoutesList;

/**
 * Recyclerview adapter to show results of route searches
 */
public class RouteCardAdapter extends InfiniteScrollingAdapter<Route> {

    private Route[] routes;
    private final Context context;

    private Object[] displayList;

    private static final int VIEW_TYPE_DATE = 1;

    public RouteCardAdapter(Activity context, RecyclerView recyclerView, InfiniteScrollingDataSource listener) {
        super(context, recyclerView, listener);
        this.context = context;
    }

    /**
     * Update the routes in this listview, and calculate where day separators should be placed
     * @param newRoutes
     */
    public void updateRoutes(RoutesList newRoutes) {
        if (newRoutes == null || newRoutes.getRoutes() == null || newRoutes.getRoutes().length < 1) {
            this.routes = null;
            this.displayList = null;
            return;
        }

        this.routes = newRoutes.getRoutes();

        ArrayList<Integer> daySeparatorPositions = new ArrayList<>();


        // Default day to compare to is today
        DateTime lastday = DateTime.now().withZone(DateTimeZone.UTC).withTimeAtStartOfDay();

        if (routes[0].getDepartureTime().withZone(DateTimeZone.UTC).withTimeAtStartOfDay().isBefore(lastday)) {
            // If the first stop is not today, add date separators everywhere
            lastday = routes[0].getDepartureTime().withTimeAtStartOfDay().minusDays(1);
        } else if (!newRoutes.getSearchTime().withTimeAtStartOfDay().withZone(DateTimeZone.UTC).equals(routes[0].getDepartureTime().withZone(DateTimeZone.UTC).withTimeAtStartOfDay())) {
            // If the search results differ from the date searched, everything after the date searched should have separators
            lastday = newRoutes.getSearchTime().withTimeAtStartOfDay();
        }
        for (int i = 0; i < routes.length; i++) {
            Route route = routes[i];

            if (route.getDepartureTime().withTimeAtStartOfDay().isAfter(lastday)) {
                lastday = route.getDepartureTime().withTimeAtStartOfDay();
                daySeparatorPositions.add(i);
            }
        }

        this.displayList = new Object[daySeparatorPositions.size() + routes.length];

        // Convert to array + take previous separators into account for position of next separator
        int dayPosition = 0;
        int routePosition = 0;
        int resultPosition = 0;

        while (resultPosition < daySeparatorPositions.size() + routes.length) {
            // Keep in mind that position shifts with the number of already placed date separators
            if (dayPosition < daySeparatorPositions.size() && resultPosition == daySeparatorPositions.get(dayPosition) + dayPosition) {
                this.displayList[resultPosition] = routes[routePosition].getDepartureTime();

                dayPosition++;
            } else {
                this.displayList[resultPosition] = routes[routePosition];
                routePosition++;
            }

            resultPosition++;
        }

        mRecyclerView.post(this::notifyDataSetChanged);
    }

    @Override
    protected int onGetItemViewType(int position) {
        if (displayList[position] instanceof DateTime) {
            return VIEW_TYPE_DATE;
        }
        return VIEW_TYPE_ITEM;
    }

    @Override
    public RecyclerView.ViewHolder onCreateItemViewHolder(ViewGroup parent, int viewType) {
        View itemView;

        if (viewType == VIEW_TYPE_DATE) {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.listview_separator_date, parent, false);
            return new DateSeparatorViewHolder(itemView);
        }

        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("use_card_layout", false)) {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.listview_route, parent, false);
        } else {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_route, parent, false);
        }

        return new RouteViewHolder(itemView);
    }

    @Override
    public void onBindItemViewHolder(RecyclerView.ViewHolder genericHolder, int position) {

        if (genericHolder instanceof DateSeparatorViewHolder) {
            DateSeparatorViewHolder holder = (DateSeparatorViewHolder) genericHolder;
            holder.bind((DateTime) displayList[position]);
            return;
        }

        RouteViewHolder holder = (RouteViewHolder) genericHolder;
        final Route route = (Route) displayList[position];

        holder.routeListItemLayout.bind(context, route, null, position);

        holder.itemView.setOnClickListener(v -> {
            if (mOnClickListener != null) {
                mOnClickListener.onRecyclerItemClick(RouteCardAdapter.this, route);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (mOnLongClickListener != null) {
                mOnLongClickListener.onRecyclerItemLongClick(RouteCardAdapter.this, route);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getListItemCount() {
        if (routes == null || displayList == null) {
            return 0;
        }
        return displayList.length;
    }

    private class RouteViewHolder extends RecyclerView.ViewHolder {

        RouteListItemLayout routeListItemLayout;

        RouteViewHolder(View view) {
            super(view);
            routeListItemLayout = view.findViewById(R.id.binder);

        }

    }

}

