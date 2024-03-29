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

package be.hyperrail.android.activities.searchresult;

import static android.net.ConnectivityManager.CONNECTIVITY_ACTION;

import android.content.Context;
import android.content.IntentFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.MenuRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import java.io.Serializable;
import java.util.Locale;

import be.hyperrail.android.R;
import be.hyperrail.android.persistence.PersistentQueryProvider;
import be.hyperrail.android.util.DateTimePicker;
import be.hyperrail.android.util.NetworkStateChangeReceiver;
import be.hyperrail.android.util.OnDateTimeSetListener;
import be.hyperrail.android.util.health.HealthState;
import be.hyperrail.android.util.health.HealthStateChecker;
import be.hyperrail.android.util.health.HealthStateCheckerListener;

/**
 * An abstract class for activities which contain a recyclerview
 */
public abstract class ResultActivity extends AppCompatActivity implements OnDateTimeSetListener, Serializable, NetworkStateChangeReceiver.ConnectionReceiverListener, HealthStateCheckerListener {

    /**
     * History & favorites provider
     */
    protected PersistentQueryProvider mPersistentQueryProvider;

    /**
     * Favorite button in menu
     */
    protected MenuItem vFavoriteMenuItem;

    /**
     * The layout root
     */
    protected View vLayoutRoot;
    private ConnectivityManager mConnectivityManager;
    private NetworkStateChangeReceiver mReceiver;
    private HealthStateChecker healthStateChecker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the layout & set the root
        setContentView(getLayout());
        vLayoutRoot = findViewById(R.id.activity);

        // Get and set the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Enable the Up button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mPersistentQueryProvider = PersistentQueryProvider.getInstance(
                this.getApplicationContext());

        mConnectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        mReceiver = new NetworkStateChangeReceiver(this);
        onNetworkConnectionChanged(isInternetAvailable());

        healthStateChecker = new HealthStateChecker(this, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            registerReceiver(mReceiver, new IntentFilter(CONNECTIVITY_ACTION));
        } catch (Exception e) {
            // Ignore
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(mReceiver);
        } catch (Exception e) {
            // Ignore
        }
    }

    private boolean isInternetAvailable() {
        NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    /**
     * Get the layout for this acitivity
     *
     * @return The ID of the layout to use for this activity. By default a simple FrameLayout with toolbar is used.
     */
    @LayoutRes
    protected int getLayout() {
        return R.layout.activity_result;
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Get the menu layout. Override to set the menu.
     */
    protected
    @MenuRes
    int getMenuLayout() {
        return R.menu.actionbar_empty;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(getMenuLayout(), menu);

        for (int i = 0; i < menu.size(); i++) {
            if (menu.getItem(i).getIcon() != null) {
                tintDrawable(menu.getItem(i).getIcon(), R.color.colorOnPrimary);
            }
        }

        vFavoriteMenuItem = menu.findItem(R.id.action_favorite);
        if (vFavoriteMenuItem != null) {
            setFavoriteDisplayState(isFavorite());
        }

        return true;
    }

    /**
     * Set or unset the favorite display state
     *
     * @param favorite true to mark this item as favorite, false to unmark it
     */
    public void setFavoriteDisplayState(boolean favorite) {
        if (favorite) {
            vFavoriteMenuItem.setIcon(R.drawable.ic_star);
            vFavoriteMenuItem.setTitle(R.string.action_unmark_as_favorite);
            tintDrawable(vFavoriteMenuItem.getIcon(), R.color.colorOnPrimary);
        } else {
            vFavoriteMenuItem.setIcon(R.drawable.ic_star_border);
            vFavoriteMenuItem.setTitle(R.string.action_mark_as_favorite);
            tintDrawable(vFavoriteMenuItem.getIcon(), R.color.colorOnPrimary);
        }
    }

    /**
     * Mark or unmark an item as favorite
     *
     * @param favorite Whether or not to mark as favorite
     */
    protected abstract void markFavorite(boolean favorite);

    protected abstract boolean isFavorite();

    /**
     * Tint a drawable
     *
     * @param drawable The drawable to tint
     * @param color    The color to tint the drawable with
     */
    private void tintDrawable(Drawable drawable, @ColorRes int color) {
        if (drawable != null) {
            drawable.mutate();
            drawable.setColorFilter(ContextCompat.getColor(this.getApplicationContext(), color),
                    PorterDuff.Mode.SRC_ATOP);
        }
    }

    /**
     * Set the subtitle of the activty
     *
     * @param title The text to set
     */
    public void setTitle(String title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    /**
     * Set the subtitle of the activty
     *
     * @param title The text to set
     */
    @SuppressWarnings("unused")
    public void setTitle(@StringRes int title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    /**
     * Set the subtitle of the activty
     *
     * @param subtitle The text to set
     */
    public void setSubTitle(String subtitle) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(subtitle);
        }
    }

    /**
     * Set the subtitle of the activty
     *
     * @param subtitle The text to set
     */
    @SuppressWarnings("unused")
    public void setSubTitle(@StringRes int subtitle) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(subtitle);
        }
    }

    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
        if (isConnected) {
            findViewById(R.id.text_status_offline).setVisibility(View.GONE);
        } else {
            findViewById(R.id.text_status_offline).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onSystemHealthChanged(HealthState currentState) {
        TextView view = findViewById(R.id.text_system_status);
        if (currentState.isHealthy()) {
            view.setVisibility(View.GONE);
        } else {
            String locale = PreferenceManager.getDefaultSharedPreferences(this).getString(
                    "pref_stations_language", "");
            if (locale == null || locale.isEmpty()) {
                // Only get locale when needed
                locale = Locale.getDefault().getISO3Language();
            }
            switch (locale) {
                case "nld":
                    view.setText(currentState.getNl());
                    break;
                case "fra":
                    view.setText(currentState.getFr());
                    break;
                case "eng":
                default:
                    view.setText(currentState.getEn());
                    break;
            }
            view.setVisibility(View.VISIBLE);
        }
    }
}
