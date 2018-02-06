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

package be.hyperrail.android.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.joda.time.DateTime;

import be.hyperrail.android.R;
import be.hyperrail.android.TrainstopContextMenu;
import be.hyperrail.android.activities.LiveboardActivity;
import be.hyperrail.android.adapter.OnRecyclerItemClickListener;
import be.hyperrail.android.adapter.OnRecyclerItemLongClickListener;
import be.hyperrail.android.adapter.TrainStopCardAdapter;
import be.hyperrail.android.infiniteScrolling.InfiniteScrollingDataSource;
import be.hyperrail.android.irail.contracts.IRailErrorResponseListener;
import be.hyperrail.android.irail.contracts.IRailSuccessResponseListener;
import be.hyperrail.android.irail.contracts.RouteTimeDefinition;
import be.hyperrail.android.irail.db.Station;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.irail.implementation.Train;
import be.hyperrail.android.irail.implementation.TrainStop;
import be.hyperrail.android.irail.implementation.requests.IrailLiveboardRequest;
import be.hyperrail.android.irail.implementation.requests.IrailTrainRequest;
import be.hyperrail.android.util.ErrorDialogFactory;

/**
 * A fragment for showing liveboard results
 */
public class TrainFragment extends RecyclerViewFragment<Train> implements InfiniteScrollingDataSource, ResultFragment<IrailTrainRequest>, OnRecyclerItemClickListener<TrainStop>, OnRecyclerItemLongClickListener<TrainStop> {

    private Train mCurrentTrain;
    private Station mScrollToStation;
    private IrailTrainRequest mRequest;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recyclerview_list, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void setRequest(@NonNull IrailTrainRequest request) {
        this.mRequest = request;
    }

    @Override
    public IrailTrainRequest getRequest() {
        return this.mRequest;
    }

    @Override
    public void onDateTimePicked(DateTime d) {
        mRequest.setSearchTime(d);
        getData();
    }

    @Override
    protected RecyclerView.Adapter getAdapter() {
        // An empty adapter for now. It will be replaced when we get the actual data
        // TODO: instead of replacing the adapter, update the contents
        return new TrainStopCardAdapter(getActivity(), null);
    }

    @Override
    protected void getInitialData() {
        // which station should we scroll to?
        mScrollToStation = mRequest.getTargetStation();
        getData();
    }

    protected void getData() {
        vRefreshLayout.setRefreshing(true);

        IrailFactory.getDataProviderInstance().abortAllQueries();

        IrailTrainRequest request = new IrailTrainRequest(mRequest.getTrainId(), mRequest.getSearchTime());
        request.setCallback(new IRailSuccessResponseListener<Train>() {
            @Override
            public void onSuccessResponse(@NonNull Train data, Object tag) {
                vRefreshLayout.setRefreshing(false);
                mCurrentTrain = data;
                showData(mCurrentTrain);
            }
        }, new IRailErrorResponseListener() {
            @Override
            public void onErrorResponse(@NonNull Exception e, Object tag) {
                vRefreshLayout.setRefreshing(false);

                // only finish if we're loading new data
                ErrorDialogFactory.showErrorDialog(e, getActivity(), mCurrentTrain == null);
            }
        }, null);
        IrailFactory.getDataProviderInstance().getTrain(request);
    }

    protected void showData(Train train) {
        getActivity().setTitle(train.getName() + " " + train.getDirection().getLocalizedName());

        TrainStopCardAdapter adapter = new TrainStopCardAdapter(getActivity(), train);
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
        adapter.setOnItemLongClickListener(this);
    }

    @Override
    public void loadNextRecyclerviewItems() {
        // Not supported
    }

    @Override
    public void loadPreviousRecyclerviewItems() {
        // Not supported
    }

    @Override
    public void onRecyclerItemClick(RecyclerView.Adapter sender, TrainStop object) {
        // TODO: trainstops should have a way to distinguish the first and last stop
        DateTime queryTime = object.getArrivalTime();
        if (queryTime == null) {
            queryTime = object.getDepartureTime();
        }
        Intent i = LiveboardActivity.createIntent(getActivity(), new IrailLiveboardRequest(object.getStation(), RouteTimeDefinition.DEPART, queryTime));
        startActivity(i);
    }

    @Override
    public void onRecyclerItemLongClick(RecyclerView.Adapter sender, TrainStop stop) {
        (new TrainstopContextMenu(getActivity(), stop)).show();
    }

}
