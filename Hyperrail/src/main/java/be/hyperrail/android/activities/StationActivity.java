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
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import org.joda.time.LocalTime;

import be.hyperrail.android.R;
import be.hyperrail.android.activities.searchresult.LiveboardActivity;
import be.hyperrail.opentransportdata.OpenTransportApi;
import be.hyperrail.opentransportdata.common.contracts.QueryTimeDefinition;
import be.hyperrail.opentransportdata.common.models.LiveboardType;
import be.hyperrail.opentransportdata.common.models.StopLocation;
import be.hyperrail.opentransportdata.common.models.StopLocationFacilities;
import be.hyperrail.opentransportdata.common.requests.LiveboardRequest;

public class StationActivity extends AppCompatActivity implements OnMapReadyCallback {

    private StopLocation mStation;

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

        findViewById(R.id.floating_action_button).setOnClickListener(
                v -> startActivity(LiveboardActivity.createIntent(StationActivity.this,
                        new LiveboardRequest(mStation, QueryTimeDefinition.EQUAL_OR_LATER, LiveboardType.DEPARTURES, null)))
        );

        setTitle(mStation.getLocalizedName());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        bind(mStation);
    }

    private void bind(StopLocation station) {
        StopLocationFacilities facilities =
                OpenTransportApi.getFacilitiesProviderInstance().getStationFacilitiesByUri(station.getSemanticId());

        // Catch stations without details
        if (facilities == null) {
            showNoDataDialog();
            return;
        }

        String openingHoursString = getOpeningHoursAsMultilineText(facilities);
        ((TextView) findViewById(R.id.text_hours)).setText(openingHoursString);
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

        findViewById(R.id.image_blue_bike).setVisibility(facilities.hasBlueBike() ? View.VISIBLE : View.GONE);
        findViewById(R.id.text_blue_bike).setVisibility(facilities.hasBlueBike() ? View.VISIBLE : View.GONE);
        // TODO: display information on accessibility
    }

    @NonNull
    private String getOpeningHoursAsMultilineText(StopLocationFacilities facilities) {
        StringBuilder openingHoursString = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            LocalTime[] openingHours = facilities.getOpeningHours(i);
            if (openingHours == null) {
                openingHoursString.append("Closed").append("\n");
            } else {
                openingHoursString.append(openingHours[0].toString("HH:mm")).append(" - ").append(openingHours[1].toString("HH:mm")).append("\n");
            }
        }
        return openingHoursString.toString();
    }

    private void showNoDataDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.station_details_not_available_title)
                .setMessage(R.string.station_details_not_available_description)
                .setNegativeButton(R.string.action_close, (dialog, which) ->
                        StationActivity.this.finish()).setOnCancelListener(
                dialog -> StationActivity.this.finish()
        ).show();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        LatLng stationLocation = new LatLng(mStation.getLatitude(), mStation.getLongitude());
        map.addMarker(new MarkerOptions().position(stationLocation).title(mStation.getLocalizedName()));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(stationLocation, 15));
        map.setBuildingsEnabled(true);
        map.setTrafficEnabled(false);
        map.setMinZoomPreference(10);
        map.setMaxZoomPreference(18);
        map.setLatLngBoundsForCameraTarget(new LatLngBounds(stationLocation, stationLocation));
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
        }
    }
}
