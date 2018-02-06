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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Objects;

import be.hyperrail.android.R;
import be.hyperrail.android.TrainstopContextMenu;
import be.hyperrail.android.irail.implementation.Route;
import be.hyperrail.android.irail.implementation.TrainStub;
import be.hyperrail.android.irail.implementation.Transfer;
import be.hyperrail.android.viewgroup.RouteTrainItemLayout;
import be.hyperrail.android.viewgroup.RouteTransferItemLayout;

/**
 * RecyclerViewAdapter which shows a detailed timeline view for routes/connections,
 * showing all transfers and trains for 1 specific connection
 */
public class RouteDetailCardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    /**
     * Whether or not this view will be embedded (required to pick the correct layout)
     */
    private final boolean embedded;

    /**
     * The route to show
     */
    private final Route route;

    private final Context context;
    private OnRecyclerItemClickListener<Object> listener;

    private final int VIEW_TYPE_TRANSFER = 0;
    private final int VIEW_TYPE_TRAIN = 1;

    public RouteDetailCardAdapter(Context context, Route route, boolean embedded) {
        this.context = context;
        this.route = route;
        this.embedded = embedded;
    }

    @Override
    public int getItemViewType(int position) {
        // All even items are stations, all odd items are trains between stations
        if (position % 2 == 0) {
            return VIEW_TYPE_TRANSFER;
        } else {
            return VIEW_TYPE_TRAIN;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_TRANSFER) {
            View itemView;

            if (embedded) {
                itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.embedded_route_detail_transfer, parent, false);
            } else if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("use_card_layout", false)) {
                itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.listview_route_detail_transfer, parent, false);
            } else {
                itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_route_detail_transfer, parent, false);
            }

            return new RouteTransferViewHolder(itemView);

        } else if (viewType == VIEW_TYPE_TRAIN) {
            View itemView;

            if (embedded) {
                itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.embedded_route_detail_train, parent, false);
            } else if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("use_card_layout", false)) {
                itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.listview_route_detail_train, parent, false);
            } else {
                itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_route_detail_train, parent, false);
            }

            return new RouteTrainViewHolder(itemView);

        } else {
            return null;
        }
    }

    /**
     * Bind a ViewHolder instance
     * Note: since recyclerview recyclers, either set or unset fields, but don't leave them as-is!
     */
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {

        if (holder instanceof RouteTransferViewHolder) {
            // Create a transfer ViewHolder

            RouteTransferViewHolder routeTransferViewHolder = (RouteTransferViewHolder) holder;
            // even (0,2,4...) : stations
            final Transfer transfer = route.getTransfers()[position / 2];
            routeTransferViewHolder.routeTransferItemLayout.bind(context, transfer, route, position / 2);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onRecyclerItemClick(RouteDetailCardAdapter.this, transfer);
                    }
                }
            });

            if (transfer.getDepartingTrain() != null) {
                if (transfer.getArrivingTrain() != null) {
                    holder.itemView.setOnLongClickListener(
                            new View.OnLongClickListener() {
                                @Override
                                public boolean onLongClick(View view) {
                                    (new TrainstopContextMenu(RouteDetailCardAdapter.this.context,
                                            transfer,route)
                                    ).show();
                                    return false;
                                }

                            }
                    );
                } else {
                    holder.itemView.setOnLongClickListener(
                            new View.OnLongClickListener() {
                                @Override
                                public boolean onLongClick(View view) {
                                    (new TrainstopContextMenu(RouteDetailCardAdapter.this.context,
                                            transfer, null,route)
                                    ).show();
                                    return false;
                                }

                            }
                    );
                }
            } else {
                if (transfer.getArrivingTrain() != null) {
                    holder.itemView.setOnLongClickListener(
                            new View.OnLongClickListener() {
                                @Override
                                public boolean onLongClick(View view) {
                                    (new TrainstopContextMenu(RouteDetailCardAdapter.this.context,
                                            null, transfer,route)
                                    ).show();
                                    return false;
                                }

                            }
                    );
                }
            }

        } else if (holder instanceof RouteTrainViewHolder) {
            // odd (1,3,...) : route between stations
            final Transfer transferBefore = route.getTransfers()[(position - 1) / 2];
            final Transfer transferAfter = route.getTransfers()[(position + 1) / 2];
            final TrainStub train = route.getTrains()[(position - 1) / 2];

            boolean isWalking = Objects.equals(train.getId(), "WALK");

            ((RouteTrainViewHolder)holder).routeTrainItemLayout.bind(context,train,route,(position - 1) / 2);

            if (!isWalking) {
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Bundle bundle = new Bundle();
                        bundle.putSerializable("train", train);
                        // Get the departure date (day) of this train
                        bundle.putSerializable("date", transferBefore.getDepartureTime());
                        // TODO: consider if these should be included
                        bundle.putSerializable("from", transferBefore.getStation());
                        bundle.putSerializable("to", transferAfter.getStation());

                        if (listener != null) {
                            listener.onRecyclerItemClick(RouteDetailCardAdapter.this, bundle);
                        }
                    }
                });

                holder.itemView.setOnLongClickListener(
                        new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View view) {
                                (new TrainstopContextMenu(RouteDetailCardAdapter.this.context,
                                        transferBefore, transferAfter,route)
                                ).show();
                                return false;
                            }

                        }
                );
            }
        }

    }

    @Override
    public int getItemCount() {
        if (route == null) {
            return 0;
        }
        if (route.getTransfers() == null) {
            return route.getTrains().length;
        }
        return route.getTrains().length + route.getTransfers().length;
    }

    public void setOnItemClickListener(OnRecyclerItemClickListener<Object> listener) {
        this.listener = listener;
    }

    /**
     * Train ViewHolder, showing train, status, duration, direction
     */
    private class RouteTrainViewHolder extends RecyclerView.ViewHolder {

        RouteTrainItemLayout routeTrainItemLayout;

        RouteTrainViewHolder(View view) {
            super(view);
            routeTrainItemLayout = view.findViewById(R.id.binder);

        }

    }

    /**
     * Transfer ViewHolder, showing station, waiting time, arrival, departure, delay, platforms, timeline
     */
    private class RouteTransferViewHolder extends RecyclerView.ViewHolder {

        RouteTransferItemLayout routeTransferItemLayout;

        RouteTransferViewHolder(View view) {
            super(view);
            routeTransferItemLayout = view.findViewById(R.id.binder);
        }

    }
}

