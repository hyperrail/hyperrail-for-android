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

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import be.hyperrail.android.R;
import be.hyperrail.android.adapter.OnRecyclerItemClickListener;
import be.hyperrail.android.fragments.LiveboardSearchFragment;
import be.hyperrail.android.irail.implementation.requests.IrailLiveboardRequest;
import be.hyperrail.android.persistence.Suggestion;

public class StationPickerActivity extends AppCompatActivity implements OnRecyclerItemClickListener<Suggestion<IrailLiveboardRequest>> {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station_picker);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setTitle(R.string.title_pick_station);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
        LiveboardSearchFragment frg = new LiveboardSearchFragment();
        frg.setAlternativeOnClickListener(this);
        getFragmentManager().beginTransaction().replace(R.id.fragment_container, frg).commit();
    }

    @Override
    public void onRecyclerItemClick(RecyclerView.Adapter sender, Suggestion<IrailLiveboardRequest> object) {
        finish();
    }
}
