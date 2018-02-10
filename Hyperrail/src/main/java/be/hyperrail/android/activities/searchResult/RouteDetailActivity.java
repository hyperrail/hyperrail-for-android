/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */


package be.hyperrail.android.activities.searchResult;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import be.hyperrail.android.R;
import be.hyperrail.android.fragments.searchResult.RouteFragment;
import be.hyperrail.android.irail.implementation.Route;

/**
 * Activity to show one specific route
 */
public class RouteDetailActivity extends ResultActivity {

    public static Intent createIntent(Context c, Route r) {
        Intent i = new Intent(c, RouteDetailActivity.class);
        i.putExtra("route", r);
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Route route = (Route) getIntent().getSerializableExtra("route");
        super.onCreate(savedInstanceState);

        setTitle(route.getDepartureStation().getLocalizedName() + " - " + route.getArrivalStation().getLocalizedName());

        DateTimeFormatter df = DateTimeFormat.forPattern(getString(R.string.warning_not_realtime_datetime));
        setSubTitle(df.print(route.getDepartureTime()));

        Fragment fragment = RouteFragment.createInstance(route);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
    }

    @Override
    protected int getLayout() {
        return R.layout.activity_result;
    }

    @Override
    protected int getMenuLayout() {
        // TODO: include notification options
        return R.menu.actionbar_main;
    }

    @Override
    public void onDateTimePicked(DateTime date) {
        // Not supported
    }

    @Override
    public void markFavorite(boolean favorite) {
        // Not supported
    }

    @Override
    public boolean isFavorite() {
        // Not supported
        return false;
    }
}
