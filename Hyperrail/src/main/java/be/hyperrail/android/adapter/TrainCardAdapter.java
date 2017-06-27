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

import android.annotation.SuppressLint;
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import be.hyperrail.android.R;
import be.hyperrail.android.irail.implementation.Train;
import be.hyperrail.android.irail.implementation.TrainStop;

/**
 * Recyclerview adapter which shows stops of a train
 */
public class TrainCardAdapter extends RecyclerView.Adapter<TrainCardAdapter.TrainStopViewHolder> {

    private final Train train;
    private final Context context;
    private onRecyclerItemClickListener<TrainStop> listener;

    public TrainCardAdapter(Context context, Train train) {
        this.context = context;
        this.train = train;
    }

    @Override
    public TrainStopViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView;

        if (! PreferenceManager.getDefaultSharedPreferences(context).getBoolean("use_card_layout", false)) {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.listview_trainstop, parent, false);
        } else {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_trainstop, parent, false);
        }
        return new TrainStopViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(TrainStopViewHolder holder, int position) {
        final TrainStop s = train.getStops()[position];

        holder.vDestination.setText(s.getStation().getLocalizedName());

        @SuppressLint("SimpleDateFormat")
        DateFormat df = new SimpleDateFormat("HH:mm");

        holder.vDepartureTime.setText(df.format(s.getDepartureTime()));
        if (s.getDepartureDelay() > 0) {
            holder.vDepartureDelay.setText(context.getString(R.string.delay, s.getDepartureDelay() / 60));
        } else {
            holder.vDepartureDelay.setText("");
        }

        holder.vArrivalTime.setText(df.format(s.getArrivalTime()));
        if (s.getArrivalDelay() > 0) {
            holder.vArrivalDelay.setText(context.getString(R.string.delay, s.getArrivalDelay() / 60));
        } else {
            holder.vArrivalDelay.setText("");
        }

        holder.vPlatform.setText(String.valueOf(s.getPlatform()));

        if (s.isDepartureCanceled()) {
            holder.vPlatform.setText("");
            holder.vPlatformContainer.setBackground(ContextCompat.getDrawable(context,R.drawable.platform_train_canceled));
            holder.vStatusText.setText(R.string.status_cancelled);
        } else {

            holder.vStatusContainer.setVisibility(View.GONE);
            holder.vPlatformContainer.setBackground(ContextCompat.getDrawable(context,R.drawable.platform_train));

            if (!s.isPlatformNormal()) {
                Drawable drawable = holder.vPlatformContainer.getBackground();
                drawable.mutate();
                drawable.setColorFilter(ContextCompat.getColor(context,R.color.colorDelay), PorterDuff.Mode.SRC_ATOP);

            }
        }

        if (s.hasLeft()) {
            if (position == 0) {
                holder.vIcon.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.timeline_departure_filled));
            } else if (position == this.getItemCount() - 1) {
                holder.vIcon.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.timeline_arrival_filled));
            } else {
                holder.vIcon.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.timeline_transfer_filled));
            }
        } else {
            if (position == 0) {
                holder.vIcon.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.timeline_departure_hollow));
            } else if (position == this.getItemCount() - 1) {
                holder.vIcon.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.timeline_arrival_hollow));
            } else {
                holder.vIcon.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.timeline_transfer_hollow));
            }
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onRecyclerItemClick(TrainCardAdapter.this, s);
                }
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

    public void setOnItemClickListener(onRecyclerItemClickListener<TrainStop> listener) {
        this.listener = listener;
    }

    public void clearOnItemClickListener() {
        this.listener = null;
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

        final TextView vDestination;
        final TextView vDepartureTime;
        final TextView vDepartureDelay;
        final TextView vArrivalTime;
        final TextView vArrivalDelay;
        final TextView vPlatform;
        final LinearLayout vPlatformContainer;
        final ImageView vIcon;

        final LinearLayout vStatusContainer;
        final TextView vStatusText;

        TrainStopViewHolder(View v) {
            super(v);
            vDestination = (TextView) v.findViewById(R.id.text_station);

            vDepartureTime = (TextView) v.findViewById(R.id.text_departure_time);
            vDepartureDelay = (TextView) v.findViewById(R.id.text_departure_delay);

            vArrivalTime = (TextView) v.findViewById(R.id.text_arrival_time);
            vArrivalDelay = (TextView) v.findViewById(R.id.text_arrival_delay);

            vPlatform = (TextView) v.findViewById(R.id.text_platform);
            vPlatformContainer = (LinearLayout) v.findViewById(R.id.layout_platform_container);

            vStatusContainer = (LinearLayout) v.findViewById(R.id.layout_train_status_container);
            vStatusText = (TextView) v.findViewById(R.id.text_train_status);

            vIcon = (ImageView) v.findViewById(R.id.image_timeline);
        }
    }
}

