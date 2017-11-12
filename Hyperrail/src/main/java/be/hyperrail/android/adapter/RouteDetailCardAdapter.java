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
import android.os.Bundle;
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

import java.util.Objects;

import be.hyperrail.android.OccupancyDialog;
import be.hyperrail.android.R;
import be.hyperrail.android.irail.implementation.Message;
import be.hyperrail.android.irail.implementation.OccupancyHelper;
import be.hyperrail.android.irail.implementation.Route;
import be.hyperrail.android.irail.implementation.TrainStub;
import be.hyperrail.android.irail.implementation.Transfer;
import be.hyperrail.android.util.DurationFormatter;

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
                itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_route_detail_transfer, parent, false);
            } else if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("use_card_layout", false)) {
                itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.listview_route_detail_transfer, parent, false);
            } else {
                itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_route_detail_transfer, parent, false);
            }

            return new RouteTransferViewHolder(itemView);

        } else if (viewType == VIEW_TYPE_TRAIN) {
            View itemView;

            if (embedded) {
                itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_route_detail_train, parent, false);
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

        DateTimeFormatter hhmm = DateTimeFormat.forPattern("HH:mm");

        if (holder instanceof RouteTransferViewHolder) {
            // Create a transfer ViewHolder

            RouteTransferViewHolder routeTransferViewHolder = (RouteTransferViewHolder) holder;
            // even (0,2,4...) : stations
            final Transfer transfer = route.getTransfers()[position / 2];

            routeTransferViewHolder.vStation.setText(transfer.getStation().getLocalizedName());

            // If we have have both and arrival and a departure, set the duration
            if (transfer.getArrivalTime() != null && transfer.getDepartureTime() != null) {
                routeTransferViewHolder.vWaitingTime.setText(
                        DurationFormatter.formatDuration(
                                transfer.getArrivalTime(), transfer.getArrivalDelay(),
                                transfer.getDepartureTime(), transfer.getDepartureDelay()
                        ));

            } else {
                routeTransferViewHolder.vWaitingTime.setText("");
                routeTransferViewHolder.vWaitingTimeContainer.setVisibility(View.GONE);
            }

            // if we have a departure, set the departure
            if (transfer.getDepartureTime() != null) {
                routeTransferViewHolder.vDepartureTime.setText(hhmm.print(transfer.getDepartureTime()));
            } else {
                routeTransferViewHolder.vDepartureTime.setText("");
                routeTransferViewHolder.vDepartureContainer.setVisibility(View.GONE);
            }

            // if we have a departure delay, set the departure delay
            if (transfer.getDepartureDelay().getStandardSeconds() > 0) {
                routeTransferViewHolder.vDepartureDelay.setText(context.getString(R.string.delay, transfer.getDepartureDelay().getStandardMinutes()));
            } else {
                routeTransferViewHolder.vDepartureDelay.setText("");
            }

            // if we have an arrival time, set the arrival time
            if (transfer.getArrivalTime() != null) {
                routeTransferViewHolder.vArrivalTime.setText(hhmm.print(transfer.getArrivalTime()));
            } else {
                routeTransferViewHolder.vArrivalTime.setText("");
                routeTransferViewHolder.vArrivalContainer.setVisibility(View.GONE);
            }

            // if we have an arrival delay, set the arrival delay
            if (transfer.getArrivalDelay().getStandardSeconds() > 0) {
                routeTransferViewHolder.vArrivalDelay.setText(context.getString(R.string.delay, transfer.getArrivalDelay().getStandardMinutes()));
            } else {
                routeTransferViewHolder.vArrivalDelay.setText("");
            }

            // If we have a departure platform
            if (transfer.getDeparturePlatform() != null) {
                routeTransferViewHolder.vDeparturePlatform.setText(transfer.getDeparturePlatform());

                // if cancelled, show icon
                if (transfer.isDepartureCanceled()) {
                    routeTransferViewHolder.vDeparturePlatform.setText("");
                    routeTransferViewHolder.vDeparturePlatformContainer.setBackground(ContextCompat.getDrawable(context, R.drawable.platform_train_canceled));
                } else {
                    routeTransferViewHolder.vDeparturePlatformContainer.setBackground(ContextCompat.getDrawable(context, R.drawable.platform_train));

                    if (!transfer.isArrivalPlatformNormal()) {
                        // if platform changed, mark in red
                        Drawable drawable = routeTransferViewHolder.vDeparturePlatformContainer.getBackground();
                        drawable.mutate();
                        drawable.setColorFilter(ContextCompat.getColor(context, R.color.colorDelay), PorterDuff.Mode.SRC_ATOP);
                    }
                }

            } else {
                // no departure platform
                routeTransferViewHolder.vDeparturePlatform.setText("");
                routeTransferViewHolder.vDeparturePlatformContainer.setVisibility(View.GONE);
            }

            if (transfer.getArrivalPlatform() != null) {
                routeTransferViewHolder.vArrivalPlatform.setText(transfer.getArrivalPlatform());

                // if cancelled, show icon
                if (transfer.isArrivalCanceled()) {
                    routeTransferViewHolder.vArrivalPlatform.setText("");
                    routeTransferViewHolder.vArrivalPlatformContainer.setBackground(ContextCompat.getDrawable(context, R.drawable.platform_train_canceled));
                } else {
                    routeTransferViewHolder.vArrivalPlatformContainer.setBackground(ContextCompat.getDrawable(context, R.drawable.platform_train));
                    if (!transfer.isArrivalPlatformNormal()) {
                        // if platform changed, mark in red
                        Drawable drawable = routeTransferViewHolder.vArrivalPlatformContainer.getBackground();
                        drawable.mutate();
                        drawable.setColorFilter(ContextCompat.getColor(context, R.color.colorDelay), PorterDuff.Mode.SRC_ATOP);
                    }
                }

            } else {
                // no arrival platform
                routeTransferViewHolder.vArrivalPlatform.setText("");
                routeTransferViewHolder.vArrivalPlatformContainer.setVisibility(View.GONE);
            }

            if (position == 0) {
                if (transfer.hasLeft()) {
                    routeTransferViewHolder.vTimeline.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_departure_filled));
                } else {
                    routeTransferViewHolder.vTimeline.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_departure_hollow));
                }
            } else if (position == this.getItemCount() - 1) {
                if (transfer.hasArrived()) {
                    routeTransferViewHolder.vTimeline.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_arrival_filled));
                } else {
                    routeTransferViewHolder.vTimeline.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_arrival_hollow));
                }
            } else {
                if (transfer.hasArrived()) {
                    if (transfer.hasLeft()) {
                        routeTransferViewHolder.vTimeline.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_transfer_filled));
                    } else {
                        routeTransferViewHolder.vTimeline.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_transfer_inprogress));
                    }
                } else {
                    routeTransferViewHolder.vTimeline.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_transfer_hollow));
                }
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onRecyclerItemClick(RouteDetailCardAdapter.this, transfer);
                    }
                }
            });

        } else if (holder instanceof RouteTrainViewHolder) {
            // Create a train ViewHolder
            RouteTrainViewHolder routeTrainViewHolder = (RouteTrainViewHolder) holder;

            // odd (1,3,...) : route between stations
            final Transfer transferBefore = route.getTransfers()[(position - 1) / 2];
            final Transfer transferAfter = route.getTransfers()[(position + 1) / 2];
            final TrainStub train = route.getTrains()[(position - 1) / 2];

            boolean isWalking = Objects.equals(train.getId(), "WALK");

            if (isWalking) {
                routeTrainViewHolder.vDirection.setText(R.string.walk_heading);
                routeTrainViewHolder.vTrainType.setVisibility(View.GONE);
                routeTrainViewHolder.vTrainNumber.setText(R.string.walk_description);
                routeTrainViewHolder.vOccupancy.setVisibility(View.GONE);
                if (transferBefore.hasArrived()) {
                    routeTrainViewHolder.vTimeline.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_walk_filled));
                } else {
                    routeTrainViewHolder.vTimeline.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_walk_hollow));
                }
            } else {
                routeTrainViewHolder.vTrainNumber.setText(train.getNumber());
                routeTrainViewHolder.vTrainType.setText(train.getType());
                routeTrainViewHolder.vOccupancy.setVisibility(View.VISIBLE);
                routeTrainViewHolder.vTrainType.setVisibility(View.VISIBLE);
                routeTrainViewHolder.vDirection.setText(train.getDirection().getLocalizedName());

                if (transferBefore.hasLeft()) {
                    if (transferAfter.hasArrived()) {
                        routeTrainViewHolder.vTimeline.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_train_filled));
                        routeTrainViewHolder.vTimeline2.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.timeline_continuous_filled));
                    } else {
                        routeTrainViewHolder.vTimeline.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_train_inprogress));
                        routeTrainViewHolder.vTimeline2.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.timeline_continuous_hollow));
                    }
                } else {
                    routeTrainViewHolder.vTimeline.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.timeline_train_hollow));
                    routeTrainViewHolder.vTimeline2.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.timeline_continuous_hollow));
                }
            }

            routeTrainViewHolder.vDuration.setText(DurationFormatter.formatDuration(transferBefore.getDepartureTime(), transferBefore.getDepartureDelay(), transferAfter.getArrivalTime(), transferAfter.getArrivalDelay()));

            if (transferBefore.isDepartureCanceled()) {
                routeTrainViewHolder.vStatusText.setText(R.string.status_cancelled);
                routeTrainViewHolder.vStatusContainer.setVisibility(View.VISIBLE);
                routeTrainViewHolder.vOccupancy.setVisibility(View.GONE);
            } else {
                routeTrainViewHolder.vOccupancy.setVisibility(View.VISIBLE);
                routeTrainViewHolder.vStatusContainer.setVisibility(View.GONE);
            }

            Message[] trainAlerts = route.getTrainalerts()[(position - 1) / 2];
            if (trainAlerts != null && trainAlerts.length > 0) {
                routeTrainViewHolder.vAlertContainer.setVisibility(View.VISIBLE);

                StringBuilder text = new StringBuilder();
                int n = trainAlerts.length;
                for (int i = 0; i < n; i++) {
                    text.append(trainAlerts[i].getHeader());
                    if (i < n - 1) {
                        text.append("\n");
                    }
                }

                routeTrainViewHolder.vAlertText.setText(text.toString());
            } else {
                routeTrainViewHolder.vAlertContainer.setVisibility(View.GONE);
            }

            routeTrainViewHolder.vOccupancy.setImageDrawable(ContextCompat.getDrawable(context, OccupancyHelper.getOccupancyDrawable(transferBefore.getDepartureOccupancy())));

            if (!isWalking) {
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Bundle bundle = new Bundle();
                        bundle.putSerializable("train", train);
                        // Get the departure date (day) of this train
                        bundle.putSerializable("date", transferBefore.getDepartureTime());
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
                                (new OccupancyDialog(RouteDetailCardAdapter.this.context,
                                        transferBefore.getDepartureConnectionSemanticId(),
                                        transferBefore.getStation().getSemanticId(),
                                        train.getSemanticId(),
                                        transferBefore.getDepartureTime())
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

    public void clearOnItemClickListener() {
        this.listener = null;
    }

    /**
     * Train ViewHolder, showing train, status, duration, direction
     */
    private class RouteTrainViewHolder extends RecyclerView.ViewHolder {

        final TextView vDirection;
        final TextView vDuration;
        final TextView vTrainType;
        final TextView vTrainNumber;

        final LinearLayout vStatusContainer;
        final TextView vStatusText;

        final ImageView vOccupancy;
        final ImageView vTimeline;
        final ImageView vTimeline2;
        final LinearLayout vAlertContainer;
        final TextView vAlertText;

        RouteTrainViewHolder(View view) {
            super(view);

            vDirection = view.findViewById(R.id.text_direction);
            vDuration = view.findViewById(R.id.text_duration);

            vTrainNumber = view.findViewById(R.id.text_train_number);
            vTrainType = view.findViewById(R.id.text_train_type);

            vStatusContainer = view.findViewById(R.id.layout_train_status_container);
            vStatusText = view.findViewById(R.id.text_train_status);

            vOccupancy = view.findViewById(R.id.image_occupancy);

            vAlertContainer = view.findViewById(R.id.alert_container);
            vAlertText = view.findViewById(R.id.alert_message);

            vTimeline = view.findViewById(R.id.image_timeline);
            vTimeline2 = view.findViewById(R.id.image_timeline_2);
        }

    }

    /**
     * Transfer ViewHolder, showing station, waiting time, arrival, departure, delay, platforms, timeline
     */
    private class RouteTransferViewHolder extends RecyclerView.ViewHolder {

        protected final TextView vDepartureTime;
        protected final LinearLayout vDepartureContainer;
        protected final TextView vDepartureDelay;

        protected final TextView vArrivalTime;
        protected final LinearLayout vArrivalContainer;
        protected final TextView vArrivalDelay;

        protected final TextView vArrivalPlatform;
        protected final LinearLayout vArrivalPlatformContainer;
        protected final TextView vDeparturePlatform;
        protected final LinearLayout vDeparturePlatformContainer;

        protected final TextView vStation;
        protected final TextView vWaitingTime;
        protected final LinearLayout vWaitingTimeContainer;

        protected final ImageView vTimeline;

        RouteTransferViewHolder(View view) {
            super(view);

            vDepartureTime = view.findViewById(R.id.text_departure_time);
            vDepartureContainer = view.findViewById(R.id.text_departure_container);
            vDepartureDelay = view.findViewById(R.id.text_departure_delay);

            vArrivalTime = view.findViewById(R.id.text_arrival_time);
            vArrivalContainer = view.findViewById(R.id.text_arrival_container);
            vArrivalDelay = view.findViewById(R.id.text_arrival_delay);

            vArrivalPlatform = view.findViewById(R.id.text_platform_arrival);
            vArrivalPlatformContainer = view.findViewById(R.id.layout_platform_arrival_container);
            vDeparturePlatform = view.findViewById(R.id.text_platform_departure);
            vDeparturePlatformContainer = view.findViewById(R.id.layout_platform_departure_container);

            vStation = view.findViewById(R.id.text_station);
            vWaitingTime = view.findViewById(R.id.text_waiting_time);
            vWaitingTimeContainer = view.findViewById(R.id.cardview_detail_1);

            vTimeline = view.findViewById(R.id.image_timeline);
        }

    }
}

