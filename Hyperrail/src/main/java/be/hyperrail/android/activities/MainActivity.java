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

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/*
 * # This Source Code Form is subject to the terms of the Mozilla Public
 * # License, v. 2.0. If a copy of the MPL was not distributed with this
 * # file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.activities;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.ColorRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;

import be.hyperrail.android.R;
import be.hyperrail.android.fragments.FeedbackFragment;
import be.hyperrail.android.fragments.LiveboardSearchFragment;
import be.hyperrail.android.fragments.RouteSearchFragment;
import be.hyperrail.android.fragments.VehicleSearchFragment;
import be.hyperrail.android.fragments.searchresult.DisturbanceListFragment;
import be.hyperrail.android.util.ReviewDialogProvider;
import be.hyperrail.android.util.ShortcutHelper;

/**
 * The main activity contains a drawer layout and fragments for search, disturbances and settings
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final int VIEW_TYPE_DISTURBANCE = 30;
    private static final int VIEW_TYPE_FEEDBACK = 50;
    private static final int VIEW_TYPE_LIVEBOARD = 0;
    private static final int VIEW_TYPE_ROUTE = 10;
    private static final int VIEW_TYPE_SETTINGS = 40;
    private static final int VIEW_TYPE_TRAIN = 20;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private Fragment mCurrentFragment;
    private int mCurrentView;
    private boolean mDualPane = false;
    private View vLayoutRoot;

    /**
     * Create a new intent to open the main activity on the route page, with the 'to' field filled in.
     *
     * @param context The current context
     * @param to      The station name to prefill
     * @return The created intent
     */
    public static Intent createRouteToIntent(Context context, String to) {
        Intent i = new Intent(context, MainActivity.class);
        i.putExtra("view", VIEW_TYPE_ROUTE);
        i.putExtra("to", to);
        return i;
    }

    /**
     * Create a new intent to open the main activity on the route page, with the 'from' field filled in.
     *
     * @param context The current context
     * @param from    The station name to prefill
     * @return The created intent
     */
    public static Intent createRouteFromIntent(Context context, String from) {
        Intent i = new Intent(context, MainActivity.class);
        i.putExtra("view", VIEW_TYPE_ROUTE);
        i.putExtra("from", from);
        return i;
    }

    private Intent createShortcutIntent(int viewType) {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("shortcut", true); // this variable allows to detect launches from shortcuts
        i.putExtra("view", viewType); // shortcut intents should not contain application specific classes - only pass the station ID
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        vLayoutRoot = findViewById(R.id.activity);

        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.app_name);

        mDualPane = (findViewById(R.id.drawer) == null);
        if (!mDualPane) {
            mDrawerLayout = findViewById(R.id.drawer);

            configureDrawer(toolbar);

            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        NavigationView navigationView = findViewById(R.id.drawer_navigation);
        //Setting Navigation View Item Selected Listener to handle the item click of the navigation menu
        navigationView.setNavigationItemSelectedListener(this);

        showLaunchDialogsIfNeeded();
        setFragmentView(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actionbar_main, menu);

        for (int i = 0; i < menu.size(); i++) {
            if (menu.getItem(i).getIcon() != null) {
                tintDrawable(menu.getItem(i).getIcon(), R.color.colorWhite);
            }
        }

        return true;
    }

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

    private void showLaunchDialogsIfNeeded() {
        if (!PreferenceManager.getDefaultSharedPreferences(this).contains("first_launch_guide")) {
            Intent i = new Intent(this, FirstLaunchGuide.class);
            startActivity(i);
        } else {
            ReviewDialogProvider.showDialogIf(this, 7, 3);
        }
    }


    private String getNameForView(int viewId) {
        switch (viewId) {
            case VIEW_TYPE_LIVEBOARD:
                return getResources().getString(R.string.title_liveboard);
            case VIEW_TYPE_ROUTE:
                return getResources().getString(R.string.title_route);
            case VIEW_TYPE_TRAIN:
                return getResources().getString(R.string.title_vehicle);
            case VIEW_TYPE_DISTURBANCE:
                return getResources().getString(R.string.title_disturbances);
        }
        return "";
    }

    private void setFragmentView(Bundle savedInstanceState) {
        // Get configured default view
        SharedPreferences defaultPreferences = PreferenceManager.getDefaultSharedPreferences(
                this.getApplicationContext());

        int defaultView = Integer.parseInt(defaultPreferences.getString("pref_startup_screen",
                String.valueOf(
                        VIEW_TYPE_ROUTE)));

        // Decide which view to show
        if (savedInstanceState == null && this.getIntent().hasExtra("view")) {
            // Based on intent
            setView(this.getIntent().getIntExtra("view", defaultView),
                    this.getIntent().getExtras());

        } else if (savedInstanceState == null) {
            // Default
            setView(defaultView, null);
        }
    }

    private void configureDrawer(Toolbar toolbar) {
        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                R.string.app_name,
                R.string.app_name
        ) {

            /**
             * Called when a vDrawerLayout has settled in a completely closed state.
             */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    toolbar.setElevation(0);
                }
            }

            /**
             * Called when a vDrawerLayout has settled in a completely open state.
             */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    toolbar.setElevation(4);
                }
            }
        };

        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("view", mCurrentView);
    }

    /**
     * Set the view to a certain fragment. Changes the subtitle as well.
     *
     * @param i    The view type constant
     * @param args The parameters for the fragment
     */
    private void setView(@ViewType int i, Bundle args) {
        Fragment frg;
        switch (i) {
            default:
            case VIEW_TYPE_SETTINGS:
                // No fragment associated with this view type
            case VIEW_TYPE_LIVEBOARD:
                frg = LiveboardSearchFragment.newInstance();
                setToolbarSubTitle(R.string.title_liveboard);
                break;
            case VIEW_TYPE_ROUTE:
                frg = RouteSearchFragment.newInstance();
                setToolbarSubTitle(R.string.title_route);
                break;
            case VIEW_TYPE_TRAIN:
                frg = VehicleSearchFragment.newInstance();
                setToolbarSubTitle(R.string.title_vehicle);
                break;
            case VIEW_TYPE_DISTURBANCE:
                frg = DisturbanceListFragment.newInstance();
                setToolbarSubTitle(R.string.title_disturbances);
                break;
            case VIEW_TYPE_FEEDBACK:
                frg = FeedbackFragment.newInstance();
                setToolbarSubTitle(R.string.title_feedback);
                break;
        }
        if (args != null) {
            frg.setArguments(args);
        }
        mCurrentFragment = frg;
        mCurrentView = i;

        MainActivity.this.getSupportFragmentManager().beginTransaction().replace(
                R.id.fragment_container, frg, "ChildViewTag").setCustomAnimations(
                android.R.animator.fade_in, android.R.animator.fade_out).commit();

        // Close drawer before loading next fragment
        mDrawerLayout.closeDrawer(GravityCompat.START);
    }

    @Override
    public void onBackPressed() {
        if (mCurrentFragment != null && mCurrentFragment.getChildFragmentManager().getBackStackEntryCount() > 0) {
            mCurrentFragment.getChildFragmentManager().popBackStack();
        } else if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Required for drawer toggle to work. Other options can be handled here as well
     *
     * @inheritDoc
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_shortcut:
                Intent shortcutIntent = createShortcutIntent(mCurrentView);
                ShortcutHelper.createShortcut(this,
                        vLayoutRoot,
                        shortcutIntent,
                        "v1-launchToFragment-" + mCurrentView,
                        getNameForView(mCurrentView),
                        getResources().getString(R.string.app_name) + " " + getNameForView(mCurrentView),
                        R.mipmap.ic_launcher);
                return true;

            default:
                return mDrawerToggle.onOptionsItemSelected(item);
        }
    }

    /**
     * On navigation through drawer
     *
     * @inheritDoc
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_liveboard:
                setView(VIEW_TYPE_LIVEBOARD, null);
                break;
            case R.id.action_route:
                setView(VIEW_TYPE_ROUTE, null);
                break;
            case R.id.action_train:
                setView(VIEW_TYPE_TRAIN, null);
                break;
            case R.id.action_disturbances:
                setView(VIEW_TYPE_DISTURBANCE, null);
                break;
            case R.id.action_settings:
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                break;
            case R.id.action_feedback:
                setView(VIEW_TYPE_FEEDBACK, null);
                break;
            case R.id.action_rate:
                Uri uri = Uri.parse("market://details?id=" + getPackageName());
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                // To count with Play market backstack, After pressing back button,
                // to taken back to our application, we need to add following flags to intent.

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                            Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                            Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                }

                try {
                    startActivity(goToMarket);
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://play.google.com/store/apps/details?id=" + getPackageName())));
                }
                break;
            case R.id.action_beta_test:
                Uri betaTestUri = Uri.parse("https://play.google.com/apps/testing/be.hyperrail.android");
                Intent becomeTester = new Intent(Intent.ACTION_VIEW, betaTestUri);
                startActivity(becomeTester);
                break;
            default:
                // Do nothing
        }
        if (!mDualPane) {
            mDrawerLayout.closeDrawers();
        }
        return true;
    }

    /**
     * Set the activity's subtitle
     *
     * @param s Sring resource to use as subtitle
     */
    private void setToolbarSubTitle(@StringRes int s) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(s);
        }
    }

    // Define this as an enumeration type, so compilers can give better advice on possible errors
    @IntDef({VIEW_TYPE_LIVEBOARD, VIEW_TYPE_ROUTE, VIEW_TYPE_TRAIN, VIEW_TYPE_DISTURBANCE, VIEW_TYPE_SETTINGS, VIEW_TYPE_FEEDBACK})
    public @interface ViewType {
    }
}