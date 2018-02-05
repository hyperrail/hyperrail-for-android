/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.fragments;


import android.app.Fragment;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import be.hyperrail.android.R;
import be.hyperrail.android.infiniteScrolling.InfiniteScrollingDataSource;

/**
 * A base class for recyclerviews with results, supporting pull-to-refresh, infinite loading, and more.
 */
public abstract class RecyclerViewFragment<T> extends Fragment implements InfiniteScrollingDataSource {

    /**
     * Recyclerview
     */
    RecyclerView vRecyclerView;

    /**
     * Pull to refresh layout
     */
    SwipeRefreshLayout vRefreshLayout;

    /**
     * Whether or not to show dividers between list items
     */
    protected boolean mShowDividers = true;


    public RecyclerViewFragment() {

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());

        // Initialize pull to refresh
        vRefreshLayout = getActivity().findViewById(R.id.swiperefresh);
        vRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        // This method performs the actual data-refresh operation.
                        // The method calls setRefreshing(false) when it's finished.
                        vRefreshLayout.setRefreshing(true);

                        // Call getInitialData to reset the data again.
                        getInitialData();
                    }
                }
        );

        // Set-up recyclerview
        vRecyclerView = view.findViewById(R.id.recyclerview_primary);
        vRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mLayoutManager.setSmoothScrollbarEnabled(true);
        vRecyclerView.setLayoutManager(mLayoutManager);

        // Show dividers in case wanted & not using the card layout
        if (mShowDividers && !PreferenceManager.getDefaultSharedPreferences(this.getActivity().getApplication()).getBoolean("use_card_layout", false)) {
            // Cards have their own division by margin, others need a divider
            vRecyclerView.addItemDecoration(new DividerItemDecoration(this.getActivity(), DividerItemDecoration.VERTICAL));
        }

        // Get & set the adapter
        RecyclerView.Adapter adapter = getAdapter();
        vRecyclerView.setAdapter(adapter);

        // Restore a previous instance state
        T restoredItems = getRestoredInstanceStateItems();
        if (restoredItems == null) {
            getInitialData();
        } else {
            showData(restoredItems);
        }
    }

    /**
     * Get the recyclerview adapter
     *
     * @return recyclerview adapter
     */
    abstract protected RecyclerView.Adapter getAdapter();

    /**
     * Get items from the previous instance state
     *
     * @return Null in case of no items, else an array of items
     */
    protected T getRestoredInstanceStateItems() {
        return null;
    }

    /**
     * Get the data
     */
    protected abstract void getData();

    /**
     * Get the initial data. Can be used to set some parameters before calling getData();
     */
    protected void getInitialData() {
        getData();
    }

    /**
     * Show data
     *
     * @param data the data to show
     */
    protected abstract void showData(T data);

}
