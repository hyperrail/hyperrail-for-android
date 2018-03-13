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
import android.widget.TextView;

import be.hyperrail.android.R;
import be.hyperrail.android.adapter.OnRecyclerItemClickListener;
import be.hyperrail.android.adapter.OnRecyclerItemLongClickListener;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

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

    protected final int VIEW_TYPE_ITEM = 600;
    protected final int VIEW_TYPE_LOADING = 601;
    protected final int VIEW_TYPE_LOAD_EARLIER = 602;

    private final InfiniteScrollingDataSource mInfiniteScrollingDataSource;

    private boolean mIsLoadingNext;
    private boolean mIsLoadingPrevious;
    private final Context context;
    private final LinearLayoutManager mRecyclerViewLayoutMgr;
    private boolean mInfiniteNextScrolling = true;
    private boolean mInfinitePrevScrolling = true;
    protected OnRecyclerItemClickListener<T> mOnClickListener;
    protected OnRecyclerItemLongClickListener<T> mOnLongClickListener;
    private boolean mLoadNextError;

    /**
     * Create a new InfiniteScrollingAdapter
     *
     * @param context                     The context
     * @param recyclerView                The recyclerview in which this adapter will be used
     * @param infiniteScrollingDataSource The listener which should be notified when new data should be loaded
     */
    protected InfiniteScrollingAdapter(Context context, RecyclerView recyclerView, InfiniteScrollingDataSource infiniteScrollingDataSource) {

        this.context = context;

        this.mInfiniteScrollingDataSource = infiniteScrollingDataSource;
        this.mRecyclerViewLayoutMgr = (LinearLayoutManager) recyclerView.getLayoutManager();
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
        if (mInfiniteNextScrolling // Check if enabled
                && !mIsLoadingNext // Only when we're not loading already
                && !mLoadNextError // Only when "tap to retry" isn't enabled
                && mInfiniteScrollingDataSource != null // Only when we have a data source linked
                && dy >= 0 // Only when scrolling downwards
                && mRecyclerViewLayoutMgr.findLastVisibleItemPosition() == InfiniteScrollingAdapter.this.getItemCount() - 1 // Only when the last item is visible
                ) {
            // Load more ...
            mIsLoadingNext = true;
            (InfiniteScrollingAdapter.this).mInfiniteScrollingDataSource.loadNextRecyclerviewItems();
        }
    }

    /**
     * Enable or disable the infinite scrolling. Useful e.g. at the end of lists.
     *
     * @param enabled true to enable infinite scrolling
     */
    public void setInfiniteScrolling(boolean enabled) {
        this.mInfiniteNextScrolling = enabled;
        this.mInfinitePrevScrolling = enabled;
        notifyDataSetChanged();
    }

    @Override
    public final int getItemViewType(int position) {
        if (mInfinitePrevScrolling && position == 0) {
            if (mIsLoadingPrevious) {
                return VIEW_TYPE_LOADING;
            } else {
                return VIEW_TYPE_LOAD_EARLIER;
            }
        } else if (mInfiniteNextScrolling && position == InfiniteScrollingAdapter.this.getItemCount() - 1) {
            return VIEW_TYPE_LOADING;
        } else {
            if (mInfinitePrevScrolling) {
                // Take into account the load earlier button
                return onGetItemViewType(position - 1);
            } else {
                return onGetItemViewType(position);
            }
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
        } else if (viewType == VIEW_TYPE_LOAD_EARLIER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_load_button, parent, false);
            return new LoadMoreButtonViewHolder(view);
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
            final LoadingViewHolder loadingViewHolder = (LoadingViewHolder) holder;
            loadingViewHolder.progressBar.setIndeterminate(true);
            loadingViewHolder.progressBar.getIndeterminateDrawable().setColorFilter(
                    ContextCompat.getColor(context, R.color.colorPrimary),
                    PorterDuff.Mode.SRC_ATOP);
            if (mLoadNextError) {
                loadingViewHolder.progressBar.setVisibility(GONE);
                loadingViewHolder.tapToRetry.setVisibility(VISIBLE);
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Load more ...
                        mIsLoadingNext = true;
                        mLoadNextError = false;
                        loadingViewHolder.progressBar.setVisibility(VISIBLE);
                        loadingViewHolder.tapToRetry.setVisibility(GONE);
                        (InfiniteScrollingAdapter.this).mInfiniteScrollingDataSource.loadNextRecyclerviewItems();
                    }
                });
            }
        } else if (holder instanceof LoadMoreButtonViewHolder) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    InfiniteScrollingAdapter.this.mIsLoadingPrevious = true;
                    InfiniteScrollingAdapter.this.notifyDataSetChanged();
                    InfiniteScrollingAdapter.this.mInfiniteScrollingDataSource.loadPreviousRecyclerviewItems();
                }
            });
        } else {
            if (mInfinitePrevScrolling) {
                // Take into account the load earlier button
                onBindItemViewHolder(holder, position - 1);
            } else {
                onBindItemViewHolder(holder, position);
            }
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
     * The number of data items + the progress bar and load earlier button, if needed
     *
     * @return The number of items in the list
     */
    @Override
    public final int getItemCount() {
        int extra = 0;
        if (mInfiniteNextScrolling) {
            extra++;
        }
        if (mInfinitePrevScrolling) {
            extra++;
        }

        return getListItemCount() + extra;

    }

    /**
     * The number of data items to be shown
     *
     * @return The number of data items to be shown
     */
    protected abstract int getListItemCount();

    public void setOnItemClickListener(OnRecyclerItemClickListener<T> listener) {
        this.mOnClickListener = listener;
    }

    public void setOnItemLongClickListener(OnRecyclerItemLongClickListener<T> listener) {
        this.mOnLongClickListener = listener;
    }

    /**
     * The protected method to indicate loading new items has been completed.
     * For use in adapters.
     */
    public void setNextLoaded() {
        mIsLoadingNext = false;
    }

    public void setNextError(boolean hasError) {
        mLoadNextError = hasError;
        notifyDataSetChanged();
    }

    public void setPrevLoaded() {
        mIsLoadingPrevious = false;
    }

    public void disableInfinitePrevious() {
        mInfinitePrevScrolling = false;
        notifyDataSetChanged();
    }

    public void disableInfiniteNext() {
        mInfiniteNextScrolling = false;
        notifyDataSetChanged();
    }

    /**
     * A ViewHolder for the spinner
     */
    private static class LoadingViewHolder extends RecyclerView.ViewHolder {

        final ProgressBar progressBar;
        final TextView tapToRetry;

        LoadingViewHolder(View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.progressBar);
            tapToRetry = itemView.findViewById(R.id.text_tap_retry);
        }
    }

    /**
     * A ViewHolder for the spinner
     */
    private static class LoadMoreButtonViewHolder extends RecyclerView.ViewHolder {

        LoadMoreButtonViewHolder(View itemView) {
            super(itemView);
        }
    }
}