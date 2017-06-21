/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package android.irail.be.hyperrail.adapter;

import android.content.Context;
import android.irail.be.hyperrail.R;
import android.irail.be.hyperrail.irail.db.Station;
import android.irail.be.hyperrail.irail.factories.IrailFactory;
import android.irail.be.hyperrail.persistence.RouteQuery;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Recyclerview to show stations (for searches, recents ,...)
 */
public class StationCardAdapter extends RecyclerView.Adapter<StationCardAdapter.StationViewHolder> {

    private final Context context;
    private RouteQuery[] suggestedStations;
    private Station[] stations;
    private boolean showSuggestions = true;
    private boolean nearbyOnTop;

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

    private stationType currentType = stationType.UNDEFINED;

    private onRecyclerItemClickListener<Station> listener;

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

        if ((showSuggestions && position < suggestedStationsLength && !nearbyOnTop) || showSuggestions && position >= stations.length && nearbyOnTop) {

            if (nearbyOnTop) {
                position = position - stations.length;
            }

            final RouteQuery q = suggestedStations[position];

            holder.vStation.setText(q.from);

            switch (q.type) {
                case FAVORITE_STATION:
                    holder.vIcon.setVisibility(View.VISIBLE);
                    holder.vIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_star));
                    break;
                case RECENT_STATION:
                    holder.vIcon.setVisibility(View.VISIBLE);
                    holder.vIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_history));
                    break;
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onRecyclerItemClick(StationCardAdapter.this, IrailFactory.getStationsProviderInstance().getStationByName(q.from));
                    }
                }
            });
        } else {

            if (!nearbyOnTop) {
                position = position - suggestedStationsLength;
            }

            final Station s = stations[position];

            holder.vStation.setText(s.getLocalizedName());

            switch (currentType) {
                case UNDEFINED:
                    holder.vIcon.setVisibility(View.INVISIBLE);
                    break;
                case NEARBY:
                    holder.vIcon.setVisibility(View.VISIBLE);
                    holder.vIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_location_on_white));
                    break;
                case SEARCHED:
                    holder.vIcon.setVisibility(View.VISIBLE);
                    holder.vIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_action_search_white));
                    break;
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onRecyclerItemClick(StationCardAdapter.this, s);
                    }
                }
            });
        }
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
        this.currentType = type;
        this.notifyDataSetChanged();
    }

    public void setSuggestionsVisible(boolean visible) {
        this.showSuggestions = visible;
        this.notifyDataSetChanged();
    }

    public void setOnItemClickListener(onRecyclerItemClickListener<Station> listener) {
        this.listener = listener;
    }

    public void clearOnItemClickListener() {
        this.listener = null;
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

        final TextView vStation;
        final ImageView vIcon;

        StationViewHolder(View v) {
            super(v);

            vStation = (TextView) v.findViewById(R.id.text_station);
            vIcon = (ImageView) v.findViewById(R.id.image_right);
        }
    }
}

