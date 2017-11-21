/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.LinearLayout;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import be.hyperrail.android.irail.contracts.IRailErrorResponseListener;
import be.hyperrail.android.irail.contracts.IRailSuccessResponseListener;
import be.hyperrail.android.irail.contracts.IrailDataProvider;
import be.hyperrail.android.irail.contracts.OccupancyLevel;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.irail.implementation.TrainStop;
import be.hyperrail.android.irail.implementation.Transfer;

/**
 * Dialog to submit occupancy data
 */
public class OccupancyDialog {

    private final View view;
    private TrainStop trainStop;
    private Transfer transfer;
    private Context context;

    public OccupancyDialog(Context context, TrainStop stop) {
        this(context);
        this.trainStop = stop;
        this.transfer = null;
    }

    public OccupancyDialog(Context context, Transfer transfer) {
        this(context);
        this.transfer = transfer;
        this.trainStop = null;
    }

    private OccupancyDialog(Context context) {
        this.context = context;
        this.view = ((Activity) context).getWindow().getDecorView().findViewById(android.R.id.content);
    }

    public void show() {
        final Dialog dialog = new Dialog(this.context, R.style.TrainLongClickDialog);
        final IrailDataProvider api = IrailFactory.getDataProviderInstance();

        dialog.setContentView(R.layout.contextmenu_spitsgids);

        LinearLayout shareDepartureETA = dialog.findViewById(R.id.button_share_departure_ETA);
        LinearLayout shareArrivalETA = dialog.findViewById(R.id.button_share_arrival_ETA);
        LinearLayout setNotification = dialog.findViewById(R.id.button_notification);

        LinearLayout lowOccupancy = dialog.findViewById(R.id.button_low_occupancy);
        LinearLayout mediumOccupancy = dialog.findViewById(R.id.button_medium_occupancy);
        LinearLayout highOccupancy = dialog.findViewById(R.id.button_high_occupancy);

        final String departureConnection;
        final String stationSemanticId;
        final String vehicleSemanticId;
        final DateTime date;
        final String ArrivalETAText;
        final String DepartureETAText;

        if (this.trainStop != null) {
            departureConnection = trainStop.getSemanticDepartureConnection();
            stationSemanticId = trainStop.getStation().getSemanticId();
            vehicleSemanticId = trainStop.getTrain().getSemanticId();
            date = trainStop.getDepartureTime();

            dialog.setTitle(trainStop.getTrain().getName() + " " +
                    trainStop.getStation().getLocalizedName());

            ArrivalETAText = "At " + DateTimeFormat.forPattern("hh:mm").print(trainStop.getDelayedArrivalTime()) + " I will arrive in " + trainStop.getStation().getLocalizedName() + " with " + trainStop.getTrain().getName();
            DepartureETAText = "At " + DateTimeFormat.forPattern("hh:mm").print(trainStop.getDelayedDepartureTime()) + " I will leave from " + trainStop.getStation().getLocalizedName() + " with " + trainStop.getTrain().getName();

        } else if (this.transfer != null) {
            departureConnection = transfer.getDepartureConnectionSemanticId();
            stationSemanticId = transfer.getStation().getSemanticId();
            vehicleSemanticId = transfer.getDepartingTrain().getSemanticId();
            date = transfer.getDepartureTime();

            dialog.setTitle(transfer.getDepartingTrain().getName() + " " + transfer.getStation().getLocalizedName());
            ArrivalETAText = "At " + DateTimeFormat.forPattern("hh:mm").print(transfer.getDelayedArrivalTime()) + " I will arrive in " + transfer.getStation().getLocalizedName() + " with " + transfer.getArrivingTrain().getName();
            DepartureETAText = "At " + DateTimeFormat.forPattern("hh:mm").print(transfer.getDelayedDepartureTime()) + " I will leave from " + transfer.getStation().getLocalizedName() + " with " + transfer.getDepartingTrain().getName();

        } else {
            throw new IllegalStateException("Contextmenu should either have a transfer or trainstop object");
        }

        lowOccupancy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                api.postOccupancy(
                        departureConnection,
                        stationSemanticId,
                        vehicleSemanticId,
                        date,
                        OccupancyLevel.LOW,
                        new IRailSuccessResponseListener<Boolean>() {
                            @Override
                            public void onSuccessResponse(Boolean data, Object tag) {
                                Snackbar.make(OccupancyDialog.this.view, R.string.spitsgids_feedback_sent, Snackbar.LENGTH_LONG).show();
                            }
                        }, new IRailErrorResponseListener<Boolean>() {
                            @Override
                            public void onErrorResponse(Exception data, Object tag) {
                                Snackbar.make(OccupancyDialog.this.view, R.string.spitsgids_feedback_error, Snackbar.LENGTH_LONG).show();
                            }
                        }, null);
                dialog.dismiss();
            }
        });

        mediumOccupancy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                api.postOccupancy(
                        departureConnection,
                        stationSemanticId,
                        vehicleSemanticId,
                        date,
                        OccupancyLevel.MEDIUM,
                        new IRailSuccessResponseListener<Boolean>() {
                            @Override
                            public void onSuccessResponse(Boolean data, Object tag) {
                                Snackbar.make(OccupancyDialog.this.view, R.string.spitsgids_feedback_sent, Snackbar.LENGTH_LONG).show();
                            }
                        }, new IRailErrorResponseListener<Boolean>() {
                            @Override
                            public void onErrorResponse(Exception data, Object tag) {
                                Snackbar.make(OccupancyDialog.this.view, R.string.spitsgids_feedback_error, Snackbar.LENGTH_LONG).show();
                            }
                        }, null);
                dialog.dismiss();
            }
        });

        highOccupancy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                api.postOccupancy(
                        departureConnection,
                        stationSemanticId,
                        vehicleSemanticId,
                        date,
                        OccupancyLevel.HIGH,
                        new IRailSuccessResponseListener<Boolean>() {
                            @Override
                            public void onSuccessResponse(Boolean data, Object tag) {
                                Snackbar.make(OccupancyDialog.this.view, R.string.spitsgids_feedback_sent, Snackbar.LENGTH_LONG).show();
                            }
                        }, new IRailErrorResponseListener<Boolean>() {
                            @Override
                            public void onErrorResponse(Exception data, Object tag) {
                                Snackbar.make(OccupancyDialog.this.view, R.string.spitsgids_feedback_error, Snackbar.LENGTH_LONG).show();
                            }
                        }, null);
                dialog.dismiss();
            }
        });

        shareArrivalETA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, ArrivalETAText);
                sendIntent.setType("text/plain");
                context.startActivity(sendIntent);
            }
        });

        shareDepartureETA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, DepartureETAText);
                sendIntent.setType("text/plain");
                context.startActivity(sendIntent);
            }
        });
        dialog.show();
    }
}
