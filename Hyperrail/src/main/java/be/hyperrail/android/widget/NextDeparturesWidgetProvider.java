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
import android.util.Log;
import android.widget.RemoteViews;

import be.hyperrail.android.R;
import be.hyperrail.android.activities.searchresult.LiveboardActivity;
import be.hyperrail.android.logging.HyperRailLog;
import be.hyperrail.opentransportdata.OpenTransportApi;
import be.hyperrail.opentransportdata.common.contracts.QueryTimeDefinition;
import be.hyperrail.opentransportdata.common.exceptions.StopLocationNotResolvedException;
import be.hyperrail.opentransportdata.common.models.LiveboardType;
import be.hyperrail.opentransportdata.common.models.StopLocation;
import be.hyperrail.opentransportdata.common.requests.LiveboardRequest;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;

public class NextDeparturesWidgetProvider extends AppWidgetProvider {
    SharedPreferences prefs;
    HyperRailLog log = HyperRailLog.getLogger(NextDeparturesWidgetProvider.class);

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        prefs = context.getSharedPreferences("widgets", 0);
        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }

        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                 int appWidgetId) {
        String id = this.prefs.getString("NextDepartures:" + appWidgetId, null);
        if (id == null) {
            Log.w("widgets", "No station ID found for NextDepartures widget id: " + appWidgetId);
            return;
        }

        StopLocation mStation;
        try {
            mStation = OpenTransportApi.getStopLocationProviderInstance().getStoplocationBySemanticId(id);
        } catch (StopLocationNotResolvedException searchException) {
            log.warning("Failed to lookup station URI for widget: " + id);
            log.logException(searchException);
            try {
                mStation = OpenTransportApi.getStopLocationProviderInstance().getStoplocationByHafasId(id);
            } catch (StopLocationNotResolvedException fallbackSearchException) {
                log.warning("Failed to lookup station ID for widget: " + id);
                log.logException(fallbackSearchException);
                mStation = null;
            }
        }

        if (mStation == null) {
            RemoteViews views = new RemoteViews(context.getPackageName(),
                    R.layout.widget_nextdepartures_error);
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, android.R.id.list);
            appWidgetManager.updateAppWidget(appWidgetId, views);
            return;
        }

        // Set up the intent that starts the NextDeparturesWidgetService, which will
        // provide the views for this collection.
        Intent serviceIntent = new Intent(context, NextDeparturesWidgetService.class);
        serviceIntent.putExtra(EXTRA_APPWIDGET_ID, appWidgetId);

        // Create an Intent to launch ExampleActivity
        Intent onClickIntent = LiveboardActivity.createIntent(context,
                new LiveboardRequest(mStation,
                        QueryTimeDefinition.EQUAL_OR_LATER,
                        LiveboardType.DEPARTURES,
                        null));
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, onClickIntent, 0);

        // Get the layout for the App Widget and attach an on-click listener
        // to the button
        RemoteViews views = new RemoteViews(context.getPackageName(),
                R.layout.widget_nextdepartures);
        views.setTextViewText(R.id.text_station, mStation.getLocalizedName());
        // Set up the RemoteViews object to use a RemoteViews adapter.
        // This adapter connects
        // to a RemoteViewsService  through the specified intent.
        // This is how you populate the data.
        views.setRemoteAdapter(android.R.id.list, serviceIntent);

        // The empty view is displayed when the collection has no items.
        // It should be in the same layout used to instantiate the RemoteViews
        // object above.
        views.setEmptyView(android.R.id.list, R.id.placeholder_no_data);

        views.setOnClickPendingIntent(R.id.binder, pendingIntent);

        // Tell the AppWidgetManager to perform an update on the current app widget
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, android.R.id.list);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

}