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


import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;

import javax.net.ssl.SSLHandshakeException;

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
     * Error message view (only visible when recylerview is hidden)
     */
    TextView vErrorText;

    /**
     * Whether or not to show dividers between list items
     */
    protected boolean mShowDividers = true;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());

        // Initialize pull to refresh
        vRefreshLayout = view.findViewById(R.id.swiperefresh);
        vRefreshLayout.setOnRefreshListener(
                () -> {
                    // This method performs the actual data-refresh operation.
                    // The method calls setRefreshing(false) when it's finished.
                    vRefreshLayout.setRefreshing(true);

                    // Call getInitialData to reset the data again.
                    getInitialData();
                }
        );

        // Set-up recyclerview
        vRecyclerView = view.findViewById(R.id.recyclerview_primary);
        vRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mLayoutManager.setSmoothScrollbarEnabled(true);
        vRecyclerView.setLayoutManager(mLayoutManager);

        vErrorText = view.findViewById(R.id.text_error_message);

        // Show dividers in case wanted & not using the card layout
        boolean useCardLayout = PreferenceManager.getDefaultSharedPreferences(
                this.getActivity().getApplication()).getBoolean("use_card_layout", false);
        if (mShowDividers && !useCardLayout) {
            // Cards have their own division by margin, others need a divider
            vRecyclerView.addItemDecoration(
                    new DividerItemDecoration(this.getActivity(), DividerItemDecoration.VERTICAL));
        }

        // Get & set the adapter
        RecyclerView.Adapter adapter = getAdapter();
        vRecyclerView.setAdapter(adapter);

        // Restore a previous instance state
        T restoredItems = getRestoredInstanceStateItems(savedInstanceState);
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
    protected abstract RecyclerView.Adapter getAdapter();

    /**
     * Get items from the previous instance state
     *
     * @return Null in case of no items, else an array of items
     */
    protected T getRestoredInstanceStateItems(Bundle savedInstanceState) {
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

    protected final void showError(Exception exception) {
        if (exception instanceof ServerError) {
            if (((ServerError) exception).networkResponse != null) {
                if (((ServerError) exception).networkResponse.statusCode == 404) {
                    showError(R.string.error_no_results_found);
                } else if (((ServerError) exception).networkResponse.statusCode == 500) {
                    showError(R.string.error_servererror_invalid_response);
                } else {
                    showError(R.string.error_servererror_invalid_response);
                }
            } else {
                showError(R.string.error_general);
            }
        } else if (exception instanceof NoConnectionError) {
            if (exception.getCause() instanceof SSLHandshakeException) {
                showError(R.string.error_network_unsafe);
            } else {
                showError(R.string.error_network_unavailable);
            }
        } else if (exception instanceof NetworkError) {
            showError(R.string.error_network_connected_unavailable);
        } else if (exception instanceof TimeoutError) {
            showError(R.string.error_network_connected_unavailable);
        } else {
            showError(R.string.error_general);
        }
    }

    protected final void showError(@StringRes int message) {
        vRecyclerView.setVisibility(View.GONE);
        vErrorText.setText(message);
    }

    protected final void resetErrorState() {
        vRecyclerView.setVisibility(View.VISIBLE);
    }
}
