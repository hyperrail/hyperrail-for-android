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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import be.hyperrail.android.infiniteScrolling.InfiniteScrollingAdapter;
import be.hyperrail.android.infiniteScrolling.InfiniteScrollingDataSource;
import be.hyperrail.android.irail.implementation.LiveBoard;
import be.hyperrail.android.irail.implementation.TrainStop;

/**
 * Recyclerview adapter to show train departures in a station
 */
public class LiveboardCardAdapter extends InfiniteScrollingAdapter<TrainStop> {

    private LiveBoard liveboard;
    private final Context context;
    private int differentdays = 1;

    private onRecyclerItemClickListener<TrainStop> listener;

    public LiveboardCardAdapter(Context context, RecyclerView recyclerView, InfiniteScrollingDataSource listener) {
        super(context, recyclerView, listener);
        this.context = context;
    }

    public void updateLiveboard(LiveBoard liveBoard) {
        this.liveboard = liveBoard;
        super.setLoaded();
        super.notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateItemViewHolder(ViewGroup parent, int viewType) {
        View itemView;

        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("use_card_layout", false)) {
            itemView = LayoutInflater.from(parent.getContext()).inflate(be.hyperrail.android.R.layout.listview_liveboard, parent, false);
        } else {
            itemView = LayoutInflater.from(parent.getContext()).inflate(be.hyperrail.android.R.layout.cardview_liveboard, parent, false);
        }

        return new LiveboardStopViewHolder(itemView);
    }

    @Override
    public void onBindItemViewHolder(RecyclerView.ViewHolder genericHolder, int position) {
        LiveboardStopViewHolder holder = (LiveboardStopViewHolder) genericHolder;

        final TrainStop stop = liveboard.getStops()[position];

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

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onRecyclerItemClick(LiveboardCardAdapter.this, stop);
                }
            }
        });
    }

    @Override
    public int getListItemCount() {
        if (liveboard == null || liveboard.getStops() == null) {
            return 0;
        }
        return liveboard.getStops().length;
    }

    public void setOnItemClickListener(onRecyclerItemClickListener<TrainStop> listener) {
        this.listener = listener;
    }

    private class LiveboardStopViewHolder extends RecyclerView.ViewHolder {

        final TextView vDestination;
        final TextView vTrainType;
        final TextView vTrainNumber;
        final TextView vDeparture;
        final TextView vDepartureDelay;
        final TextView vDelayTime;
        final TextView vPlatform;
        final LinearLayout vPlatformContainer;

        final LinearLayout vStatusContainer;
        final TextView vStatusText;

        LiveboardStopViewHolder(View view) {
            super(view);
            vDestination = (TextView) view.findViewById(be.hyperrail.android.R.id.text_destination);
            vTrainNumber = ((TextView) view.findViewById(be.hyperrail.android.R.id.text_train_number));
            vTrainType = ((TextView) view.findViewById(be.hyperrail.android.R.id.text_train_type));
            vDeparture = (TextView) view.findViewById(be.hyperrail.android.R.id.text_departure_time);
            vDepartureDelay = (TextView) view.findViewById(be.hyperrail.android.R.id.text_departure_delay);
            vDelayTime = (TextView) view.findViewById(be.hyperrail.android.R.id.text_delay_time);
            vPlatform = (TextView) view.findViewById(be.hyperrail.android.R.id.text_platform);
            vPlatformContainer = (LinearLayout) view.findViewById(be.hyperrail.android.R.id.layout_platform_container);

            vStatusContainer = (LinearLayout) view.findViewById(be.hyperrail.android.R.id.layout_train_status_container);
            vStatusText = (TextView) view.findViewById(be.hyperrail.android.R.id.text_train_status);
        }
    }
}

