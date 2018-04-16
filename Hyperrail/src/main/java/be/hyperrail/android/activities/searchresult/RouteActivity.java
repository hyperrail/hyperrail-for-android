/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.activities.searchresult;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;

import org.joda.time.DateTime;

import be.hyperrail.android.R;
import be.hyperrail.android.fragments.searchresult.RoutesFragment;
import be.hyperrail.android.irail.contracts.RouteTimeDefinition;
import be.hyperrail.android.irail.contracts.StationNotResolvedException;
import be.hyperrail.android.irail.db.Station;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.irail.implementation.requests.IrailRoutesRequest;
import be.hyperrail.android.persistence.Suggestion;
import be.hyperrail.android.util.OnDateTimeSetListener;

import static be.hyperrail.android.persistence.SuggestionType.FAVORITE;

public class RouteActivity extends ResultActivity implements OnDateTimeSetListener {

    @SuppressWarnings("FieldCanBeLocal")
    private FirebaseAnalytics mFirebaseAnalytics;

    private IrailRoutesRequest mRequest;
    private RoutesFragment mFragment;

    public static Intent createIntent(Context context, @NonNull IrailRoutesRequest request) {
        Intent i = new Intent(context, RouteActivity.class);
        i.putExtra("request", request);
        return i;
    }

    private Intent createShortcutIntent() {
        // Shortcut intents shouldn't contain application specific classes.
        // They shouldn't contain a search time either, since shortcuts should always show actual information
        Intent i = new Intent(this, RouteActivity.class);
        i.putExtra("shortcut", true);
        i.putExtra("from", mRequest.getOrigin().getHafasId());
        i.putExtra("to", mRequest.getDestination().getHafasId());
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Validate the intent used to create this activity
        if (getIntent().hasExtra("shortcut")) {
            Station origin;
            Station destination;
            try {
                origin = IrailFactory.getStationsProviderInstance().getStationByHID(getIntent().getStringExtra("from"));
                destination = IrailFactory.getStationsProviderInstance().getStationByHID(getIntent().getStringExtra("to"));
            } catch (StationNotResolvedException e) {
                Toast.makeText(this, R.string.station_not_found, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            this.mRequest = new IrailRoutesRequest(origin, destination, RouteTimeDefinition.DEPART_AT, null);
        } else {
            this.mRequest = (IrailRoutesRequest) getIntent().getSerializableExtra("request");
        }
        super.onCreate(savedInstanceState);

        this.setHeader();

        mFragment = RoutesFragment.createInstance(mRequest);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, mFragment).commit();

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, mRequest.getOrigin().getHafasId() + mRequest.getDestination().getHafasId());
        bundle.putString(FirebaseAnalytics.Param.ORIGIN, mRequest.getOrigin().getName());
        bundle.putString(FirebaseAnalytics.Param.DESTINATION, mRequest.getDestination().getName());
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "route");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_SEARCH_RESULTS, bundle);
    }

    private void setHeader() {
        setTitle(mRequest.getOrigin().getLocalizedName() + " - " + mRequest.getDestination().getLocalizedName());
        setSubTitle(mRequest.isNow() ? getString(R.string.time_now) : mRequest.getSearchTime().toString(getString(R.string.warning_not_realtime_datetime)));
    }


    @Override
    protected int getLayout() {
        return R.layout.activity_result;
    }

    @Override
    protected int getMenuLayout() {
        return R.menu.actionbar_searchresult_routes;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_swap:
                // Create a new request with reversed origin and destination station
                this.mRequest = new IrailRoutesRequest(mRequest.getDestination(), mRequest.getOrigin(), mRequest.getTimeDefinition(), mRequest.isNow() ? null : mRequest.getSearchTime());
                this.setFavoriteDisplayState(this.isFavorite());
                this.setHeader();
                mFragment.setRequest(this.mRequest);
                return true;
            case R.id.action_shortcut:
                Intent shortcutIntent = createShortcutIntent();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ShortcutInfo.Builder mShortcutInfoBuilder = new ShortcutInfo.Builder(this, mRequest.getOrigin().getLocalizedName() + " - " + mRequest.getDestination().getLocalizedName());
                    mShortcutInfoBuilder.setShortLabel(mRequest.getOrigin().getLocalizedName() + " - " + mRequest.getDestination().getLocalizedName());

                    mShortcutInfoBuilder.setLongLabel("Route from " + mRequest.getOrigin().getLocalizedName() + " to " + mRequest.getDestination().getLocalizedName());
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
                    addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, mRequest.getOrigin().getLocalizedName() + " - " + mRequest.getDestination().getLocalizedName());
                    addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.mipmap.ic_launcher));
                    addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                    getApplicationContext().sendBroadcast(addIntent);
                }
                Snackbar.make(vLayoutRoot, R.string.shortcut_created, Snackbar.LENGTH_LONG).show();

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public void onDateTimePicked(DateTime date) {
        setSubTitle(date == null ? getString(R.string.time_now) : date.toString(getString(R.string.warning_not_realtime_datetime)));
        mRequest.setSearchTime(date);
        mFragment.onDateTimePicked(date);
    }

    public void markFavorite(boolean favorite) {
        if (favorite) {
            mPersistentQueryProvider.store(new Suggestion<>(mRequest, FAVORITE));
            Snackbar.make(vLayoutRoot, R.string.marked_route_favorite, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.undo, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            RouteActivity.this.markFavorite(false);
                        }
                    })
                    .show();
        } else {
            mPersistentQueryProvider.delete(new Suggestion<>(mRequest, FAVORITE));
            Snackbar.make(vLayoutRoot, R.string.unmarked_route_favorite, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.undo, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            RouteActivity.this.markFavorite(true);
                        }
                    })
                    .show();
        }
        setFavoriteDisplayState(favorite);
    }

    public boolean isFavorite() {
        return mPersistentQueryProvider.isFavorite(mRequest);
    }

}
