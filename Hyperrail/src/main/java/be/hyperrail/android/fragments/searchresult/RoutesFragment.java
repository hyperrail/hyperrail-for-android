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


package be.hyperrail.android.fragments.searchresult;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.joda.time.DateTime;

import be.hyperrail.android.R;
import be.hyperrail.android.activities.searchresult.RouteDetailActivity;
import be.hyperrail.android.adapter.OnRecyclerItemClickListener;
import be.hyperrail.android.adapter.OnRecyclerItemLongClickListener;
import be.hyperrail.android.adapter.RouteCardAdapter;
import be.hyperrail.android.infiniteScrolling.InfiniteScrollingDataSource;
import be.hyperrail.opentransportdata.OpenTransportApi;
import be.hyperrail.opentransportdata.common.contracts.TransportDataSource;
import be.hyperrail.opentransportdata.common.models.Route;
import be.hyperrail.opentransportdata.common.models.RoutesList;
import be.hyperrail.opentransportdata.common.requests.ExtendRoutePlanningRequest;
import be.hyperrail.opentransportdata.common.requests.RequestType;
import be.hyperrail.opentransportdata.common.requests.ResultExtensionType;
import be.hyperrail.opentransportdata.common.requests.RoutePlanningRequest;

/**
 * A fragment for showing liveboard results
 */
public class RoutesFragment extends RecyclerViewFragment<RoutesList> implements InfiniteScrollingDataSource, ResultFragment<RoutePlanningRequest>, OnRecyclerItemClickListener<Route>, OnRecyclerItemLongClickListener<Route> {

    public static final String INSTANCESTATE_KEY_REQUEST = "request";
    public static final String INSTANCESTATE_KEY_RESULT = "result";
    private RoutesList mCurrentRouteResult;
    private RouteCardAdapter mRouteCardAdapter;
    private RoutePlanningRequest mRequest;

    public static RoutesFragment createInstance(RoutePlanningRequest request) {
        RoutesFragment frg = new RoutesFragment();
        frg.mRequest = request;
        return frg;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey(INSTANCESTATE_KEY_REQUEST)) {
            mRequest = (RoutePlanningRequest) savedInstanceState.getSerializable(INSTANCESTATE_KEY_REQUEST);
        }
        return inflater.inflate(R.layout.fragment_recyclerview_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void setRequest(@NonNull RoutePlanningRequest request) {
        this.mRequest = request;
        getInitialData();
    }

    @Override
    public RoutePlanningRequest getRequest() {
        return this.mRequest;
    }

    @Override
    public void onDateTimePicked(DateTime d) {
        mRequest.setSearchTime(d);
        getData();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(INSTANCESTATE_KEY_RESULT, mCurrentRouteResult);
        outState.putSerializable(INSTANCESTATE_KEY_REQUEST, mRequest);
    }

    @Override
    protected RoutesList getRestoredInstanceStateItems(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey(INSTANCESTATE_KEY_RESULT)) {
            this.mCurrentRouteResult = (RoutesList) savedInstanceState.get(INSTANCESTATE_KEY_RESULT);
        }
        return mCurrentRouteResult;
    }

    @Override
    protected RecyclerView.Adapter getAdapter() {
        if (mRouteCardAdapter == null) {
            mRouteCardAdapter = new RouteCardAdapter(getActivity(), vRecyclerView, this);
            mRouteCardAdapter.setOnItemClickListener(this);
            mRouteCardAdapter.setOnItemLongClickListener(this);
        }
        return mRouteCardAdapter;
    }

    @Override
    protected void getInitialData() {
        getData();
    }

    protected void getData() {
        // Clear the view
        showData(null);
        mCurrentRouteResult = null;

        // Restore infinite scrolling
        mRouteCardAdapter.setInfiniteScrolling(true);

        TransportDataSource api = OpenTransportApi.getDataProviderInstance();
        api.abortQueries(RequestType.ROUTEPLANNING);

        RoutePlanningRequest request = new RoutePlanningRequest(mRequest.getOrigin(),
                mRequest.getDestination(),
                mRequest.getTimeDefinition(),
                mRequest.getSearchTime());
        request.setCallback((data, tag) -> {
                    vRefreshLayout.setRefreshing(false);
                    resetErrorState();
                    mCurrentRouteResult = data;
                    showData(mCurrentRouteResult);

                    // Scroll past the load earlier item
                    ((LinearLayoutManager) vRecyclerView.getLayoutManager()).scrollToPositionWithOffset(
                            1, 0);
                }, (e, tag) -> {
                    // only finish if we're loading new data
                    mRouteCardAdapter.setInfiniteScrolling(false);
                    showError(e);
                },
                null);

        api.getRoutePlanning(request);
    }

    public void loadNextRecyclerviewItems() {
        if (mCurrentRouteResult == null) {
            mRouteCardAdapter.setNextLoaded();
            return;
        }

        ExtendRoutePlanningRequest request = new ExtendRoutePlanningRequest(mCurrentRouteResult, ResultExtensionType.APPEND);
        request.setCallback(
                (data, tag) -> {
                    // data consists of both old and new routes
                    resetErrorState();
                    if (data.getRoutes().length == mCurrentRouteResult.getRoutes().length) {
                        mRouteCardAdapter.disableInfiniteNext(); // Nothing new anymore
                    }

                    mCurrentRouteResult = data;
                    showData(mCurrentRouteResult);

                    mRouteCardAdapter.setNextLoaded();

                    // Scroll past the "load earlier"
                    LinearLayoutManager mgr = ((LinearLayoutManager) vRecyclerView.getLayoutManager());
                    if (mgr.findFirstVisibleItemPosition() == 0) {
                        mgr.scrollToPositionWithOffset(1, 0);
                    }

                }, (e, tag) -> {
                    mRouteCardAdapter.setNextError(true);
                    mRouteCardAdapter.setNextLoaded();
                }, null);
        OpenTransportApi.getDataProviderInstance().extendRoutePlanning(request);
    }

    public void loadPreviousRecyclerviewItems() {
        if (mCurrentRouteResult == null) {
            mRouteCardAdapter.setPrevLoaded();
            return;
        }

        ExtendRoutePlanningRequest request = new ExtendRoutePlanningRequest(mCurrentRouteResult, ResultExtensionType.PREPEND);
        request.setCallback(
                (data, tag) -> {
                    resetErrorState();

                    // data consists of both old and new routes
                    if (data.getRoutes().length == mCurrentRouteResult.getRoutes().length) {
                        mRouteCardAdapter.disableInfinitePrevious();
                    }

                    int oldLength = mRouteCardAdapter.getItemCount();

                    mCurrentRouteResult = data;
                    showData(mCurrentRouteResult);

                    int newLength = mRouteCardAdapter.getItemCount();

                    // Scroll past the load earlier item
                    ((LinearLayoutManager) vRecyclerView.getLayoutManager()).scrollToPositionWithOffset(newLength - oldLength, 0);

                    mRouteCardAdapter.setPrevLoaded();
                }, (e, tag) -> {
                    mRouteCardAdapter.setPrevError(true);
                    mRouteCardAdapter.setPrevLoaded();
                }, null);
        OpenTransportApi.getDataProviderInstance().extendRoutePlanning(request);
    }

    protected void showData(RoutesList routeList) {
        mRouteCardAdapter.updateRoutes(routeList);
    }

    @Override
    public void onRecyclerItemClick(RecyclerView.Adapter sender, Route object) {
        // Nothing to do, collapsing/expanding of items is handled by the adapter.
    }

    @Override
    public void onRecyclerItemLongClick(RecyclerView.Adapter sender, Route object) {
        Intent i = RouteDetailActivity.createIntent(getActivity(), object);
        this.startActivity(i);
    }

}
