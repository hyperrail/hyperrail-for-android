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
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import be.hyperrail.android.R;
import be.hyperrail.android.persistence.Suggestion;
import be.hyperrail.android.persistence.SuggestionType;
import be.hyperrail.opentransportdata.common.contracts.QueryTimeDefinition;
import be.hyperrail.opentransportdata.common.models.LiveboardType;
import be.hyperrail.opentransportdata.common.models.StopLocation;
import be.hyperrail.opentransportdata.common.requests.LiveboardRequest;

/**
 * Recyclerview to show stations (for searches, recents ,...)
 */
public class StationSuggestionsCardAdapter extends RecyclerView.Adapter<StationSuggestionsCardAdapter.StationViewHolder> {

    private final Context context;
    private List<Suggestion<LiveboardRequest>> suggestedStations;
    private StopLocation[] stations;
    private boolean showSuggestions = true;
    private boolean nearbyOnTop;
    private OnRecyclerItemLongClickListener<Suggestion<LiveboardRequest>> longClickListener;
    private OnRecyclerItemClickListener<Suggestion<LiveboardRequest>> listener;

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

    public StationSuggestionsCardAdapter(Context context, StopLocation[] stations) {
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
        StopLocation station;

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

        LiveboardRequest request = new LiveboardRequest(station,
                QueryTimeDefinition.EQUAL_OR_LATER,
                LiveboardType.DEPARTURES,
                null);
        final Suggestion<LiveboardRequest> suggestion = new Suggestion<>(request, SuggestionType.LIST);

        holder.itemView.setOnClickListener(view -> {
            if (listener != null) {
                listener.onRecyclerItemClick(StationSuggestionsCardAdapter.this,
                        suggestion);
            }
        });

        holder.itemView.setOnLongClickListener(view -> {
            if (longClickListener != null) {
                longClickListener.onRecyclerItemLongClick(StationSuggestionsCardAdapter.this,
                        suggestion);
            }
            return false;
        });
    }

    private void bindSuggestionViewHolder(StationViewHolder holder, int position) {
        if (nearbyOnTop) {
            position = position - stations.length;
        }

        final Suggestion<LiveboardRequest> suggestion = suggestedStations.get(position);

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

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecyclerItemClick(StationSuggestionsCardAdapter.this, suggestion);
            }
        });

        holder.itemView.setOnLongClickListener(view -> {
            if (longClickListener != null) {
                longClickListener.onRecyclerItemLongClick(StationSuggestionsCardAdapter.this, suggestion);
            }
            return false;
        });
    }

    public void setSuggestedStations(List<Suggestion<LiveboardRequest>> suggestedStations) {
        this.suggestedStations = suggestedStations;
        this.notifyDataSetChanged();
    }

    public void setSearchResultStations(StopLocation[] stations) {
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

    public void setOnItemClickListener(OnRecyclerItemClickListener<Suggestion<LiveboardRequest>> listener) {
        this.listener = listener;
    }

    public void setOnLongItemClickListener(OnRecyclerItemLongClickListener<Suggestion<LiveboardRequest>> listener) {
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

        final TextView vStation;
        final ImageView vIcon;

        StationViewHolder(View v) {
            super(v);

            vStation = v.findViewById(R.id.text_station);
            vIcon = v.findViewById(R.id.image_right);
        }
    }
}

