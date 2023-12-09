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
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;

import androidx.core.app.NotificationCompat.Builder;
import androidx.core.app.NotificationCompat.DecoratedCustomViewStyle;
import androidx.core.content.ContextCompat;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import be.hyperrail.android.R.color;
import be.hyperrail.android.R.drawable;
import be.hyperrail.android.R.id;
import be.hyperrail.android.R.layout;
import be.hyperrail.android.R.string;
import be.hyperrail.android.R.style;
import be.hyperrail.android.activities.searchresult.VehicleActivity;
import be.hyperrail.android.util.NotificationLayoutBuilder;
import be.hyperrail.opentransportdata.OpenTransportApi;
import be.hyperrail.opentransportdata.common.contracts.TransportDataSource;
import be.hyperrail.opentransportdata.common.contracts.TransportOccupancyLevel;
import be.hyperrail.opentransportdata.common.models.RouteLeg;
import be.hyperrail.opentransportdata.common.models.RouteLegType;
import be.hyperrail.opentransportdata.common.models.Transfer;
import be.hyperrail.opentransportdata.common.models.TransferType;
import be.hyperrail.opentransportdata.common.models.VehicleStop;
import be.hyperrail.opentransportdata.common.requests.OccupancyPostRequest;
import be.hyperrail.opentransportdata.common.requests.VehicleRequest;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * This dialogs allows users to submit occupancy data, pin notifications or send ETA messages for transfers and trains and train stops.
 */
public class VehiclePopupContextMenu {

    private static final int TYPE_TRANSFER = 0;
    private static final int TYPE_TRAIN_STOP = 1;
    private static final int TYPE_ROUTE_TRAIN = 2;

    private static final String NOTIFICATION_CHANNEL_GLIMPSE = "glimpse";
    public static final DateTimeFormatter DATETIMEFORMAT_HHMM = DateTimeFormat.forPattern("HH:mm");
    private final View mActivityView;
    private final VehicleStop mVehicleStop;
    private final Activity mContext;

    private final int type;
    private final RouteLeg mLeg;
    private final Transfer mTransfer;

    /**
     * Create a popupdialog, with the correct fields for a train stop, either from a liveboard or train schedule.
     *
     * @param context The activity which is currently being shown / from where this dialog is requested
     * @param stop    The stop for which information should be shown
     */
    public VehiclePopupContextMenu(@NonNull Activity context, @NonNull VehicleStop stop) {
        this.mContext = context;
        this.mActivityView = context.getWindow().getDecorView().findViewById(android.R.id.content);

        this.mVehicleStop = stop;
        this.mLeg = null;
        this.mTransfer = null;

        this.type = TYPE_TRAIN_STOP;
    }

    /**
     * Create a popupdialog, with the correct fields for a transfer, either the departure, transfer or arrival for a train route
     *
     * @param transfer The transfer for which information should be shown
     * @param context  The activity which is currently being shown / from where this dialog is requested
     */
    public VehiclePopupContextMenu(@NonNull Activity context, @NonNull Transfer transfer) {
        this.mContext = context;
        this.mActivityView = context.getWindow().getDecorView().findViewById(android.R.id.content);

        this.mTransfer = transfer;
        this.mVehicleStop = null;
        this.mLeg = null;

        this.type = TYPE_TRANSFER;
    }

    /**
     * Create a popupdialog, with the correct fields for a train on a route, identified by the departure before and the arrival after this train.
     *
     * @param context The activity which is currently being shown / from where this dialog is requested
     */
    public VehiclePopupContextMenu(@NonNull Activity context, @NonNull RouteLeg leg) {
        this.mContext = context;
        this.mActivityView = context.getWindow().getDecorView().findViewById(android.R.id.content);

        this.mLeg = leg;
        this.mTransfer = null;
        this.mVehicleStop = null;

        this.type = TYPE_ROUTE_TRAIN;
    }

    /**
     * Show the dialog
     */
    public void show() {
        Dialog vDialog = new Dialog(this.mContext, style.TrainLongClickDialog);
        TransportDataSource mApiInstance = OpenTransportApi.getDataProviderInstance();

        vDialog.setContentView(layout.contextmenu_spitsgids);

        LinearLayout vSetNotification = vDialog.findViewById(id.button_notification);

        if (type == TYPE_TRAIN_STOP) {
            bindTrainStop(vDialog, mApiInstance);
        } else if (type == TYPE_TRANSFER) {
            bindTransfer(vDialog, mApiInstance);
        } else if (type == TYPE_ROUTE_TRAIN) {
            bindTrain(vDialog, mApiInstance);
        }

        // When this contextmenu was called on a train stop or transfer in a route (but not on a train in a route!)
        if (type == TYPE_TRAIN_STOP || (type == TYPE_TRANSFER && mTransfer.getType() != TransferType.ARRIVAL)) {
            bindNotificationButton(vDialog, vSetNotification);
        } else {
            vSetNotification.setVisibility(View.GONE);
        }


        vDialog.show();
    }

    /**
     * Update the dialog UI to show all buttons for a train on a route
     *
     * @param vDialog      The dialog view
     * @param mApiInstance An instance of a data provider
     */
    private void bindTrain(Dialog vDialog, TransportDataSource mApiInstance) {
        String mDepartureConnection;
        String mStationSemanticId;
        String mVehicleSemanticId;
        DateTime mDateTime;

        String mDepartureEtaText;
        String mArrivalEtaText;

        vDialog.setTitle(
                mLeg.getVehicleInformation().getName() + " " +
                mLeg.getDeparture().getStation().getLocalizedName() + "-" + mLeg.getArrival().getStation().getLocalizedName());

        // Occupancy + departure ETA
        if (mLeg.getType() != RouteLegType.WALK) {

            mDepartureConnection = mLeg.getDeparture().getSemanticId();
            mStationSemanticId = mLeg.getDeparture().getStation().getSemanticId();
            mVehicleSemanticId = mLeg.getVehicleInformation().getSemanticId();
            mDateTime = mLeg.getDeparture().getTime();
            bindOccupancyButtons(vDialog, mApiInstance, mDepartureConnection, mStationSemanticId,
                    mVehicleSemanticId, mDateTime);

            mDepartureEtaText = String.format(
                    mContext.getString(string.ETA_transfer_departure),
                    DATETIMEFORMAT_HHMM.print(
                            mLeg.getDeparture().getDelayedTime()),
                    mLeg.getDeparture().getStation().getLocalizedName(),
                    mLeg.getVehicleInformation().getName());
        } else {
            mDepartureEtaText = null;
        }

        if (mLeg.getType() != RouteLegType.WALK) {
            mArrivalEtaText = String.format(mContext.getString(string.ETA_transfer_arrival),
                    DATETIMEFORMAT_HHMM.print(
                            mLeg.getArrival().getDelayedTime()),
                    mLeg.getArrival().getStation().getLocalizedName(),
                    mLeg.getVehicleInformation().getName());
        } else {
            mArrivalEtaText = null;
        }

        bindETAButtons(vDialog, mDepartureEtaText, mArrivalEtaText
        );
    }

    /**
     * Update the dialog UI to show all buttons for a transfer on a route
     *
     * @param vDialog      The dialog view
     * @param mApiInstance An instance of a data provider
     */
    private void bindTransfer(Dialog vDialog, TransportDataSource mApiInstance) {
        String mDepartureEtaText;
        String mArrivalEtaText;

        vDialog.setTitle(mTransfer.getStopLocation().getLocalizedName());

        if (mTransfer.getType() == TransferType.DEPARTURE || mTransfer.getType() == TransferType.TRANSFER) {
            String mDepartureConnection = mTransfer.getDepartureSemanticId();
            String mStationSemanticId = mTransfer.getStopLocation().getSemanticId();
            String mVehicleSemanticId = mTransfer.getDepartingLeg().getVehicleInformation().getSemanticId();
            DateTime mDateTime = mTransfer.getDepartureTime();

            bindOccupancyButtons(vDialog, mApiInstance, mDepartureConnection,
                    mStationSemanticId,
                    mVehicleSemanticId, mDateTime);

            mDepartureEtaText = String.format(
                    mContext.getString(string.ETA_transfer_departure),
                    DATETIMEFORMAT_HHMM.print(
                            mTransfer.getDelayedDepartureTime()),
                    mTransfer.getStopLocation().getLocalizedName(),
                    mTransfer.getDepartingLeg().getVehicleInformation().getName());
        } else {
            mDepartureEtaText = null;
        }

        if (mTransfer.getType() == TransferType.ARRIVAL || mTransfer.getType() == TransferType.TRANSFER) {
            mArrivalEtaText = String.format(mContext.getString(string.ETA_transfer_arrival),
                    DATETIMEFORMAT_HHMM.print(
                            mTransfer.getDelayedArrivalTime()),
                    mTransfer.getStopLocation().getLocalizedName(),
                    mTransfer.getArrivingLeg().getVehicleInformation().getName());
        } else {
            mArrivalEtaText = null;
        }

        bindETAButtons(vDialog, mDepartureEtaText, mArrivalEtaText
        );
    }

    /**
     * Update the dialog UI to show all buttons for a train stop
     *
     * @param vDialog      The dialog view
     * @param mApiInstance An instance of a data provider
     */
    private void bindTrainStop(Dialog vDialog, TransportDataSource mApiInstance) {
        vDialog.setTitle(mVehicleStop.getStopLocation().getLocalizedName());

        String mDepartureConnection = mVehicleStop.getDepartureUri();
        String mStationSemanticId = mVehicleStop.getStopLocation().getSemanticId();
        String mVehicleSemanticId = mVehicleStop.getVehicle().getSemanticId();
        DateTime mDateTime = mVehicleStop.getDepartureTime();

        bindOccupancyButtons(vDialog, mApiInstance, mDepartureConnection, mStationSemanticId,
                mVehicleSemanticId, mDateTime);

        vDialog.setTitle(mVehicleStop.getVehicle().getName() + " " +
                         mVehicleStop.getStopLocation().getLocalizedName());

        String mDepartureEtaText = String.format(mContext.getString(string.ETA_stop_departure),
                DATETIMEFORMAT_HHMM.print(
                        mVehicleStop.getDelayedDepartureTime()
                ),
                mVehicleStop.getStopLocation().getLocalizedName(),
                mVehicleStop.getVehicle().getName());

        String mArrivalEtaText = String.format(mContext.getString(string.ETA_stop_arrival),
                DATETIMEFORMAT_HHMM.print(
                        mVehicleStop.getDelayedArrivalTime()
                ),
                mVehicleStop.getStopLocation().getLocalizedName(),
                mVehicleStop.getVehicle().getName());

        bindETAButtons(vDialog, mDepartureEtaText, mArrivalEtaText
        );
    }

    /**
     * Update the ETA buttons to share the right text
     *
     * @param vDialog           The dialog view
     * @param mDepartureEtaText The departure ETA text
     * @param mArrivalEtaText   The arrival ETA text
     */
    private void bindETAButtons(final Dialog vDialog, final String mDepartureEtaText, final String mArrivalEtaText) {
        LinearLayout vShareDepartureEta = vDialog.findViewById(
                id.button_share_departure_ETA);
        LinearLayout vShareArrivalEta = vDialog.findViewById(id.button_share_arrival_ETA);


        if (mArrivalEtaText == null) {
            vShareArrivalEta.setVisibility(View.GONE);
        } else {
            vShareArrivalEta.setOnClickListener(new OnClickListener() {
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
            vShareDepartureEta.setOnClickListener(new OnClickListener() {
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
    }

    /**
     * Update the notification button listener, so it will set the right notification on click
     *
     * @param vDialog          The dialog view
     * @param vSetNotification The pin notification button
     */
    private void bindNotificationButton(final Dialog vDialog, LinearLayout vSetNotification) {
        vSetNotification.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                NotificationManager mNotifyMgr =
                        (NotificationManager) mContext.getSystemService(NOTIFICATION_SERVICE);

                if (mNotifyMgr == null) {
                    return;
                }

                if (VERSION.SDK_INT >= VERSION_CODES.O) {
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

                Builder mBuilder =
                        new Builder(mContext, NOTIFICATION_CHANNEL_GLIMPSE)
                                .setSmallIcon(drawable.ic_hyperrail_notification);

                Intent resultIntent;

                mBuilder.setColor(ContextCompat.getColor(mContext, color.colorPrimary));

                String notificationTag;
                if (mVehicleStop != null) {
                    mBuilder.setCustomBigContentView(
                            NotificationLayoutBuilder.createNotificationLayout(mContext,
                                    mVehicleStop));
                    mBuilder.setSubText(
                            "VehicleJourney at  " + mVehicleStop.getStopLocation().getLocalizedName() + " towards " + mVehicleStop.getVehicle().getHeadsign());
                    resultIntent = VehicleActivity.createIntent(mContext, new VehicleRequest(
                            mVehicleStop.getVehicle().getId(), mVehicleStop.getDepartureTime()));
                    notificationTag = mVehicleStop.getDepartureUri();
                } else {
                    VehicleStop notificationStop = mTransfer.getDepartingLegAsVehicleStop();
                    mBuilder.setCustomBigContentView(
                            NotificationLayoutBuilder.createNotificationLayout(mContext,
                                    notificationStop));
                    mBuilder.setSubText(
                            "VehicleJourney at  " + notificationStop.getStopLocation().getLocalizedName() + " towards " + notificationStop.getVehicle().getHeadsign());
                    resultIntent = VehicleActivity.createIntent(mContext, new VehicleRequest(
                            notificationStop.getVehicle().getId(), notificationStop.getDepartureTime()));
                    notificationTag = notificationStop.getDepartureUri();
                }

                mBuilder.setStyle(new DecoratedCustomViewStyle());

                PendingIntent resultPendingIntent =
                        PendingIntent.getActivity(
                                mContext,
                                0,
                                resultIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                        );
                mBuilder.setContentIntent(resultPendingIntent);

                // Sets an ID for the notification
                int mNotificationId = notificationTag.hashCode();
                // Gets an instance of the NotificationManager service

                // Builds the notification and issues it.
                mNotifyMgr.notify(notificationTag, mNotificationId, mBuilder.build());
                vDialog.dismiss();
            }
        });
    }

    /**
     * Update the click handlers for the occupancy buttons
     * <p>
     * For details on the field values, see http://docs.irail.be/#occupancy.
     *
     * @param vDialog              The dialog view
     * @param mApiInstance         An instance of a data provider
     * @param mDepartureConnection The departureConnection for which to update the occupancy
     * @param mStationSemanticId   The station semantic id for which to update the occupancy
     * @param mVehicleSemanticId   The vehicle semantic id for which to update the occupancy
     * @param mDateTime            The datetime for which to update the occupancy
     */
    private void bindOccupancyButtons(final Dialog vDialog, final TransportDataSource mApiInstance, final String mDepartureConnection, final String mStationSemanticId, final String mVehicleSemanticId, final DateTime mDateTime) {
        LinearLayout vLowOccupancy = vDialog.findViewById(id.button_low_occupancy);
        LinearLayout vMediumOccupancy = vDialog.findViewById(id.button_medium_occupancy);
        LinearLayout vHighOccupancy = vDialog.findViewById(id.button_high_occupancy);

        vLowOccupancy.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                OccupancyPostRequest request = new OccupancyPostRequest(
                        mDepartureConnection,
                        mStationSemanticId,
                        mVehicleSemanticId,
                        mDateTime,
                        TransportOccupancyLevel.LOW);
                setButtonConfirmationCallback(request);
                mApiInstance.postOccupancy(request);
                vDialog.dismiss();
            }
        });

        vMediumOccupancy.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                OccupancyPostRequest request = new OccupancyPostRequest(
                        mDepartureConnection,
                        mStationSemanticId,
                        mVehicleSemanticId,
                        mDateTime,
                        TransportOccupancyLevel.MEDIUM);
                setButtonConfirmationCallback(request);
                mApiInstance.postOccupancy(request);
                vDialog.dismiss();
            }
        });

        vHighOccupancy.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                OccupancyPostRequest request = new OccupancyPostRequest(
                        mDepartureConnection,
                        mStationSemanticId,
                        mVehicleSemanticId,
                        mDateTime,
                        TransportOccupancyLevel.HIGH);
                setButtonConfirmationCallback(request);
                mApiInstance.postOccupancy(request);
                vDialog.dismiss();
            }
        });
    }

    private void setButtonConfirmationCallback(OccupancyPostRequest request) {
        request.setCallback(
                (data, tag) -> Snackbar.make(
                        VehiclePopupContextMenu.this.mActivityView,
                        string.spitsgids_feedback_sent,
                        Snackbar.LENGTH_LONG).show(),
                (error, tag) -> Snackbar.make(
                        VehiclePopupContextMenu.this.mActivityView,
                        string.spitsgids_feedback_error,
                        Snackbar.LENGTH_LONG).show()
                , null);
    }
}
