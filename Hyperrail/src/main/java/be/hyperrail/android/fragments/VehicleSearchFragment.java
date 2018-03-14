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

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.fragments;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import be.hyperrail.android.R;
import be.hyperrail.android.activities.searchresult.VehicleActivity;
import be.hyperrail.android.adapter.OnRecyclerItemClickListener;
import be.hyperrail.android.adapter.OnRecyclerItemLongClickListener;
import be.hyperrail.android.adapter.VehicleSuggestionsCardAdapter;
import be.hyperrail.android.irail.implementation.requests.IrailVehicleRequest;
import be.hyperrail.android.persistence.PersistentQueryProvider;
import be.hyperrail.android.persistence.Suggestion;
import be.hyperrail.android.persistence.SuggestionType;

/**
 * Fragment to let users search stations, and pick one to show its liveboard
 */
public class VehicleSearchFragment extends Fragment implements OnRecyclerItemClickListener<Suggestion<IrailVehicleRequest>>, OnRecyclerItemLongClickListener<Suggestion<IrailVehicleRequest>> {

    private RecyclerView recentTrainsRecyclerView;
    private EditText vTrainSearchField;

    private PersistentQueryProvider persistentQueryProvider;
    private Suggestion<IrailVehicleRequest> mLastSelectedQuery;
    private VehicleSuggestionsCardAdapter mIrailTrainRequestAdapter;

    public static VehicleSearchFragment newInstance() {
        return new VehicleSearchFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_vehicle_search, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Create an instance of GoogleAPIClient.
        persistentQueryProvider = PersistentQueryProvider.getInstance(this.getActivity());

        recentTrainsRecyclerView = view.findViewById(R.id.recyclerview_primary);

        recentTrainsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recentTrainsRecyclerView.setItemAnimator(new DefaultItemAnimator());
        registerForContextMenu(recentTrainsRecyclerView);

        mIrailTrainRequestAdapter = new VehicleSuggestionsCardAdapter(this.getActivity());
        mIrailTrainRequestAdapter.setOnItemClickListener(this);
        mIrailTrainRequestAdapter.setOnLongItemClickListener(this);
        recentTrainsRecyclerView.setAdapter(mIrailTrainRequestAdapter);

        LoadSuggestionsTask t = new LoadSuggestionsTask(this);
        t.execute(persistentQueryProvider);

        vTrainSearchField = view.findViewById(R.id.input_train);

        // Handle special keys in "from" text
        vTrainSearchField.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    if (vTrainSearchField.getText().length() != 0) {
                        doSearch();
                    }
                    return true;
                }
                return false;
            }
        });

        view.findViewById(R.id.button_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doSearch();
            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("station", vTrainSearchField.getText().toString());
    }

    /**
     * Open a trainActivity for a certain train id
     *
     * @param id The train id which should be loaded
     */
    private void openTrain(String id) {
        IrailVehicleRequest request = new IrailVehicleRequest(id, null);
        persistentQueryProvider.store(new Suggestion<>(request, SuggestionType.HISTORY));
        Intent i = VehicleActivity.createIntent(getActivity(), request);
        startActivity(i);
    }

    private void doSearch() {
        String searchQuery = vTrainSearchField.getText().toString();
        // Remove spaces
        searchQuery = searchQuery.replace(" ", "");
        Pattern p = Pattern.compile("\\w{1,3}\\d{2,6}");
        Matcher m = p.matcher(searchQuery);
        if (m.matches()) {
            openTrain(searchQuery.toUpperCase());
            return;
        }

        if (this.getView() != null) {
            Snackbar.make(this.getView(), R.string.error_train_id_invalid, Snackbar.LENGTH_LONG)
                    .show();
        }
    }

    @Override
    public void onRecyclerItemClick(RecyclerView.Adapter sender, Suggestion<IrailVehicleRequest> object) {
        openTrain(object.getData().getVehicleId());
    }

    @Override
    public void onRecyclerItemLongClick(RecyclerView.Adapter sender, Suggestion<IrailVehicleRequest> object) {
        mLastSelectedQuery = object;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (mLastSelectedQuery != null) {
            getActivity().getMenuInflater().inflate(R.menu.context_history, menu);
            menu.setHeaderTitle(mLastSelectedQuery.getData().getVehicleId());
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_delete && mLastSelectedQuery != null) {
            if (mLastSelectedQuery.getType() == SuggestionType.FAVORITE) {
                persistentQueryProvider.delete(mLastSelectedQuery);
                Snackbar.make(recentTrainsRecyclerView, R.string.unmarked_station_favorite, Snackbar.LENGTH_LONG).show();
            } else {
                persistentQueryProvider.delete(mLastSelectedQuery);
            }
            mIrailTrainRequestAdapter.setSuggestedTrains(persistentQueryProvider.getAllTrains());
        }

        // handle menu here - get item index or ID from info
        return super.onContextItemSelected(item);
    }

    private static class LoadSuggestionsTask extends AsyncTask<PersistentQueryProvider, Void, List<Suggestion<IrailVehicleRequest>>> {

        private WeakReference<VehicleSearchFragment> fragmentReference;

        // only retain a weak reference to the activity
        LoadSuggestionsTask(VehicleSearchFragment context) {
            fragmentReference = new WeakReference<>(context);
        }

        @Override
        protected List<Suggestion<IrailVehicleRequest>> doInBackground(PersistentQueryProvider... provider) {
            return provider[0].getAllTrains();
        }

        @Override
        protected void onPostExecute(List<Suggestion<IrailVehicleRequest>> suggestions) {
            super.onPostExecute(suggestions);

            // get a reference to the activity if it is still there
            VehicleSearchFragment fragment = fragmentReference.get();
            if (fragment == null) return;

            fragment.mIrailTrainRequestAdapter.setSuggestedTrains(suggestions);
        }
    }
}
