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

import com.google.android.material.snackbar.Snackbar;

import org.joda.time.DateTime;

import be.hyperrail.android.R;
import be.hyperrail.android.fragments.searchresult.VehicleFragment;
import be.hyperrail.android.logging.HyperRailLog;
import be.hyperrail.android.persistence.Suggestion;
import be.hyperrail.android.persistence.SuggestionType;
import be.hyperrail.android.util.ShortcutHelper;
import be.hyperrail.opentransportdata.OpenTransportApi;
import be.hyperrail.opentransportdata.be.irail.IrailVehicleInfo;
import be.hyperrail.opentransportdata.common.requests.RequestType;
import be.hyperrail.opentransportdata.common.requests.VehicleRequest;

/**
 * Activity to show a train
 */
public class VehicleActivity extends ResultActivity {

    HyperRailLog log = HyperRailLog.getLogger(VehicleActivity.class);
    private VehicleRequest mRequest;
    private VehicleFragment fragment;

    public static Intent createIntent(Context context, VehicleRequest request) {
        Intent i = new Intent(context, VehicleActivity.class);
        i.putExtra("request", request);
        return i;
    }

    public Intent createShortcutIntent() {
        Intent i = new Intent(this, VehicleActivity.class);
        i.putExtra("shortcut", true);
        i.putExtra("id", mRequest.getVehicleId());
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Validate the intent used to create this activity
        if (getIntent().hasExtra("shortcut")) {
            mRequest = new VehicleRequest(getIntent().getStringExtra("id"), null);
        } else {
            mRequest = (VehicleRequest) getIntent().getSerializableExtra("request");
        }
        log.setDebugVariable("vehicleId", mRequest.getVehicleId());


        fragment = VehicleFragment.createInstance(mRequest);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();

        setTitle(R.string.title_vehicle);
        setSubTitle(IrailVehicleInfo.getVehicleName(mRequest.getVehicleId()));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_shortcut) {
            Intent shortcutIntent = this.createShortcutIntent();
            ShortcutHelper.createShortcut(this,
                    vLayoutRoot,
                    shortcutIntent,
                    mRequest.getVehicleId(),
                    IrailVehicleInfo.getVehicleName(mRequest.getVehicleId()),
                    "VehicleJourney " + IrailVehicleInfo.getVehicleName(mRequest.getVehicleId()),
                    R.mipmap.ic_shortcut_train);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected int getLayout() {
        return R.layout.activity_result;
    }

    @Override
    protected int getMenuLayout() {
        return R.menu.actionbar_searchresult_train;
    }

    @Override
    public void onDateTimePicked(DateTime date) {
        fragment.onDateTimePicked(date);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        OpenTransportApi.getDataProviderInstance().abortQueries(RequestType.VEHICLECOMPOSITION);
        OpenTransportApi.getDataProviderInstance().abortQueries(RequestType.VEHICLEJOURNEY);
    }

    @Override
    public void markFavorite(boolean favorite) {
        if (favorite) {
            mPersistentQueryProvider.store(new Suggestion<>(mRequest, SuggestionType.FAVORITE));
            Snackbar.make(vLayoutRoot, R.string.marked_train_favorite, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.undo, v -> VehicleActivity.this.markFavorite(false))
                    .show();
        } else {
            mPersistentQueryProvider.delete(new Suggestion<>(mRequest, SuggestionType.FAVORITE));
            Snackbar.make(vLayoutRoot, R.string.unmarked_train_favorite, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.undo, v -> VehicleActivity.this.markFavorite(true))
                    .show();
        }
        setFavoriteDisplayState(favorite);
    }

    @Override
    public boolean isFavorite() {
        return mPersistentQueryProvider.isFavorite(mRequest);
    }

    /**
     * Update the request used for determining the activity title and favorite status
     *
     * @param request The new request
     */
    public void setRequest(VehicleRequest request) {
        mRequest = request;
    }
}

