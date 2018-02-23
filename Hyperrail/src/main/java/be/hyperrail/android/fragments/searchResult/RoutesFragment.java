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


package be.hyperrail.android.fragments.searchResult;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.joda.time.DateTime;

import be.hyperrail.android.R;
import be.hyperrail.android.activities.searchResult.RouteDetailActivity;
import be.hyperrail.android.adapter.OnRecyclerItemClickListener;
import be.hyperrail.android.adapter.OnRecyclerItemLongClickListener;
import be.hyperrail.android.adapter.RouteCardAdapter;
import be.hyperrail.android.infiniteScrolling.InfiniteScrollingAdapter;
import be.hyperrail.android.infiniteScrolling.InfiniteScrollingDataSource;
import be.hyperrail.android.irail.contracts.IRailErrorResponseListener;
import be.hyperrail.android.irail.contracts.IRailSuccessResponseListener;
import be.hyperrail.android.irail.contracts.IrailDataProvider;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.irail.implementation.Route;
import be.hyperrail.android.irail.implementation.RouteAppendHelper;
import be.hyperrail.android.irail.implementation.RouteResult;
import be.hyperrail.android.irail.implementation.requests.IrailRoutesRequest;
import be.hyperrail.android.util.ErrorDialogFactory;

/**
 * A fragment for showing liveboard results
 */
public class RoutesFragment extends RecyclerViewFragment<RouteResult> implements InfiniteScrollingDataSource, ResultFragment<IrailRoutesRequest>, OnRecyclerItemClickListener<Route>, OnRecyclerItemLongClickListener<Route> {

    private RouteResult mCurrentRouteResult;
    private RouteCardAdapter mRouteCardAdapter;
    private IrailRoutesRequest mRequest;

    public static RoutesFragment createInstance(IrailRoutesRequest request) {
        RoutesFragment frg = new RoutesFragment();
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

        if (savedInstanceState != null && savedInstanceState.containsKey("routes")) {
            this.mCurrentRouteResult = (RouteResult) savedInstanceState.get("routes");
        }
    }

    @Override
    public void setRequest(@NonNull IrailRoutesRequest request) {
        this.mRequest = request;
        getInitialData();
    }

    @Override
    public IrailRoutesRequest getRequest() {
        return this.mRequest;
    }

    @Override
    public void onDateTimePicked(DateTime d) {
        mRequest.setSearchTime(d);
        getData();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("routes", mCurrentRouteResult);
    }

    @Override
    protected RouteResult getRestoredInstanceStateItems() {
        if (mCurrentRouteResult == null) {
            return null;
        } else {
            return mCurrentRouteResult;
        }
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
        Log.d("RouteActivity", "Get original data");

        // Clear the view
        showData(null);

        // Restore infinite scrolling
        ((RouteCardAdapter) vRecyclerView.getAdapter()).setInfiniteScrolling(true);

        IrailDataProvider api = IrailFactory.getDataProviderInstance();
        api.abortAllQueries();

        IrailRoutesRequest request = new IrailRoutesRequest(mRequest.getOrigin(), mRequest.getDestination(), mRequest.getTimeDefinition(), mRequest.getSearchTime());
        request.setCallback(new IRailSuccessResponseListener<RouteResult>() {
                                @Override
                                public void onSuccessResponse(@NonNull RouteResult data, Object tag) {
                                    vRefreshLayout.setRefreshing(false);
                                    mCurrentRouteResult = data;
                                    showData(mCurrentRouteResult);

                                    // Scroll past the load earlier item
                                    ((LinearLayoutManager) vRecyclerView.getLayoutManager()).scrollToPositionWithOffset(1, 0);
                                }
                            }, new IRailErrorResponseListener() {
                                @Override
                                public void onErrorResponse(@NonNull Exception e, Object tag) {
                                    // only finish if we're loading new data
                                    ((RouteCardAdapter) vRecyclerView.getAdapter()).setInfiniteScrolling(false);
                                    ErrorDialogFactory.showErrorDialog(e, getActivity(), mCurrentRouteResult == null);
                                }
                            },
                null);

        api.getRoutes(request);
    }

    public void loadNextRecyclerviewItems() {
        if (mCurrentRouteResult == null) {
            ((InfiniteScrollingAdapter) vRecyclerView.getAdapter()).setNextLoaded();
            return;
        }

        RouteAppendHelper appendHelper = new RouteAppendHelper();
        appendHelper.appendRouteResult(mCurrentRouteResult, new IRailSuccessResponseListener<RouteResult>() {
            @Override
            public void onSuccessResponse(@NonNull RouteResult data, Object tag) {
                // data consists of both old and new routes

                if (data.getRoutes().length == mCurrentRouteResult.getRoutes().length) {
                    ((
                            InfiniteScrollingAdapter) vRecyclerView.getAdapter()).disableInfiniteNext();
                    // ErrorDialogFactory.showErrorDialog(new FileNotFoundException("No results"), RouteActivity.this,  (mSearchDate == null));
                }

                mCurrentRouteResult = data;
                showData(mCurrentRouteResult);

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
                ErrorDialogFactory.showErrorDialog(e, getActivity(), false);
                ((RouteCardAdapter) vRecyclerView.getAdapter()).setNextLoaded();
                ((RouteCardAdapter) vRecyclerView.getAdapter()).setInfiniteScrolling(false);
            }
        });
    }

    public void loadPreviousRecyclerviewItems() {
        if (mCurrentRouteResult == null) {
            ((InfiniteScrollingAdapter) vRecyclerView.getAdapter()).setPrevLoaded();
            return;
        }

        RouteAppendHelper appendHelper = new RouteAppendHelper();
        appendHelper.prependRouteResult(mCurrentRouteResult, new IRailSuccessResponseListener<RouteResult>() {
            @Override
            public void onSuccessResponse(@NonNull RouteResult data, Object tag) {
                // data consists of both old and new routes
                if (data.getRoutes().length == mCurrentRouteResult.getRoutes().length) {
                    // ErrorDialogFactory.showErrorDialog(new FileNotFoundException("No results"), RouteActivity.this,  (mSearchDate == null));
                    ((InfiniteScrollingAdapter) vRecyclerView.getAdapter()).disableInfinitePrevious();
                }

                mCurrentRouteResult = data;
                showData(mCurrentRouteResult);

                // Scroll past the load earlier item
                ((LinearLayoutManager) vRecyclerView.getLayoutManager()).scrollToPositionWithOffset(1, 0);

                ((InfiniteScrollingAdapter) vRecyclerView.getAdapter()).setPrevLoaded();
            }
        }, new IRailErrorResponseListener() {
            @Override
            public void onErrorResponse(@NonNull Exception e, Object tag) {
                ErrorDialogFactory.showErrorDialog(e, getActivity(), false);
                ((RouteCardAdapter) vRecyclerView.getAdapter()).setInfiniteScrolling(false);
                ((RouteCardAdapter) vRecyclerView.getAdapter()).setPrevLoaded();
            }
        });
    }

    protected void showData(RouteResult routeList) {
        ((RouteCardAdapter) vRecyclerView.getAdapter()).updateRoutes(routeList);
    }

    @Override
    public void onRecyclerItemClick(RecyclerView.Adapter sender, Route object) {

    }

    @Override
    public void onRecyclerItemLongClick(RecyclerView.Adapter sender, Route object) {
        Intent i = RouteDetailActivity.createIntent(getActivity(), object);
        this.startActivity(i);
    }

}
