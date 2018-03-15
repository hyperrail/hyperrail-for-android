/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.widget;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import be.hyperrail.android.R;
import be.hyperrail.android.irail.contracts.IRailSuccessResponseListener;
import be.hyperrail.android.irail.contracts.RouteTimeDefinition;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.irail.implementation.Liveboard;
import be.hyperrail.android.irail.implementation.OccupancyHelper;
import be.hyperrail.android.irail.implementation.VehicleStop;
import be.hyperrail.android.irail.implementation.requests.IrailLiveboardRequest;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;

class NextDeparturesRemoteViewsDataProvider implements RemoteViewsService.RemoteViewsFactory {
    private final Context mContext;
    private final Intent mIntent;
    private Liveboard mLiveboard;

    public NextDeparturesRemoteViewsDataProvider(Context applicationContext, Intent intent) {
        mContext = applicationContext;
        mIntent = intent;
    }

    @Override
    public void onCreate() {
        initLiveboard();
    }

    private void initLiveboard() {
        String id = mContext.getSharedPreferences("widgets", 0).getString(
                "NextDepartures:" + mIntent.getIntExtra(EXTRA_APPWIDGET_ID, 0), null);
        if (id == null) {
            throw new IllegalStateException();
        }

        IrailLiveboardRequest request = new IrailLiveboardRequest(
                IrailFactory.getStationsProviderInstance().getStationById(id),
                RouteTimeDefinition.DEPART_AT,
                Liveboard.LiveboardType.DEPARTURES,
                null
        );
        request.setCallback(new IRailSuccessResponseListener<Liveboard>() {
            @Override
            public void onSuccessResponse(@NonNull Liveboard data, Object tag) {
                Log.w("widgets", "Received iRail data...");
                NextDeparturesRemoteViewsDataProvider.this.mLiveboard = data;
            }
        }, null, null);
        Log.w("widgets", "Requesting iRail data...");
        IrailFactory.getDataProviderInstance().getLiveboard(request);

        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            e.printStackTrace();
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
        if (mLiveboard == null) {
            return 0;
        }
        return mLiveboard.getStops().length;
    }

    @Override
    public RemoteViews getViewAt(int position) {
        // Construct a remote views item based on the app widget item XML file,
        // and set the text based on the position.
        RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.listview_liveboard_widget);
        VehicleStop stop = mLiveboard.getStops()[position];

        bindTimeAndDelays(rv, stop);

        rv.setTextViewText(R.id.text_destination, stop.getDestination().getLocalizedName());

        rv.setTextViewText(R.id.text_train_number, stop.getVehicle().getNumber());
        rv.setTextViewText(R.id.text_train_type, stop.getVehicle().getType());
        rv.setTextViewText(R.id.text_platform, stop.getPlatform());

        if (stop.isDepartureCanceled()) {
            rv.setTextViewText(R.id.text_platform, "X");

            rv.setTextViewText(R.id.text_train_status,
                               mContext.getString(R.string.status_cancelled));
            rv.setViewVisibility(R.id.layout_train_status_container, View.VISIBLE);
            rv.setViewVisibility(R.id.container_occupancy, View.GONE);
        } else {
            rv.setViewVisibility(R.id.layout_train_status_container, View.GONE);
            rv.setViewVisibility(R.id.container_occupancy, View.VISIBLE);
        }

        rv.setBitmap(R.id.container_occupancy, "setImageDrawable",
                     ((BitmapDrawable) ContextCompat.getDrawable(mContext,
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
                                   mContext.getString(R.string.delay,
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
                                   mContext.getString(R.string.delay,
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

//... include adapter-like methods here. See the StackView Widget sample.

}
