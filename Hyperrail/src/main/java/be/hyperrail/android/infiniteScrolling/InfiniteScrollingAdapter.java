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

package be.hyperrail.android.infiniteScrolling;

import android.content.Context;
import android.graphics.PorterDuff;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import be.hyperrail.android.R;
import be.hyperrail.android.adapter.OnRecyclerItemClickListener;
import be.hyperrail.android.adapter.OnRecyclerItemLongClickListener;

/**
 * This class provides a reusable base for infinite scrolling recyclerviews.
 * When scrolled to the bottom, a spinner will be shown, and loadMoreRecyclerviewItems() is called.
 * This method should update the data set of the adapter, after which the adapter should call setLoaded().
 * As long as setLoaded isn't called, loadMoreRecyclerviewItems() won't be called again!
 * Once setLoaded is called, everything is back as before, now with more items in the listview.
 *
 * @param <T> The type  that will be returned on click to the listener.
 */
public abstract class InfiniteScrollingAdapter<T> extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    protected final int VIEW_TYPE_ITEM = 601;
    protected final int VIEW_TYPE_LOADING = 600;

    private final InfiniteScrollingDataSource mOnLoadMoreListener;
    private boolean isLoading;
    private final Context context;
    private final LinearLayoutManager linearLayoutManager;
    private boolean infiniteScrollingEnabled = true;
    protected OnRecyclerItemClickListener<T> onClickListener;
    protected OnRecyclerItemLongClickListener<T> onLongClickListener;

    /**
     * Create a new InfiniteScrollingAdapter
     *
     * @param context                      The context
     * @param recyclerView                 The recyclerview in which this adapter will be used
     * @param mInfiniteScrollingDataSource The listener which should be notified when the end of the list is reached
     */
    protected InfiniteScrollingAdapter(Context context, RecyclerView recyclerView, final InfiniteScrollingDataSource mInfiniteScrollingDataSource) {

        this.context = context;

        this.mOnLoadMoreListener = mInfiniteScrollingDataSource;
        this.linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                checkInfiniteScrolling(recyclerView, dx, dy);
            }
        });
    }

    /**
     * Check if the listener should be called, and call it if necessary
     *
     * @param recyclerView The recyclerview in which a scroll occurred
     * @param dx           The scroll distance along the x axis
     * @param dy           The scroll distance along the y axis
     */
    private void checkInfiniteScrolling(RecyclerView recyclerView, int dx, int dy) {
        if (infiniteScrollingEnabled && !isLoading && mOnLoadMoreListener != null && dy >= 0 && linearLayoutManager.findLastVisibleItemPosition() == InfiniteScrollingAdapter.this.getItemCount() - 1) {
            // Load more ...
            isLoading = true;
            (InfiniteScrollingAdapter.this).mOnLoadMoreListener.loadMoreRecyclerviewItems();
        }
    }

    /**
     * Enable or disable the infinite scrolling. Useful e.g. at the end of lists.
     *
     * @param enabled true to enable infinite scrolling
     */
    public void setInfiniteScrolling(boolean enabled) {
        this.infiniteScrollingEnabled = enabled;
        notifyDataSetChanged();
    }

    @Override
    public final int getItemViewType(int position) {
        if (infiniteScrollingEnabled && position == InfiniteScrollingAdapter.this.getItemCount() - 1) {
            return VIEW_TYPE_LOADING;
        } else {
            return onGetItemViewType(position);
        }
    }

    protected int onGetItemViewType(int position) {
        return VIEW_TYPE_ITEM;
    }

    /**
     * Determine if the ViewHolder should be a spinner or a data item
     *
     * @inheritDoc
     */
    @Override
    public final RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_LOADING) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_loading, parent, false);
            return new LoadingViewHolder(view);
        } else {
            return onCreateItemViewHolder(parent, viewType);
        }
    }

    /**
     * Determine the ViewHolder for data items
     *
     * @inheritDoc
     */
    protected abstract RecyclerView.ViewHolder onCreateItemViewHolder(ViewGroup parent, int viewType);

    /**
     * In case of a spinner, bind here, else call method in overriding class
     *
     * @param holder   The ViewHolder to be bound
     * @param position The position to be bound
     */
    @Override
    public final void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof LoadingViewHolder) {
            LoadingViewHolder loadingViewHolder = (LoadingViewHolder) holder;
            loadingViewHolder.progressBar.setIndeterminate(true);
            loadingViewHolder.progressBar.getIndeterminateDrawable().setColorFilter(
                    ContextCompat.getColor(context, R.color.colorPrimary),
                    PorterDuff.Mode.SRC_ATOP);
        } else {
            onBindItemViewHolder(holder, position);
        }
    }

    /**
     * Bind a ViewHolder for data items
     *
     * @param holder   The ViewHolder to be bound
     * @param position The position to be bound
     */
    protected abstract void onBindItemViewHolder(RecyclerView.ViewHolder holder, int position);

    /**
     * The number of data items + the progress bar
     *
     * @return The number of data items + 1
     */
    @Override
    public final int getItemCount() {
        if (infiniteScrollingEnabled) {
            return getListItemCount() + 1;
        } else {
            return getListItemCount();
        }
    }

    /**
     * The number of data items to be shown
     *
     * @return The number of data items to be shown
     */
    protected abstract int getListItemCount();

    public void setOnItemClickListener(OnRecyclerItemClickListener<T> listener) {
        this.onClickListener = listener;
    }

    public void setOnItemLongClickListener(OnRecyclerItemLongClickListener<T> listener) {
        this.onLongClickListener = listener;
    }


    /**
     * The protected method to indicate loading new items has been completed.
     * For use in adapters.
     */
    protected void setLoaded() {
        isLoading = false;
    }

    /**
     * A public method for resetting the scrolling state, for use by code everywhere except adapters.
     * This can be used when loading more data failed, thus meaning the adapter can't add more data & call setLoaded.
     */
    public void resetInfiniteScrollingState() {
        setLoaded();
    }

    /**
     * A ViewHolder for the spinner
     */
    private static class LoadingViewHolder extends RecyclerView.ViewHolder {

        final ProgressBar progressBar;

        LoadingViewHolder(View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }
}