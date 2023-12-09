/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RemoteViews;

import be.hyperrail.android.R;
import be.hyperrail.android.activities.searchresult.LiveboardActivity;
import be.hyperrail.android.logging.HyperRailLog;
import be.hyperrail.opentransportdata.OpenTransportApi;
import be.hyperrail.opentransportdata.common.exceptions.StopLocationNotResolvedException;
import be.hyperrail.opentransportdata.common.models.StopLocation;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;

import java.util.Objects;

public class NextDeparturesWidgetProvider extends AppWidgetProvider {
    private static final HyperRailLog log = HyperRailLog.getLogger(NextDeparturesWidgetProvider.class);
    private static final String INTENT_EXTRA_STATION_ID = "stationId";
    private static final String INTENT_ACTION_LAUNCH_LIVEBOARD_VIEW = "launchLiveboardView";
    private SharedPreferences prefs;

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    private SharedPreferences getSharedPreferences(Context context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences("widgets", 0);
        }
        return prefs;
    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                 int appWidgetId) {
        log.debug("Updating widget " + appWidgetId);
        String id = getSharedPreferences(context).getString("NextDepartures:" + appWidgetId, null);
        if (id == null) {
            log.warning("No station ID found for NextDepartures widget id: " + appWidgetId);
            return;
        }

        StopLocation station;
        try {
            station = OpenTransportApi.getStopLocationProviderInstance().getStoplocationBySemanticId(id);
        } catch (StopLocationNotResolvedException searchException) {
            log.warning("Failed to lookup station URI for widget: " + id);
            log.logException(searchException);
            try {
                station = OpenTransportApi.getStopLocationProviderInstance().getStoplocationByHafasId(id);
            } catch (StopLocationNotResolvedException fallbackSearchException) {
                log.warning("Failed to lookup station ID for widget: " + id);
                log.logException(fallbackSearchException);
                station = null;
            }
        }

        if (station == null) {
            setErrorLayout(context, appWidgetManager, appWidgetId);
            return;
        }

        updateAppWidgetData(context, appWidgetManager, appWidgetId, station);
    }

    private void updateAppWidgetData(Context context, AppWidgetManager appWidgetManager,
                                     int appWidgetId, StopLocation station) {
        // Get the layout for the App Widget and attach an on-click listener
        // to the button
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                R.layout.widget_nextdepartures);
        remoteViews.setTextViewText(R.id.text_station, station.getLocalizedName());

        // Set up the intent that starts the NextDeparturesWidgetService, which will
        // provide the views for this collection.
        Intent serviceIntent = new Intent(context, NextDeparturesWidgetService.class);
        serviceIntent.putExtra(EXTRA_APPWIDGET_ID, appWidgetId);
        serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));
        // Set up the RemoteViews object to use a RemoteViews adapter.
        // This adapter connects to a RemoteViewsService  through the specified intent.
        remoteViews.setRemoteAdapter(android.R.id.list, serviceIntent);
        remoteViews.setEmptyView(android.R.id.list, R.id.placeholder_no_data);
        remoteViews.setOnClickPendingIntent(R.id.binder, createOnClickIntent(context, station));

        // Tell the AppWidgetManager to perform an update on the current app widget
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, android.R.id.list);
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }

    private PendingIntent createOnClickIntent(Context context, StopLocation station) {
        Intent onClickIntent = new Intent(context, NextDeparturesWidgetProvider.class);
        onClickIntent.setAction(INTENT_ACTION_LAUNCH_LIVEBOARD_VIEW);
        onClickIntent.putExtra(INTENT_EXTRA_STATION_ID, station);
        return PendingIntent.getActivity(context, 0, onClickIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void setErrorLayout(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(),
                R.layout.widget_nextdepartures_error);
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, android.R.id.list);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        updateAppWidget(context, appWidgetManager, appWidgetId);
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (Objects.equals(intent.getAction(), INTENT_ACTION_LAUNCH_LIVEBOARD_VIEW)) {
            String stationId = intent.getStringExtra(INTENT_EXTRA_STATION_ID);
            Intent liveboardLaunchIntent = LiveboardActivity.createShortcutIntent(context, stationId);
            liveboardLaunchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(liveboardLaunchIntent);
        }
    }
}