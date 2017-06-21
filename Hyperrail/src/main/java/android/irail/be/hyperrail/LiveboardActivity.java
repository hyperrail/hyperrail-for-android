/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package android.irail.be.hyperrail;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.irail.be.hyperrail.adapter.LiveboardCardAdapter;
import android.irail.be.hyperrail.adapter.onRecyclerItemClickListener;
import android.irail.be.hyperrail.infiniteScrolling.InfiniteScrollingAdapter;
import android.irail.be.hyperrail.infiniteScrolling.InfiniteScrollingDataSource;
import android.irail.be.hyperrail.irail.contracts.IrailDataResponse;
import android.irail.be.hyperrail.irail.db.Station;
import android.irail.be.hyperrail.irail.implementation.ApiResponse;
import android.irail.be.hyperrail.irail.implementation.LiveBoard;
import android.irail.be.hyperrail.irail.implementation.TrainStop;
import android.irail.be.hyperrail.util.ErrorDialogFactory;
import android.irail.be.hyperrail.util.OnDateTimeSetListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Activity to show a liveboard
 */
public class LiveboardActivity extends RecyclerViewActivity<LiveBoard> implements onRecyclerItemClickListener<TrainStop>, OnDateTimeSetListener, InfiniteScrollingDataSource {

    private LiveBoard mCurrentLiveboard;
    private Station mCurrentStation;

    private AsyncTask runningTask;

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
        if (runningTask != null  && runningTask.getStatus() != AsyncTask.Status.FINISHED){
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
        LiveboardCardAdapter adapter = new LiveboardCardAdapter(this, vRecyclerView, this, null);
        adapter.setOnItemClickListener(this);
        return adapter;
    }

    @Override
    protected void getInitialData() {
        if (mCurrentLiveboard == null) {
            getData();
        } else {
            showData(mCurrentLiveboard);
        }
    }

    @Override
    protected void getData() {
        AsyncTask<Station, Integer, IrailDataResponse<LiveBoard>> t = new AsyncTask<Station, Integer, IrailDataResponse<LiveBoard>>() {

            @Override
            protected void onPostExecute(IrailDataResponse<LiveBoard> response) {
                super.onPostExecute(response);

                vRefreshLayout.setRefreshing(false);

                if (response.isSuccess()) {
                    // store retrieved data
                    mCurrentLiveboard = response.getData();

                    // Show retrieved data
                    showData(mCurrentLiveboard);

                    // If we didn't get a result, try the next data
                    if (response.getData().getStops().length == 0) {
                        LiveboardActivity.this.getNextData();
                    } else {
                        // Enable infinite scrolling, in case it was disabled during a previous search
                        ((InfiniteScrollingAdapter)vRecyclerView.getAdapter()).setInfiniteScrolling(true);
                    }

                } else {
                    // only finish if we're loading new data
                    ErrorDialogFactory.showErrorDialog(response.getException(), LiveboardActivity.this, mCurrentLiveboard == null);
                }

            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                if (mSearchDate != null) {
                    vWarningNotRealtime.setVisibility(View.VISIBLE);
                    @SuppressLint("SimpleDateFormat")
                    DateFormat df = new SimpleDateFormat(getString(R.string.warning_not_realtime_datetime));
                    vWarningNotRealtimeText.setText(String.format("%s %s", getString(R.string.warning_not_realtime), df.format(mSearchDate)));
                } else {
                    vWarningNotRealtime.setVisibility(View.GONE);
                }
            }

            @Override
            protected IrailDataResponse<LiveBoard> doInBackground(Station... stations) {
                    if (mSearchDate == null) {
                        return stations[0].getLiveBoard();
                    } else {
                        return stations[0].getLiveBoard(mSearchDate);
                    }
            }
        };

        if (runningTask != null && runningTask.getStatus() != AsyncTask.Status.FINISHED){
            runningTask.cancel(true);
        }

        t.execute(mCurrentStation);
        runningTask = t;
    }

    @Override
    protected void getNextData() {
        if (mCurrentLiveboard == null) {
            return;
        }

        AsyncTask<LiveBoard, Integer, ApiResponse<TrainStop[]>> t = new AsyncTask<LiveBoard, Integer, ApiResponse<TrainStop[]>>() {

            @Override
            protected void onPostExecute(ApiResponse<TrainStop[]> liveboard) {
                super.onPostExecute(liveboard);

                if (! liveboard.isSuccess()) {
                    ErrorDialogFactory.showErrorDialog(liveboard.getException(), LiveboardActivity.this, false);
                    ((LiveboardCardAdapter) vRecyclerView.getAdapter()).resetInfiniteScrollingState();
                    return;
                }

                if ( liveboard.getData().length == 0){
                    ((InfiniteScrollingAdapter)vRecyclerView.getAdapter()).setInfiniteScrolling(false);
                }

                showData(mCurrentLiveboard);
            }

            @Override
            protected ApiResponse<TrainStop[]> doInBackground(LiveBoard... liveboards) {
                return mCurrentLiveboard.getNextStops();
            }
        };
        t.execute(mCurrentLiveboard);
        runningTask = t;
    }

    @Override
    protected void showData(LiveBoard liveBoard) {
        if (liveBoard != null) {
            setTitle(getString(R.string.title_departures));
            setSubTitle(liveBoard.getLocalizedName());
        }

        if (mSearchDate == null){
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
            persistentQueryProvider.addFavoriteStation(mCurrentStation.getLocalizedName());
            Snackbar.make(vLayoutRoot, R.string.marked_station_favorite, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.undo, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            LiveboardActivity.this.markFavorite(false);
                        }
                    })
                    .show();
        } else {
            persistentQueryProvider.removeFavoriteStation(mCurrentStation.getLocalizedName());
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
        return mCurrentStation != null && persistentQueryProvider.isFavoriteStation(mCurrentStation.getLocalizedName());

    }

}
