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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.joda.time.DateTime;

import java.io.FileNotFoundException;

import be.hyperrail.android.R;
import be.hyperrail.android.TrainstopContextMenu;
import be.hyperrail.android.activities.searchResult.TrainActivity;
import be.hyperrail.android.adapter.LiveboardCardAdapter;
import be.hyperrail.android.adapter.OnRecyclerItemClickListener;
import be.hyperrail.android.adapter.OnRecyclerItemLongClickListener;
import be.hyperrail.android.infiniteScrolling.InfiniteScrollingAdapter;
import be.hyperrail.android.infiniteScrolling.InfiniteScrollingDataSource;
import be.hyperrail.android.irail.contracts.IRailErrorResponseListener;
import be.hyperrail.android.irail.contracts.IRailSuccessResponseListener;
import be.hyperrail.android.irail.contracts.IrailDataProvider;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.irail.implementation.LiveBoard;
import be.hyperrail.android.irail.implementation.LiveboardAppendHelper;
import be.hyperrail.android.irail.implementation.TrainStop;
import be.hyperrail.android.irail.implementation.requests.IrailLiveboardRequest;
import be.hyperrail.android.irail.implementation.requests.IrailTrainRequest;
import be.hyperrail.android.util.ErrorDialogFactory;

/**
 * A fragment for showing liveboard results
 */
public class LiveboardFragment extends RecyclerViewFragment<LiveBoard> implements InfiniteScrollingDataSource, ResultFragment<IrailLiveboardRequest>, OnRecyclerItemClickListener<TrainStop>, OnRecyclerItemLongClickListener<TrainStop> {

    private LiveBoard mCurrentLiveboard;
    private LiveboardCardAdapter mLiveboardCardAdapter;
    private IrailLiveboardRequest mRequest;

    public static LiveboardFragment createInstance(IrailLiveboardRequest request) {
        // Clone the request so we can't accidentally modify the original
        LiveboardFragment frg = new LiveboardFragment();
        frg.mRequest = request;
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
    public void setRequest(@NonNull IrailLiveboardRequest request) {
        this.mRequest = request;
        getInitialData();
    }

    @Override
    public IrailLiveboardRequest getRequest() {
        return this.mRequest;
    }

    @Override
    public void onDateTimePicked(DateTime d) {
        mRequest.setSearchTime(d);
        getData();
    }

    @Override
    protected RecyclerView.Adapter getAdapter() {
        if (mLiveboardCardAdapter == null) {
            mLiveboardCardAdapter = new LiveboardCardAdapter(this.getActivity(), vRecyclerView, this);
            mLiveboardCardAdapter.setOnItemClickListener(this);
            mLiveboardCardAdapter.setOnItemLongClickListener(this);
        }
        return mLiveboardCardAdapter;
    }

    @Override
    protected void getInitialData() {
        getData();
    }

    @Override
    protected void getData() {
        if (this.vRefreshLayout.isRefreshing()) {
            // Disable infinite scrolling for now to prevent having 2 loading icons
            // Also prevents the loadNext method from trying to load all the time during initial load
            ((InfiniteScrollingAdapter) vRecyclerView.getAdapter()).setInfiniteScrolling(false);
        }

        mCurrentLiveboard = null;
        showData(null);

        IrailDataProvider api = IrailFactory.getDataProviderInstance();
        // Don't abort all queries: there might be multiple fragments at the same screen!

        mRequest.setCallback(new IRailSuccessResponseListener<LiveBoard>() {
            @Override
            public void onSuccessResponse(@NonNull LiveBoard data, Object tag) {
                vRefreshLayout.setRefreshing(false);

                // store retrieved data
                mCurrentLiveboard = data;
                // Show retrieved data
                showData(mCurrentLiveboard);

                // If we didn't get a result, try the next data
                if (data.getStops().length == 0) {
                    LiveboardFragment.this.loadNextRecyclerviewItems();
                } else {
                    // Enable infinite scrolling again
                    ((InfiniteScrollingAdapter) vRecyclerView.getAdapter()).setInfiniteScrolling(true);
                }

                // Scroll past the load earlier item
                ((LinearLayoutManager) vRecyclerView.getLayoutManager()).scrollToPositionWithOffset(1, 0);
            }

        }, new IRailErrorResponseListener() {
            @Override
            public void onErrorResponse(@NonNull Exception e, Object tag) {
                vRefreshLayout.setRefreshing(false);
                // only finish if we're loading new data
                ErrorDialogFactory.showErrorDialog(e, LiveboardFragment.this.getActivity(), mCurrentLiveboard == null);
            }
        }, null);
        api.getLiveboard(mRequest);
    }

    @Override
    public void loadNextRecyclerviewItems() {
        // When not yet initialized with the first data, don't load more
        // mCurrentLiveboard won't be empty after the first response: its stops array might be empty, but the object will be NonNull
        if (mCurrentLiveboard == null) {
            ((InfiniteScrollingAdapter) vRecyclerView.getAdapter()).setNextLoaded();
            return;
        }

        LiveboardAppendHelper helper = new LiveboardAppendHelper();
        helper.appendLiveboard(mCurrentLiveboard, new IRailSuccessResponseListener<LiveBoard>() {
            @Override
            public void onSuccessResponse(@NonNull LiveBoard data, Object tag) {
                // Compare the new one with the old one to check if stops have been added
                if (data.getStops().length == mCurrentLiveboard.getStops().length) {
                    ((InfiniteScrollingAdapter) vRecyclerView.getAdapter()).setInfiniteScrolling(false);
                    ErrorDialogFactory.showErrorDialog(new FileNotFoundException("No results"), getActivity(), data.getStops().length == 0);
                    ((InfiniteScrollingAdapter) vRecyclerView.getAdapter()).disableInfiniteNext();
                }
                mCurrentLiveboard = data;
                showData(mCurrentLiveboard);

                ((InfiniteScrollingAdapter) vRecyclerView.getAdapter()).setNextLoaded();

                // Scroll past the "load earlier"
                LinearLayoutManager mgr = ((LinearLayoutManager) vRecyclerView.getLayoutManager());
                if (mgr.findFirstVisibleItemPosition() == 0) {
                    mgr.scrollToPositionWithOffset(1, 0);
                }

            }
        }, new IRailErrorResponseListener() {
            @Override
            public void onErrorResponse(@NonNull Exception e, Object tag) {
                ErrorDialogFactory.showErrorDialog(e, LiveboardFragment.this.getActivity(), false);
                ((LiveboardCardAdapter) vRecyclerView.getAdapter()).setNextLoaded();
            }
        });
    }

    @Override
    public void loadPreviousRecyclerviewItems() {
        // When not yet initialized with the first data, don't load previous
        if (mCurrentLiveboard == null) {
            ((InfiniteScrollingAdapter) vRecyclerView.getAdapter()).setPrevLoaded();
            return;
        }

        LiveboardAppendHelper helper = new LiveboardAppendHelper();
        helper.prependLiveboard(mCurrentLiveboard, new IRailSuccessResponseListener<LiveBoard>() {
            @Override
            public void onSuccessResponse(@NonNull LiveBoard data, Object tag) {
                // Compare the new one with the old one to check if stops have been added
                if (data.getStops().length == mCurrentLiveboard.getStops().length) {
                    ErrorDialogFactory.showErrorDialog(new FileNotFoundException("No results"), getActivity(), false);
                    ((InfiniteScrollingAdapter) vRecyclerView.getAdapter()).disableInfinitePrevious();
                }
                mCurrentLiveboard = data;
                showData(mCurrentLiveboard);

                // Scroll past the load earlier item
                ((LinearLayoutManager) vRecyclerView.getLayoutManager()).scrollToPositionWithOffset(1, 0);

                ((InfiniteScrollingAdapter) vRecyclerView.getAdapter()).setPrevLoaded();
            }
        }, new IRailErrorResponseListener() {
            @Override
            public void onErrorResponse(@NonNull Exception e, Object tag) {
                ErrorDialogFactory.showErrorDialog(e, LiveboardFragment.this.getActivity(), false);
                ((LiveboardCardAdapter) vRecyclerView.getAdapter()).setPrevLoaded();
            }
        });
    }

    @Override
    protected void showData(LiveBoard liveBoard) {
        mLiveboardCardAdapter.updateLiveboard(liveBoard);
    }


    @Override
    public void onRecyclerItemClick(RecyclerView.Adapter sender, TrainStop object) {
        Intent i = TrainActivity.createIntent(getActivity(), new IrailTrainRequest(object.getTrain().getId(), object.getDepartureTime()));
        startActivity(i);
    }

    @Override
    public void onRecyclerItemLongClick(RecyclerView.Adapter sender, TrainStop object) {
        (new TrainstopContextMenu(getActivity(), object)).show();
    }

}
