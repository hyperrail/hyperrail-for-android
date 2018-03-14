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

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.List;

import be.hyperrail.android.R;
import be.hyperrail.android.irail.implementation.VehicleStub;
import be.hyperrail.android.irail.implementation.requests.IrailVehicleRequest;
import be.hyperrail.android.persistence.Suggestion;

/**
 * Recyclerview to show stations (for searches, recents ,...)
 */
public class VehicleSuggestionsCardAdapter extends RecyclerView.Adapter<VehicleSuggestionsCardAdapter.TrainViewHolder> {

    private final Context context;
    private List<Suggestion<IrailVehicleRequest>> suggestedTrains;

    private OnRecyclerItemLongClickListener<Suggestion<IrailVehicleRequest>> longClickListener;
    private OnRecyclerItemClickListener<Suggestion<IrailVehicleRequest>> listener;

    public VehicleSuggestionsCardAdapter(Context context) {
        this.context = context;
    }

    @Override
    public TrainViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView;

        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("use_card_layout", false)) {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.listview_station, parent, false);
        } else {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_station, parent, false);
        }

        return new TrainViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(TrainViewHolder holder, int position) {

        final Suggestion<IrailVehicleRequest> t = suggestedTrains.get(position);
        String title = VehicleStub.getVehicleName(t.getData().getVehicleId());
        if (t.getData().getDepartureTime() != null) {
            DateTimeFormatter df = DateTimeFormat.forPattern("HH:mm");
            title += " - " + df.print(t.getData().getDepartureTime());
        }
        if (t.getData().getOrigin() != null) {
            title += " - " + t.getData().getOrigin().getLocalizedName();
        }
        if (t.getData().getDirection() != null) {
            title += " - " + t.getData().getDirection().getLocalizedName();
        }
        holder.vStation.setText(title);

        switch (t.getType()) {
            case FAVORITE:
                holder.vIcon.setVisibility(View.VISIBLE);
                holder.vIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_star));
                break;
            case HISTORY:
                holder.vIcon.setVisibility(View.VISIBLE);
                holder.vIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_history));
                break;
            default:
                // No default icon
                break;
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onRecyclerItemClick(VehicleSuggestionsCardAdapter.this, t);
                }
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (longClickListener != null) {
                    longClickListener.onRecyclerItemLongClick(VehicleSuggestionsCardAdapter.this, t);
                }
                return false;
            }
        });
    }

    public void setSuggestedTrains(List<Suggestion<IrailVehicleRequest>> suggestions) {
        this.suggestedTrains = suggestions;
        this.notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnRecyclerItemClickListener<Suggestion<IrailVehicleRequest>> listener) {
        this.listener = listener;
    }

    public void setOnLongItemClickListener(OnRecyclerItemLongClickListener<Suggestion<IrailVehicleRequest>> listener) {
        this.longClickListener = listener;
    }

    @Override
    public int getItemCount() {
        if (suggestedTrains != null) {
            return suggestedTrains.size();
        }
        return 0;
    }

    /**
     * Viewholder to show the station name, along with an icon indicating the reason why this station is shown (recent, favorite, ..)
     */
    class TrainViewHolder extends RecyclerView.ViewHolder {

        protected final TextView vStation;
        protected final ImageView vIcon;

        TrainViewHolder(View v) {
            super(v);

            vStation = v.findViewById(R.id.text_station);
            vIcon = v.findViewById(R.id.image_right);
        }
    }
}

