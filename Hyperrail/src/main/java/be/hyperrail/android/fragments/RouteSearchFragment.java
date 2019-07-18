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
import androidx.annotation.NonNull;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.firebase.perf.metrics.AddTrace;

import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.lang.ref.WeakReference;
import java.util.List;

import be.hyperrail.android.R;
import be.hyperrail.android.activities.searchresult.RouteActivity;
import be.hyperrail.android.adapter.OnRecyclerItemClickListener;
import be.hyperrail.android.adapter.OnRecyclerItemLongClickListener;
import be.hyperrail.android.adapter.RouteSuggestionsCardAdapter;
import be.hyperrail.android.persistence.PersistentQueryProvider;
import be.hyperrail.android.persistence.Suggestion;
import be.hyperrail.android.persistence.SuggestionType;
import be.hyperrail.android.util.DateTimePicker;
import be.hyperrail.android.util.ErrorDialogFactory;
import be.hyperrail.android.util.OnDateTimeSetListener;
import be.hyperrail.opentransportdata.OpenTransportApi;
import be.hyperrail.opentransportdata.common.contracts.QueryTimeDefinition;
import be.hyperrail.opentransportdata.common.contracts.TransportStopsDataSource;
import be.hyperrail.opentransportdata.common.models.StopLocation;
import be.hyperrail.opentransportdata.common.requests.RoutePlanningRequest;

/**
 * A simple {@link Fragment} subclass.
 */
public class RouteSearchFragment extends Fragment implements OnRecyclerItemClickListener<Suggestion<RoutePlanningRequest>>, OnDateTimeSetListener, OnRecyclerItemLongClickListener<Suggestion<RoutePlanningRequest>> {

    private AutoCompleteTextView vFromText;
    private AutoCompleteTextView vToText;
    private TextView vDatetime;
    private Spinner vArriveDepart;
    private LinearLayout vArriveDepartContainer;

    private DateTime searchDateTime = null;

    private PersistentQueryProvider persistentQueryProvider;

    private Suggestion<RoutePlanningRequest> mLastSelectedQuery;
    private RecyclerView mSuggestionsRecyclerView;
    private RouteSuggestionsCardAdapter mSuggestionsAdapter;
    private LoadSuggestionsTask activeSuggestionsUpdateTask;


    @Override
    @AddTrace(name = "RouteSearchFragment.onCreateView")
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        return inflater.inflate(R.layout.fragment_route_search, container, false);
    }

    @Override
    @AddTrace(name = "RouteSearchFragment.onViewCreated")
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        vFromText = view.findViewById(R.id.input_from);
        vToText = view.findViewById(R.id.input_to);

        vDatetime = view.findViewById(R.id.input_datetime);
        vArriveDepart = view.findViewById(R.id.input_arrivedepart);

        persistentQueryProvider = PersistentQueryProvider.getInstance(this.getActivity());
        activeSuggestionsUpdateTask = new LoadSuggestionsTask(this);
        activeSuggestionsUpdateTask.execute(persistentQueryProvider);

        mSuggestionsRecyclerView = view.findViewById(R.id.recyclerview_primary);
        mSuggestionsRecyclerView.setLayoutManager(new LinearLayoutManager(this.getActivity()));
        registerForContextMenu(mSuggestionsRecyclerView);

        createSuggestionsAdapter();

        // Load autocomplete information
        LoadAutoCompleteTask loadAutoCompleteTask = new LoadAutoCompleteTask(this);
        loadAutoCompleteTask.execute(OpenTransportApi.getStopLocationProviderInstance());

        createKeyListeners();
        createClickListeners(view);

        vArriveDepartContainer = view.findViewById(R.id.container_arrivedepart);

        if (this.getArguments() != null && (this.getArguments().containsKey("from") || this.getArguments().containsKey("to"))) {
            prefillFieldsFromArguments();
        } else if (savedInstanceState != null) {
            vFromText.setText(savedInstanceState.getString("from", ""), false);
            vToText.setText(savedInstanceState.getString("to", ""), false);
        }
    }

    private void prefillFieldsFromArguments() {
        vFromText.setText(this.getArguments().getString("from", ""), false);
        vToText.setText(this.getArguments().getString("to", ""), false);

        if (!this.getArguments().containsKey("to")) {
            vToText.requestFocus();
        } else {
            vFromText.requestFocus();
        }
    }

    private void createClickListeners(@NonNull View view) {
        // Initialize search button
        final Button searchButton = view.findViewById(R.id.button_search);
        searchButton.setOnClickListener(v -> doSearch());

        // Initialize swap button
        Button swapButton = view.findViewById(R.id.button_swap);
        swapButton.setOnClickListener(v -> {
            Editable from = vFromText.getText();
            Editable to = vToText.getText();
            vToText.setText(from);
            vFromText.setText(to);
            if (vToText.getText().length() == 0) {
                vToText.requestFocus();
            } else if (vFromText.getText().length() == 0) {
                vFromText.requestFocus();
            } else {
                searchButton.requestFocus();
            }
        });

        Button vPickDateTime = view.findViewById(R.id.button_pickdatetime);
        View.OnClickListener l = v -> {
            DateTimePicker picker = new DateTimePicker(getActivity());
            picker.setListener(RouteSearchFragment.this);
            picker.pick();
        };
        vDatetime.setOnClickListener(l);
        vPickDateTime.setOnClickListener(l);
    }

    private void createKeyListeners() {
        // Handle special keys in "from" text
        vFromText.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                if (vFromText.isPopupShowing()) {
                    vFromText.setText((String) vFromText.getAdapter().getItem(0));
                }
                moveFocusOrSearch(vFromText, vToText);

                return true;
            }
            return false;
        });

        // Handle special keys in "to" text
        vToText.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                if (vToText.isPopupShowing()) {
                    vToText.setText((String) vToText.getAdapter().getItem(0));
                }

                //noinspection StatementWithEmptyBody
                moveFocusOrSearch(vToText, vFromText);
                return true;
            }
            return false;
        });
    }

    /**
     * Move the focus to another text field when not everything has been filled in, or search when all data has been entered.
     *
     * @param currentField
     * @param fieldToSwitchFocusTo
     */
    private void moveFocusOrSearch(AutoCompleteTextView currentField, AutoCompleteTextView fieldToSwitchFocusTo) {
        //noinspection StatementWithEmptyBody
        if (currentField.getText().length() == 0) {
            // keep focus on From text
        } else if (fieldToSwitchFocusTo.getText().length() == 0) {
            fieldToSwitchFocusTo.requestFocus();
        } else {
            doSearch();
        }
    }

    private void createSuggestionsAdapter() {
        mSuggestionsAdapter = new RouteSuggestionsCardAdapter(this.getActivity(), null);
        mSuggestionsAdapter.setOnItemClickListener(this);
        mSuggestionsAdapter.setOnLongItemClickListener(this);
        // Hide the recyclerview when empty - a placeholder will shift into place automaticly
        mSuggestionsAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if (mSuggestionsAdapter.getItemCount() == 0) {
                    mSuggestionsRecyclerView.setVisibility(View.GONE);
                } else {
                    mSuggestionsRecyclerView.setVisibility(View.VISIBLE);
                }
            }
        });
        mSuggestionsRecyclerView.setAdapter(mSuggestionsAdapter);
    }

    @Override
    public void onRecyclerItemLongClick(RecyclerView.Adapter sender, Suggestion<RoutePlanningRequest> object) {
        mLastSelectedQuery = object;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        if (getActivity() == null) {
            return;
        }

        getActivity().getMenuInflater().inflate(R.menu.context_history, menu);
        if (mLastSelectedQuery != null) {
            menu.setHeaderTitle(mLastSelectedQuery.getData().getOrigin().getLocalizedName() + " - " + mLastSelectedQuery.getData().getDestination().getLocalizedName());
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_delete && mLastSelectedQuery != null) {
            if (mLastSelectedQuery.getType() == SuggestionType.FAVORITE) {
                persistentQueryProvider.delete(mLastSelectedQuery);
                Snackbar.make(mSuggestionsRecyclerView, R.string.unmarked_route_favorite, Snackbar.LENGTH_LONG).show();
            } else {
                persistentQueryProvider.delete(mLastSelectedQuery);
            }
            mSuggestionsAdapter.setSuggestedRoutes(persistentQueryProvider.getAllRoutes());
        }

        // handle menu here - get item index or ID from info
        return super.onContextItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (activeSuggestionsUpdateTask.getStatus() == AsyncTask.Status.FINISHED) {
            activeSuggestionsUpdateTask.cancel(true);
            activeSuggestionsUpdateTask = new LoadSuggestionsTask(this);
            activeSuggestionsUpdateTask.execute(persistentQueryProvider);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("from", vFromText.getText().toString());
        outState.putString("to", vToText.getText().toString());
    }

    public static RouteSearchFragment newInstance() {
        RouteSearchFragment fragment = new RouteSearchFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    private void doSearch() {
        doSearch(vFromText.getText().toString(), vToText.getText().toString());
    }

    private void doSearch(String from, String to) {
        if (!validateInputFields(from, to)) {
            return;
        }

        TransportStopsDataSource stopLocationProvider = OpenTransportApi.getStopLocationProviderInstance();
        StopLocation fromStopLocation = stopLocationProvider.getStoplocationByExactName(from);
        StopLocation toStopLocation = stopLocationProvider.getStoplocationByExactName(to);

        doSearch(fromStopLocation, toStopLocation);
    }

    private boolean validateInputFields(String from, String to) {
        View currentView = this.getView();

        if (from == null || from.trim().length() == 0) {
            if (currentView != null) {
                Snackbar.make(currentView, R.string.error_route_from_missing, Snackbar.LENGTH_LONG)
                        .show();
            } else {
                ErrorDialogFactory.showInvalidDepartureStationError(this.getActivity(), false);
            }
            return false;
        } else if (to == null || to.trim().length() == 0) {
            if (currentView != null) {
                Snackbar.make(currentView, R.string.error_route_to_missing, Snackbar.LENGTH_LONG)
                        .show();
            } else {
                ErrorDialogFactory.showInvalidDestinationStationError(this.getActivity(), false);
            }
            return false;
        }
        return true;
    }

    private void doSearch(StopLocation from, StopLocation to) {
        if (from == null) {
            ErrorDialogFactory.showInvalidDepartureStationError(this.getActivity(), false);
            return;
        }

        if (to == null) {
            ErrorDialogFactory.showInvalidDestinationStationError(this.getActivity(), false);
            return;
        }

        if (from.equals(to)) {
            ErrorDialogFactory.showDepartureEqualsArrivalStationError(this.getActivity(), false);
            return;
        }


        QueryTimeDefinition timedef;
        if (vArriveDepart.getSelectedItemPosition() == 0) {
            timedef = QueryTimeDefinition.EQUAL_OR_LATER;
        } else {
            timedef = QueryTimeDefinition.EQUAL_OR_EARLIER;
        }

        DateTime d = null;
        if (searchDateTime != null) {
            d = searchDateTime;
        }

        RoutePlanningRequest request = new RoutePlanningRequest(from, to, timedef, d);
        persistentQueryProvider.store(new Suggestion<>(request, SuggestionType.HISTORY));

        Intent i = RouteActivity.createIntent(getActivity(), request);
        startActivity(i);
    }

    @Override
    public void onRecyclerItemClick(RecyclerView.Adapter sender, Suggestion<RoutePlanningRequest> object) {
        doSearch(object.getData().getOrigin(), object.getData().getDestination());
    }

    @Override
    public void onDateTimePicked(DateTime d) {
        searchDateTime = d;

        DateTime now = new DateTime();

        DateTimeFormatter df = DateTimeFormat.forPattern("dd MMMM yyyy");
        DateTimeFormatter tf = DateTimeFormat.forPattern("HH:mm");

        String day = df.print(searchDateTime);
        String time = tf.print(searchDateTime);
        String at = getActivity().getResources().getString(R.string.time_at);

        if (now.get(DateTimeFieldType.year()) == searchDateTime.get(DateTimeFieldType.year())) {
            if (now.get(DateTimeFieldType.dayOfYear()) == searchDateTime.get(DateTimeFieldType.dayOfYear())) {
                day = getActivity().getResources().getString(R.string.time_today);
            } else  //noinspection RedundantCast
                if (now.get(DateTimeFieldType.dayOfYear()) + 1 == (int) searchDateTime.get(DateTimeFieldType.dayOfYear())) {
                    day = getActivity().getResources().getString(R.string.time_tomorrow);
                }
        }
        vDatetime.setText(day + " " + at + " " + time);
    }

    private static class LoadSuggestionsTask extends AsyncTask<PersistentQueryProvider, Void, List<Suggestion<RoutePlanningRequest>>> {

        private WeakReference<RouteSearchFragment> fragmentReference;

        // only retain a weak reference to the activity
        LoadSuggestionsTask(RouteSearchFragment context) {
            fragmentReference = new WeakReference<>(context);
        }

        @Override
        protected List<Suggestion<RoutePlanningRequest>> doInBackground(PersistentQueryProvider... provider) {
            return provider[0].getAllRoutes();
        }

        @Override
        protected void onPostExecute(List<Suggestion<RoutePlanningRequest>> suggestions) {
            super.onPostExecute(suggestions);

            // get a reference to the activity if it is still there
            RouteSearchFragment fragment = fragmentReference.get();
            if (fragment == null) {
                return;
            }

            fragment.mSuggestionsAdapter.setSuggestedRoutes(suggestions);
        }
    }

    private static class LoadAutoCompleteTask extends AsyncTask<TransportStopsDataSource, Void, String[]> {

        private WeakReference<RouteSearchFragment> fragmentReference;

        // only retain a weak reference to the activity
        LoadAutoCompleteTask(RouteSearchFragment context) {
            fragmentReference = new WeakReference<>(context);
        }

        @Override
        protected String[] doInBackground(TransportStopsDataSource... provider) {
            Thread.currentThread().setName("LoadAutoCompleteTask");
            return provider[0].getStoplocationsNames(provider[0].getStoplocationsOrderedBySize());

        }

        @Override
        protected void onPostExecute(String[] stations) {
            super.onPostExecute(stations);

            // get a reference to the activity if it is still there
            RouteSearchFragment fragment = fragmentReference.get();
            if (fragment == null || fragment.getActivity() == null) {
                return;
            }

            // Initialize autocomplete
            ArrayAdapter<String> autocompleteAdapter = new ArrayAdapter<>(fragment.getActivity(),
                    android.R.layout.simple_dropdown_item_1line, stations);

            fragment.vFromText.setAdapter(autocompleteAdapter);
            fragment.vToText.setAdapter(autocompleteAdapter);
        }
    }

}

