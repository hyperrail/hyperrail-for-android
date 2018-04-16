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

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.widget.RemoteViews;

import be.hyperrail.android.R;
import be.hyperrail.android.adapter.OnRecyclerItemClickListener;
import be.hyperrail.android.fragments.LiveboardSearchFragment;
import be.hyperrail.android.irail.implementation.requests.IrailLiveboardRequest;
import be.hyperrail.android.persistence.Suggestion;

public class NextDeparturesWidgetConfiguration extends AppCompatActivity implements OnRecyclerItemClickListener<Suggestion<IrailLiveboardRequest>> {

    Intent resultValue;
    int mAppWidgetId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);

        setContentView(R.layout.activity_station_picker);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setTitle(R.string.title_pick_station);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
        LiveboardSearchFragment frg = new LiveboardSearchFragment();
        frg.setAlternativeOnClickListener(this);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, frg).commit();

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        } else {
            throw new IllegalStateException();
        }


        // If you receive an intent without the appropriate ID, then the system should kill this Activity//
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }

        resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void onRecyclerItemClick(RecyclerView.Adapter sender, Suggestion<IrailLiveboardRequest> object) {
        SharedPreferences prefs = getSharedPreferences("widgets", 0);
        prefs.edit().putString("NextDepartures:" + mAppWidgetId, object.getData().getStation().getHafasId()).commit();

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);

        RemoteViews views = new RemoteViews(getPackageName(),
                R.layout.widget_nextdepartures);
        views.setTextViewText(R.id.text_station, object.getData().getStation().getLocalizedName());
        appWidgetManager.updateAppWidget(mAppWidgetId, views);
        setResult(RESULT_OK, resultValue);
        finish();
    }
}
