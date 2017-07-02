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
import android.support.v7.widget.RecyclerView;

import org.joda.time.DateTime;

import be.hyperrail.android.adapter.OnRecyclerItemClickListener;
import be.hyperrail.android.adapter.TrainCardAdapter;
import be.hyperrail.android.irail.contracts.IrailDataResponse;
import be.hyperrail.android.irail.db.Station;
import be.hyperrail.android.irail.implementation.Train;
import be.hyperrail.android.irail.implementation.TrainStop;
import be.hyperrail.android.irail.implementation.TrainStub;
import be.hyperrail.android.util.ErrorDialogFactory;

/**
 * Activity to show a train
 */
public class TrainActivity extends RecyclerViewActivity<Train> implements OnRecyclerItemClickListener<TrainStop> {

    private Station mScrollToStation;
    private Train mTrain;
    private TrainStub mCurrentSearchQuery;
    private DateTime mTrainDate;

    private AsyncTask runningTask;

    public static Intent createIntent(Context context, TrainStub stub, DateTime day) {
        Intent i = new Intent(context, TrainActivity.class);
        i.putExtra("stub", stub);
        i.putExtra("date", day);
        return i;
    }

    public static Intent createIntent(Context context, TrainStub stub, Station currentStation, DateTime day) {
        Intent i = createIntent(context, stub, day);
        i.putExtra("currentStation", currentStation);
        return i;
    }

    public static Intent createIntent(Context context, TrainStub stub, Station currentStation, Station destinationStation, DateTime day) {
        Intent i = createIntent(context, stub,currentStation,day);
        i.putExtra("destinationStation", destinationStation);
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey("train")) {
            this.mTrain = (Train) savedInstanceState.get("train");
        }

        super.onCreate(savedInstanceState);
        setTitle(R.string.title_train);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("train",mTrain);
    }

    @Override
    protected Train getRestoredInstanceStateItems() {
            return mTrain;
    }

    @Override
    protected int getLayout() {
        return R.layout.activity_train;
    }

    @Override
    protected int getMenuLayout() {
        return R.menu.actionbar_main;
    }

    @Override
    protected RecyclerView.Adapter getAdapter() {
        return new TrainCardAdapter(this, null);
    }

    protected void getData() {
        AsyncTask<TrainStub, Integer, IrailDataResponse<Train>> t = new AsyncTask<TrainStub, Integer, IrailDataResponse<Train>>() {

            @Override
            protected void onPostExecute(IrailDataResponse<Train> response) {
                super.onPostExecute(response);

                vRefreshLayout.setRefreshing(false);

                if (response.isSuccess()) {
                    mTrain = response.getData();
                    showData(mTrain);
                } else {
                    // only finish if we're loading new data
                    ErrorDialogFactory.showErrorDialog(response.getException(),TrainActivity.this, mTrain == null);
                }

            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                vRefreshLayout.setRefreshing(true);
            }

            @Override
            protected IrailDataResponse<Train> doInBackground(TrainStub... trains) {
                if (trains != null && trains.length > 0) {
                    return trains[0].getTrain(mTrainDate);
                } else {
                    return null;
                }
            }
        };

        if (runningTask != null && runningTask.getStatus() != AsyncTask.Status.FINISHED){
            // Keep the existing task running to reload this data
            return;
        }

        t.execute(mCurrentSearchQuery);

        runningTask = t;
    }

    @Override
    protected void getInitialData() {
        // which station should we scroll to?
        mScrollToStation = null;
        if (getIntent().hasExtra("currentStation")) {
            mScrollToStation = (Station) getIntent().getSerializableExtra("currentStation");
        }

        mCurrentSearchQuery = (TrainStub) getIntent().getSerializableExtra("stub");
       if (getIntent().hasExtra("date")){
           mTrainDate = (DateTime) getIntent().getSerializableExtra("date");
       } else {
           mTrainDate = new DateTime();
       }

        getData();
    }

    protected void getNextData() {
        // No next data
    }

    protected void showData(Train train) {
        setSubTitle(train.getName() + " " + train.getDirection().getLocalizedName());

        TrainCardAdapter adapter = new TrainCardAdapter(this, train);
        vRecyclerView.setAdapter(adapter);
        if (mScrollToStation != null) {
            int i = train.getStopNumberForStation(mScrollToStation);
            if (i >= 0) {
                vRecyclerView.scrollToPosition(i);

                // unset this value. On next refresh, show everything
                mScrollToStation = null;
            }
        }
        adapter.setOnItemClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (runningTask != null && runningTask.getStatus() != AsyncTask.Status.FINISHED){
            runningTask.cancel(true);
        }
    }

    @Override
    public void markFavorite(boolean favorite) {

    }

    @Override
    public boolean isFavorite() {
        return false;
    }

    @Override
    public void onRecyclerItemClick(RecyclerView.Adapter sender, TrainStop object) {
        Intent i = LiveboardActivity.createIntent(getApplicationContext(), object.getStation());
        startActivity(i);
    }
}
