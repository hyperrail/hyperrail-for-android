/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;

import com.google.firebase.analytics.FirebaseAnalytics;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.FileNotFoundException;

import be.hyperrail.android.adapter.LiveboardCardAdapter;
import be.hyperrail.android.adapter.OnRecyclerItemClickListener;
import be.hyperrail.android.infiniteScrolling.InfiniteScrollingAdapter;
import be.hyperrail.android.infiniteScrolling.InfiniteScrollingDataSource;
import be.hyperrail.android.irail.contracts.IrailResponseListener;
import be.hyperrail.android.irail.db.Station;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.irail.implementation.LiveBoard;
import be.hyperrail.android.irail.implementation.LiveboardAppendHelper;
import be.hyperrail.android.irail.implementation.TrainStop;
import be.hyperrail.android.util.ErrorDialogFactory;
import be.hyperrail.android.util.OnDateTimeSetListener;

/**
 * Activity to show a liveboard
 */
public class LiveboardActivity extends RecyclerViewActivity<LiveBoard> implements OnRecyclerItemClickListener<TrainStop>, OnDateTimeSetListener, InfiniteScrollingDataSource, IrailResponseListener<LiveBoard> {

    private LiveBoard mCurrentLiveboard;
    private Station mCurrentStation;

    private AsyncTask runningTask;
    private FirebaseAnalytics mFirebaseAnalytics;

    private static final int NEW_DATA = 0;
    private static final int APPEND_DATA = 1;

    public static Intent createIntent(Context context, Station station) {
        Intent i = new Intent(context, LiveboardActivity.class);
        i.putExtra("station", station);
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.mCurrentStation = (Station) getIntent().getSerializableExtra("station");

        if (savedInstanceState != null && savedInstanceState.containsKey("liveboard")) {
            this.mCurrentLiveboard = (LiveBoard) savedInstanceState.get("liveboard");
        }

        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.title_departures));

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, mCurrentStation.getId());
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, mCurrentStation.getName());
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "liveboard");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_SEARCH_RESULTS, bundle);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("liveboard", mCurrentLiveboard);
    }

    @Override
    protected LiveBoard getRestoredInstanceStateItems() {
        return mCurrentLiveboard;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (runningTask != null && runningTask.getStatus() != AsyncTask.Status.FINISHED) {
            runningTask.cancel(true);
        }
    }

    @Override
    protected int getLayout() {
        return R.layout.activity_liveboard;
    }

    @Override
    protected int getMenuLayout() {
        return R.menu.actionbar_searchresult_station;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_from:
                startActivity(MainActivity.createRouteFromIntent(getApplicationContext(), mCurrentStation.getName()));
                return true;
            case R.id.action_to:
                startActivity(MainActivity.createRouteToIntent(getApplicationContext(), mCurrentStation.getName()));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected InfiniteScrollingAdapter<TrainStop> getAdapter() {
        LiveboardCardAdapter adapter = new LiveboardCardAdapter(this, vRecyclerView, this);
        adapter.setOnItemClickListener(this);
        return adapter;
    }

    @Override
    protected void getInitialData() {
        getData();
    }

    @Override
    protected void getData() {

        if (mSearchDate != null) {
            vWarningNotRealtime.setVisibility(View.VISIBLE);
            DateTimeFormatter df = DateTimeFormat.forPattern(getString(R.string.warning_not_realtime_datetime));
            vWarningNotRealtimeText.setText(String.format("%s %s", getString(R.string.warning_not_realtime), df.print(mSearchDate)));
        } else {
            vWarningNotRealtime.setVisibility(View.GONE);
        }
        mCurrentLiveboard = null;
        showData(null);

        IrailFactory.getDataProviderInstance().abortAllQueries();
        if (mSearchDate == null) {
            mCurrentStation.getLiveBoard(this, NEW_DATA);
        } else {
            mCurrentStation.getLiveBoard(this, NEW_DATA, mSearchDate);
        }
    }

    @Override
    protected void getNextData() {
        if (mCurrentLiveboard == null) {
            return;
        }

        LiveboardAppendHelper helper = new LiveboardAppendHelper();
        helper.appendLiveboard(this, APPEND_DATA, mCurrentLiveboard);
    }

    @Override
    protected void showData(LiveBoard liveBoard) {
        if (liveBoard != null) {
            setSubTitle(liveBoard.getLocalizedName());
        }

        if (mSearchDate == null) {
            vWarningNotRealtime.setVisibility(View.GONE);
        }

        ((LiveboardCardAdapter) vRecyclerView.getAdapter()).updateLiveboard(liveBoard);
    }

    @Override
    public void onRecyclerItemClick(RecyclerView.Adapter sender, TrainStop object) {
        Intent i = TrainActivity.createIntent(getApplicationContext(),
                object.getTrain(),
                mCurrentLiveboard,
                object.getDepartureTime());
        startActivity(i);
    }

    @Override
    public void markFavorite(boolean favorite) {
        // Don't favorite stuff before it's loaded
        if (mCurrentLiveboard == null) {
            return;
        }

        if (favorite) {
            mPersistentQuaryProvider.addFavoriteStation(mCurrentStation);
            Snackbar.make(vLayoutRoot, R.string.marked_station_favorite, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.undo, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            LiveboardActivity.this.markFavorite(false);
                        }
                    })
                    .show();
        } else {
            mPersistentQuaryProvider.removeFavoriteStation(mCurrentStation);
            Snackbar.make(vLayoutRoot, R.string.unmarked_station_favorite, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.undo, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            LiveboardActivity.this.markFavorite(true);
                        }
                    })
                    .show();
        }
        setFavoriteDisplayState(favorite);
    }

    @Override
    public boolean isFavorite() {
        // If it's not loaded, it's not a favorite
        return mCurrentStation != null && mPersistentQuaryProvider.isFavoriteStation(mCurrentStation);

    }

    @Override
    public void onIrailSuccessResponse(LiveBoard data, int tag) {
        switch (tag) {
            case NEW_DATA:
                // store retrieved data
                mCurrentLiveboard = data;
                // Show retrieved data
                showData(mCurrentLiveboard);

                // If we didn't get a result, try the next data
                if (data.getStops().length == 0) {
                    LiveboardActivity.this.getNextData();
                } else {
                    // Enable infinite scrolling, in case it was disabled during a previous search
                    ((InfiniteScrollingAdapter) vRecyclerView.getAdapter()).setInfiniteScrolling(true);
                }
                break;

            case APPEND_DATA:
                // Compare the new one with the old one to check if stops have been added
                if (data.getStops().length == mCurrentLiveboard.getStops().length) {
                    ((InfiniteScrollingAdapter) vRecyclerView.getAdapter()).setInfiniteScrolling(false);
                    if (mCurrentLiveboard == null) {
                        ErrorDialogFactory.showErrorDialog(new FileNotFoundException("No results"), LiveboardActivity.this, (mSearchDate == null));
                    }
                }
                mCurrentLiveboard = data;
                showData(mCurrentLiveboard);
        }
    }

    @Override
    public void onIrailErrorResponse(Exception e, int tag) {
        switch (tag) {
            case APPEND_DATA:
                ErrorDialogFactory.showErrorDialog(e, LiveboardActivity.this, false);
                ((LiveboardCardAdapter) vRecyclerView.getAdapter()).resetInfiniteScrollingState();
        }
    }
}
