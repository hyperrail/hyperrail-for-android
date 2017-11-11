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

package be.hyperrail.android;

import android.Manifest;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
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
import com.google.firebase.crash.FirebaseCrash;

import be.hyperrail.android.adapter.OnRecyclerItemClickListener;
import be.hyperrail.android.adapter.OnRecyclerItemLongClickListener;
import be.hyperrail.android.adapter.StationCardAdapter;
import be.hyperrail.android.irail.contracts.IrailStationProvider;
import be.hyperrail.android.irail.db.Station;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.persistence.PersistentQueryProvider;
import be.hyperrail.android.persistence.StationSuggestion;
import be.hyperrail.android.persistence.Suggestion;
import be.hyperrail.android.persistence.SuggestionType;

import static be.hyperrail.android.persistence.SuggestionType.HISTORY;
import static java.util.logging.Level.INFO;

/**
 * Fragment to let users search stations, and pick one to show its liveboard
 */
public class LiveboardSearchFragment extends Fragment implements OnRecyclerItemClickListener<Suggestion<StationSuggestion>>, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, OnRecyclerItemLongClickListener<Suggestion<StationSuggestion>> {

    private static final String LogTag = "LiveboardSearch";
    private static final int COARSE_LOCATION_REQUEST = 1;
    private RecyclerView stationRecyclerView;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private EditText vStationSearchField;

    private int mNumberOfNearbyStations = 3;

    private PersistentQueryProvider persistentQueryProvider;
    private boolean mNearbyOnTop;
    private Suggestion<StationSuggestion> mLastSelectedQuery;
    private StationCardAdapter mStationAdapter;

    public LiveboardSearchFragment() {
        // Required empty public constructor
    }

    public static LiveboardSearchFragment newInstance() {
        return new LiveboardSearchFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_liveboard_search, container, false);
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Create an instance of GoogleAPIClient.

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.getActivity());

        mNumberOfNearbyStations = Integer.valueOf(preferences.getString("stations_nearby_count", "3"));

        int order = Integer.valueOf(preferences.getString("stations_order", "0"));
        // 0 || 1: suggestions before nearby
        // 2 || 3: nearby before suggestions
        mNearbyOnTop = (order == 2 || order == 3);

        persistentQueryProvider = new PersistentQueryProvider(this.getActivity());

        stationRecyclerView = view.findViewById(R.id.recyclerview_primary);

        stationRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        stationRecyclerView.setItemAnimator(new DefaultItemAnimator());
        registerForContextMenu(stationRecyclerView);

        mStationAdapter = new StationCardAdapter(this.getActivity(), null);
        mStationAdapter.setOnItemClickListener(this);
        mStationAdapter.setOnLongItemClickListener(this);
        stationRecyclerView.setAdapter(mStationAdapter);

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
            vStationSearchField.setText(stationName);
            loadStations(stationName);
        } else {
            loadStations("");
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
        setSuggestedStations();
    }

    private void setSuggestedStations() {
        stationRecyclerView = this.getActivity().findViewById(R.id.recyclerview_primary);
        // TODO pixel on Android O requires this much validation. There should be a better way for this.
        if (stationRecyclerView != null && stationRecyclerView.getAdapter() != null && stationRecyclerView.getAdapter() instanceof  StationCardAdapter) {
            ((StationCardAdapter) stationRecyclerView.getAdapter()).setSuggestedStations(persistentQueryProvider.getAllStations());
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("station", vStationSearchField.getText().toString());
    }

    private void loadStations(String s) {
        IrailStationProvider stationProvider = IrailFactory.getStationsProviderInstance();

        // remove whitespaces
        s = s.trim();

        if (s.length() > 0) {
            setStations(stationProvider.getStationsByNameOrderBySize(s));
            setStationType(StationCardAdapter.stationType.SEARCHED);
            setSuggestionsVisible(false);

        } else if (mLastLocation != null) {
            setStations(stationProvider.getStationsOrderByLocationAndSize(mLastLocation, mNumberOfNearbyStations));
            setStationType(StationCardAdapter.stationType.NEARBY);
            setNearbyOnTop(mNearbyOnTop);
            setSuggestionsVisible(true);

        } else {
            setStations(stationProvider.getStationsOrderBySize());
            setStationType(StationCardAdapter.stationType.UNDEFINED);
            setNearbyOnTop(false);
            setSuggestionsVisible(true);

        }
    }

    /**
     * On start, enable location
     */
    @Override
    public void onStart() {
        super.onStart();
        setSuggestedStations();
        mGoogleApiClient.connect();
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
        persistentQueryProvider.store(new Suggestion<>(new StationSuggestion(station),HISTORY));
        Intent i = LiveboardActivity.createIntent(getActivity(), station);
        startActivity(i);
    }

    /**
     * Set the list of stations
     *
     * @param stations The new array of stations
     */
    private void setStations(Station[] stations) {
        if (stations != null) {
            FirebaseCrash.logcat(INFO.intValue(), LogTag, "Setting liveboard search list to " + stations.length + " stations");
        } else {
            FirebaseCrash.logcat(INFO.intValue(), LogTag, "Setting liveboard search list to 0 stations");
        }

        StationCardAdapter adapter = (StationCardAdapter) stationRecyclerView.getAdapter();
        adapter.setStations(stations);
    }

    /**
     * Set the type of stations (determines the icon). Does not apply to recents or favorites.
     *
     * @param type The type of the stations shown, determines the icon shown next to it.
     */
    private void setStationType(StationCardAdapter.stationType type) {
        StationCardAdapter adapter = (StationCardAdapter) stationRecyclerView.getAdapter();
        adapter.setStationIconType(type);
    }

    /**
     * Show/hide recents and favorites
     *
     * @param visible True to show recents and favorites
     */
    private void setSuggestionsVisible(boolean visible) {
        StationCardAdapter adapter = (StationCardAdapter) stationRecyclerView.getAdapter();
        adapter.setSuggestionsVisible(visible);
    }

    /**
     * Whether or not to show nearby stations before the favorite and recent stations
     *
     * @param nearbyOnTop Whether or not to show nearby stations before the favorite and recent stations
     */
    private void setNearbyOnTop(boolean nearbyOnTop) {
        StationCardAdapter adapter = (StationCardAdapter) stationRecyclerView.getAdapter();
        adapter.setNearbyOnTop(nearbyOnTop);
    }

    @Override
    public void onRecyclerItemClick(RecyclerView.Adapter sender, Suggestion<StationSuggestion> object) {
        openLiveboard(object.getData());
    }

    @Override
    public void onRecyclerItemLongClick(RecyclerView.Adapter sender, Suggestion<StationSuggestion> object) {
            mLastSelectedQuery = object;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (mLastSelectedQuery != null) {
            getActivity().getMenuInflater().inflate(R.menu.context_history, menu);
            menu.setHeaderTitle(mLastSelectedQuery.getData().getLocalizedName());
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
        if (!PreferenceManager.getDefaultSharedPreferences(this.getActivity()).getBoolean("stations_enable_nearby", true)) {
            // Don't use this feature if it's disabled in settings
            return;
        }

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {

                Intent i = new Intent(getActivity(), PermissionRequestExplanation.class);
                i.putExtra("title", getResources().getString(R.string.permission_description_location_title));
                i.putExtra("description", getResources().getString(R.string.permission_description_location_description));
                i.putExtra("icon", R.drawable.ic_location_on_48);
                i.putExtra("permission", Manifest.permission.ACCESS_COARSE_LOCATION);
                i.putExtra("preference", "stations_enable_nearby");
                startActivity(i);
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(
                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                            COARSE_LOCATION_REQUEST);
                }

            }
        } else {
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
}
