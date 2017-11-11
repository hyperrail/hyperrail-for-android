/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.LinearLayout;

import org.joda.time.DateTime;

import be.hyperrail.android.irail.contracts.IRailErrorResponseListener;
import be.hyperrail.android.irail.contracts.IRailSuccessResponseListener;
import be.hyperrail.android.irail.contracts.IrailDataProvider;
import be.hyperrail.android.irail.contracts.OccupancyLevel;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.irail.implementation.TrainStop;

/**
 * Dialog to submit occupancy data
 */
public class OccupancyDialog {

    private final View view;
    private Context context;
    private final String departureConnection;
    private final String stationSemanticId;
    private final String vehicleSemanticId;
    private final DateTime date;

    public OccupancyDialog(Context context, TrainStop stop) {
        this(context, stop.getSemanticDepartureConnection(), stop.getStation().getSemanticId(), stop.getTrain().getSemanticId(), stop.getDepartureTime());
    }

    public OccupancyDialog(Context context, String departureConnection, String stationSemanticId, String vehicleSemanticId, DateTime date) {
        this.context = context;
        this.view = ((Activity) context).getWindow().getDecorView().findViewById(android.R.id.content);
        this.departureConnection = departureConnection;
        this.stationSemanticId = stationSemanticId;
        this.vehicleSemanticId = vehicleSemanticId;
        this.date = date;
    }

    public void show() {
        final Dialog dialog = new Dialog(this.context, R.style.TrainLongClickDialog);
        final IrailDataProvider api = IrailFactory.getDataProviderInstance();

        dialog.setContentView(R.layout.dialog_spitsgids);
        dialog.setTitle(R.string.occupancy_question);

        LinearLayout lowOccupancy = dialog.findViewById(R.id.button_low_occupancy);
        LinearLayout mediumOccupancy = dialog.findViewById(R.id.button_medium_occupancy);
        LinearLayout highOccupancy = dialog.findViewById(R.id.button_high_occupancy);

        lowOccupancy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                api.postOccupancy(
                        OccupancyDialog.this.departureConnection,
                        OccupancyDialog.this.stationSemanticId,
                        OccupancyDialog.this.vehicleSemanticId,
                        OccupancyDialog.this.date,
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
                        OccupancyDialog.this.departureConnection,
                        OccupancyDialog.this.stationSemanticId,
                        OccupancyDialog.this.vehicleSemanticId,
                        OccupancyDialog.this.date,
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
                        OccupancyDialog.this.departureConnection,
                        OccupancyDialog.this.stationSemanticId,
                        OccupancyDialog.this.vehicleSemanticId,
                        OccupancyDialog.this.date,
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

        dialog.show();
    }
}
