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
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import be.hyperrail.android.R;
import be.hyperrail.opentransportdata.common.models.Disturbance;

/**
 * Recyclerview adapter to show a list with disturbances on the net
 */
public class DisturbanceCardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_DISTURBANCE = 0;
    private static final int VIEW_TYPE_NO_RESULTS = 1;
    private final Context context;
    private Disturbance[] disturbances;
    private OnRecyclerItemClickListener<Disturbance> listener;

    public DisturbanceCardAdapter(Context context, Disturbance[] disturbances) {
        this.context = context;
        this.disturbances = disturbances;
    }

    public void updateDisturbances(Disturbance[] disturbances) {
        this.disturbances = disturbances;
        super.notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if ((this.disturbances == null || this.disturbances.length == 0) && position == 0) {
            return VIEW_TYPE_NO_RESULTS;
        } else {
            return VIEW_TYPE_DISTURBANCE;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView;

        if (viewType == VIEW_TYPE_NO_RESULTS) {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.listview_no_disturbances, parent, false);
            return new NoResultsViewHolder(itemView);
        }

        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("use_card_layout", false)) {
            itemView = LayoutInflater.from(parent.getContext()).inflate(be.hyperrail.android.R.layout.listview_disturbance, parent, false);
        } else {
            itemView = LayoutInflater.from(parent.getContext()).inflate(be.hyperrail.android.R.layout.cardview_disturbance, parent, false);
        }

        return new DisturbanceViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewholder, int position) {

        if (viewholder instanceof DisturbanceViewHolder) {
            DisturbanceViewHolder holder = (DisturbanceViewHolder) viewholder;
            final Disturbance disturbance = disturbances[position];

            holder.vTitle.setText(disturbance.getTitle());
            holder.vDescription.setText(Html.fromHtml(disturbance.getDescription()));

            DateTimeFormatter df = DateTimeFormat.forPattern("EEE dd/MM/yy HH:mm");
            holder.vDate.setText(df.print(disturbance.getTime()));

            holder.vDetailsContainer.setVisibility(View.GONE);
            holder.vCollapseToggle.setOnClickListener(v -> {
                if (holder.vDetailsContainer.getVisibility() == View.GONE) {
                    holder.vDetailsContainer.setVisibility(View.VISIBLE);
                    holder.vCollapseToggle.setImageResource(R.drawable.ic_unfold_less);
                } else {
                    holder.vDetailsContainer.setVisibility(View.GONE);
                    holder.vCollapseToggle.setImageResource(R.drawable.ic_unfold_more);
                }
            });

            if (disturbance.getType() == Disturbance.Type.DISTURBANCE) {
                // Important information is shown directly
                holder.vDetailsContainer.setVisibility(View.VISIBLE);
                holder.vCollapseToggle.setImageResource(R.drawable.ic_unfold_less);
            }

            if (disturbance.getType() == Disturbance.Type.PLANNED) {
                holder.vTypeIcon.setImageResource(R.drawable.ic_query_builder);
                holder.vTypeIcon.setColorFilter(R.color.colorMuted);
            } else {
                holder.vTypeIcon.setImageResource(R.drawable.ic_action_warning);
                holder.vTypeIcon.setColorFilter(R.color.colorDelay);
            }

            holder.itemView.setOnClickListener(view -> {
                if (listener != null) {
                    listener.onRecyclerItemClick(DisturbanceCardAdapter.this, disturbance);
                }
            });
        }

        // If placeholder: no binding required
    }

    @Override
    public int getItemCount() {
        if (disturbances == null || disturbances.length == 0) {
            return 1;
        }
        return disturbances.length;
    }

    public void setOnItemClickListener(OnRecyclerItemClickListener<Disturbance> listener) {
        this.listener = listener;
    }

    class DisturbanceViewHolder extends RecyclerView.ViewHolder {

        final ImageView vTypeIcon;
        final ImageView vCollapseToggle;
        final View vDetailsContainer;
        final TextView vTitle;
        final TextView vDescription;
        final TextView vDate;

        DisturbanceViewHolder(View view) {
            super(view);
            vTitle = view.findViewById(R.id.text_title);
            vDescription = view.findViewById(R.id.text_description);
            vDate = view.findViewById(R.id.text_date);
            vTypeIcon = view.findViewById(R.id.img_disturbance_type);
            vCollapseToggle = view.findViewById(R.id.img_toggle_collapse);
            vDetailsContainer = view.findViewById(R.id.container_details);
        }
    }

    class NoResultsViewHolder extends RecyclerView.ViewHolder {
        NoResultsViewHolder(View view) {
            super(view);
        }
    }
}

