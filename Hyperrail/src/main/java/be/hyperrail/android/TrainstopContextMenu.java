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
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.LinearLayout;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import be.hyperrail.android.activities.LiveboardActivity;
import be.hyperrail.android.activities.TrainActivity;
import be.hyperrail.android.irail.contracts.IRailErrorResponseListener;
import be.hyperrail.android.irail.contracts.IRailSuccessResponseListener;
import be.hyperrail.android.irail.contracts.IrailDataProvider;
import be.hyperrail.android.irail.contracts.OccupancyLevel;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.irail.implementation.Route;
import be.hyperrail.android.irail.implementation.TrainStop;
import be.hyperrail.android.irail.implementation.Transfer;
import be.hyperrail.android.viewgroup.NotificationLayoutBuilder;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Dialog to submit occupancy data
 */
public class TrainstopContextMenu {

    private static final String NOTIFICATION_CHANNEL_GLIMPSE = "glimpse";
    private final View mActivityView;
    private Transfer mDepartureTransfer;
    private Route route;
    private TrainStop mTrainStop;
    private Transfer mArrivalTransfer;
    private Context mContext;

    public TrainstopContextMenu(Context context, TrainStop stop) {
        this(context);
        this.route = null;
        this.mTrainStop = stop;
        this.mArrivalTransfer = null;
        this.mDepartureTransfer = null;
    }

    public TrainstopContextMenu(Context context, Transfer transfer, Route route) {
        this(context);
        this.mArrivalTransfer = transfer;
        this.mDepartureTransfer = transfer;
        this.route = route;
        this.mTrainStop = null;
    }

    public TrainstopContextMenu(Context context, Transfer departureTransfer, Transfer arrivalTransfer, Route route) {
        this(context);
        this.mArrivalTransfer = arrivalTransfer;
        this.mDepartureTransfer = departureTransfer;
        this.route = route;
        this.mTrainStop = null;
    }

    private TrainstopContextMenu(Context context) {
        this.mContext = context;
        this.mActivityView = ((Activity) context).getWindow().getDecorView().findViewById(android.R.id.content);
    }

    public void show() {
        final Dialog vDialog = new Dialog(this.mContext, R.style.TrainLongClickDialog);
        final IrailDataProvider mApiInstance = IrailFactory.getDataProviderInstance();

        vDialog.setContentView(R.layout.contextmenu_spitsgids);

        final LinearLayout vShareDepartureEta = vDialog.findViewById(R.id.button_share_departure_ETA);
        final LinearLayout vShareArrivalEta = vDialog.findViewById(R.id.button_share_arrival_ETA);
        final LinearLayout vSetNotification = vDialog.findViewById(R.id.button_notification);

        final LinearLayout vOccupancyContainer = vDialog.findViewById(R.id.container_occupancy);
        final LinearLayout vLowOccupancy = vDialog.findViewById(R.id.button_low_occupancy);
        final LinearLayout vMediumOccupancy = vDialog.findViewById(R.id.button_medium_occupancy);
        final LinearLayout vHighOccupancy = vDialog.findViewById(R.id.button_high_occupancy);

        final String mDepartureConnection;
        final String mStationSemanticId;
        final String mVehicleSemanticId;
        final DateTime mDateTime;
        final String mArrivalEtaText;
        final String mDepartureEtaText;

        if (this.mTrainStop != null) {
            mDepartureConnection = mTrainStop.getDepartureSemanticId();
            mStationSemanticId = mTrainStop.getStation().getSemanticId();
            mVehicleSemanticId = mTrainStop.getTrain().getSemanticId();
            mDateTime = mTrainStop.getDepartureTime();

            vDialog.setTitle(mTrainStop.getTrain().getName() + " " +
                    mTrainStop.getStation().getLocalizedName());

            mArrivalEtaText = String.format(mContext.getString(R.string.ETA_stop_arrival), DateTimeFormat.forPattern("hh:mm").print(mTrainStop.getDelayedArrivalTime()), mTrainStop.getStation().getLocalizedName(), mTrainStop.getTrain().getName());
            mDepartureEtaText = String.format(mContext.getString(R.string.ETA_stop_departure), DateTimeFormat.forPattern("hh:mm").print(mTrainStop.getDelayedDepartureTime()), mTrainStop.getStation().getLocalizedName(), mTrainStop.getTrain().getName());

        } else {
            if (mDepartureTransfer == null && mArrivalTransfer == null) {
                throw new IllegalStateException("Contextmenu should either have a mArrivalTransfer or trainstop object");
            }

            // Occupancy + departure ETA
            if (mDepartureTransfer != null && mDepartureTransfer.getDepartingTrain() != null && !mDepartureTransfer.getDepartingTrain().getId().equals("WALK")) {
                mDepartureConnection = mDepartureTransfer.getDepartureConnectionSemanticId();
                mStationSemanticId = mDepartureTransfer.getStation().getSemanticId();
                mVehicleSemanticId = mDepartureTransfer.getDepartingTrain().getSemanticId();
                mDateTime = mDepartureTransfer.getDepartureTime();

                mDepartureEtaText = String.format(mContext.getString(R.string.ETA_transfer_departure), DateTimeFormat.forPattern("hh:mm").print(mDepartureTransfer.getDelayedDepartureTime()), mDepartureTransfer.getStation().getLocalizedName(), mDepartureTransfer.getDepartingTrain().getName());
            } else {
                vOccupancyContainer.setVisibility(View.GONE);
                mDepartureEtaText = null;

                mDepartureConnection = null;
                mStationSemanticId = null;
                mVehicleSemanticId = null;
                mDateTime = null;
            }

            if (this.mArrivalTransfer != null && mArrivalTransfer.getArrivingTrain() != null && !mArrivalTransfer.getArrivingTrain().getId().equals("WALK")) {
                mArrivalEtaText = String.format(mContext.getString(R.string.ETA_transfer_arrival), DateTimeFormat.forPattern("hh:mm").print(mArrivalTransfer.getDelayedArrivalTime()), mArrivalTransfer.getStation().getLocalizedName(), mArrivalTransfer.getArrivingTrain().getName());
            } else {
                mArrivalEtaText = null;
            }

            if (mDepartureTransfer == mArrivalTransfer) {
                vDialog.setTitle(mArrivalTransfer.getStation().getLocalizedName());
            } else {
                if (mDepartureTransfer != null && mArrivalTransfer != null) {
                    vDialog.setTitle(mDepartureTransfer.getDepartingTrain().getName() + " " +
                            mDepartureTransfer.getStation().getLocalizedName() + "-" + mArrivalTransfer.getStation().getLocalizedName());
                } else if (mDepartureTransfer != null) {
                    vDialog.setTitle(mDepartureTransfer.getDepartingTrain().getName() + " " +
                            mDepartureTransfer.getStation().getLocalizedName());
                } else {
                    vDialog.setTitle(mArrivalTransfer.getArrivingTrain().getName() + " " +
                            mArrivalTransfer.getStation().getLocalizedName());
                }

            }
        }

        vLowOccupancy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mApiInstance.postOccupancy(
                        mDepartureConnection,
                        mStationSemanticId,
                        mVehicleSemanticId,
                        mDateTime,
                        OccupancyLevel.LOW,
                        new IRailSuccessResponseListener<Boolean>() {
                            @Override
                            public void onSuccessResponse(Boolean data, Object tag) {
                                Snackbar.make(TrainstopContextMenu.this.mActivityView, R.string.spitsgids_feedback_sent, Snackbar.LENGTH_LONG).show();
                            }
                        }, new IRailErrorResponseListener<Boolean>() {
                            @Override
                            public void onErrorResponse(Exception data, Object tag) {
                                Snackbar.make(TrainstopContextMenu.this.mActivityView, R.string.spitsgids_feedback_error, Snackbar.LENGTH_LONG).show();
                            }
                        }, null);
                vDialog.dismiss();
            }
        });

        vMediumOccupancy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mApiInstance.postOccupancy(
                        mDepartureConnection,
                        mStationSemanticId,
                        mVehicleSemanticId,
                        mDateTime,
                        OccupancyLevel.MEDIUM,
                        new IRailSuccessResponseListener<Boolean>() {
                            @Override
                            public void onSuccessResponse(Boolean data, Object tag) {
                                Snackbar.make(TrainstopContextMenu.this.mActivityView, R.string.spitsgids_feedback_sent, Snackbar.LENGTH_LONG).show();
                            }
                        }, new IRailErrorResponseListener<Boolean>() {
                            @Override
                            public void onErrorResponse(Exception data, Object tag) {
                                Snackbar.make(TrainstopContextMenu.this.mActivityView, R.string.spitsgids_feedback_error, Snackbar.LENGTH_LONG).show();
                            }
                        }, null);
                vDialog.dismiss();
            }
        });

        vHighOccupancy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mApiInstance.postOccupancy(
                        mDepartureConnection,
                        mStationSemanticId,
                        mVehicleSemanticId,
                        mDateTime,
                        OccupancyLevel.HIGH,
                        new IRailSuccessResponseListener<Boolean>() {
                            @Override
                            public void onSuccessResponse(Boolean data, Object tag) {
                                Snackbar.make(TrainstopContextMenu.this.mActivityView, R.string.spitsgids_feedback_sent, Snackbar.LENGTH_LONG).show();
                            }
                        }, new IRailErrorResponseListener<Boolean>() {
                            @Override
                            public void onErrorResponse(Exception data, Object tag) {
                                Snackbar.make(TrainstopContextMenu.this.mActivityView, R.string.spitsgids_feedback_error, Snackbar.LENGTH_LONG).show();
                            }
                        }, null);
                vDialog.dismiss();
            }
        });

        // When this contextmenu was called on a train stop or transfer in a route (but not on a train in a route!)
        if (mTrainStop != null || mArrivalTransfer == mDepartureTransfer) {
            vSetNotification.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    NotificationManager mNotifyMgr =
                            (NotificationManager) mContext.getSystemService(NOTIFICATION_SERVICE);

                    if (mNotifyMgr == null) {
                        return;
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_GLIMPSE,
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
                    if (mTrainStop != null) {
                        mBuilder.setCustomBigContentView(NotificationLayoutBuilder.createNotificationLayout(mContext, mTrainStop));
                        mBuilder.setSubText("Train at  " + mTrainStop.getStation().getLocalizedName() + " towards " + mTrainStop.getTrain().getDirection().getLocalizedName());
                        resultIntent = TrainActivity.createIntent(mContext, mTrainStop.getTrain(), mTrainStop.getStation(), mTrainStop.getDepartureTime());

                    } else {
                        if (mDepartureTransfer != null) {
                            mBuilder.setSubText("Transfer at  " + mDepartureTransfer.getStation().getLocalizedName());
                            resultIntent = LiveboardActivity.createIntent(mContext, mDepartureTransfer.getStation(), mDepartureTransfer.getArrivalTime());
                        } else {
                            mBuilder.setSubText("Transfer at  " + mArrivalTransfer.getStation().getLocalizedName());
                            resultIntent = LiveboardActivity.createIntent(mContext, mArrivalTransfer.getStation(), mArrivalTransfer.getArrivalTime());
                        }
                        mBuilder.setCustomBigContentView(NotificationLayoutBuilder.createNotificationLayout(mContext, mArrivalTransfer));

                    }

                    mBuilder.setStyle(new android.support.v4.app.NotificationCompat.DecoratedCustomViewStyle());


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

                }
            });
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
                }
            });
        }

        vDialog.show();
    }
}
