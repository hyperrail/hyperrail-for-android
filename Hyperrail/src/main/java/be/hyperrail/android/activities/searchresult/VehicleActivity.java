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
import android.support.design.widget.Snackbar;
import android.view.MenuItem;
import android.view.View;

import com.google.firebase.analytics.FirebaseAnalytics;

import org.joda.time.DateTime;

import be.hyperrail.android.R;
import be.hyperrail.android.fragments.searchresult.VehicleFragment;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.irail.implementation.VehicleStub;
import be.hyperrail.android.irail.implementation.requests.IrailVehicleRequest;
import be.hyperrail.android.persistence.Suggestion;
import be.hyperrail.android.persistence.SuggestionType;

/**
 * Activity to show a train
 */
public class VehicleActivity extends ResultActivity {

    @SuppressWarnings("FieldCanBeLocal")
    private FirebaseAnalytics mFirebaseAnalytics;
    
    private IrailVehicleRequest mRequest;
    private VehicleFragment fragment;

    public static Intent createIntent(Context context, IrailVehicleRequest request) {
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

        // Validate the intent used to create this activity
        if (getIntent().hasExtra("shortcut")) {
            mRequest = new IrailVehicleRequest(getIntent().getStringExtra("id"), null);
        } else {
            mRequest = (IrailVehicleRequest) getIntent().getSerializableExtra("request");
        }

        super.onCreate(savedInstanceState);

        fragment = VehicleFragment.createInstance(mRequest);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();

        setTitle(R.string.title_vehicle);
        setSubTitle(VehicleStub.getVehicleName(mRequest.getVehicleId()));

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, mRequest.getVehicleId());
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, mRequest.getVehicleId());
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "vehicle");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_SEARCH_RESULTS, bundle);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_shortcut) {
            Intent shortcutIntent = this.createShortcutIntent();
            // TODO: replace train ID with a name
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ShortcutInfo.Builder mShortcutInfoBuilder = new ShortcutInfo.Builder(this, mRequest.getVehicleId());
                mShortcutInfoBuilder.setShortLabel(mRequest.getVehicleId());

                mShortcutInfoBuilder.setLongLabel("Vehicle " + mRequest.getVehicleId());
                mShortcutInfoBuilder.setIcon(Icon.createWithResource(this, R.mipmap.ic_shortcut_train));
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
                addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, mRequest.getVehicleId());
                addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.mipmap.ic_shortcut_train));
                addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                getApplicationContext().sendBroadcast(addIntent);
            }
            Snackbar.make(vLayoutRoot, R.string.shortcut_created, Snackbar.LENGTH_LONG).show();

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
        IrailFactory.getDataProviderInstance().abortAllQueries();
    }

    @Override
    public void markFavorite(boolean favorite) {
        if (favorite) {
            //noinspection ConstantConditions
            mPersistentQueryProvider.store(new Suggestion<>(mRequest, SuggestionType.FAVORITE));
            Snackbar.make(vLayoutRoot, R.string.marked_train_favorite, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.undo, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            VehicleActivity.this.markFavorite(false);
                        }
                    })
                    .show();
        } else {
            //noinspection ConstantConditions
            mPersistentQueryProvider.delete(new Suggestion<>(mRequest, SuggestionType.FAVORITE));
            Snackbar.make(vLayoutRoot, R.string.unmarked_train_favorite, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.undo, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            VehicleActivity.this.markFavorite(true);
                        }
                    })
                    .show();
        }
        setFavoriteDisplayState(favorite);
    }

    @Override
    public boolean isFavorite() {
        //noinspection ConstantConditions
        return mPersistentQueryProvider.isFavorite(mRequest);
    }

    /**
     * Update the request used for determining the activity title and favorite status
     *
     * @param request The new request
     */
    public void setRequest(IrailVehicleRequest request) {
        mRequest = request;
    }
}

