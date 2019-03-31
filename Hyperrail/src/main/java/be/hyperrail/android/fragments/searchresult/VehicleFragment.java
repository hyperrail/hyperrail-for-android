/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.fragments.searchresult;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.joda.time.DateTime;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import be.hyperrail.android.R;
import be.hyperrail.android.VehiclePopupContextMenu;
import be.hyperrail.android.activities.searchresult.LiveboardActivity;
import be.hyperrail.android.activities.searchresult.VehicleActivity;
import be.hyperrail.android.adapter.OnRecyclerItemClickListener;
import be.hyperrail.android.adapter.OnRecyclerItemLongClickListener;
import be.hyperrail.android.adapter.VehicleStopCardAdapter;
import be.hyperrail.android.infiniteScrolling.InfiniteScrollingDataSource;
import be.hyperrail.android.persistence.PersistentQueryProvider;
import be.hyperrail.android.persistence.Suggestion;
import be.hyperrail.android.persistence.SuggestionType;
import be.hyperrail.opentransportdata.OpenTransportApi;
import be.hyperrail.opentransportdata.common.contracts.QueryTimeDefinition;
import be.hyperrail.opentransportdata.common.models.LiveboardType;
import be.hyperrail.opentransportdata.common.models.StopLocation;
import be.hyperrail.opentransportdata.common.models.VehicleJourney;
import be.hyperrail.opentransportdata.common.models.VehicleStop;
import be.hyperrail.opentransportdata.common.requests.LiveboardRequest;
import be.hyperrail.opentransportdata.common.requests.VehicleRequest;

/**
 * A fragment for showing liveboard results
 */
public class VehicleFragment extends RecyclerViewFragment<VehicleJourney> implements InfiniteScrollingDataSource,
        ResultFragment<VehicleRequest>, OnRecyclerItemClickListener<VehicleStop>, OnRecyclerItemLongClickListener<VehicleStop> {

    private VehicleJourney mCurrentTrain;
    private VehicleRequest mRequest;
    private VehicleStopCardAdapter mRecyclerviewAdapter;
    private MapView mMap;
    private MyLocationNewOverlay mLocationOverlay;
    private RotationGestureOverlay mRotationGestureOverlay;

    public static VehicleFragment createInstance(VehicleRequest request) {
        VehicleFragment frg = new VehicleFragment();
        frg.mRequest = request;
        return frg;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey("request")) {
            mRequest = (VehicleRequest) savedInstanceState.getSerializable("request");
        }
        return inflater.inflate(R.layout.fragment_recyclerview_list_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("trains_map", true)) {
            mMap = view.findViewById(R.id.map);
            mMap.setTileSource(TileSourceFactory.MAPNIK);
        } else {
            View mapView = view.findViewById(R.id.map);
            if (mapView != null) {
                mapView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("request", mRequest);
        outState.putSerializable("result", mCurrentTrain);
    }

    @Override
    protected VehicleJourney getRestoredInstanceStateItems(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey("result")) {
            this.mCurrentTrain = (VehicleJourney) savedInstanceState.get("result");
        }
        return mCurrentTrain;
    }

    @Override
    public void setRequest(@NonNull VehicleRequest request) {
        this.mRequest = request;
        //getInitialData();
    }

    @Override
    public VehicleRequest getRequest() {
        return this.mRequest;
    }

    @Override
    public void onDateTimePicked(DateTime d) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected RecyclerView.Adapter getAdapter() {
        if (mRecyclerviewAdapter == null) {
            mRecyclerviewAdapter = new VehicleStopCardAdapter(getActivity(), null);
        }
        mRecyclerviewAdapter.setOnItemClickListener(this);
        mRecyclerviewAdapter.setOnItemLongClickListener(this);
        return mRecyclerviewAdapter;
    }

    @Override
    protected void getInitialData() {
        getData();
    }

    protected void getData() {
        vRefreshLayout.setRefreshing(true);

        OpenTransportApi.getDataProviderInstance().abortAllQueries();

        VehicleRequest request = new VehicleRequest(mRequest.getVehicleId(),
                mRequest.getSearchTime());
        request.setCallback((data, tag) -> {
            resetErrorState();
            vRefreshLayout.setRefreshing(false);
            mCurrentTrain = data;
            showData(mCurrentTrain);
        }, (e, tag) -> {
            vRefreshLayout.setRefreshing(false);

            // only finish if we're loading new data
            showError(e);
        }, null);
        OpenTransportApi.getDataProviderInstance().getVehicleJourney(request);
    }

    protected void showData(VehicleJourney train) {
        getActivity().setTitle(train.getName() + " " + train.getHeadsign());

        mRecyclerviewAdapter.updateTrain(train);
        mRequest.setOrigin(train.getStops()[0].getStopLocation());
        mRequest.setDirection(train.getLastStopLocation());

        // Update the request in the activity, so additional information will be stored when marking it as favorite
        if (getActivity() instanceof VehicleActivity) {
            ((VehicleActivity) getActivity()).setRequest(mRequest);
        }

        PersistentQueryProvider.getInstance(getActivity()).store(
                new Suggestion<>(mRequest, SuggestionType.HISTORY));

        if (!mRequest.isNow()) {
            int i = train.getIndexForDepartureTime(mRequest.getSearchTime());
            if (i >= 0) {
                vRecyclerView.scrollToPosition(i);
            }
        }

        if (mMap != null) {
            visualizeDataOnMap();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mMap.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMap.onResume();
    }

    @Override
    public void loadNextRecyclerviewItems() {
        // Not supported
    }

    @Override
    public void loadPreviousRecyclerviewItems() {
        // Not supported
    }

    @Override
    public void onRecyclerItemClick(RecyclerView.Adapter sender, VehicleStop object) {
        DateTime queryTime = object.getArrivalTime();
        if (queryTime == null) {
            queryTime = object.getDepartureTime();
        }
        Intent i = LiveboardActivity.createIntent(getActivity(),
                new LiveboardRequest(object.getStopLocation(),
                        QueryTimeDefinition.DEPART_AT,
                        LiveboardType.DEPARTURES,
                        queryTime));
        startActivity(i);
    }

    @Override
    public void onRecyclerItemLongClick(RecyclerView.Adapter sender, VehicleStop stop) {
        (new VehiclePopupContextMenu(getActivity(), stop)).show();
    }

    public void visualizeDataOnMap() {
        if (mCurrentTrain == null) {
            return;
        }

        IMapController mapController = mMap.getController();
        mapController.setZoom(9.5);

        GeoPoint startPoint = new GeoPoint(48.8583, 2.2944);
        mapController.setCenter(startPoint);

        enableOsmGestures();

        final GeoPoint stopLocations[] = new GeoPoint[mCurrentTrain.getStops().length];
        final List<GeoPoint> passedLocations = new ArrayList<>();
        final List<GeoPoint> futureLocations = new ArrayList<>();

        Drawable coloredMarker = ContextCompat.getDrawable(getActivity(), R.drawable.timeline_dot);
        Drawable mutedMarker = ContextCompat.getDrawable(getActivity(), R.drawable.timeline_dot_muted);

        for (int i = 0; i < mCurrentTrain.getStops().length; i++) {
            StopLocation s = mCurrentTrain.getStops()[i].getStopLocation();
            stopLocations[i] = new GeoPoint(s.getLatitude(), s.getLongitude());
            if (mCurrentTrain.getStops()[i].hasLeft()) {
                passedLocations.add(stopLocations[i]);
                addMarkerToMap(mutedMarker, s, stopLocations[i]);
            } else {
                futureLocations.add(stopLocations[i]);
                addMarkerToMap(coloredMarker, s, stopLocations[i]);
            }

        }

        addPolylineToMap(passedLocations, R.color.colorMuted);
        addPolylineToMap(futureLocations, R.color.colorPrimary);

        if (passedLocations.size() > 0 && futureLocations.size() > 0) {
            addPolylineToMap(
                    Arrays.asList(passedLocations.get(passedLocations.size() - 1), futureLocations.get(0)),
                    R.color.colorPrimary);
        }

        mMap.zoomToBoundingBox(calculateBoundingBox(Arrays.asList(stopLocations)), false);

        if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableOsmCurrentLocationView();
        }

    }

    private void addPolylineToMap(List<GeoPoint> passedLocations, int p) {
        Polyline passedPolyline = new Polyline(mMap);
        passedPolyline.setPoints(passedLocations);
        passedPolyline.setColor(p);
        passedPolyline.setGeodesic(false);
        mMap.getOverlayManager().add(passedPolyline);
    }

    private void addMarkerToMap(Drawable mutedMarker, StopLocation s, GeoPoint stopLocation) {
        Marker newMarker = new Marker(mMap);
        newMarker.setDraggable(false);
        newMarker.setIcon(mutedMarker);
        newMarker.setPosition(stopLocation);
        newMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        newMarker.setTitle(s.getLocalizedName());
        mMap.getOverlays().add(newMarker);
    }

    private void enableOsmCurrentLocationView() {
        this.mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(getView().getContext()), mMap);
        this.mLocationOverlay.enableMyLocation();
        mMap.getOverlays().add(this.mLocationOverlay);
    }

    private void enableOsmGestures() {
        mMap.setMultiTouchControls(true);
        mRotationGestureOverlay = new RotationGestureOverlay(getView().getContext(), mMap);
        mRotationGestureOverlay.setEnabled(true);
        mMap.setMultiTouchControls(true);
        mMap.getOverlays().add(this.mRotationGestureOverlay);
    }

    public BoundingBox calculateBoundingBox(List<GeoPoint> points) {

        if (points == null || points.size() == 0) {
            return new BoundingBox(0, 0, 0, 0);
        }

        double north = points.get(0).getLatitude();
        double south = points.get(0).getLatitude();
        double east = points.get(0).getLongitude();
        double west = points.get(0).getLongitude();

        for (int i = 0; i < points.size(); i++) {
            if (points.get(i) == null) {
                continue;
            }

            double lat = points.get(i).getLatitude();
            double lon = points.get(i).getLongitude();

            north = Math.max(lat, north);
            south = Math.min(lat, south);
            east = Math.max(lon, east);
            west = Math.min(lon, west);
        }

        return new BoundingBox(north, east, south, west);

    }
}
