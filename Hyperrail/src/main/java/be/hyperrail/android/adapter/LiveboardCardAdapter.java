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
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.joda.time.DateTime;

import java.util.ArrayList;

import be.hyperrail.android.R;
import be.hyperrail.android.infiniteScrolling.InfiniteScrollingAdapter;
import be.hyperrail.android.infiniteScrolling.InfiniteScrollingDataSource;
import be.hyperrail.android.irail.implementation.Liveboard;
import be.hyperrail.android.irail.implementation.VehicleStop;
import be.hyperrail.android.irail.implementation.VehicleStopType;
import be.hyperrail.android.viewgroup.LiveboardStopLayout;

import static be.hyperrail.android.irail.implementation.Liveboard.LiveboardType.DEPARTURES;
import static org.joda.time.Days.daysBetween;

/**
 * Recyclerview adapter to show train departures in a station
 */
public class LiveboardCardAdapter extends InfiniteScrollingAdapter<VehicleStop> {

    private Liveboard liveboard;
    private final Context context;
    private final static int STYLE_LIST = 0;
    private final static int STYLE_CARD = 1;
    private int style = STYLE_LIST;
    private Object[] displayList;

    protected final static int VIEW_TYPE_DATE = 1;

    public LiveboardCardAdapter(Context context, RecyclerView recyclerView, InfiniteScrollingDataSource listener) {
        super(context, recyclerView, listener);
        this.context = context;
    }

    public void updateLiveboard(Liveboard liveBoard) {
        this.liveboard = liveBoard;

        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("use_card_layout", false)) {
            style = STYLE_CARD;
        } else {
            style = STYLE_LIST;
        }

        ArrayList<Integer> daySeparatorPositions = new ArrayList<>();

        if (liveboard == null || liveboard.getStops() == null || liveboard.getStops().length == 0) {
            displayList = null;
            return;
        }

        // Default day to compare to is today
        DateTime dateCompareObj = DateTime.now().withTimeAtStartOfDay();
        DateTime stoptime = liveBoard.getStops()[0].getType() == VehicleStopType.DEPARTURE ?
                liveBoard.getStops()[0].getDepartureTime() :
                liveboard.getStops()[0].getArrivalTime();

        if (!stoptime.withTimeAtStartOfDay().isEqual(dateCompareObj)) {
            // If the first stop is not today, add date separators everywhere
            dateCompareObj = stoptime.withTimeAtStartOfDay().minusDays(1);
        } else if (!liveboard.getSearchTime().withTimeAtStartOfDay().equals(stoptime.withTimeAtStartOfDay())) {
            // If the search results differ from the date searched, everything after the date searched should have separators
            dateCompareObj = liveboard.getSearchTime().withTimeAtStartOfDay();
        }

        for (int i = 0; i < liveboard.getStops().length; i++) {
            stoptime = liveBoard.getStops()[i].getType() == VehicleStopType.DEPARTURE ? liveBoard.getStops()[i].getDepartureTime() : liveboard.getStops()[i].getArrivalTime();

            if (daysBetween(stoptime.toLocalDate(), dateCompareObj.toLocalDate()).getDays() != 0) {
                dateCompareObj = stoptime.withTimeAtStartOfDay();
                daySeparatorPositions.add(i);
            }
        }

        // Log.d("DateSeparator", "Detected " + daySeparatorPositions.size() + " day changes");
        this.displayList = new Object[daySeparatorPositions.size() + liveBoard.getStops().length];

        // Convert to array + take previous separators into account for position of next separator
        int dayPosition = 0;
        int stopPosition = 0;
        int resultPosition = 0;
        while (resultPosition < daySeparatorPositions.size() + liveBoard.getStops().length) {
            // Keep in mind that position shifts with the number of already placed date separators
            if (dayPosition < daySeparatorPositions.size() && resultPosition == daySeparatorPositions.get(dayPosition) + dayPosition) {
                if (liveboard.getLiveboardType() == DEPARTURES) {
                    this.displayList[resultPosition] = liveBoard.getStops()[stopPosition].getDepartureTime();
                } else {
                    this.displayList[resultPosition] = liveBoard.getStops()[stopPosition].getArrivalTime();
                }
                dayPosition++;
            } else {
                this.displayList[resultPosition] = liveboard.getStops()[stopPosition];
                stopPosition++;
            }

            resultPosition++;
        }

        super.notifyDataSetChanged();
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
            itemView = LayoutInflater.from(parent.getContext()).inflate(be.hyperrail.android.R.layout.listview_separator_date, parent, false);
            return new DateSeparatorViewHolder(itemView);
        }

        if (style == STYLE_LIST) {
            itemView = LayoutInflater.from(parent.getContext()).inflate(be.hyperrail.android.R.layout.listview_liveboard, parent, false);
        } else {
            itemView = LayoutInflater.from(parent.getContext()).inflate(be.hyperrail.android.R.layout.cardview_liveboard, parent, false);
        }

        return new LiveboardStopViewHolder(itemView);
    }

    @Override
    public void onBindItemViewHolder(RecyclerView.ViewHolder genericHolder, int position) {
        if (genericHolder instanceof DateSeparatorViewHolder) {
            DateSeparatorViewHolder holder = (DateSeparatorViewHolder) genericHolder;
            holder.bind((DateTime) displayList[position]);
            return;
        }

        final VehicleStop stop = (VehicleStop) displayList[position];
        LiveboardStopViewHolder holder = (LiveboardStopViewHolder) genericHolder;
        holder.liveboardStopView.bind(context, stop, liveboard, position);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnClickListener != null) {
                    mOnClickListener.onRecyclerItemClick(LiveboardCardAdapter.this, stop);
                }
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (mOnLongClickListener != null) {
                    mOnLongClickListener.onRecyclerItemLongClick(LiveboardCardAdapter.this, stop);
                }
                return false;
            }
        });
    }

    @Override
    public int getListItemCount() {
        if (liveboard == null || liveboard.getStops() == null || displayList == null) {
            return 0;
        }
        return displayList.length;
    }

    private class LiveboardStopViewHolder extends RecyclerView.ViewHolder {

        LiveboardStopLayout liveboardStopView;

        LiveboardStopViewHolder(View view) {
            super(view);
            liveboardStopView = view.findViewById(R.id.binder);
        }
    }
}

