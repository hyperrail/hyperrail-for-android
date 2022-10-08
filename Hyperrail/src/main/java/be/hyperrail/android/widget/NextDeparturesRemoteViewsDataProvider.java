/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.widget;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;

import androidx.core.content.ContextCompat;

import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import be.hyperrail.android.R;
import be.hyperrail.android.activities.searchresult.LiveboardActivity;
import be.hyperrail.android.logging.HyperRailLog;
import be.hyperrail.opentransportdata.OpenTransportApi;
import be.hyperrail.opentransportdata.common.contracts.QueryTimeDefinition;
import be.hyperrail.opentransportdata.common.exceptions.StopLocationNotResolvedException;
import be.hyperrail.opentransportdata.common.models.Liveboard;
import be.hyperrail.opentransportdata.common.models.LiveboardType;
import be.hyperrail.opentransportdata.common.models.VehicleStop;
import be.hyperrail.opentransportdata.common.requests.LiveboardRequest;
import be.hyperrail.opentransportdata.util.OccupancyHelper;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;

class NextDeparturesRemoteViewsDataProvider implements RemoteViewsService.RemoteViewsFactory {
    private static final HyperRailLog log = HyperRailLog.getLogger(NextDeparturesWidgetProvider.class);
    private final Context context;
    private final Intent creationIntent;
    private Liveboard liveboard;
    private boolean errorDuringUpdate = false;

    public NextDeparturesRemoteViewsDataProvider(Context applicationContext, Intent intent) {
        context = applicationContext;
        creationIntent = intent;
    }

    @Override
    public void onCreate() {
        initLiveboard();
    }

    private void initLiveboard() {
        String id = context.getSharedPreferences("widgets", 0).getString(
                "NextDepartures:" + creationIntent.getIntExtra(EXTRA_APPWIDGET_ID, 0), null);
        if (id == null) {
            this.errorDuringUpdate = true;
            return;
        }

        LiveboardRequest request = null;
        try {
            request = new LiveboardRequest(
                    OpenTransportApi.getStopLocationProviderInstance().getStoplocationBySemanticId(id),
                    QueryTimeDefinition.EQUAL_OR_LATER,
                    LiveboardType.DEPARTURES,
                    null
            );
        } catch (StopLocationNotResolvedException e) {
            this.errorDuringUpdate = true;
            return;
        }
        request.setCallback((data, tag) -> {
            log.info("Received iRail data...");
            NextDeparturesRemoteViewsDataProvider.this.liveboard = data;
        }, null, null);
        log.info("Requesting iRail data...");
        OpenTransportApi.getDataProviderInstance().getLiveboard(request);

        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            // Nothing to do
        }
    }

    @Override
    public void onDataSetChanged() {
        initLiveboard();
    }

    @Override
    public void onDestroy() {
        // Nothing to do, cursors would be closed here
    }

    @Override
    public int getCount() {
        if (liveboard == null) {
            return 0;
        }
        return Math.min(liveboard.getStops().length, 20);
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (errorDuringUpdate) {
            return new RemoteViews(context.getPackageName(), R.layout.widget_nextdepartures_error);
        }

        // Construct a remote views item based on the app widget item XML file,
        // and set the text based on the position.
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.listview_liveboard_widget);
        VehicleStop stop = liveboard.getStops()[position];
        Intent onClickIntent = LiveboardActivity.createIntent(context,
                new LiveboardRequest(stop.getStopLocation(),
                        QueryTimeDefinition.EQUAL_OR_LATER,
                        LiveboardType.DEPARTURES,
                        null));

        int flags;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        } else {
            flags = PendingIntent.FLAG_UPDATE_CURRENT;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, onClickIntent, flags);
        bindTimeAndDelays(rv, stop);
        rv.setOnClickPendingIntent(R.id.binder, pendingIntent);
        rv.setTextViewText(R.id.text_destination, stop.getHeadsign());

        rv.setTextViewText(R.id.text_train_number, stop.getVehicle().getNumber());
        rv.setTextViewText(R.id.text_train_type, stop.getVehicle().getType());
        rv.setTextViewText(R.id.text_platform, stop.getPlatform());

        if (stop.isDepartureCanceled()) {
            rv.setTextViewText(R.id.text_platform, "X");

            rv.setTextViewText(R.id.text_train_status,
                    context.getString(R.string.status_cancelled));
            rv.setViewVisibility(R.id.layout_train_status_container, View.VISIBLE);
            rv.setViewVisibility(R.id.container_occupancy, View.GONE);
        } else {
            rv.setViewVisibility(R.id.layout_train_status_container, View.GONE);
            rv.setViewVisibility(R.id.container_occupancy, View.VISIBLE);
        }

        rv.setBitmap(R.id.container_occupancy, "setImageDrawable",
                ((BitmapDrawable) ContextCompat.getDrawable(context,
                        OccupancyHelper.getOccupancyDrawable(
                                stop.getOccupancyLevel())
                )).getBitmap()
        );

        // Return the remote views object.
        return rv;
    }

    private void bindTimeAndDelays(RemoteViews rv, VehicleStop stop) {
        DateTimeFormatter df = DateTimeFormat.forPattern("HH:mm");

        if (stop.getDepartureTime() != null) {
            rv.setTextViewText(R.id.text_time1, df.print(stop.getDepartureTime()));
            if (stop.getDepartureDelay().getStandardSeconds() > 0) {
                rv.setTextViewText(R.id.text_delay1,
                        context.getString(R.string.delay,
                                stop.getDepartureDelay().getStandardMinutes()));
                rv.setTextViewText(R.id.text_delay_time, df.print(stop.getDelayedDepartureTime()));
            } else {
                rv.setTextViewText(R.id.text_delay1, "");
                rv.setTextViewText(R.id.text_delay_time, "");
            }
        } else {
            // support for arrivals
            rv.setTextViewText(R.id.text_time1, df.print(stop.getArrivalTime()));
            if (stop.getArrivalDelay().getStandardSeconds() > 0) {
                rv.setTextViewText(R.id.text_delay1,
                        context.getString(R.string.delay,
                                stop.getArrivalDelay().getStandardMinutes()));
                rv.setTextViewText(R.id.text_delay_time, df.print(stop.getDelayedArrivalTime()));
            } else {
                rv.setTextViewText(R.id.text_delay1, "");
                rv.setTextViewText(R.id.text_delay_time, "");
            }
        }
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

}
