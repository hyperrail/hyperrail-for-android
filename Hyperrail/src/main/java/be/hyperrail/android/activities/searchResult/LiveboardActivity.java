/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.activities.searchResult;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.MenuItem;
import android.view.View;

import com.google.firebase.analytics.FirebaseAnalytics;

import org.joda.time.DateTime;

import be.hyperrail.android.R;
import be.hyperrail.android.activities.MainActivity;
import be.hyperrail.android.activities.ResultActivity;
import be.hyperrail.android.activities.StationActivity;
import be.hyperrail.android.fragments.searchResult.LiveboardFragment;
import be.hyperrail.android.irail.contracts.RouteTimeDefinition;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.irail.implementation.requests.IrailLiveboardRequest;
import be.hyperrail.android.persistence.Suggestion;
import be.hyperrail.android.persistence.SuggestionType;

/**
 * Activity to show a liveboard
 */
public class LiveboardActivity extends ResultActivity {

    private IrailLiveboardRequest mRequest;

    LiveboardFragment fragment;
    @SuppressWarnings("FieldCanBeLocal")
    private FirebaseAnalytics mFirebaseAnalytics;

    public static Intent createIntent(Context context, IrailLiveboardRequest request) {
        Intent i = new Intent(context, LiveboardActivity.class);
        i.putExtra("request", request);
        return i;
    }

    private Intent createShortcutIntent() {
        Intent i = new Intent(this, LiveboardActivity.class);
        i.putExtra("shortcut", true); // this variable allows to detect launches from shortcuts
        i.putExtra("station", mRequest.getStation().getId()); // shortcut intents should not contain application specific classes - only pass the station ID
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Validate the intent used to create this activity
        if (getIntent().hasExtra("shortcut") && getIntent().hasExtra("station")) {
            // A valid shortcut intent, for which we have to parse the station
            this.mRequest = new IrailLiveboardRequest(IrailFactory.getStationsProviderInstance().getStationById(getIntent().getStringExtra("station")), RouteTimeDefinition.DEPART, null);
        } else {
            // Validate a normal intent
            if (!getIntent().hasExtra("request")) {
                throw new IllegalStateException("A liveboard activity should be created by passing a valid request");
            }

            this.mRequest = (IrailLiveboardRequest) getIntent().getSerializableExtra("request");
        }

        super.onCreate(savedInstanceState);

        // Title and subtitle belong to the activity, and are therefore a responsibility of this class
        setTitle(mRequest.getStation().getLocalizedName());
        setSubTitle(mRequest.isNow() ? getString(R.string.time_now) : mRequest.getSearchTime().toString(getString(R.string.warning_not_realtime_datetime)));

        fragment = LiveboardFragment.createInstance(mRequest);

        getFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, mRequest.getStation().getId());
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, mRequest.getStation().getName());
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "liveboard");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_SEARCH_RESULTS, bundle);
    }

    @Override
    protected int getLayout() {
        return R.layout.activity_result_tabbed;
    }

    @Override
    protected int getMenuLayout() {
        return R.menu.actionbar_searchresult_liveboard;
    }

    @Override
    public void onDateTimePicked(DateTime date) {
        setSubTitle(date == null ? getString(R.string.time_now) : mRequest.getSearchTime().toString(getString(R.string.warning_not_realtime_datetime)));
        fragment.onDateTimePicked(date);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_from:
                startActivity(MainActivity.createRouteFromIntent(getApplicationContext(), mRequest.getStation().getName()));
                return true;
            case R.id.action_to:
                startActivity(MainActivity.createRouteToIntent(getApplicationContext(), mRequest.getStation().getName()));
                return true;
            case R.id.action_details:
                startActivity(StationActivity.createIntent(getApplicationContext(), mRequest.getStation()));
                return true;
            case R.id.action_shortcut:
                Intent shortcutIntent = createShortcutIntent();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ShortcutInfo.Builder mShortcutInfoBuilder = new ShortcutInfo.Builder(this, mRequest.getStation().getId());
                    mShortcutInfoBuilder.setShortLabel(mRequest.getStation().getLocalizedName());

                    mShortcutInfoBuilder.setLongLabel("Departures from " + mRequest.getStation().getLocalizedName());
                    mShortcutInfoBuilder.setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher));
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
                    addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, mRequest.getStation().getLocalizedName());
                    addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.mipmap.ic_launcher));
                    addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                    getApplicationContext().sendBroadcast(addIntent);
                }
                Snackbar.make(vLayoutRoot, R.string.shortcut_created, Snackbar.LENGTH_LONG).show();

                return true;

        }
        // If none of the custom menu items match, pass this to the parent
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void markFavorite(boolean favorite) {
        if (favorite) {
            mPersistentQueryProvider.store(new Suggestion<>(mRequest, SuggestionType.FAVORITE));
            Snackbar.make(vLayoutRoot, R.string.marked_station_favorite, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.undo, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            LiveboardActivity.this.markFavorite(false);
                        }
                    })
                    .show();
        } else {
            mPersistentQueryProvider.delete(new Suggestion<>(mRequest, SuggestionType.FAVORITE));
            Snackbar.make(vLayoutRoot, R.string.unmarked_station_favorite, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.undo, new View.OnClickListener() {
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
}
