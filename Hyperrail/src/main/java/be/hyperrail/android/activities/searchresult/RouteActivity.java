/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.activities.searchresult;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;

import org.joda.time.DateTime;

import be.hyperrail.android.R;
import be.hyperrail.android.fragments.searchresult.RoutesFragment;
import be.hyperrail.android.persistence.Suggestion;
import be.hyperrail.android.util.OnDateTimeSetListener;
import be.hyperrail.android.util.ShortcutHelper;
import be.hyperrail.opentransportdata.OpenTransportApi;
import be.hyperrail.opentransportdata.common.contracts.QueryTimeDefinition;
import be.hyperrail.opentransportdata.common.exceptions.StopLocationNotResolvedException;
import be.hyperrail.opentransportdata.common.models.StopLocation;
import be.hyperrail.opentransportdata.common.requests.RoutePlanningRequest;

import static be.hyperrail.android.persistence.SuggestionType.FAVORITE;

public class RouteActivity extends ResultActivity implements OnDateTimeSetListener {

    private RoutePlanningRequest mRequest;
    private RoutesFragment mFragment;

    public static Intent createIntent(Context context, @NonNull RoutePlanningRequest request) {
        Intent i = new Intent(context, RouteActivity.class);
        i.putExtra("request", request);
        return i;
    }

    private Intent createShortcutIntent() {
        // Shortcut intents shouldn't contain application specific classes.
        // They shouldn't contain a search time either, since shortcuts should always show actual information
        Intent i = new Intent(this, RouteActivity.class);
        i.putExtra("shortcut", true);
        i.putExtra("from", mRequest.getOrigin().getSemanticId());
        i.putExtra("to", mRequest.getDestination().getSemanticId());
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Validate the intent used to create this activity
        if (getIntent().hasExtra("shortcut")) {
            StopLocation origin;
            StopLocation destination;
            try {
                // These methods can also handle URIs, which are used instead of IDs since v1.1.1, 2019/07/19
                origin = OpenTransportApi.getStopLocationProviderInstance().getStoplocationByHafasId(getIntent().getStringExtra("from"));
                destination = OpenTransportApi.getStopLocationProviderInstance().getStoplocationByHafasId(getIntent().getStringExtra("to"));
            } catch (StopLocationNotResolvedException e) {
                Toast.makeText(this, R.string.station_not_found, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            this.mRequest = new RoutePlanningRequest(origin, destination, QueryTimeDefinition.EQUAL_OR_LATER, null);
        } else {
            this.mRequest = (RoutePlanningRequest) getIntent().getSerializableExtra("request");
        }

        if (this.mRequest == null) {
            Toast.makeText(this, R.string.station_not_found, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        this.setHeader();

        mFragment = RoutesFragment.createInstance(mRequest);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, mFragment).commit();
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
                this.mRequest = new RoutePlanningRequest(mRequest.getDestination(), mRequest.getOrigin(), mRequest.getTimeDefinition(), mRequest.isNow() ? null : mRequest.getSearchTime());
                this.setFavoriteDisplayState(this.isFavorite());
                this.setHeader();
                mFragment.setRequest(this.mRequest);
                return true;
            case R.id.action_shortcut:
                Intent shortcutIntent = createShortcutIntent();
                ShortcutHelper.createShortcut(this,
                        vLayoutRoot,
                        shortcutIntent,
                        mRequest.getOrigin().getSemanticId() + "::" + mRequest.getDestination().getSemanticId(),
                        mRequest.getOrigin().getLocalizedName() + " - " + mRequest.getDestination().getLocalizedName(),
                        "Route from " + mRequest.getOrigin().getLocalizedName() + " to " + mRequest.getDestination().getLocalizedName(),
                        R.mipmap.ic_launcher);
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
                    .setAction(R.string.undo, v -> RouteActivity.this.markFavorite(false))
                    .show();
        } else {
            mPersistentQueryProvider.delete(new Suggestion<>(mRequest, FAVORITE));
            Snackbar.make(vLayoutRoot, R.string.unmarked_route_favorite, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.undo, v -> RouteActivity.this.markFavorite(true))
                    .show();
        }
        setFavoriteDisplayState(favorite);
    }

    public boolean isFavorite() {
        return mPersistentQueryProvider.isFavorite(mRequest);
    }

}
