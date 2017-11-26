/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.joda.time.LocalTime;

import be.hyperrail.android.irail.db.Station;
import be.hyperrail.android.irail.db.StationFacilities;

public class StationActivity extends AppCompatActivity implements OnMapReadyCallback {

    private Station mStation;

    public static Intent createIntent(Context context, Station station) {
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

        mStation = (Station) getIntent().getSerializableExtra("station");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        bind(mStation);
    }

    private GoogleMap mMap;

    private void bind(Station station) {
        StationFacilities facilities = station.getStationFacilities();
        StringBuilder openingHoursString = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            LocalTime[] openingHours = facilities.getOpeningHours(i);
            if (openingHours == null) {
                openingHoursString.append("Closed");
            } else {
                openingHoursString.append(openingHours[0].toString("HH:mm")).append(" - ").append(openingHours[1].toString("HH:mm")).append("\n");
            }
        }
        ((TextView) findViewById(R.id.text_hours)).setText(openingHoursString.toString());
        ((TextView) findViewById(R.id.text_station)).setText(station.getLocalizedName());
        ((TextView) findViewById(R.id.text_address)).setText(String.format("%s %s %s", facilities.getStreet(), facilities.getZip(), facilities.getCity()));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney, Australia, and move the camera.
        LatLng stationLocation = new LatLng(mStation.getLatitude(), mStation.getLongitude());
        mMap.addMarker(new MarkerOptions().position(stationLocation).title(mStation.getLocalizedName()));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(stationLocation,19));

    }
}
