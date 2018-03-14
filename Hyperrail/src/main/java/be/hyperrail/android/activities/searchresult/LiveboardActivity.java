/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.activities.searchresult;

import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutInfo.Builder;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.analytics.FirebaseAnalytics.Event;
import com.google.firebase.analytics.FirebaseAnalytics.Param;

import org.joda.time.DateTime;

import be.hyperrail.android.R.id;
import be.hyperrail.android.R.layout;
import be.hyperrail.android.R.menu;
import be.hyperrail.android.R.mipmap;
import be.hyperrail.android.R.string;
import be.hyperrail.android.activities.MainActivity;
import be.hyperrail.android.activities.StationActivity;
import be.hyperrail.android.fragments.searchresult.LiveboardFragment;
import be.hyperrail.android.irail.contracts.RouteTimeDefinition;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.irail.implementation.requests.IrailLiveboardRequest;
import be.hyperrail.android.persistence.Suggestion;
import be.hyperrail.android.persistence.SuggestionType;

import static be.hyperrail.android.irail.implementation.LiveBoard.LiveboardType.ARRIVALS;
import static be.hyperrail.android.irail.implementation.LiveBoard.LiveboardType.DEPARTURES;

/**
 * Activity to show a liveboard
 */
public class LiveboardActivity extends ResultActivity {

    private IrailLiveboardRequest mRequest;

    @SuppressWarnings("FieldCanBeLocal")
    private FirebaseAnalytics mFirebaseAnalytics;
    private DeparturesArrivalsAdapter departuresArrivalsAdapter;

    public static Intent createIntent(Context context, IrailLiveboardRequest request) {
        Intent i = new Intent(context, LiveboardActivity.class);
        i.putExtra("request", request);
        return i;
    }

    private Intent createShortcutIntent() {
        Intent i = new Intent(this, LiveboardActivity.class);
        i.putExtra("shortcut", true); // this variable allows to detect launches from shortcuts
        i.putExtra("station",
                   mRequest.getStation().getId()); // shortcut intents should not contain application specific classes - only pass the station ID
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Validate the intent used to create this activity
        if (getIntent().hasExtra("shortcut") && getIntent().hasExtra("station")) {
            // A valid shortcut intent, for which we have to parse the station
            this.mRequest = new IrailLiveboardRequest(
                    IrailFactory.getStationsProviderInstance().getStationById(
                            getIntent().getStringExtra("station")), RouteTimeDefinition.DEPART_AT, DEPARTURES,
                    null);
        } else {
            // Validate a normal intent
            if (!getIntent().hasExtra("request")) {
                throw new IllegalStateException(
                        "A liveboard activity should be created by passing a valid request");
            }

            this.mRequest = (IrailLiveboardRequest) getIntent().getSerializableExtra("request");
        }

        super.onCreate(savedInstanceState);

        // Title and subtitle belong to the activity, and are therefore a responsibility of this class
        setTitle(mRequest.getStation().getLocalizedName());
        setSubTitle(
                mRequest.isNow() ? getString(string.time_now) : mRequest.getSearchTime().toString(
                        getString(string.warning_not_realtime_datetime)));


        departuresArrivalsAdapter = new DeparturesArrivalsAdapter(
                getSupportFragmentManager());
        ViewPager viewPager = findViewById(id.pager);
        viewPager.setAdapter(departuresArrivalsAdapter);


        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP && getSupportActionBar() != null) {
            getSupportActionBar().setElevation(0);
        }


        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        Bundle bundle = new Bundle();
        bundle.putString(Param.ITEM_ID, mRequest.getStation().getId());
        bundle.putString(Param.ITEM_NAME, mRequest.getStation().getName());
        bundle.putString(Param.CONTENT_TYPE, "liveboard");
        mFirebaseAnalytics.logEvent(Event.VIEW_SEARCH_RESULTS, bundle);
    }

    @Override
    protected int getLayout() {
        return layout.activity_result_tabbed;
    }

    @Override
    protected int getMenuLayout() {
        return menu.actionbar_searchresult_liveboard;
    }

    @Override
    public void onDateTimePicked(DateTime date) {
        setSubTitle(date == null ? getString(string.time_now) : date.toString(
                getString(string.warning_not_realtime_datetime)));
        if (departuresArrivalsAdapter.getFragments()[0] != null) {
            departuresArrivalsAdapter.getFragments()[0].onDateTimePicked(date);
        }
        if (departuresArrivalsAdapter.getFragments()[1] != null) {
            departuresArrivalsAdapter.getFragments()[1].onDateTimePicked(date);
        }
        mRequest.setSearchTime(date);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case id.action_from:
                startActivity(MainActivity.createRouteFromIntent(getApplicationContext(),
                                                                 mRequest.getStation().getName()));
                return true;

            case id.action_to:
                startActivity(MainActivity.createRouteToIntent(getApplicationContext(),
                                                               mRequest.getStation().getName()));
                return true;

            case id.action_details:
                startActivity(StationActivity.createIntent(getApplicationContext(),
                                                           mRequest.getStation()));
                return true;

            case id.action_shortcut:
                Intent shortcutIntent = createShortcutIntent();
                if (VERSION.SDK_INT >= VERSION_CODES.O) {
                    Builder mShortcutInfoBuilder = new Builder(this,
                                                               mRequest.getStation().getId());
                    mShortcutInfoBuilder.setShortLabel(mRequest.getStation().getLocalizedName());

                    mShortcutInfoBuilder.setLongLabel(
                            "Departures from " + mRequest.getStation().getLocalizedName());
                    mShortcutInfoBuilder.setIcon(
                            Icon.createWithResource(this, mipmap.ic_launcher));
                    shortcutIntent.setAction(Intent.ACTION_CREATE_SHORTCUT);
                    mShortcutInfoBuilder.setIntent(shortcutIntent);
                    ShortcutInfo mShortcutInfo = mShortcutInfoBuilder.build();
                    ShortcutManager mShortcutManager = getSystemService(ShortcutManager.class);

                    if (mShortcutManager != null) {
                        mShortcutManager.requestPinShortcut(mShortcutInfo, null);
                    }
                } else {
                    Intent addIntent = new Intent();
                    addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
                    addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME,
                                       mRequest.getStation().getLocalizedName());
                    addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                                       ShortcutIconResource.fromContext(
                                               getApplicationContext(), mipmap.ic_launcher));
                    addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                    getApplicationContext().sendBroadcast(addIntent);
                }
                Snackbar.make(vLayoutRoot, string.shortcut_created, Snackbar.LENGTH_LONG).show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void markFavorite(boolean favorite) {
        if (favorite) {
            mPersistentQueryProvider.store(new Suggestion<>(mRequest, SuggestionType.FAVORITE));
            Snackbar.make(vLayoutRoot, string.marked_station_favorite, Snackbar.LENGTH_SHORT)
                    .setAction(string.undo, new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            LiveboardActivity.this.markFavorite(false);
                        }
                    })
                    .show();
        } else {
            mPersistentQueryProvider.delete(new Suggestion<>(mRequest, SuggestionType.FAVORITE));
            Snackbar.make(vLayoutRoot, string.unmarked_station_favorite, Snackbar.LENGTH_SHORT)
                    .setAction(string.undo, new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            LiveboardActivity.this.markFavorite(true);
                        }
                    })
                    .show();
        }
        setFavoriteDisplayState(favorite);
    }

    @Override
    public boolean isFavorite() {
        return mPersistentQueryProvider.isFavorite(mRequest);
    }


    private class DeparturesArrivalsAdapter extends FragmentPagerAdapter {

        LiveboardFragment[] fragments = new LiveboardFragment[2];

        DeparturesArrivalsAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            if (i == 0 && fragments[i] == null) {
                fragments[0] = LiveboardFragment.createInstance(
                        mRequest.withLiveboardType(DEPARTURES));
            } else if (i == 1 && fragments[1] == null) {
                fragments[1] = LiveboardFragment.createInstance(
                        mRequest.withLiveboardType(ARRIVALS));
            }
            return fragments[i];
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position == 0) {
                return LiveboardActivity.this.getString(string.title_departures);
            } else {
                return LiveboardActivity.this.getString(string.title_arrivals);
            }
        }

        public LiveboardFragment[] getFragments() {
            return fragments;
        }
    }
}
