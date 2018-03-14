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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;

import be.hyperrail.android.R;
import be.hyperrail.android.fragments.FeedbackFragment;
import be.hyperrail.android.fragments.LiveboardSearchFragment;
import be.hyperrail.android.fragments.RouteSearchFragment;
import be.hyperrail.android.fragments.VehicleSearchFragment;
import be.hyperrail.android.fragments.searchresult.DisturbanceListFragment;

/**
 * The main activity contains a drawer layout and fragments for search, disturbances and settings
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;

    private Fragment mCurrentFragment;
    private int mCurrentView;

    private static final int VIEW_TYPE_LIVEBOARD = 0;
    private static final int VIEW_TYPE_ROUTE = 10;
    private static final int VIEW_TYPE_DISTURBANCE = 20;
    private static final int VIEW_TYPE_TRAIN = 30;
    private static final int VIEW_TYPE_SETTINGS = 40;
    private static final int VIEW_TYPE_FEEDBACK = 50;

    private boolean mDualPane = false;

    private final Handler mDrawerNavigationHandler = new Handler();

    // Define this as an enumeration type, so compilers can give better advice on possible errors
    @IntDef({VIEW_TYPE_LIVEBOARD, VIEW_TYPE_ROUTE, VIEW_TYPE_DISTURBANCE, VIEW_TYPE_TRAIN, VIEW_TYPE_SETTINGS, VIEW_TYPE_FEEDBACK})
    public @interface ViewType {
    }

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setTitle(R.string.app_name);
        mDualPane = (findViewById(R.id.drawer) == null);

        if (!mDualPane) {
            mDrawerLayout = findViewById(R.id.drawer);

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

            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        NavigationView navigationView = findViewById(R.id.drawer_navigation);
        //Setting Navigation View Item Selected Listener to handle the item click of the navigation menu
        navigationView.setNavigationItemSelectedListener(this);

        // Get configured default view
        SharedPreferences defaultPreferences = PreferenceManager.getDefaultSharedPreferences(
                this.getApplicationContext());
        int defaultView = Integer.parseInt(defaultPreferences.getString("pref_startup_screen",
                                                                        String.valueOf(
                                                                                VIEW_TYPE_LIVEBOARD)));

        // Decide which view to show
        if (savedInstanceState == null && this.getIntent().hasExtra("view")) {
            // Based on intent
            setView(this.getIntent().getIntExtra("view", defaultView),
                    this.getIntent().getExtras());
            // mCurrentFragment.setParameters(this.getIntent().getExtras());

        } else if (savedInstanceState == null) {
            // Default
            setView(defaultView, null);
        }
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
                setSubTitle(R.string.title_liveboard);
                break;
            case VIEW_TYPE_ROUTE:
                frg = RouteSearchFragment.newInstance();
                setSubTitle(R.string.title_route);
                break;
            case VIEW_TYPE_TRAIN:
                frg = VehicleSearchFragment.newInstance();
                setSubTitle(R.string.title_vehicle);
                break;
            case VIEW_TYPE_DISTURBANCE:
                frg = DisturbanceListFragment.newInstance();
                setSubTitle(R.string.title_disturbances);
                break;
            case VIEW_TYPE_FEEDBACK:
                frg = FeedbackFragment.newInstance();
                setSubTitle(R.string.title_feedback);
                break;
        }
        if (args != null) {
            frg.setArguments(args);
        }
        mCurrentFragment = frg;
        mCurrentView = i;

        // Allow drawer to close smooth
       /* mDrawerNavigationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!MainActivity.this.isFinishing()) {*/
        MainActivity.this.getSupportFragmentManager().beginTransaction().replace(
                R.id.fragment_container, frg, "ChildViewTag").setCustomAnimations(
                android.R.animator.fade_in, android.R.animator.fade_out).commit();
        /*        }
            }
        }, 200);*/

        // Close drawer before loading next fragment
        mDrawerLayout.closeDrawer(Gravity.START);
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
        return mDrawerToggle.onOptionsItemSelected(item);

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
    private void setSubTitle(@StringRes int s) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(s);
        }
    }
}