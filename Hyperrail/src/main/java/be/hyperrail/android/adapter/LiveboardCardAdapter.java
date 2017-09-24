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
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;

import be.hyperrail.android.OccupancyDialog;
import be.hyperrail.android.R;
import be.hyperrail.android.infiniteScrolling.InfiniteScrollingAdapter;
import be.hyperrail.android.infiniteScrolling.InfiniteScrollingDataSource;
import be.hyperrail.android.irail.implementation.LiveBoard;
import be.hyperrail.android.irail.implementation.OccupancyHelper;
import be.hyperrail.android.irail.implementation.TrainStop;

/**
 * Recyclerview adapter to show train departures in a station
 */
public class LiveboardCardAdapter extends InfiniteScrollingAdapter<TrainStop> {

    private LiveBoard liveboard;
    private final Context context;

    private Object[] displayList;

    protected final static int VIEW_TYPE_DATE = 1;

    private OnRecyclerItemClickListener<TrainStop> listener;

    public LiveboardCardAdapter(Context context, RecyclerView recyclerView, InfiniteScrollingDataSource listener) {
        super(context, recyclerView, listener);
        this.context = context;
    }

    public void updateLiveboard(LiveBoard liveBoard) {
        this.liveboard = liveBoard;

        ArrayList<Integer> daySeparatorPositions = new ArrayList<>();

        if (liveboard != null && liveboard.getStops() != null && liveboard.getStops().length > 0) {
            // Default day to compare to is today
            DateTime lastday = DateTime.now().withTimeAtStartOfDay();

            if (!liveboard.getStops()[0].getDepartureTime().withTimeAtStartOfDay().isEqual(lastday)) {
                // If the first stop is not today, add date separators everywhere
                lastday = liveboard.getStops()[0].getDepartureTime().withTimeAtStartOfDay().minusDays(1);
            } else if (!liveboard.getSearchTime().withTimeAtStartOfDay().equals(liveboard.getStops()[0].getDepartureTime().withTimeAtStartOfDay())) {
                // If the search results differ from the date searched, everything after the date searched should have separators
                lastday = liveboard.getSearchTime().withTimeAtStartOfDay();
            }

            for (int i = 0; i < liveboard.getStops().length; i++) {
                TrainStop stop = liveBoard.getStops()[i];

                if (stop.getDepartureTime().withTimeAtStartOfDay().isAfter(lastday)) {
                    lastday = stop.getDepartureTime().withTimeAtStartOfDay();
                    daySeparatorPositions.add(i);
                }
            }

            Log.d("DateSeparator", "Detected " + daySeparatorPositions.size() + " day changes");
            this.displayList = new Object[daySeparatorPositions.size() + liveBoard.getStops().length];

            // Convert to array + take previous separators into account for position of next separator
            int dayPosition = 0;
            int stopPosition = 0;
            int resultPosition = 0;
            while (resultPosition < daySeparatorPositions.size() + liveBoard.getStops().length) {
                // Keep in mind that position shifts with the number of already placed date separators
                if (dayPosition < daySeparatorPositions.size() && resultPosition == daySeparatorPositions.get(dayPosition) + dayPosition) {
                    DateSeparator separator = new DateSeparator();
                    separator.separatorDate = liveBoard.getStops()[stopPosition].getDepartureTime();

                    this.displayList[resultPosition] = separator;

                    dayPosition++;
                } else {
                    this.displayList[resultPosition] = liveboard.getStops()[stopPosition];
                    stopPosition++;
                }

                resultPosition++;
            }
        } else {
            displayList = null;
        }

        super.setLoaded();
        super.notifyDataSetChanged();
    }

    @Override
    protected int onGetItemViewType(int position) {
        if (displayList[position] instanceof DateSeparator) {
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

        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("use_card_layout", false)) {
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

            DateTimeFormatter df = DateTimeFormat.forPattern("EEE dd MMMMMMMM yyyy");
            holder.vDateText.setText(df.print(((DateSeparator) displayList[position]).separatorDate));
            return;
        }

        LiveboardStopViewHolder holder = (LiveboardStopViewHolder) genericHolder;

        final TrainStop stop = (TrainStop) displayList[position];

        holder.vDestination.setText(stop.getDestination().getLocalizedName());

        holder.vTrainNumber.setText(stop.getTrain().getNumber());
        holder.vTrainType.setText(stop.getTrain().getType());

        DateTimeFormatter df = DateTimeFormat.forPattern("HH:mm");

        holder.vDeparture.setText(df.print(stop.getDepartureTime()));
        if (stop.getDepartureDelay().getStandardSeconds() > 0) {
            holder.vDepartureDelay.setText(context.getString(be.hyperrail.android.R.string.delay, stop.getDepartureDelay().getStandardMinutes()));
            holder.vDelayTime.setText(df.print(stop.getDelayedDepartureTime()));
        } else {
            holder.vDepartureDelay.setText("");
            holder.vDelayTime.setText("");
        }

        holder.vPlatform.setText(String.valueOf(stop.getPlatform()));

        if (stop.isDepartureCanceled()) {
            holder.vPlatform.setText("");
            holder.vPlatformContainer.setBackground(ContextCompat.getDrawable(context, be.hyperrail.android.R.drawable.platform_train_canceled));
            holder.vStatusText.setText(be.hyperrail.android.R.string.status_cancelled);
        } else {
            holder.vStatusContainer.setVisibility(View.GONE);
            holder.vPlatformContainer.setBackground(ContextCompat.getDrawable(context, be.hyperrail.android.R.drawable.platform_train));
            if (!stop.isPlatformNormal()) {
                Drawable drawable = holder.vPlatformContainer.getBackground();
                drawable.mutate();
                drawable.setColorFilter(ContextCompat.getColor(context, be.hyperrail.android.R.color.colorDelay), PorterDuff.Mode.SRC_ATOP);
            }
        }

        holder.vOccupancy.setImageDrawable(ContextCompat.getDrawable(context, OccupancyHelper.getOccupancyDrawable(stop.getOccupancyLevel())));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onRecyclerItemClick(LiveboardCardAdapter.this, stop);
                }
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                (new OccupancyDialog(LiveboardCardAdapter.this.context, stop)).show();
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

    public void setOnItemClickListener(OnRecyclerItemClickListener<TrainStop> listener) {
        this.listener = listener;
    }

    private class LiveboardStopViewHolder extends RecyclerView.ViewHolder {

        protected final TextView vDestination;
        protected final TextView vTrainType;
        protected final TextView vTrainNumber;
        protected final TextView vDeparture;
        protected final TextView vDepartureDelay;
        protected final TextView vDelayTime;
        protected final TextView vPlatform;
        protected final LinearLayout vPlatformContainer;

        protected final LinearLayout vStatusContainer;
        protected final TextView vStatusText;

        protected final ImageView vOccupancy;

        LiveboardStopViewHolder(View view) {
            super(view);
            vDestination = view.findViewById(be.hyperrail.android.R.id.text_destination);
            vTrainNumber = view.findViewById(be.hyperrail.android.R.id.text_train_number);
            vTrainType = view.findViewById(be.hyperrail.android.R.id.text_train_type);
            vDeparture = view.findViewById(be.hyperrail.android.R.id.text_departure_time);
            vDepartureDelay = view.findViewById(be.hyperrail.android.R.id.text_departure_delay);
            vDelayTime = view.findViewById(be.hyperrail.android.R.id.text_delay_time);
            vPlatform = view.findViewById(be.hyperrail.android.R.id.text_platform);
            vPlatformContainer = view.findViewById(be.hyperrail.android.R.id.layout_platform_container);

            vStatusContainer = view.findViewById(be.hyperrail.android.R.id.layout_train_status_container);
            vStatusText = view.findViewById(be.hyperrail.android.R.id.text_train_status);
            vOccupancy = view.findViewById(R.id.image_occupancy);
        }
    }

    private class DateSeparator {

        DateTime separatorDate;
    }
}

