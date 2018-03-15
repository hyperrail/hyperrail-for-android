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

package be.hyperrail.android.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.perf.metrics.AddTrace;

import java.lang.ref.WeakReference;
import java.util.List;

import be.hyperrail.android.R;
import be.hyperrail.android.activities.searchresult.LiveboardActivity;
import be.hyperrail.android.adapter.OnRecyclerItemClickListener;
import be.hyperrail.android.adapter.OnRecyclerItemLongClickListener;
import be.hyperrail.android.adapter.StationSuggestionsCardAdapter;
import be.hyperrail.android.irail.contracts.IrailStationProvider;
import be.hyperrail.android.irail.contracts.RouteTimeDefinition;
import be.hyperrail.android.irail.db.Station;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.irail.implementation.Liveboard;
import be.hyperrail.android.irail.implementation.requests.IrailLiveboardRequest;
import be.hyperrail.android.persistence.PersistentQueryProvider;
import be.hyperrail.android.persistence.Suggestion;
import be.hyperrail.android.persistence.SuggestionType;

import static be.hyperrail.android.persistence.SuggestionType.HISTORY;

/**
 * Fragment to let users search stations, and pick one to show its liveboard
 */
public class LiveboardSearchFragment extends Fragment implements OnRecyclerItemClickListener<Suggestion<IrailLiveboardRequest>>, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, OnRecyclerItemLongClickListener<Suggestion<IrailLiveboardRequest>> {

    private static final int COARSE_LOCATION_REQUEST = 1;

    private RecyclerView stationRecyclerView;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private EditText vStationSearchField;

    private int mNumberOfNearbyStations = 3;

    private PersistentQueryProvider persistentQueryProvider;
    private boolean mNearbyOnTop;
    private Suggestion<IrailLiveboardRequest> mLastSelectedQuery;
    private StationSuggestionsCardAdapter mStationAdapter;

    UpdateSuggestionsTask activeSuggestionsUpdateTask;

    /**
     * This alternative onclicklistener allows to "catch" clicks. This way we can reuse this fragment for purposes other than just searching to show liveboards
     */
    private OnRecyclerItemClickListener<Suggestion<IrailLiveboardRequest>> alternativeOnClickListener;

    public LiveboardSearchFragment() {
        // Required empty public constructor
    }

    public static LiveboardSearchFragment newInstance() {
        return new LiveboardSearchFragment();
    }

    @Override
    @AddTrace(name = "LiveboardSearchFragment.onCreateView")
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment

        return inflater.inflate(R.layout.fragment_liveboard_search, container, false);
    }

    @Override
    @AddTrace(name = "LiveboardSearchFragment.onViewCreated")
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        stationRecyclerView = view.findViewById(R.id.recyclerview_primary);
        stationRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        stationRecyclerView.setItemAnimator(new DefaultItemAnimator());
        registerForContextMenu(stationRecyclerView);

        mStationAdapter = new StationSuggestionsCardAdapter(this.getActivity(), null);
        mStationAdapter.setOnItemClickListener(this);
        mStationAdapter.setOnLongItemClickListener(this);
        stationRecyclerView.setAdapter(mStationAdapter);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.getActivity());

        mNumberOfNearbyStations = Integer.valueOf(preferences.getString("stations_nearby_count", "3"));

        int order = Integer.valueOf(preferences.getString("stations_order", "0"));
        // 0 || 1: suggestions before nearby
        // 2 || 3: nearby before suggestions
        mNearbyOnTop = (order == 2 || order == 3);

        persistentQueryProvider = PersistentQueryProvider.getInstance(this.getActivity());
        activeSuggestionsUpdateTask = new UpdateSuggestionsTask(this);
        activeSuggestionsUpdateTask.execute(persistentQueryProvider);

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        vStationSearchField = view.findViewById(R.id.input_station);

        if (savedInstanceState != null) {
            // Restore the search field and results
            String stationName = savedInstanceState.getString("station", "");
            if (!stationName.isEmpty()) {
                vStationSearchField.setText(stationName);
                loadStations(stationName);
            }
        }

        vStationSearchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadStations(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        // Setting suggested stations will be handled on fragment start
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("station", vStationSearchField.getText().toString());
    }

    private void loadStations(String s) {
        IrailStationProvider stationProvider = IrailFactory.getStationsProviderInstance();

        // remove whitespaces
        s = s.trim();

        if (s.length() > 0) {
            // Text search
            setStations(stationProvider.getStationsByNameOrderBySize(s), StationSuggestionsCardAdapter.stationType.SEARCHED);
            mStationAdapter.setSuggestionsVisible(false);
            mStationAdapter.setSearchResultType(StationSuggestionsCardAdapter.stationType.SEARCHED);
        } else if (mLastLocation != null) {
            // Nearby stations
            setStations(stationProvider.getStationsOrderByLocationAndSize(mLastLocation, mNumberOfNearbyStations), StationSuggestionsCardAdapter.stationType.NEARBY);
            mStationAdapter.showNearbyStationsOnTop(mNearbyOnTop);
            mStationAdapter.setSuggestionsVisible(true);
            mStationAdapter.setSearchResultType(StationSuggestionsCardAdapter.stationType.NEARBY);
        } else {
            // Just a list of popular stations as fallback
            setStations(stationProvider.getStationsOrderBySize(), StationSuggestionsCardAdapter.stationType.UNDEFINED);
            mStationAdapter.showNearbyStationsOnTop(false);
            mStationAdapter.setSuggestionsVisible(true);
            mStationAdapter.setSearchResultType(StationSuggestionsCardAdapter.stationType.UNDEFINED);
        }
    }

    /**
     * On start, enable location
     */
    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();

        // If not pending or running, start a new task
        if (activeSuggestionsUpdateTask.getStatus() == AsyncTask.Status.FINISHED) {
            activeSuggestionsUpdateTask.cancel(true);
            activeSuggestionsUpdateTask = new UpdateSuggestionsTask(this);
            activeSuggestionsUpdateTask.execute(persistentQueryProvider);
        }
    }

    /**
     * On stop, enable location
     */
    @Override
    public void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    /**
     * Open a liveboardActivity for a certain station
     *
     * @param station The station which liveboard should be loaded
     */
    private void openLiveboard(Station station) {
        IrailLiveboardRequest request = new IrailLiveboardRequest(station, RouteTimeDefinition.DEPART_AT, Liveboard.LiveboardType.DEPARTURES, null);
        persistentQueryProvider.store(new Suggestion<>(request, HISTORY));
        Intent i = LiveboardActivity.createIntent(getActivity(), request);
        startActivity(i);
    }

    /**
     * Set the list of stations
     *
     * @param stations The new array of stations
     */
    private void setStations(Station[] stations, StationSuggestionsCardAdapter.stationType type) {
        mStationAdapter.setSearchResultStations(stations);
        mStationAdapter.setSearchResultType(type);
    }

    @Override
    public void onRecyclerItemClick(RecyclerView.Adapter sender, Suggestion<IrailLiveboardRequest> object) {
        if (alternativeOnClickListener == null) {
            openLiveboard(object.getData().getStation());
        } else {
            alternativeOnClickListener.onRecyclerItemClick(sender, object);
        }
    }

    @Override
    public void onRecyclerItemLongClick(RecyclerView.Adapter sender, Suggestion<IrailLiveboardRequest> object) {
        mLastSelectedQuery = object;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (mLastSelectedQuery != null) {
            getActivity().getMenuInflater().inflate(R.menu.context_history, menu);
            menu.setHeaderTitle(mLastSelectedQuery.getData().getStation().getLocalizedName());
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_delete && mLastSelectedQuery != null) {
            if (mLastSelectedQuery.getType() == SuggestionType.FAVORITE) {
                persistentQueryProvider.delete(mLastSelectedQuery);
                Snackbar.make(stationRecyclerView, R.string.unmarked_station_favorite, Snackbar.LENGTH_LONG).show();
            } else {
                persistentQueryProvider.delete(mLastSelectedQuery);
            }
            mStationAdapter.setSuggestedStations(persistentQueryProvider.getAllStations());
        }

        // handle menu here - get item index or ID from info
        return super.onContextItemSelected(item);

    }

    /**
     * Checks for setting, permission, and if both are set, get the last known location of the device (coarse precision / city level)
     */
    private void getLastLocation() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
        if (preferences.contains("stations_enable_nearby") && !preferences.getBoolean("stations_enable_nearby", true)) {
            // Don't use this feature if it's disabled in settings
            return;
        }

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED || !preferences.contains("stations_enable_nearby")) {

            // Should we show an explanation?
            // Explain anyway
            /*if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    Manifest.permission.ACCESS_COARSE_LOCATION)) */

            AlertDialog explanation = (new AlertDialog.Builder(this.getActivity())).create();
            explanation.setTitle(R.string.permission_description_location_title);
            explanation.setMessage(getResources().getString(R.string.permission_description_location_description));
            explanation.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.Yes), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    preferences.edit().putBoolean("stations_enable_nearby", true).apply();
                    // Ask
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        LiveboardSearchFragment.this.requestPermissions(
                                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                COARSE_LOCATION_REQUEST);
                    }
                }
            });
            explanation.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.No), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Disable
                    preferences.edit().putBoolean("stations_enable_nearby", false).apply();
                }
            });

            explanation.show();
        } else {
            // TODO: update to https://developers.google.com/android/reference/com/google/android/gms/location/FusedLocationProviderClient AFTER updating to Google Play Libraries to 12.0
            // https://stackoverflow.com/a/46482065/1889679
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);

            if (mLastLocation != null) {
                loadStations(vStationSearchField.getText().toString());
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {

        switch (requestCode) {
            case COARSE_LOCATION_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // fire this event again
                    getLastLocation();

                } else {
                    PreferenceManager.getDefaultSharedPreferences(this.getActivity()).edit().putBoolean("stations_enable_nearby", true).apply();
                    // Don't use this feature if we don't have permission

                }
                break;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    /**
     * On Google play location service connection
     *
     * @inheritDoc
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        getLastLocation();
    }

    public void setAlternativeOnClickListener(OnRecyclerItemClickListener<Suggestion<IrailLiveboardRequest>> alternativeOnClickListener) {
        this.alternativeOnClickListener = alternativeOnClickListener;
    }

    private static class UpdateSuggestionsTask extends AsyncTask<PersistentQueryProvider, Void, List<Suggestion<IrailLiveboardRequest>>> {

        private WeakReference<LiveboardSearchFragment> fragmentReference;

        // only retain a weak reference to the activity
        UpdateSuggestionsTask(LiveboardSearchFragment context) {
            fragmentReference = new WeakReference<>(context);
        }

        @Override
        protected List<Suggestion<IrailLiveboardRequest>> doInBackground(PersistentQueryProvider... provider) {
            return provider[0].getAllStations();
        }

        @Override
        protected void onPostExecute(List<Suggestion<IrailLiveboardRequest>> suggestions) {
            super.onPostExecute(suggestions);

            // get a reference to the activity if it is still there
            LiveboardSearchFragment fragment = fragmentReference.get();
            if (fragment == null) return;

            fragment.mStationAdapter.setSuggestedStations(suggestions);
        }
    }

}
