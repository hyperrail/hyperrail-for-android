/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package android.irail.be.hyperrail;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Intent;
import android.irail.be.hyperrail.adapter.RouteHistoryCardAdapter;
import android.irail.be.hyperrail.adapter.onRecyclerItemClickListener;
import android.irail.be.hyperrail.irail.contracts.IrailStationProvider;
import android.irail.be.hyperrail.irail.contracts.RouteTimeDefinition;
import android.irail.be.hyperrail.irail.db.Station;
import android.irail.be.hyperrail.irail.factories.IrailFactory;
import android.irail.be.hyperrail.persistence.PersistentQueryProvider;
import android.irail.be.hyperrail.persistence.RouteQuery;
import android.irail.be.hyperrail.util.DateTimePicker;
import android.irail.be.hyperrail.util.ErrorDialogFactory;
import android.irail.be.hyperrail.util.OnDateTimeSetListener;
import android.irail.be.hyperrail.util.Swipable;
import android.irail.be.hyperrail.util.SwipeDetector;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * A simple {@link Fragment} subclass.
 */
public class RouteSearchFragment extends Fragment implements onRecyclerItemClickListener<RouteQuery>, OnDateTimeSetListener, Swipable {

    private AutoCompleteTextView vFromText;
    private AutoCompleteTextView vToText;
    private TextView vDatetime;
    private Spinner vArriveDepart;
    private LinearLayout vArriveDepartContainer;

    private Calendar searchDateTime = null;
    private Bundle parameters;

    private PersistentQueryProvider persistentQueryProvider;

    private final int TAG_ACCENT_SEARCH = 1;

    public RouteSearchFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_route_search, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize autocomplete
        IrailStationProvider stationProvider = IrailFactory.getStationsProviderInstance();
        String[] names = stationProvider.getStationNames(stationProvider.getStationsOrderBySize());

        ArrayAdapter<String> autocompleteAdapter = new ArrayAdapter<>(this.getActivity(),
                android.R.layout.simple_dropdown_item_1line, names);
        vFromText = (AutoCompleteTextView)
                view.findViewById(R.id.input_from);
        vToText = (AutoCompleteTextView) view.findViewById(R.id.input_to);
        vFromText.setAdapter(autocompleteAdapter);
        vToText.setAdapter(autocompleteAdapter);

        // Handle special keys in "from" text
        vFromText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {

                    if (vFromText.isPopupShowing()) {
                        vFromText.setText((String) vFromText.getAdapter().getItem(0));
                    }

                    //noinspection StatementWithEmptyBody
                    if (vFromText.getText().length() == 0) {
                        // keep focus on From text
                    } else if (vToText.getText().length() == 0) {
                        vToText.requestFocus();
                    } else {
                        doSearch();
                    }
                    return true;
                }
                return false;
            }
        });

        // Handle special keys in "to" text
        vToText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    if (vToText.isPopupShowing()) {
                        vToText.setText((String) vToText.getAdapter().getItem(0));
                    }

                    //noinspection StatementWithEmptyBody
                    if (vToText.getText().length() == 0) {
                        // keep focus on To text
                    } else if (vFromText.getText().length() == 0) {
                        vFromText.requestFocus();
                    } else {
                        doSearch();
                    }
                    return true;
                }
                return false;
            }
        });

        // Initialize search button
        final Button searchButton = (Button) view.findViewById(R.id.button_search);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doSearch();
            }
        });

        // Initialize swap button
        Button swapButton = (Button) view.findViewById(R.id.button_swap);
        swapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
            }
        });

        vDatetime = (TextView) view.findViewById(R.id.input_datetime);
        vArriveDepart = (Spinner) view.findViewById(R.id.input_arrivedepart);

        Button vPickDateTime = (Button) view.findViewById(R.id.button_pickdatetime);
        View.OnClickListener l = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DateTimePicker picker = new DateTimePicker(getActivity());
                picker.setListener(RouteSearchFragment.this);
                picker.pick();
            }
        };
        vDatetime.setOnClickListener(l);
        vPickDateTime.setOnClickListener(l);

        // Use a normal swipe detector, since we don't need flings or velocity
        SwipeDetector accentSearchSwipeDetector = new SwipeDetector(getActivity(), this, TAG_ACCENT_SEARCH);

        vArriveDepartContainer = (LinearLayout) view.findViewById(R.id.container_arrivedepart);

        // Register all controls in accent search area for swipes
        view.findViewById(R.id.accentSearchContainer).setOnTouchListener(accentSearchSwipeDetector);
        vArriveDepartContainer.setOnTouchListener(accentSearchSwipeDetector);
        view.findViewById(R.id.container_to).setOnTouchListener(accentSearchSwipeDetector);
        view.findViewById(R.id.container_from).setOnTouchListener(accentSearchSwipeDetector);
        vToText.setOnTouchListener(accentSearchSwipeDetector);
        vFromText.setOnTouchListener(accentSearchSwipeDetector);
        vArriveDepart.setOnTouchListener(accentSearchSwipeDetector);
        vDatetime.setOnTouchListener(accentSearchSwipeDetector);

        if (!PreferenceManager.getDefaultSharedPreferences(this.getActivity()).getBoolean("routes_always_datetime", true)) {
            hideDateTimeRow();
        }

        persistentQueryProvider = new PersistentQueryProvider(this.getActivity());

        RecyclerView suggestions = (RecyclerView) this.getActivity().findViewById(R.id.recyclerview_primary);
        suggestions.setLayoutManager(new LinearLayoutManager(this.getActivity()));

        RouteHistoryCardAdapter suggestionAdapter = new RouteHistoryCardAdapter(this.getActivity(), persistentQueryProvider.getAllRoutes());
        suggestionAdapter.setOnItemClickListener(this);

        suggestions.setAdapter(suggestionAdapter);
        suggestionAdapter.notifyDataSetChanged();

        if (parameters != null) {
            vFromText.setText(parameters.getString("from", ""), false);
            vToText.setText(parameters.getString("to", ""), false);
        } else if (savedInstanceState != null) {
            vFromText.setText(savedInstanceState.getString("from", ""), false);
            vToText.setText(savedInstanceState.getString("to", ""), false);
        }

    }

    private void showDateTimeRow() {
        vArriveDepartContainer.setVisibility(View.VISIBLE);
    }

    private void hideDateTimeRow() {
        vArriveDepartContainer.setVisibility(View.GONE);
    }

    @Override
    public void onStart() {
        super.onStart();
        setSuggestions();
    }

    private void setSuggestions() {
        Log.d("RouteSearch", "updating suggestions");
        RecyclerView suggestions = (RecyclerView) this.getActivity().findViewById(R.id.recyclerview_primary);

        RouteHistoryCardAdapter suggestionAdapter = (RouteHistoryCardAdapter) suggestions.getAdapter();
        suggestionAdapter.updateHistory(persistentQueryProvider.getAllRoutes());

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("from", vFromText.getText().toString());
        outState.putString("to", vToText.getText().toString());
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        parameters = savedInstanceState;
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

        if (from == null || from.trim().length() == 0) {
            Snackbar.make(this.getView(), R.string.error_route_from_missing, Snackbar.LENGTH_LONG)
                    .show();
            return;
        } else if (to == null || to.trim().length() == 0) {
            Snackbar.make(this.getView(), R.string.error_route_to_missing, Snackbar.LENGTH_LONG)
                    .show();
            return;
        }

        IrailStationProvider p = IrailFactory.getStationsProviderInstance();
        Station station_from = p.getStationByName(from);

        if (station_from == null) {
            ErrorDialogFactory.showInvalidDepartureStationError(this.getActivity(), false);
            return;
        }

        Station station_to = p.getStationByName(to);
        if (station_to == null) {
            ErrorDialogFactory.showInvalidDestinationStationError(this.getActivity(), false);
            return;
        }

        persistentQueryProvider.addRecentRoute(from, to);
        RouteTimeDefinition arrivedepart;
        if (vArriveDepart.getSelectedItemPosition() == 0) {
            arrivedepart = RouteTimeDefinition.DEPART;
        } else {
            arrivedepart = RouteTimeDefinition.ARRIVE;
        }

        Date d = null;
        if (searchDateTime != null) {
            d = searchDateTime.getTime();
        }
        Intent i = RouteActivity.createIntent(getActivity(), from, to, d, arrivedepart);
        startActivity(i);
    }

    @Override
    public void onRecyclerItemClick(RecyclerView.Adapter sender, RouteQuery object) {
        doSearch(object.from, object.to);
    }

    @Override
    public void onDateTimePicked(Date d) {
        searchDateTime = Calendar.getInstance();
        searchDateTime.setTime(d);

        Calendar now = Calendar.getInstance();

        @SuppressLint("SimpleDateFormat") SimpleDateFormat df = new SimpleDateFormat("dd MMMM yyyy");
        @SuppressLint("SimpleDateFormat") SimpleDateFormat tf = new SimpleDateFormat("HH:mm");

        String day = df.format(searchDateTime.getTime());
        String time = tf.format(searchDateTime.getTime());
        String at = getActivity().getResources().getString(R.string.time_at);

        if (now.get(Calendar.YEAR) == searchDateTime.get(Calendar.YEAR)) {
            if (now.get(Calendar.DAY_OF_YEAR) == searchDateTime.get(Calendar.DAY_OF_YEAR)) {
                day = getActivity().getResources().getString(R.string.today);
            } else if (now.get(Calendar.DAY_OF_YEAR) + 1 == (int) searchDateTime.get(Calendar.DAY_OF_YEAR)) {
                day = getActivity().getResources().getString(R.string.tomorrow);
            }
        }
        vDatetime.setText(day + " " + at + " " + time);
    }

    @Override
    public void swipedBottomToTop(View v, int tag) {
        if (tag == TAG_ACCENT_SEARCH) {
            hideDateTimeRow();
        }
    }

    @Override
    public void swipedLeftToRight(View v, int tag) {

    }

    @Override
    public void swipedRightToLeft(View v, int tag) {

    }

    @Override
    public void swipedTopToBottom(View v, int tag) {
        if (tag == TAG_ACCENT_SEARCH) {
            showDateTimeRow();
        }
    }
}

