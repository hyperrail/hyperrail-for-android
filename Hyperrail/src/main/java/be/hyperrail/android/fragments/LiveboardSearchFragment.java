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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.perf.metrics.AddTrace;

import java.lang.ref.WeakReference;
import java.util.List;

import be.hyperrail.android.R;
import be.hyperrail.android.activities.searchresult.LiveboardActivity;
import be.hyperrail.android.adapter.OnRecyclerItemClickListener;
import be.hyperrail.android.adapter.OnRecyclerItemLongClickListener;
import be.hyperrail.android.adapter.StationSuggestionsCardAdapter;
import be.hyperrail.android.persistence.PersistentQueryProvider;
import be.hyperrail.android.persistence.Suggestion;
import be.hyperrail.android.persistence.SuggestionType;
import be.hyperrail.opentransportdata.OpenTransportApi;
import be.hyperrail.opentransportdata.common.contracts.QueryTimeDefinition;
import be.hyperrail.opentransportdata.common.contracts.TransportStopsDataSource;
import be.hyperrail.opentransportdata.common.models.LiveboardType;
import be.hyperrail.opentransportdata.common.models.StopLocation;
import be.hyperrail.opentransportdata.common.requests.LiveboardRequest;

import static be.hyperrail.android.persistence.SuggestionType.HISTORY;

/**
 * Fragment to let users search stations, and pick one to show its liveboard
 */
public class LiveboardSearchFragment extends Fragment implements OnRecyclerItemClickListener<Suggestion<LiveboardRequest>>, OnRecyclerItemLongClickListener<Suggestion<LiveboardRequest>> {

    private static final int COARSE_LOCATION_REQUEST = 1;
    public static final String PREF_ENABLE_NEARBY_STATIONS = "stations_enable_nearby";
    public static final String PREF_STATIONS_ORDER = "stations_order";
    public static final String PREF_STATIONS_NEARBY_COUNT = "stations_nearby_count";

    private RecyclerView stationRecyclerView;
    private Location mLastLocation;
    private EditText vStationSearchField;

    private int mNumberOfNearbyStations = 3;

    private PersistentQueryProvider persistentQueryProvider;
    private boolean mNearbyOnTop;
    private Suggestion<LiveboardRequest> mLastSelectedQuery;
    private StationSuggestionsCardAdapter mStationAdapter;

    UpdateSuggestionsTask activeSuggestionsUpdateTask;
    AlertDialog permissionExplanationDialog;
    /**
     * This alternative onclicklistener allows to "catch" clicks. This way we can reuse this fragment for purposes other than just searching to show liveboards
     */
    private OnRecyclerItemClickListener<Suggestion<LiveboardRequest>> alternativeOnClickListener;
    private FusedLocationProviderClient mFusedLocationClient;

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
        // Hide the recyclerview when empty - a placeholder will shift into place automatically
        mStationAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if (mStationAdapter.getItemCount() == 0) {
                    stationRecyclerView.setVisibility(View.GONE);
                } else {
                    stationRecyclerView.setVisibility(View.VISIBLE);
                }
            }
        });
        stationRecyclerView.setAdapter(mStationAdapter);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.getActivity());

        //noinspection ConstantConditions
        mNumberOfNearbyStations = Integer.valueOf(preferences.getString(PREF_STATIONS_NEARBY_COUNT, "3"));

        //noinspection ConstantConditions
        int order = Integer.valueOf(preferences.getString(PREF_STATIONS_ORDER, "0"));
        // 0 || 1: suggestions before nearby
        // 2 || 3: nearby before suggestions
        mNearbyOnTop = (order == 2 || order == 3);

        persistentQueryProvider = PersistentQueryProvider.getInstance(this.getActivity());
        activeSuggestionsUpdateTask = new UpdateSuggestionsTask(this);
        activeSuggestionsUpdateTask.execute(persistentQueryProvider);

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
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadStations(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());
        // Setting suggested stations will be handled on fragment start
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("station", vStationSearchField.getText().toString());
    }

    private void loadStations(String s) {
        TransportStopsDataSource stationProvider = OpenTransportApi.getStopLocationProviderInstance();

        // remove whitespaces
        s = s.trim();

        if (s.length() > 0) {
            // Text search
            setStations(stationProvider.getStoplocationsByNameOrderBySize(s), StationSuggestionsCardAdapter.stationType.SEARCHED);
            mStationAdapter.setSuggestionsVisible(false);
            mStationAdapter.setSearchResultType(StationSuggestionsCardAdapter.stationType.SEARCHED);
        } else if (mLastLocation != null) {
            // Nearby stations
            setStations(stationProvider.getStoplocationsOrderedByLocationAndSize(mLastLocation, mNumberOfNearbyStations), StationSuggestionsCardAdapter.stationType.NEARBY);
            mStationAdapter.showNearbyStationsOnTop(mNearbyOnTop);
            mStationAdapter.setSuggestionsVisible(true);
            mStationAdapter.setSearchResultType(StationSuggestionsCardAdapter.stationType.NEARBY);
        } else {
            // Just a list of popular stations as fallback
            setStations(stationProvider.getStoplocationsOrderedBySize(), StationSuggestionsCardAdapter.stationType.UNDEFINED);
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
        getLastLocation();
        // If not pending or running, start a new task
        if (activeSuggestionsUpdateTask.getStatus() == AsyncTask.Status.FINISHED) {
            activeSuggestionsUpdateTask.cancel(true);
            activeSuggestionsUpdateTask = new UpdateSuggestionsTask(this);
            activeSuggestionsUpdateTask.execute(persistentQueryProvider);
        }
    }

    /**
     * Open a liveboardActivity for a certain station
     *
     * @param station The station which liveboard should be loaded
     */
    private void openLiveboard(StopLocation station) {
        LiveboardRequest request = new LiveboardRequest(station, QueryTimeDefinition.EQUAL_OR_LATER, LiveboardType.DEPARTURES, null);
        persistentQueryProvider.store(new Suggestion<>(request, HISTORY));
        Intent i = LiveboardActivity.createIntent(getActivity(), request);
        startActivity(i);
    }

    /**
     * Set the list of stations
     *
     * @param stations The new array of stations
     */
    private void setStations(StopLocation[] stations, StationSuggestionsCardAdapter.stationType type) {
        mStationAdapter.setSearchResultStations(stations);
        mStationAdapter.setSearchResultType(type);
    }

    @Override
    public void onRecyclerItemClick(RecyclerView.Adapter sender, Suggestion<LiveboardRequest> object) {
        if (alternativeOnClickListener == null) {
            openLiveboard(object.getData().getStation());
        } else {
            alternativeOnClickListener.onRecyclerItemClick(sender, object);
        }
    }

    @Override
    public void onRecyclerItemLongClick(RecyclerView.Adapter sender, Suggestion<LiveboardRequest> object) {
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
        if (preferences.contains(PREF_ENABLE_NEARBY_STATIONS) && !preferences.getBoolean(PREF_ENABLE_NEARBY_STATIONS, true)) {
            // Don't use this feature if it's disabled in settings
            return;
        }

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED || !preferences.contains(PREF_ENABLE_NEARBY_STATIONS)) {

            if (permissionExplanationDialog != null && permissionExplanationDialog.isShowing()) {
                // We're already showing an instance
                return;
            }
            permissionExplanationDialog = (new AlertDialog.Builder(this.getActivity())).create();
            permissionExplanationDialog.setTitle(R.string.permission_description_location_title);
            permissionExplanationDialog.setMessage(getResources().getString(R.string.permission_description_location_description));
            permissionExplanationDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.Yes), (dialog, which) -> {
                preferences.edit().putBoolean(PREF_ENABLE_NEARBY_STATIONS, true).apply();
                // Ask
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    LiveboardSearchFragment.this.requestPermissions(
                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                            COARSE_LOCATION_REQUEST);
                }
            });

            permissionExplanationDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.No), (dialog, which) -> {
                // Disable
                preferences.edit().putBoolean(PREF_ENABLE_NEARBY_STATIONS, false).apply();
            });

            permissionExplanationDialog.show();
        } else {
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            mLastLocation = location;
                            // Load the stations again. Location will be considered as it is available in a field.
                            loadStations(vStationSearchField.getText().toString());
                        }
                    });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == COARSE_LOCATION_REQUEST) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // fire this event again
                getLastLocation();

            } else {
                PreferenceManager.getDefaultSharedPreferences(this.getActivity()).edit().putBoolean(PREF_ENABLE_NEARBY_STATIONS, true).apply();
                // Don't use this feature if we don't have permission

            }
        }
    }

    public void setAlternativeOnClickListener(OnRecyclerItemClickListener<Suggestion<LiveboardRequest>> alternativeOnClickListener) {
        this.alternativeOnClickListener = alternativeOnClickListener;
    }

    private static class UpdateSuggestionsTask extends AsyncTask<PersistentQueryProvider, Void, List<Suggestion<LiveboardRequest>>> {

        private WeakReference<LiveboardSearchFragment> fragmentReference;

        // only retain a weak reference to the activity
        UpdateSuggestionsTask(LiveboardSearchFragment context) {
            fragmentReference = new WeakReference<>(context);
        }

        @Override
        protected List<Suggestion<LiveboardRequest>> doInBackground(PersistentQueryProvider... provider) {
            return provider[0].getAllStations();
        }

        @Override
        protected void onPostExecute(List<Suggestion<LiveboardRequest>> suggestions) {
            super.onPostExecute(suggestions);

            // get a reference to the activity if it is still there
            LiveboardSearchFragment fragment = fragmentReference.get();
            if (fragment == null) {
                return;
            }

            fragment.mStationAdapter.setSuggestedStations(suggestions);
        }
    }

}
