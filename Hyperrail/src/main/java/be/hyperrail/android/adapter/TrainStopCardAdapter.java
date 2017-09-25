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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import be.hyperrail.android.R;
import be.hyperrail.android.irail.implementation.OccupancyHelper;
import be.hyperrail.android.irail.implementation.Train;
import be.hyperrail.android.irail.implementation.TrainStop;

/**
 * Recyclerview adapter which shows stops of a train
 */
public class TrainStopCardAdapter extends RecyclerView.Adapter<TrainStopCardAdapter.TrainStopViewHolder> {

    private final Train train;
    private final Context context;
    private OnRecyclerItemClickListener<TrainStop> clickListener;
    private OnRecyclerItemLongClickListener<TrainStop> longClickListener;

    public TrainStopCardAdapter(Context context, Train train) {
        this.context = context;
        this.train = train;
    }

    @Override
    public TrainStopViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView;

        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("use_card_layout", false)) {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.listview_trainstop, parent, false);
        } else {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_trainstop, parent, false);
        }
        return new TrainStopViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(TrainStopViewHolder holder, int position) {
        final TrainStop stop = train.getStops()[position];

        holder.vDestination.setText(stop.getStation().getLocalizedName());

        DateTimeFormatter df = DateTimeFormat.forPattern("HH:mm");

        holder.vDepartureTime.setText(df.print(stop.getDepartureTime()));
        if (stop.getDepartureDelay().getStandardSeconds() > 0) {
            holder.vDepartureDelay.setText(context.getString(R.string.delay, stop.getDepartureDelay().getStandardMinutes()));
        } else {
            holder.vDepartureDelay.setText("");
        }

        holder.vArrivalTime.setText(df.print(stop.getArrivalTime()));
        if (stop.getArrivalDelay().getStandardSeconds() > 0) {
            holder.vArrivalDelay.setText(context.getString(R.string.delay, stop.getArrivalDelay().getStandardMinutes()));
        } else {
            holder.vArrivalDelay.setText("");
        }

        holder.vPlatform.setText(String.valueOf(stop.getPlatform()));

        if (stop.isDepartureCanceled()) {
            holder.vPlatform.setText("");
            holder.vPlatformContainer.setBackground(ContextCompat.getDrawable(context, R.drawable.platform_train_canceled));
            holder.vStatusText.setText(R.string.status_cancelled);
        } else {

            holder.vStatusContainer.setVisibility(View.GONE);
            holder.vPlatformContainer.setBackground(ContextCompat.getDrawable(context, R.drawable.platform_train));

            if (!stop.isPlatformNormal()) {
                Drawable drawable = holder.vPlatformContainer.getBackground();
                drawable.mutate();
                drawable.setColorFilter(ContextCompat.getColor(context, R.color.colorDelay), PorterDuff.Mode.SRC_ATOP);

            }
        }

        if (stop.hasLeft()) {
            if (position == 0) {
                holder.vIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_departure_filled));
            } else if (position == this.getItemCount() - 1) {
                holder.vIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_arrival_filled));
            } else {
                holder.vIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_transfer_filled));
            }
        } else {
            if (position == 0) {
                holder.vIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_departure_hollow));
            } else if (position == this.getItemCount() - 1) {
                holder.vIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_arrival_hollow));
            } else {
                holder.vIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_transfer_hollow));
            }
        }

        holder.vOccupancy.setImageDrawable(ContextCompat.getDrawable(context, OccupancyHelper.getOccupancyDrawable(stop.getOccupancyLevel())));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clickListener != null) {
                    clickListener.onRecyclerItemClick(TrainStopCardAdapter.this, stop);
                }
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (longClickListener != null) {
                    longClickListener.onRecyclerItemLongClick(TrainStopCardAdapter.this, stop);
                }
                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        if (train == null || train.getStops() == null) {
            return 0;
        }
        return train.getStops().length;
    }

    public void setOnItemClickListener(OnRecyclerItemClickListener<TrainStop> listener) {
        this.clickListener = listener;
    }

    public void setOnItemLongClickListener(OnRecyclerItemLongClickListener<TrainStop> listener) {
        this.longClickListener = listener;
    }

    /**
     * Viewholder to show a train stop:
     * - Timeline
     * - Arrival & departure
     * - Delays
     * - Station & platform
     * - Whether or not the stop has been cancelled
     */
    class TrainStopViewHolder extends RecyclerView.ViewHolder {

        protected final TextView vDestination;
        protected final TextView vDepartureTime;
        protected final TextView vDepartureDelay;
        protected final TextView vArrivalTime;
        protected final TextView vArrivalDelay;
        protected final TextView vPlatform;
        protected final LinearLayout vPlatformContainer;
        protected final ImageView vIcon;

        protected final LinearLayout vStatusContainer;
        protected final TextView vStatusText;

        protected final ImageView vOccupancy;

        TrainStopViewHolder(View v) {
            super(v);
            vDestination = v.findViewById(R.id.text_station);

            vDepartureTime = v.findViewById(R.id.text_departure_time);
            vDepartureDelay = v.findViewById(R.id.text_departure_delay);

            vArrivalTime = v.findViewById(R.id.text_arrival_time);
            vArrivalDelay = v.findViewById(R.id.text_arrival_delay);

            vPlatform = v.findViewById(R.id.text_platform);
            vPlatformContainer = v.findViewById(R.id.layout_platform_container);

            vStatusContainer = v.findViewById(R.id.layout_train_status_container);
            vStatusText = v.findViewById(R.id.text_train_status);

            vIcon = v.findViewById(R.id.image_timeline);
            vOccupancy = v.findViewById(R.id.image_occupancy);
        }
    }
}

