/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.activities.searchresult;

import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.snackbar.Snackbar;

import org.joda.time.DateTime;

import be.hyperrail.android.R;
import be.hyperrail.android.R.id;
import be.hyperrail.android.R.layout;
import be.hyperrail.android.R.menu;
import be.hyperrail.android.R.string;
import be.hyperrail.android.activities.MainActivity;
import be.hyperrail.android.activities.StationActivity;
import be.hyperrail.android.fragments.searchresult.LiveboardFragment;
import be.hyperrail.android.persistence.Suggestion;
import be.hyperrail.android.persistence.SuggestionType;
import be.hyperrail.android.util.ShortcutHelper;
import be.hyperrail.opentransportdata.OpenTransportApi;
import be.hyperrail.opentransportdata.common.exceptions.StopLocationNotResolvedException;
import be.hyperrail.opentransportdata.common.requests.LiveboardRequest;

import static be.hyperrail.opentransportdata.common.contracts.QueryTimeDefinition.EQUAL_OR_LATER;
import static be.hyperrail.opentransportdata.common.models.LiveboardType.ARRIVALS;
import static be.hyperrail.opentransportdata.common.models.LiveboardType.DEPARTURES;


/**
 * Activity to show a liveboard
 */
public class LiveboardActivity extends ResultActivity {

    private LiveboardRequest mRequest;

    private DeparturesArrivalsAdapter departuresArrivalsAdapter;

    public static Intent createIntent(Context context, LiveboardRequest request) {
        Intent i = new Intent(context, LiveboardActivity.class);
        i.putExtra("request", request);
        return i;
    }

    private Intent createShortcutIntent() {
        Intent i = new Intent(this, LiveboardActivity.class);
        i.putExtra("shortcut", true); // this variable allows to detect launches from shortcuts
        i.putExtra("station", mRequest.getStation().getSemanticId()); // shortcut intents should not contain application specific classes - only pass the station ID
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Validate the intent used to create this activity
        if (getIntent().hasExtra("shortcut") && getIntent().hasExtra("station")) {
            // A valid shortcut intent, for which we have to parse the station
            try {
                this.mRequest = new LiveboardRequest(
                        OpenTransportApi.getStopLocationProviderInstance().getStoplocationByHafasId(
                                getIntent().getStringExtra("station")), EQUAL_OR_LATER, DEPARTURES,
                        null);
            } catch (StopLocationNotResolvedException e) {
                Toast.makeText(this, R.string.station_not_found, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        } else if (getIntent().hasExtra("request")) {
            this.mRequest = (LiveboardRequest) getIntent().getSerializableExtra("request");
        }

        if (mRequest == null) {
            Toast.makeText(this, R.string.station_not_found, Toast.LENGTH_LONG).show();
            finish();
            return;
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
                ShortcutHelper.createShortcut(this,
                        vLayoutRoot,
                        shortcutIntent,
                        mRequest.getStation().getSemanticId(),
                        mRequest.getStation().getLocalizedName(),
                        "Departures from " + mRequest.getStation().getLocalizedName(),
                        R.mipmap.ic_shortcut_liveboard);
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
