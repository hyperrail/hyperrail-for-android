/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.fragments.searchresult;

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
import be.hyperrail.android.VehiclePopupContextMenu;
import be.hyperrail.android.activities.searchresult.VehicleActivity;
import be.hyperrail.android.adapter.LiveboardCardAdapter;
import be.hyperrail.android.adapter.OnRecyclerItemClickListener;
import be.hyperrail.android.adapter.OnRecyclerItemLongClickListener;
import be.hyperrail.android.infiniteScrolling.InfiniteScrollingDataSource;
import be.hyperrail.opentransportdata.OpenTransportApi;
import be.hyperrail.opentransportdata.common.contracts.TransportDataErrorResponseListener;
import be.hyperrail.opentransportdata.common.contracts.TransportDataSource;
import be.hyperrail.opentransportdata.common.contracts.TransportDataSuccessResponseListener;
import be.hyperrail.opentransportdata.common.models.Liveboard;
import be.hyperrail.opentransportdata.common.models.VehicleStop;
import be.hyperrail.opentransportdata.common.requests.ExtendLiveboardRequest;
import be.hyperrail.opentransportdata.common.requests.LiveboardRequest;
import be.hyperrail.opentransportdata.common.requests.ResultExtensionType;
import be.hyperrail.opentransportdata.common.requests.VehicleRequest;

/**
 * A fragment for showing liveboard results
 */
public class LiveboardFragment extends RecyclerViewFragment<Liveboard> implements InfiniteScrollingDataSource,
        ResultFragment<LiveboardRequest>, OnRecyclerItemClickListener<VehicleStop>, OnRecyclerItemLongClickListener<VehicleStop> {

    public static final String INSTANCESTATE_KEY_LIVEBOARD = "result";
    public static final String INSTANCESTATE_KEY_REQUEST = "request";
    private Liveboard mCurrentLiveboard;
    private LiveboardCardAdapter mLiveboardCardAdapter;
    private LiveboardRequest mRequest;

    public static LiveboardFragment createInstance(LiveboardRequest request) {
        // Clone the request so we can't accidentally modify the original
        LiveboardFragment frg = new LiveboardFragment();
        frg.mRequest = request;
        return frg;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey(INSTANCESTATE_KEY_REQUEST)) {
            mRequest = (LiveboardRequest) savedInstanceState.getSerializable(INSTANCESTATE_KEY_REQUEST);
        }
        return inflater.inflate(R.layout.fragment_recyclerview_list, container, false);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(INSTANCESTATE_KEY_REQUEST, mRequest);
        outState.putSerializable(INSTANCESTATE_KEY_LIVEBOARD, mCurrentLiveboard);
    }

    @Override
    protected Liveboard getRestoredInstanceStateItems(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey(INSTANCESTATE_KEY_LIVEBOARD)) {
            mCurrentLiveboard = (Liveboard) savedInstanceState.getSerializable(INSTANCESTATE_KEY_LIVEBOARD);
        }
        return mCurrentLiveboard;
    }

    @Override
    public void setRequest(@NonNull LiveboardRequest request) {
        this.mRequest = request;
        getInitialData();
    }

    @Override
    public LiveboardRequest getRequest() {
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

        mCurrentLiveboard = null;
        showData(null);

        // Disable infinite scrolling while refreshing to prevent having 2 loading icons
        mLiveboardCardAdapter.setInfiniteScrolling(!this.vRefreshLayout.isRefreshing());

        TransportDataSource api = OpenTransportApi.getDataProviderInstance();
        // Don't abort all queries: there might be multiple fragments at the same screen!

        mRequest.setCallback(new TransportDataSuccessResponseListener<Liveboard>() {
            @Override
            public void onSuccessResponse(@NonNull Liveboard data, Object tag) {
                resetErrorState();
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
                    mLiveboardCardAdapter.setInfiniteScrolling(true);
                }

                // Scroll past the load earlier item
                ((LinearLayoutManager) vRecyclerView.getLayoutManager()).scrollToPositionWithOffset(1, 0);
            }

        }, new TransportDataErrorResponseListener() {
            @Override
            public void onErrorResponse(@NonNull Exception e, Object tag) {
                vRefreshLayout.setRefreshing(false);
                // only finish if we're loading new data
                showError(e);
            }
        }, null);
        api.getLiveboard(mRequest);
    }

    @Override
    public void loadNextRecyclerviewItems() {
        // When not yet initialized with the first data, don't load more
        // mCurrentLiveboard won't be empty after the first response: its stops array might be empty, but the object will be NonNull
        if (mCurrentLiveboard == null) {
            mLiveboardCardAdapter.setNextLoaded();
            return;
        }

        ExtendLiveboardRequest request = new ExtendLiveboardRequest(mCurrentLiveboard, ResultExtensionType.APPEND);
        request.setCallback(new TransportDataSuccessResponseListener<Liveboard>() {
            @Override
            public void onSuccessResponse(@NonNull Liveboard data, Object tag) {
                resetErrorState();
                // Compare the new one with the old one to check if stops have been added
                if (data.getStops().length == mCurrentLiveboard.getStops().length) {
                    showError(new FileNotFoundException("No results"));
                    mLiveboardCardAdapter.disableInfiniteNext();
                }
                mCurrentLiveboard = data;
                showData(mCurrentLiveboard);

                mLiveboardCardAdapter.setNextLoaded();

                // Scroll past the "load earlier"
                LinearLayoutManager mgr = ((LinearLayoutManager) vRecyclerView.getLayoutManager());
                if (mgr.findFirstVisibleItemPosition() == 0) {
                    mgr.scrollToPositionWithOffset(1, 0);
                }
            }
        }, new TransportDataErrorResponseListener() {
            @Override
            public void onErrorResponse(@NonNull Exception e, Object tag) {
                mLiveboardCardAdapter.setNextError(true);
                mLiveboardCardAdapter.setNextLoaded();
            }
        }, null);
        OpenTransportApi.getDataProviderInstance().extendLiveboard(request);
    }

    @Override
    public void loadPreviousRecyclerviewItems() {
        // When not yet initialized with the first data, don't load previous
        if (mCurrentLiveboard == null) {
            mLiveboardCardAdapter.setPrevLoaded();
            return;
        }

        ExtendLiveboardRequest request = new ExtendLiveboardRequest(mCurrentLiveboard, ResultExtensionType.PREPEND);
        request.setCallback(new TransportDataSuccessResponseListener<Liveboard>() {
            @Override
            public void onSuccessResponse(@NonNull Liveboard data, Object tag) {
                resetErrorState();
                // Compare the new one with the old one to check if stops have been added
                if (data.getStops().length == mCurrentLiveboard.getStops().length) {
                    // mLiveboardCardAdapter.setPrevError(true); //TODO: find a way to make clear to the user that no data is available
                    mLiveboardCardAdapter.disableInfinitePrevious();
                }

                int oldLength = mLiveboardCardAdapter.getItemCount();

                mCurrentLiveboard = data;
                showData(mCurrentLiveboard);

                int newLength = mLiveboardCardAdapter.getItemCount();
                // Scroll past the load earlier item
                ((LinearLayoutManager) vRecyclerView.getLayoutManager()).scrollToPositionWithOffset(newLength - oldLength, 0);

                mLiveboardCardAdapter.setPrevLoaded();
            }
        }, new TransportDataErrorResponseListener() {
            @Override
            public void onErrorResponse(@NonNull Exception e, Object tag) {
                mLiveboardCardAdapter.setPrevError(true);
                mLiveboardCardAdapter.setPrevLoaded();
            }
        }, null);
        OpenTransportApi.getDataProviderInstance().extendLiveboard(request);
    }

    @Override
    protected void showData(Liveboard liveBoard) {
        mLiveboardCardAdapter.updateLiveboard(liveBoard);
    }


    @Override
    public void onRecyclerItemClick(RecyclerView.Adapter sender, VehicleStop object) {
        Intent i = VehicleActivity.createIntent(getActivity(), new VehicleRequest(object.getVehicle().getId(), object.getDepartureTime()));
        startActivity(i);
    }

    @Override
    public void onRecyclerItemLongClick(RecyclerView.Adapter sender, VehicleStop object) {
        (new VehiclePopupContextMenu(getActivity(), object)).show();
    }

}
