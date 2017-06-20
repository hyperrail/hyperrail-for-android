/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package android.irail.be.hyperrail;

import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.irail.be.hyperrail.infiniteScrolling.InfiniteScrollingDataSource;
import android.irail.be.hyperrail.persistence.PersistentQueryProvider;
import android.irail.be.hyperrail.util.DateTimePicker;
import android.irail.be.hyperrail.util.OnDateTimeSetListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.ColorRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.MenuRes;
import android.support.annotation.StringRes;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Date;

/**
 * An abstract class for activities which contain a recyclerview
 *
 * @param <T>
 */
public abstract class RecyclerViewActivity<T> extends AppCompatActivity implements OnDateTimeSetListener, InfiniteScrollingDataSource {

    /**
     * Recyclerview
     */
    RecyclerView vRecyclerView;

    /**
     * Pull to refresh layout
     */
    SwipeRefreshLayout vRefreshLayout;

    /**
     * Warning bar when showing results which aren't realtime (not 'now')
     */
    LinearLayout vWarningNotRealtime;

    /**
     * Warning bar when showing results which aren't realtime (not 'now')
     */
    TextView vWarningNotRealtimeText;

    final LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);

    /**
     * History & favorites provider
     */
    PersistentQueryProvider persistentQueryProvider;

    /**
     * Favorite button in menu
     */
    private MenuItem vFavoriteMenuItem;

    /**
     * The layout root
     */
    protected View vLayoutRoot;
    /**
     * Date for which should be searched. Null for 'now'.
     */
    protected Date mSearchDate;

    /**
     * Reference to shared preferences
     */
    protected SharedPreferences mSharedPreferences;

    /**
     * Wether or not to show dividers between list items
     */
    protected boolean show_dividers = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the layout & set the root
        setContentView(getLayout());
        vLayoutRoot = findViewById(R.id.activity);

        // Get and set the toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize history & favorites, preferences
        persistentQueryProvider = new PersistentQueryProvider(this.getApplicationContext());
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());

        // Initialize pull to refresh
        vRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
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

        // Enable the Up button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Set-up recyclerview
        vRecyclerView = (RecyclerView) findViewById(R.id.recyclerview_primary);
        vRecyclerView.setItemAnimator(new DefaultItemAnimator());
        vRecyclerView.setLayoutManager(mLayoutManager);

        // Show dividers in case wanted & not using the card layout
        if (show_dividers && !PreferenceManager.getDefaultSharedPreferences(this.getApplication()).getBoolean("use_card_layout", false)) {
            // Cards have their own division by margin, others need a divider
            vRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        }

        // Get & set the adapter
        RecyclerView.Adapter adapter = getAdapter();
        vRecyclerView.setAdapter(adapter);

        // Initialize the realtime warning
        vWarningNotRealtime = (LinearLayout) findViewById(R.id.warning_not_realtime);
        vWarningNotRealtimeText = (TextView) findViewById(R.id.warning_not_realtime_text);
        Button vWarningNotRealtimeButton = (Button) findViewById(R.id.warning_not_realtime_close);

        // Set a listener to reset the datetime when the realtime warning is closed
        if (vWarningNotRealtimeButton != null) {
            vWarningNotRealtimeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onDateTimePicked(null);
                }
            });
        }

        // Restore a previous instance state
        T restoredItems = getRestoredInstanceStateItems();
        if (restoredItems == null) {
            getInitialData();
        } else {
            showData(restoredItems);
        }
    }

    /**
     * Get the activity layout
     *
     * @return
     */
    abstract protected
    @LayoutRes
    int getLayout();

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_time:
                // Show a datetime picker when needed
                DateTimePicker picker = new DateTimePicker(this);
                picker.setListener(this);
                picker.pick();
                return true;
            case R.id.action_favorite:
                // Mark or unmark as favorite
                markFavorite(!isFavorite());
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Get the menu layout
     */
    protected
    @MenuRes
    int getMenuLayout() {
        return R.menu.actionbar_searchresult;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(getMenuLayout(), menu);

        vFavoriteMenuItem = menu.findItem(R.id.action_favorite);
        if (vFavoriteMenuItem != null) {
            setFavoriteDisplayState(isFavorite());
        }

        return true;
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
     * Get the next data items
     */
    protected abstract void getNextData();

    /**
     * Show data
     *
     * @param data
     */
    protected abstract void showData(T data);

    @Override
    public void onDateTimePicked(Date date) {
        mSearchDate = date;

        // empty the view while loading
        this.showData(null);

        getData();
    }

    @Override
    public void loadMoreRecyclerviewItems() {
        getNextData();
    }

    /**
     * Set or unset the favorite display state
     *
     * @param favorite
     */
    public void setFavoriteDisplayState(boolean favorite) {
        if (favorite) {
            vFavoriteMenuItem.setIcon(R.drawable.ic_star);
            vFavoriteMenuItem.setTitle(R.string.action_unmark_as_favorite);
            tintDrawable(vFavoriteMenuItem.getIcon(), R.color.colorTextLight);
        } else {
            vFavoriteMenuItem.setIcon(R.drawable.ic_star_border);
            vFavoriteMenuItem.setTitle(R.string.action_mark_as_favorite);
            tintDrawable(vFavoriteMenuItem.getIcon(), R.color.colorTextLight);
        }
    }

    /**
     * Mark or unmark an item as favorite
     *
     * @param favorite Whether or not to mark as favorite
     */
    public abstract void markFavorite(boolean favorite);

    public abstract boolean isFavorite();

    /**
     * Tint a drawable
     *
     * @param drawable The drawable to tint
     * @param color    The color to tint the drawable with
     */
    public void tintDrawable(Drawable drawable, @ColorRes int color) {
        if (drawable != null) {
            drawable.mutate();
            drawable.setColorFilter(getResources().getColor(color), PorterDuff.Mode.SRC_ATOP);
        }
    }

    /**
     * Set the subtitle of the application
     *
     * @param subtitle The text to set
     */
    public void setSubTitle(String subtitle) {
        getSupportActionBar().setSubtitle(subtitle);
    }

    /**
     * Set the subtitle of the application
     *
     * @param subtitle The text to set
     */
    public void setSubTitle(@StringRes int subtitle) {
        getSupportActionBar().setSubtitle(subtitle);
    }

}
