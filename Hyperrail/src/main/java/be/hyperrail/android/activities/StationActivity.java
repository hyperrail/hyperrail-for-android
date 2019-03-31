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

package be.hyperrail.android.activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import org.joda.time.LocalTime;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import be.hyperrail.android.R;
import be.hyperrail.android.activities.searchresult.LiveboardActivity;
import be.hyperrail.opentransportdata.OpenTransportApi;
import be.hyperrail.opentransportdata.common.contracts.QueryTimeDefinition;
import be.hyperrail.opentransportdata.common.models.LiveboardType;
import be.hyperrail.opentransportdata.common.models.StopLocation;
import be.hyperrail.opentransportdata.common.models.StopLocationFacilities;
import be.hyperrail.opentransportdata.common.requests.LiveboardRequest;

public class StationActivity extends AppCompatActivity {

    private StopLocation mStation;
    private MapView mMap;
    private RotationGestureOverlay mRotationGestureOverlay;

    @Override
    protected void onPause() {
        super.onPause();
        mMap.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMap.onResume();
    }

    private MyLocationNewOverlay mLocationOverlay;

    public static Intent createIntent(Context context, StopLocation station) {
        Intent i = new Intent(context, StationActivity.class);
        i.putExtra("station", station);
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mStation = (StopLocation) getIntent().getSerializableExtra("station");
        mMap = findViewById(R.id.map);
        findViewById(R.id.floating_action_button).setOnClickListener(
                v -> startActivity(LiveboardActivity.createIntent(StationActivity.this,
                        new LiveboardRequest(mStation, QueryTimeDefinition.DEPART_AT, LiveboardType.DEPARTURES, null)))
        );

        setTitle(mStation.getLocalizedName());

        bind(mStation);
    }

    private void bind(StopLocation station) {
        StopLocationFacilities facilities =
                OpenTransportApi.getFacilitiesProviderInstance().getStationFacilitiesByUri(station.getSemanticId());

        // Catch stations without details
        if (facilities == null) {
            AlertDialog errorDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.station_details_not_available_title)
                    .setMessage(R.string.station_details_not_available_description)
                    .setNegativeButton(R.string.action_close,
                            (dialog, which) -> StationActivity.this.finish())
                    .setOnCancelListener(
                            dialog -> StationActivity.this.finish())
                    .show();
            return;
        }

        StringBuilder openingHoursString = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            LocalTime[] openingHours = facilities.getOpeningHours(i);
            if (openingHours == null) {
                openingHoursString.append("Closed").append("\n");
            } else {
                openingHoursString.append(openingHours[0].toString("HH:mm")).append(" - ").append(openingHours[1].toString("HH:mm")).append("\n");
            }
        }
        ((TextView) findViewById(R.id.text_hours)).setText(openingHoursString.toString());
        ((TextView) findViewById(R.id.text_station)).setText(station.getLocalizedName());
        ((TextView) findViewById(R.id.text_address)).setText(String.format("%s %s %s", facilities.getStreet(), facilities.getZip(), facilities.getCity()));

        findViewById(R.id.image_ticket).setVisibility(facilities.hasTicketVendingMachines() ? View.VISIBLE : View.GONE);
        findViewById(R.id.text_ticket).setVisibility(facilities.hasTicketVendingMachines() ? View.VISIBLE : View.GONE);

        findViewById(R.id.image_tram).setVisibility(facilities.hasTram() ? View.VISIBLE : View.GONE);
        findViewById(R.id.text_tram).setVisibility(facilities.hasTram() ? View.VISIBLE : View.GONE);

        findViewById(R.id.image_bus).setVisibility(facilities.hasBus() ? View.VISIBLE : View.GONE);
        findViewById(R.id.text_bus).setVisibility(facilities.hasBus() ? View.VISIBLE : View.GONE);

        findViewById(R.id.image_subway).setVisibility(facilities.hasMetro() ? View.VISIBLE : View.GONE);
        findViewById(R.id.text_subway).setVisibility(facilities.hasMetro() ? View.VISIBLE : View.GONE);

        findViewById(R.id.image_taxi).setVisibility(facilities.hasTaxi() ? View.VISIBLE : View.GONE);
        findViewById(R.id.text_taxi).setVisibility(facilities.hasTaxi() ? View.VISIBLE : View.GONE);

        findViewById(R.id.image_bike).setVisibility(facilities.hasBike() ? View.VISIBLE : View.GONE);
        findViewById(R.id.text_bike).setVisibility(facilities.hasBike() ? View.VISIBLE : View.GONE);

        findViewById(R.id.image_blue_bike).setVisibility(facilities.hasBlue_bike() ? View.VISIBLE : View.GONE);
        findViewById(R.id.text_blue_bike).setVisibility(facilities.hasBlue_bike() ? View.VISIBLE : View.GONE);
        // TODO: display information on accessibility

        displayOsmMap(station);
    }

    public void displayOsmMap(StopLocation station) {
        GeoPoint stationLocation = new GeoPoint(mStation.getLatitude(), mStation.getLongitude());
        Marker marker = new Marker(mMap);
        marker.setTitle(station.getLocalizedName());
        marker.setPosition(stationLocation);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableOsmCurrentLocationView();
        }

        enableOsmGestures();
    }


    private void enableOsmCurrentLocationView() {
        this.mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mMap);
        this.mLocationOverlay.enableMyLocation();
        mMap.getOverlays().add(this.mLocationOverlay);
    }

    private void enableOsmGestures() {
        mMap.setMultiTouchControls(true);
        mRotationGestureOverlay = new RotationGestureOverlay(this, mMap);
        mRotationGestureOverlay.setEnabled(true);
        mMap.setMultiTouchControls(true);
        mMap.getOverlays().add(this.mRotationGestureOverlay);
    }

}
