/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android;/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

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

import be.hyperrail.android.irail.contracts.IRailSuccessResponseListener;
import be.hyperrail.android.irail.contracts.RouteTimeDefinition;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.irail.implementation.LiveBoard;
import be.hyperrail.android.irail.implementation.OccupancyHelper;
import be.hyperrail.android.irail.implementation.VehicleStop;
import be.hyperrail.android.irail.implementation.requests.IrailLiveboardRequest;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;

public class NextDeparturesWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new NextDeparturesRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}

class NextDeparturesRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private final Context mContext;
    private final Intent mIntent;
    private LiveBoard mLiveboard;

    public NextDeparturesRemoteViewsFactory(Context applicationContext, Intent intent) {
        mContext = applicationContext;
        mIntent = intent;
    }

    @Override
    public void onCreate() {
        String id = mContext.getSharedPreferences("widgets", 0).getString("NextDepartures:" + mIntent.getIntExtra(EXTRA_APPWIDGET_ID, 0), null);
        if (id == null) {
            throw new IllegalStateException();
        }

        IrailLiveboardRequest request = new IrailLiveboardRequest(
                IrailFactory.getStationsProviderInstance().getStationById(id),
                RouteTimeDefinition.DEPART,
                null
        );
        request.setCallback(new IRailSuccessResponseListener<LiveBoard>() {
            @Override
            public void onSuccessResponse(@NonNull LiveBoard data, Object tag) {
                Log.w("widgets", "Received iRail data...");
                NextDeparturesRemoteViewsFactory.this.mLiveboard = data;
            }
        }, null, null);
        Log.w("widgets", "Requesting iRail data...");
        IrailFactory.getDataProviderInstance().getLiveboard(request);
    }

    @Override
    public void onDataSetChanged() {

    }

    @Override
    public void onDestroy() {

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
        RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.listview_liveboard);
        VehicleStop stop = mLiveboard.getStops()[0];

        rv.setTextViewText(R.id.text_destination, stop.getDestination().getLocalizedName());

        rv.setTextViewText(R.id.text_train_number, stop.getTrain().getNumber());
        rv.setTextViewText(R.id.text_train_type, stop.getTrain().getType());

        DateTimeFormatter df = DateTimeFormat.forPattern("HH:mm");

        if (stop.getDepartureTime() != null) {
            rv.setTextViewText(R.id.text_departure_time, df.print(stop.getDepartureTime()));
            if (stop.getDepartureDelay().getStandardSeconds() > 0) {
                rv.setTextViewText(R.id.text_departure_delay, mContext.getString(be.hyperrail.android.R.string.delay, stop.getDepartureDelay().getStandardMinutes()));
                rv.setTextViewText(R.id.text_delay_time, df.print(stop.getDelayedDepartureTime()));
            } else {
                rv.setTextViewText(R.id.text_departure_delay, "");
                rv.setTextViewText(R.id.text_delay_time, "");
            }
        } else {
            // support for arrivals
            rv.setTextViewText(R.id.text_departure_time, df.print(stop.getArrivalTime()));
            if (stop.getArrivalDelay().getStandardSeconds() > 0) {
                rv.setTextViewText(R.id.text_departure_delay, mContext.getString(be.hyperrail.android.R.string.delay, stop.getArrivalDelay().getStandardMinutes()));
                rv.setTextViewText(R.id.text_delay_time, df.print(stop.getDelayedArrivalTime()));
            } else {
                rv.setTextViewText(R.id.text_departure_delay, "");
                rv.setTextViewText(R.id.text_delay_time, "");
            }
        }

        rv.setTextViewText(R.id.text_platform, stop.getPlatform());

        if (stop.isDepartureCanceled()) {
            rv.setTextViewText(R.id.text_platform, "X");

            rv.setTextViewText(R.id.text_train_status, mContext.getString(be.hyperrail.android.R.string.status_cancelled));
            rv.setViewVisibility(R.id.layout_train_status_container, View.VISIBLE);
            rv.setViewVisibility(R.id.container_occupancy, View.GONE);
        } else {
            rv.setViewVisibility(R.id.layout_train_status_container, View.GONE);
            rv.setViewVisibility(R.id.container_occupancy, View.VISIBLE);
        }

        rv.setBitmap(R.id.container_occupancy, "setImageDrawable",
                ((BitmapDrawable) ContextCompat.getDrawable(mContext,
                        OccupancyHelper.getOccupancyDrawable(stop.getOccupancyLevel())
                )).getBitmap()
        );

        // Return the remote views object.
        return rv;
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