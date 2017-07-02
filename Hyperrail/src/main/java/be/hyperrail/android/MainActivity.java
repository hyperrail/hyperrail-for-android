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

package be.hyperrail.android;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

/**
 * The main activity contains a drawer layout and fragments for search, disturbances and settings
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout vDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;

    private Fragment mCurrentFragment;
    private int mCurrentView;

    private static final int VIEW_TYPE_LIVEBOARD = 0;
    private static final int VIEW_TYPE_ROUTE = 1;
    private static final int VIEW_TYPE_DISTURBANCE = 2;
    private static final int VIEW_TYPE_SETTINGS = 3;
    private static final int VIEW_TYPE_FEEDBACK = 4;

    private boolean mDualPane = false;

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

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setTitle(R.string.app_name);
        mDualPane = (findViewById(R.id.drawer) == null);

        if (!mDualPane) {
            vDrawerLayout = (DrawerLayout) findViewById(R.id.drawer);

            mDrawerToggle = new ActionBarDrawerToggle(
                    this,
                    vDrawerLayout,
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

            vDrawerLayout.addDrawerListener(mDrawerToggle);
            mDrawerToggle.syncState();

            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        NavigationView navigationView = (NavigationView) findViewById(R.id.drawer_navigation);
        //Setting Navigation View Item Selected Listener to handle the item click of the navigation menu
        navigationView.setNavigationItemSelectedListener(this);

        // Get configured default view
        SharedPreferences defaultPreferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        int defaultView = Integer.parseInt(defaultPreferences.getString("pref_startup_screen", String.valueOf(VIEW_TYPE_LIVEBOARD)));

        // Decide which view to show
        if (savedInstanceState == null && this.getIntent().hasExtra("view")) {
            // Based on intent
            Log.d("MainAct", "Setting view type to " + this.getIntent().getIntExtra("view", defaultView) + " from intent");
            setView(this.getIntent().getIntExtra("view", defaultView), this.getIntent().getExtras());
           // mCurrentFragment.setParameters(this.getIntent().getExtras());

        } else if (savedInstanceState == null) {
            // Default
            Log.d("MainAct", "Setting view type to " + defaultView + " from default");
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
     * @param i The view type constant
     * @param args The parameters for the fragment
     */
    private void setView(int i, Bundle args) {
        Fragment frg = null;
        switch (i) {
            default:
            case VIEW_TYPE_LIVEBOARD:
                frg = LiveboardSearchFragment.newInstance();
                setSubTitle(R.string.title_liveboard);
                break;
            case VIEW_TYPE_ROUTE:
                frg = RouteSearchFragment.newInstance();
                setSubTitle(R.string.title_route);
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
        if (args != null){
            frg.setArguments(args);
        }
        mCurrentFragment = frg;
        mCurrentView = i;
        getFragmentManager().beginTransaction().replace(R.id.activity_main_fragment_container, frg).commit();
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
     * @inheritDoc
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        return mDrawerToggle.onOptionsItemSelected(item);

    }

    /**
     * On navigation through drawer
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
            case R.id.action_disturbances:
                setView(VIEW_TYPE_DISTURBANCE, null);
                break;
            case R.id.action_settings:
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                break;
            case R.id.action_feedback:
                setView(VIEW_TYPE_FEEDBACK,null);
                break;
        }
        if (!mDualPane) {
            vDrawerLayout.closeDrawers();
        }
        return true;
    }

    /**
     * Set the activity's subtitle
     * @param s Sring resource to use as subtitle
     */
    private void setSubTitle(@StringRes int s){
        if (getSupportActionBar() != null){
            getSupportActionBar().setSubtitle(s);
        }
    }
}