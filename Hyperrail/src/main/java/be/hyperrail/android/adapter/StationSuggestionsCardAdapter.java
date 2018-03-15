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
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import be.hyperrail.android.R;
import be.hyperrail.android.irail.contracts.RouteTimeDefinition;
import be.hyperrail.android.irail.db.Station;
import be.hyperrail.android.irail.implementation.Liveboard;
import be.hyperrail.android.irail.implementation.requests.IrailLiveboardRequest;
import be.hyperrail.android.persistence.Suggestion;
import be.hyperrail.android.persistence.SuggestionType;

/**
 * Recyclerview to show stations (for searches, recents ,...)
 */
public class StationSuggestionsCardAdapter extends RecyclerView.Adapter<StationSuggestionsCardAdapter.StationViewHolder> {

    private final Context context;
    private List<Suggestion<IrailLiveboardRequest>> suggestedStations;
    private Station[] stations;
    private boolean showSuggestions = true;
    private boolean nearbyOnTop;
    private OnRecyclerItemLongClickListener<Suggestion<IrailLiveboardRequest>> longClickListener;
    private OnRecyclerItemClickListener<Suggestion<IrailLiveboardRequest>> listener;

    private stationType currentDisplayType = stationType.UNDEFINED;

    /**
     * Show nearby stations before suggestiosn
     *
     * @param nearbyOnTop Whether or not to show nearby stations on top of favorite/recents (suggestions)
     */
    public void showNearbyStationsOnTop(boolean nearbyOnTop) {
        this.nearbyOnTop = nearbyOnTop;
        this.notifyDataSetChanged();
    }

    public enum stationType {
        NEARBY,
        SEARCHED,
        UNDEFINED
    }

    public StationSuggestionsCardAdapter(Context context, Station[] stations) {
        this.context = context;
        this.stations = stations;
    }

    @NonNull
    @Override
    public StationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView;

        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("use_card_layout",
                                                                               false)) {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.listview_station,
                                                                        parent, false);
        } else {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_station,
                                                                        parent, false);
        }

        return new StationViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull StationViewHolder holder, int position) {

        int suggestedStationsLength = 0;
        boolean suggestionsVisible = showSuggestions && suggestedStations != null;

        if (suggestionsVisible) {
            suggestedStationsLength = suggestedStations.size();
        }

        if ((suggestionsVisible &&
                (position < suggestedStationsLength && !nearbyOnTop) ||
                (position >= stations.length && nearbyOnTop))
                ) {
            bindSuggestionViewHolder(holder, position);
        } else {
            bindNearbyViewHolder(holder, position, suggestedStationsLength);
        }
    }

    private void bindNearbyViewHolder(StationViewHolder holder, int position, int suggestedStationsLength) {
        Station station;

        if (!nearbyOnTop) {
            station = stations[position - suggestedStationsLength];
        } else {
            station = stations[position];
        }
        holder.vStation.setText(station.getLocalizedName());

        switch (currentDisplayType) {
            case NEARBY:
                holder.vIcon.setVisibility(View.VISIBLE);
                holder.vIcon.setImageDrawable(
                        ContextCompat.getDrawable(context, R.drawable.ic_location_on_white));
                break;
            case SEARCHED:
                holder.vIcon.setVisibility(View.VISIBLE);
                holder.vIcon.setImageDrawable(
                        ContextCompat.getDrawable(context, R.drawable.ic_action_search_white));
                break;
            default:
            case UNDEFINED:
                holder.vIcon.setVisibility(View.INVISIBLE);
                break;
        }

        IrailLiveboardRequest request = new IrailLiveboardRequest(station,
                                                                  RouteTimeDefinition.DEPART_AT,
                                                                  Liveboard.LiveboardType.DEPARTURES,
                                                                  null);
        final Suggestion<IrailLiveboardRequest> suggestion = new Suggestion<>(request, SuggestionType.LIST);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onRecyclerItemClick(StationSuggestionsCardAdapter.this,
                                                 suggestion);
                }
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (longClickListener != null) {
                    longClickListener.onRecyclerItemLongClick(StationSuggestionsCardAdapter.this,
                                                              suggestion);
                }
                return false;
            }
        });
    }

    private void bindSuggestionViewHolder(StationViewHolder holder, int position) {
        if (nearbyOnTop) {
            position = position - stations.length;
        }

        final Suggestion<IrailLiveboardRequest> suggestion = suggestedStations.get(position);

        holder.vStation.setText(suggestion.getData().getStation().getLocalizedName());

        switch (suggestion.getType()) {
            case FAVORITE:
                holder.vIcon.setVisibility(View.VISIBLE);
                holder.vIcon.setImageDrawable(
                        ContextCompat.getDrawable(context, R.drawable.ic_star));
                break;
            case HISTORY:
                holder.vIcon.setVisibility(View.VISIBLE);
                holder.vIcon.setImageDrawable(
                        ContextCompat.getDrawable(context, R.drawable.ic_history));
                break;
            default:
                // No default icon
                break;
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onRecyclerItemClick(StationSuggestionsCardAdapter.this, suggestion);
                }
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (longClickListener != null) {
                    longClickListener.onRecyclerItemLongClick(StationSuggestionsCardAdapter.this, suggestion);
                }
                return false;
            }
        });
    }

    public void setSuggestedStations(List<Suggestion<IrailLiveboardRequest>> suggestedStations) {
        this.suggestedStations = suggestedStations;
        this.notifyDataSetChanged();
    }

    public void setSearchResultStations(Station[] stations) {
        this.stations = stations;
        this.notifyDataSetChanged();
    }

    public void setSearchResultType(stationType type) {
        this.currentDisplayType = type;
        this.notifyDataSetChanged();
    }

    public void setSuggestionsVisible(boolean visible) {
        this.showSuggestions = visible;
        this.notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnRecyclerItemClickListener<Suggestion<IrailLiveboardRequest>> listener) {
        this.listener = listener;
    }

    public void setOnLongItemClickListener(OnRecyclerItemLongClickListener<Suggestion<IrailLiveboardRequest>> listener) {
        this.longClickListener = listener;
    }

    @Override
    public int getItemCount() {
        int result = 0;

        if (stations != null) {
            result += stations.length;
        }
        if (showSuggestions && suggestedStations != null) {
            result += suggestedStations.size();
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

            vStation = v.findViewById(R.id.text_station);
            vIcon = v.findViewById(R.id.image_right);
        }
    }
}

