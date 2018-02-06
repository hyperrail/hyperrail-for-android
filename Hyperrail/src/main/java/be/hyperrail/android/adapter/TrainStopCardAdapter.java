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

import be.hyperrail.android.R;
import be.hyperrail.android.irail.implementation.Train;
import be.hyperrail.android.irail.implementation.TrainStop;
import be.hyperrail.android.viewgroup.TrainStopLayout;

/**
 * Recyclerview adapter which shows stops of a train
 */
public class TrainStopCardAdapter extends RecyclerView.Adapter<TrainStopCardAdapter.TrainStopViewHolder> {

    private Train mTrain;
    private final Context context;
    private OnRecyclerItemClickListener<TrainStop> clickListener;
    private OnRecyclerItemLongClickListener<TrainStop> longClickListener;

    public TrainStopCardAdapter(Context context, Train train) {
        this.context = context;
        this.mTrain = train;
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
        final TrainStop stop = mTrain.getStops()[position];

        holder.trainStopLayout.bind(context, stop, mTrain, position);

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
        if (mTrain == null || mTrain.getStops() == null) {
            return 0;
        }
        return mTrain.getStops().length;
    }

    public void setOnItemClickListener(OnRecyclerItemClickListener<TrainStop> listener) {
        this.clickListener = listener;
    }

    public void setOnItemLongClickListener(OnRecyclerItemLongClickListener<TrainStop> listener) {
        this.longClickListener = listener;
    }

    public void updateTrain(Train train) {
        this.mTrain = train;
        notifyDataSetChanged();
    }

    class TrainStopViewHolder extends RecyclerView.ViewHolder {

        TrainStopLayout trainStopLayout;

        TrainStopViewHolder(View view) {
            super(view);
            trainStopLayout = view.findViewById(R.id.binder);
        }
    }
}