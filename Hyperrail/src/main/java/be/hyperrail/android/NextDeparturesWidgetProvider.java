/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import be.hyperrail.android.irail.db.Station;

import static android.appwidget.AppWidgetManager.ACTION_APPWIDGET_DELETED;
import static android.appwidget.AppWidgetManager.ACTION_APPWIDGET_DISABLED;
import static android.appwidget.AppWidgetManager.ACTION_APPWIDGET_ENABLED;
import static android.appwidget.AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED;
import static android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE;

public class NextDeparturesWidgetProvider extends AppWidgetProvider {

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        switch (intent.getAction()) {
            case ACTION_APPWIDGET_UPDATE:
            case ACTION_APPWIDGET_DELETED:
            case ACTION_APPWIDGET_ENABLED:
            case ACTION_APPWIDGET_DISABLED:
            case ACTION_APPWIDGET_OPTIONS_CHANGED:
        }
    }

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int N = appWidgetIds.length;

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i = 0; i < N; i++) {
            int appWidgetId = appWidgetIds[i];
            Station mStation = null; // todo: load
            // Create an Intent to launch ExampleActivity
            Intent intent = LiveboardActivity.createIntent(context, mStation);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

            // Get the layout for the App Widget and attach an on-click listener
            // to the button
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_nextdepartures);
            views.setOnClickPendingIntent(R.id.binder, pendingIntent);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}