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

import be.hyperrail.android.R;
import be.hyperrail.android.irail.db.Station;
import be.hyperrail.android.persistence.RouteQuery;

/**
 * Recyclerview to show stations (for searches, recents ,...)
 */
public class StationCardAdapter extends RecyclerView.Adapter<StationCardAdapter.StationViewHolder> {

    private final Context context;
    private RouteQuery[] suggestedStations;
    private Station[] stations;
    private boolean showSuggestions = true;
    private boolean nearbyOnTop;
    private OnRecyclerItemLongClickListener<Object> longClickListener;
    private OnRecyclerItemClickListener<Station> listener;

    private stationType currentDisplayType = stationType.UNDEFINED;

    /**
     * Show nearby stations before suggestiosn
     *
     * @param nearbyOnTop Whether or not to show nearby stations on top of favorite/recents (suggestions)
     */
    public void setNearbyOnTop(boolean nearbyOnTop) {
        this.nearbyOnTop = nearbyOnTop;
        this.notifyDataSetChanged();
    }

    public enum stationType {
        NEARBY,
        SEARCHED,
        UNDEFINED
    }

    public StationCardAdapter(Context context, Station[] stations) {
        this.context = context;
        this.stations = stations;
    }

    @Override
    public StationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView;

        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("use_card_layout", false)) {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.listview_station, parent, false);
        } else {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_station, parent, false);
        }

        return new StationViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(StationViewHolder holder, int position) {

        int suggestedStationsLength = 0;

        if (showSuggestions && suggestedStations != null) {
            suggestedStationsLength = suggestedStations.length;
        }

        if ((showSuggestions && suggestedStations != null && position < suggestedStationsLength && !nearbyOnTop) ||
                (showSuggestions && suggestedStations != null && position >= stations.length && nearbyOnTop)) {
            bindSuggestionViewHolder(holder, position);
        } else {
            bindNearbyViewHolder(holder, position, suggestedStationsLength);
        }
    }

    private void bindNearbyViewHolder(StationViewHolder holder, int position, int suggestedStationsLength) {
        if (!nearbyOnTop) {
            position = position - suggestedStationsLength;
        }

        final Station station = stations[position];

        holder.vStation.setText(station.getLocalizedName());

        switch (currentDisplayType) {
            case NEARBY:
                holder.vIcon.setVisibility(View.VISIBLE);
                holder.vIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_location_on_white));
                break;
            case SEARCHED:
                holder.vIcon.setVisibility(View.VISIBLE);
                holder.vIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_action_search_white));
                break;
            default:
            case UNDEFINED:
                holder.vIcon.setVisibility(View.INVISIBLE);
                break;
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onRecyclerItemClick(StationCardAdapter.this, station);
                }
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (longClickListener != null) {
                    longClickListener.onRecyclerItemLongClick(StationCardAdapter.this, station);
                }
                return false;
            }
        });
    }

    private void bindSuggestionViewHolder(StationViewHolder holder, int position) {
        if (nearbyOnTop) {
            position = position - stations.length;
        }

        final RouteQuery q = suggestedStations[position];

        holder.vStation.setText(q.fromName);

        switch (q.type) {
            case FAVORITE_STATION:
                holder.vIcon.setVisibility(View.VISIBLE);
                holder.vIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_star));
                break;
            case RECENT_STATION:
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
                    listener.onRecyclerItemClick(StationCardAdapter.this, q.from);
                }
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (longClickListener != null) {
                    longClickListener.onRecyclerItemLongClick(StationCardAdapter.this, q);
                }
                return false;
            }
        });
    }

    public void setSuggestedStations(RouteQuery[] suggestedStations) {
        this.suggestedStations = suggestedStations;
        this.notifyDataSetChanged();
    }

    public void setStations(Station[] stations) {
        this.stations = stations;
        this.notifyDataSetChanged();
    }

    public void setStationIconType(stationType type) {
        this.currentDisplayType = type;
        this.notifyDataSetChanged();
    }

    public void setSuggestionsVisible(boolean visible) {
        this.showSuggestions = visible;
        this.notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnRecyclerItemClickListener<Station> listener) {
        this.listener = listener;
    }

    public void setOnLongItemClickListener(OnRecyclerItemLongClickListener<Object> listener) {
        this.longClickListener = listener;
    }

    @Override
    public int getItemCount() {
        int result = 0;

        if (stations != null) {
            result += stations.length;
        }
        if (showSuggestions && suggestedStations != null) {
            result += suggestedStations.length;
        }
        return result;
    }

    /**
     * Viewholder to show the station name, along with an icon indicating the reason why this station is shown (recent, favorite, ..)
     */
    class StationViewHolder extends RecyclerView.ViewHolder {

        protected final TextView vStation;
        protected final ImageView vIcon;

        StationViewHolder(View v) {
            super(v);

            vStation = (TextView) v.findViewById(R.id.text_station);
            vIcon = (ImageView) v.findViewById(R.id.image_right);
        }
    }
}

