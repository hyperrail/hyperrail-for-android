/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android;

import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.LinearLayout;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import be.hyperrail.android.activities.searchResult.LiveboardActivity;
import be.hyperrail.android.activities.searchResult.VehicleActivity;
import be.hyperrail.android.irail.contracts.IRailErrorResponseListener;
import be.hyperrail.android.irail.contracts.IRailSuccessResponseListener;
import be.hyperrail.android.irail.contracts.IrailDataProvider;
import be.hyperrail.android.irail.contracts.OccupancyLevel;
import be.hyperrail.android.irail.contracts.RouteTimeDefinition;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.irail.implementation.Route;
import be.hyperrail.android.irail.implementation.Transfer;
import be.hyperrail.android.irail.implementation.TransferType;
import be.hyperrail.android.irail.implementation.VehicleStop;
import be.hyperrail.android.irail.implementation.requests.IrailLiveboardRequest;
import be.hyperrail.android.irail.implementation.requests.IrailPostOccupancyRequest;
import be.hyperrail.android.irail.implementation.requests.IrailVehicleRequest;
import be.hyperrail.android.viewgroup.NotificationLayoutBuilder;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * This dialogs allows users to submit occupancy data, pin notifications or send ETA messages for transfers and trains and train stops.
 */
public class VehiclePopupContextMenu {

    private static final int TYPE_TRANSFER = 0;
    private static final int TYPE_TRAIN_STOP = 1;
    private static final int TYPE_ROUTE_TRAIN = 2;

    private static final String NOTIFICATION_CHANNEL_GLIMPSE = "glimpse";
    private final View mActivityView;
    private final Transfer mDepartureTransfer;
    private final Route route;
    private final VehicleStop mVehicleStop;
    private final Transfer mArrivalTransfer;
    private final Activity mContext;

    private int type;

    public VehiclePopupContextMenu(@NonNull Activity context, @NonNull VehicleStop stop) {
        this.mContext = context;
        this.mActivityView = context.getWindow().getDecorView().findViewById(android.R.id.content);

        this.route = null;
        this.mVehicleStop = stop;
        this.mArrivalTransfer = null;
        this.mDepartureTransfer = null;

        this.type = TYPE_TRAIN_STOP;
    }

    public VehiclePopupContextMenu(@NonNull Activity context, @NonNull Transfer transfer, @NonNull Route route) {
        this.mContext = context;
        this.mActivityView = context.getWindow().getDecorView().findViewById(android.R.id.content);

        this.mArrivalTransfer = transfer;
        this.mDepartureTransfer = transfer;
        this.route = route;
        this.mVehicleStop = null;

        this.type = TYPE_TRANSFER;
    }

    public VehiclePopupContextMenu(@NonNull Activity context, @NonNull Transfer departureTransfer, @NonNull Transfer arrivalTransfer, @NonNull Route route) {
        this.mContext = context;
        this.mActivityView = context.getWindow().getDecorView().findViewById(android.R.id.content);

        this.mArrivalTransfer = arrivalTransfer;
        this.mDepartureTransfer = departureTransfer;
        this.route = route;
        this.mVehicleStop = null;

        this.type = TYPE_ROUTE_TRAIN;
    }


    public void show() {
        final Dialog vDialog = new Dialog(this.mContext, R.style.TrainLongClickDialog);
        final IrailDataProvider mApiInstance = IrailFactory.getDataProviderInstance();

        vDialog.setContentView(R.layout.contextmenu_spitsgids);

        final LinearLayout vShareDepartureEta = vDialog.findViewById(
                R.id.button_share_departure_ETA);
        final LinearLayout vShareArrivalEta = vDialog.findViewById(R.id.button_share_arrival_ETA);
        final LinearLayout vSetNotification = vDialog.findViewById(R.id.button_notification);

        final LinearLayout vOccupancyContainer = vDialog.findViewById(R.id.container_occupancy);

        final String mDepartureConnection;
        final String mStationSemanticId;
        final String mVehicleSemanticId;
        final DateTime mDateTime;
        final String mArrivalEtaText;
        final String mDepartureEtaText;


        if (type == TYPE_TRAIN_STOP) {
            vDialog.setTitle(mVehicleStop.getStation().getLocalizedName());

            mDepartureConnection = mVehicleStop.getDepartureSemanticId();
            mStationSemanticId = mVehicleStop.getStation().getSemanticId();
            mVehicleSemanticId = mVehicleStop.getTrain().getSemanticId();
            mDateTime = mVehicleStop.getDepartureTime();
            setOccupancyButtons(vDialog, mApiInstance, mDepartureConnection, mStationSemanticId,
                                mVehicleSemanticId, mDateTime);

            vDialog.setTitle(mVehicleStop.getTrain().getName() + " " +
                                     mVehicleStop.getStation().getLocalizedName());

            mDepartureEtaText = String.format(mContext.getString(R.string.ETA_stop_departure),
                                              DateTimeFormat.forPattern("hh:mm").print(
                                                      mVehicleStop.getDelayedDepartureTime()),
                                              mVehicleStop.getStation().getLocalizedName(),
                                              mVehicleStop.getTrain().getName());
            mArrivalEtaText = String.format(mContext.getString(R.string.ETA_stop_arrival),
                                            DateTimeFormat.forPattern("hh:mm").print(
                                                    mVehicleStop.getDelayedArrivalTime()),
                                            mVehicleStop.getStation().getLocalizedName(),
                                            mVehicleStop.getTrain().getName());

        } else if (type == TYPE_TRANSFER) {

            vDialog.setTitle(mDepartureTransfer.getStation().getLocalizedName());

            if (mDepartureTransfer.getType() == TransferType.DEPARTURE || mDepartureTransfer.getType() == TransferType.TRANSFER) {
                mDepartureConnection = mDepartureTransfer.getDepartureSemanticId();
                mStationSemanticId = mDepartureTransfer.getStation().getSemanticId();
                mVehicleSemanticId = mDepartureTransfer.getDepartingRouteLeg().getVehicleInformation().getSemanticId();
                mDateTime = mDepartureTransfer.getDepartureTime();
                setOccupancyButtons(vDialog, mApiInstance, mDepartureConnection, mStationSemanticId,
                                    mVehicleSemanticId, mDateTime);

                mDepartureEtaText = String.format(
                        mContext.getString(R.string.ETA_transfer_departure),
                        DateTimeFormat.forPattern("hh:mm").print(
                                mDepartureTransfer.getDelayedDepartureTime()),
                        mDepartureTransfer.getStation().getLocalizedName(),
                        mDepartureTransfer.getDepartingRouteLeg().getVehicleInformation().getName());
            } else {
                mDepartureEtaText = null;
            }

            if (mDepartureTransfer.getType() == TransferType.ARRIVAL || mDepartureTransfer.getType() == TransferType.TRANSFER) {
                mArrivalEtaText = String.format(mContext.getString(R.string.ETA_transfer_arrival),
                                                DateTimeFormat.forPattern("hh:mm").print(
                                                        mArrivalTransfer.getDelayedArrivalTime()),
                                                mArrivalTransfer.getStation().getLocalizedName(),
                                                mArrivalTransfer.getArrivingRouteLeg().getVehicleInformation().getName());
            } else {
                mArrivalEtaText = null;
            }

        } else if (type == TYPE_ROUTE_TRAIN) {
            vDialog.setTitle(
                    mDepartureTransfer.getDepartingRouteLeg().getVehicleInformation().getName() + " " +
                            mDepartureTransfer.getStation().getLocalizedName() + "-" + mArrivalTransfer.getStation().getLocalizedName());

            // Occupancy + departure ETA
            if (!mDepartureTransfer.getDepartingRouteLeg().getVehicleInformation().getId().equals(
                    "WALK")) {

                mDepartureConnection = mDepartureTransfer.getDepartureSemanticId();
                mStationSemanticId = mDepartureTransfer.getStation().getSemanticId();
                mVehicleSemanticId = mDepartureTransfer.getDepartingRouteLeg().getVehicleInformation().getSemanticId();
                mDateTime = mDepartureTransfer.getDepartureTime();
                setOccupancyButtons(vDialog, mApiInstance, mDepartureConnection, mStationSemanticId,
                                    mVehicleSemanticId, mDateTime);

                mDepartureEtaText = String.format(
                        mContext.getString(R.string.ETA_transfer_departure),
                        DateTimeFormat.forPattern("hh:mm").print(
                                mDepartureTransfer.getDelayedDepartureTime()),
                        mDepartureTransfer.getStation().getLocalizedName(),
                        mDepartureTransfer.getDepartingRouteLeg().getVehicleInformation().getName());
            } else {
                vOccupancyContainer.setVisibility(View.GONE);
                mDepartureEtaText = null;
            }

            if (!mArrivalTransfer.getArrivingRouteLeg().getVehicleInformation().getId().equals(
                    "WALK")) {
                mArrivalEtaText = String.format(mContext.getString(R.string.ETA_transfer_arrival),
                                                DateTimeFormat.forPattern("hh:mm").print(
                                                        mArrivalTransfer.getDelayedArrivalTime()),
                                                mArrivalTransfer.getStation().getLocalizedName(),
                                                mArrivalTransfer.getArrivingRouteLeg().getVehicleInformation().getName());
            } else {
                mArrivalEtaText = null;
            }
        } else {
            mDepartureEtaText = null;
            mArrivalEtaText = null;
        }

        // When this contextmenu was called on a train stop or transfer in a route (but not on a train in a route!)
        if (type == TYPE_TRAIN_STOP || type == TYPE_TRANSFER) {
            setNotification(vDialog, vSetNotification);
        } else {
            vSetNotification.setVisibility(View.GONE);
        }

        if (mArrivalEtaText == null) {
            vShareArrivalEta.setVisibility(View.GONE);
        } else {
            vShareArrivalEta.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent sendIntent = new Intent(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, mArrivalEtaText);
                    sendIntent.setType("text/plain");
                    mContext.startActivity(sendIntent);
                    vDialog.dismiss();
                }
            });
        }

        if (mDepartureEtaText == null) {
            vShareDepartureEta.setVisibility(View.GONE);
        } else {
            vShareDepartureEta.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent sendIntent = new Intent(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, mDepartureEtaText);
                    sendIntent.setType("text/plain");
                    mContext.startActivity(sendIntent);
                    vDialog.dismiss();
                }
            });
        }

        vDialog.show();
    }

    private void setNotification(final Dialog vDialog, LinearLayout vSetNotification) {
        vSetNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NotificationManager mNotifyMgr =
                        (NotificationManager) mContext.getSystemService(NOTIFICATION_SERVICE);

                if (mNotifyMgr == null) {
                    return;
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel notificationChannel = new NotificationChannel(
                            NOTIFICATION_CHANNEL_GLIMPSE,
                            "Pinned departures", NotificationManager.IMPORTANCE_DEFAULT);

                    // Configure the notification channel.
                    notificationChannel.setDescription("Glimpse at pinned departures");
                    notificationChannel.enableLights(false);
                    notificationChannel.setSound(null, null);
                    notificationChannel.setLightColor(Color.RED);
                    notificationChannel.setVibrationPattern(null);
                    notificationChannel.enableVibration(false);
                    mNotifyMgr.createNotificationChannel(notificationChannel);
                }

                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(mContext, NOTIFICATION_CHANNEL_GLIMPSE)
                                .setSmallIcon(R.drawable.ic_hyperrail_notification);

                Intent resultIntent;

                mBuilder.setColor(ContextCompat.getColor(mContext, R.color.colorPrimary));
                if (mVehicleStop != null) {
                    mBuilder.setCustomBigContentView(
                            NotificationLayoutBuilder.createNotificationLayout(mContext,
                                                                               mVehicleStop));
                    mBuilder.setSubText(
                            "Vehicle at  " + mVehicleStop.getStation().getLocalizedName() + " towards " + mVehicleStop.getTrain().getDirection().getLocalizedName());
                    resultIntent = VehicleActivity.createIntent(mContext, new IrailVehicleRequest(
                            mVehicleStop.getTrain().getId(), mVehicleStop.getDepartureTime()));

                } else {
                    if (mDepartureTransfer != null) {
                        mBuilder.setSubText(
                                "Transfer at  " + mDepartureTransfer.getStation().getLocalizedName());
                        resultIntent = LiveboardActivity.createIntent(mContext,
                                                                      new IrailLiveboardRequest(
                                                                              mDepartureTransfer.getStation(),
                                                                              RouteTimeDefinition.DEPART,
                                                                              mDepartureTransfer.getArrivalTime()));
                    } else {
                        mBuilder.setSubText(
                                "Transfer at  " + mArrivalTransfer.getStation().getLocalizedName());
                        resultIntent = LiveboardActivity.createIntent(mContext,
                                                                      new IrailLiveboardRequest(
                                                                              mArrivalTransfer.getStation(),
                                                                              RouteTimeDefinition.DEPART,
                                                                              mArrivalTransfer.getArrivalTime()));
                    }
                    mBuilder.setCustomBigContentView(
                            NotificationLayoutBuilder.createNotificationLayout(mContext,
                                                                               mArrivalTransfer));

                }

                mBuilder.setStyle(new NotificationCompat.DecoratedCustomViewStyle());


                PendingIntent resultPendingIntent =
                        PendingIntent.getActivity(
                                mContext,
                                0,
                                resultIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );
                mBuilder.setContentIntent(resultPendingIntent);

                // Sets an ID for the notification
                int mNotificationId = 1;
                // Gets an instance of the NotificationManager service

                // Builds the notification and issues it.
                mNotifyMgr.notify(mNotificationId, mBuilder.build());
                vDialog.dismiss();
            }
        });
    }

    private void setOccupancyButtons(final Dialog vDialog, final IrailDataProvider mApiInstance, final String mDepartureConnection, final String mStationSemanticId, final String mVehicleSemanticId, final DateTime mDateTime) {
        final LinearLayout vLowOccupancy = vDialog.findViewById(R.id.button_low_occupancy);
        final LinearLayout vMediumOccupancy = vDialog.findViewById(R.id.button_medium_occupancy);
        final LinearLayout vHighOccupancy = vDialog.findViewById(R.id.button_high_occupancy);

        vLowOccupancy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IrailPostOccupancyRequest request = new IrailPostOccupancyRequest(
                        mDepartureConnection,
                        mStationSemanticId,
                        mVehicleSemanticId,
                        mDateTime,
                        OccupancyLevel.LOW);
                request.setCallback(

                        new IRailSuccessResponseListener<Boolean>() {
                            @Override
                            public void onSuccessResponse(@NonNull Boolean data, Object tag) {
                                Snackbar.make(VehiclePopupContextMenu.this.mActivityView,
                                              R.string.spitsgids_feedback_sent,
                                              Snackbar.LENGTH_LONG).show();
                            }
                        }, new IRailErrorResponseListener() {
                            @Override
                            public void onErrorResponse(@NonNull Exception data, Object tag) {
                                Snackbar.make(VehiclePopupContextMenu.this.mActivityView,
                                              R.string.spitsgids_feedback_error,
                                              Snackbar.LENGTH_LONG).show();
                            }
                        }, null);
                mApiInstance.postOccupancy(request);
                vDialog.dismiss();
            }
        });

        vMediumOccupancy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IrailPostOccupancyRequest request = new IrailPostOccupancyRequest(
                        mDepartureConnection,
                        mStationSemanticId,
                        mVehicleSemanticId,
                        mDateTime,
                        OccupancyLevel.MEDIUM);
                request.setCallback(

                        new IRailSuccessResponseListener<Boolean>() {
                            @Override
                            public void onSuccessResponse(@NonNull Boolean data, Object tag) {
                                Snackbar.make(VehiclePopupContextMenu.this.mActivityView,
                                              R.string.spitsgids_feedback_sent,
                                              Snackbar.LENGTH_LONG).show();
                            }
                        }, new IRailErrorResponseListener() {
                            @Override
                            public void onErrorResponse(@NonNull Exception data, Object tag) {
                                Snackbar.make(VehiclePopupContextMenu.this.mActivityView,
                                              R.string.spitsgids_feedback_error,
                                              Snackbar.LENGTH_LONG).show();
                            }
                        }, null);
                mApiInstance.postOccupancy(request);
                vDialog.dismiss();
            }
        });

        vHighOccupancy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IrailPostOccupancyRequest request = new IrailPostOccupancyRequest(
                        mDepartureConnection,
                        mStationSemanticId,
                        mVehicleSemanticId,
                        mDateTime,
                        OccupancyLevel.HIGH);
                request.setCallback(

                        new IRailSuccessResponseListener<Boolean>() {
                            @Override
                            public void onSuccessResponse(@NonNull Boolean data, Object tag) {
                                Snackbar.make(VehiclePopupContextMenu.this.mActivityView,
                                              R.string.spitsgids_feedback_sent,
                                              Snackbar.LENGTH_LONG).show();
                            }
                        }, new IRailErrorResponseListener() {
                            @Override
                            public void onErrorResponse(@NonNull Exception data, Object tag) {
                                Snackbar.make(VehiclePopupContextMenu.this.mActivityView,
                                              R.string.spitsgids_feedback_error,
                                              Snackbar.LENGTH_LONG).show();
                            }
                        }, null);
                mApiInstance.postOccupancy(request);
                vDialog.dismiss();
            }
        });
    }
}
