/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.fragments.searchResult;

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
import be.hyperrail.android.activities.searchResult.LiveboardActivity;
import be.hyperrail.android.activities.searchResult.TrainActivity;
import be.hyperrail.android.adapter.OnRecyclerItemClickListener;
import be.hyperrail.android.adapter.OnRecyclerItemLongClickListener;
import be.hyperrail.android.adapter.TrainStopCardAdapter;
import be.hyperrail.android.infiniteScrolling.InfiniteScrollingDataSource;
import be.hyperrail.android.irail.contracts.IRailErrorResponseListener;
import be.hyperrail.android.irail.contracts.IRailSuccessResponseListener;
import be.hyperrail.android.irail.contracts.IrailRequest;
import be.hyperrail.android.irail.contracts.RouteTimeDefinition;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.irail.implementation.Train;
import be.hyperrail.android.irail.implementation.TrainStop;
import be.hyperrail.android.irail.implementation.requests.IrailLiveboardRequest;
import be.hyperrail.android.irail.implementation.requests.IrailTrainRequest;
import be.hyperrail.android.persistence.PersistentQueryProvider;
import be.hyperrail.android.persistence.Suggestion;
import be.hyperrail.android.persistence.SuggestionType;
import be.hyperrail.android.util.ErrorDialogFactory;

/**
 * A fragment for showing liveboard results
 */
public class TrainFragment extends RecyclerViewFragment<Train> implements InfiniteScrollingDataSource, ResultFragment<IrailTrainRequest>, OnRecyclerItemClickListener<TrainStop>, OnRecyclerItemLongClickListener<TrainStop> {

    private Train mCurrentTrain;
    private IrailTrainRequest mRequest;
    private TrainStopCardAdapter mRecyclerviewAdapter;

    public static TrainFragment createInstance(IrailTrainRequest request) {
        TrainFragment frg = new TrainFragment();
        frg.setRequest(request);
        return frg;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recyclerview_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
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
        if (mRecyclerviewAdapter == null) {
            mRecyclerviewAdapter = new TrainStopCardAdapter(getActivity(), null);
        }
        mRecyclerviewAdapter.setOnItemClickListener(this);
        mRecyclerviewAdapter.setOnItemLongClickListener(this);
        return mRecyclerviewAdapter;
    }

    @Override
    protected void getInitialData() {
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

        mRecyclerviewAdapter.updateTrain(train);
        mRequest.setOrigin(train.getStops()[0].getStation());
        mRequest.setDirection(train.getDirection());

        // Update the request in the activity, so additional information will be stored when marking it as favorite
        if (getActivity() instanceof TrainActivity) {
            ((TrainActivity) getActivity()).setRequest(mRequest);
        }

        PersistentQueryProvider.getInstance(getActivity()).store(new Suggestion<IrailRequest>(mRequest, SuggestionType.HISTORY));

        if (!mRequest.isNow()) {
            int i = train.getStopnumberForDepartureTime(mRequest.getSearchTime());
            if (i >= 0) {
                vRecyclerView.scrollToPosition(i);
            }
        }
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
        // TODO: TrainStop objects should have a way to distinguish the first and last stop
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
