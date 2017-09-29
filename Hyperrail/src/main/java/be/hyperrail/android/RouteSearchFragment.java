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

package be.hyperrail.android;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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

import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import be.hyperrail.android.adapter.OnRecyclerItemClickListener;
import be.hyperrail.android.adapter.OnRecyclerItemLongClickListener;
import be.hyperrail.android.adapter.RouteSuggestionsCardAdapter;
import be.hyperrail.android.irail.contracts.IrailStationProvider;
import be.hyperrail.android.irail.contracts.RouteTimeDefinition;
import be.hyperrail.android.irail.db.Station;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.persistence.PersistentQueryProvider;
import be.hyperrail.android.persistence.RouteQuery;
import be.hyperrail.android.util.DateTimePicker;
import be.hyperrail.android.util.ErrorDialogFactory;
import be.hyperrail.android.util.OnDateTimeSetListener;
import be.hyperrail.android.util.Swipable;
import be.hyperrail.android.util.SwipeDetector;

/**
 * A simple {@link Fragment} subclass.
 */
public class RouteSearchFragment extends Fragment implements OnRecyclerItemClickListener<RouteQuery>, OnDateTimeSetListener, Swipable, OnRecyclerItemLongClickListener<RouteQuery> {

    private AutoCompleteTextView vFromText;
    private AutoCompleteTextView vToText;
    private TextView vDatetime;
    private Spinner vArriveDepart;
    private LinearLayout vArriveDepartContainer;

    private DateTime searchDateTime = null;

    private PersistentQueryProvider persistentQueryProvider;

    private final int TAG_ACCENT_SEARCH = 1;
    private RouteQuery mLastSelectedQuery;
    private RecyclerView mSuggestionsRecyclerView;
    private RouteSuggestionsCardAdapter mSuggestionsAdapter;

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

        vFromText = view.findViewById(R.id.input_from);
        vToText = view.findViewById(R.id.input_to);

        vDatetime = view.findViewById(R.id.input_datetime);
        vArriveDepart = view.findViewById(R.id.input_arrivedepart);

        persistentQueryProvider = new PersistentQueryProvider(this.getActivity());

        mSuggestionsRecyclerView = view.findViewById(R.id.recyclerview_primary);
        mSuggestionsRecyclerView.setLayoutManager(new LinearLayoutManager(this.getActivity()));
        registerForContextMenu(mSuggestionsRecyclerView);

        mSuggestionsAdapter = new RouteSuggestionsCardAdapter(this.getActivity(), null);
        mSuggestionsAdapter.setOnItemClickListener(this);
        mSuggestionsAdapter.setOnLongItemClickListener(this);
        mSuggestionsRecyclerView.setAdapter(mSuggestionsAdapter);

        // Initialize autocomplete
        IrailStationProvider stationProvider = IrailFactory.getStationsProviderInstance();
        String[] names = stationProvider.getStationNames(stationProvider.getStationsOrderBySize());

        ArrayAdapter<String> autocompleteAdapter = new ArrayAdapter<>(this.getActivity(),
                android.R.layout.simple_dropdown_item_1line, names);

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
        final Button searchButton = view.findViewById(R.id.button_search);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doSearch();
            }
        });

        // Initialize swap button
        Button swapButton = view.findViewById(R.id.button_swap);
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

        Button vPickDateTime = view.findViewById(R.id.button_pickdatetime);
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

        vArriveDepartContainer = view.findViewById(R.id.container_arrivedepart);

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

        if (this.getArguments() != null && (this.getArguments().containsKey("from") || this.getArguments().containsKey("to"))) {
            vFromText.setText(this.getArguments().getString("from", ""), false);
            vToText.setText(this.getArguments().getString("to", ""), false);

            if (!this.getArguments().containsKey("to")) {
                vToText.requestFocus();
            } else {
                vFromText.requestFocus();
            }
        } else if (savedInstanceState != null) {
            vFromText.setText(savedInstanceState.getString("from", ""), false);
            vToText.setText(savedInstanceState.getString("to", ""), false);
        }
        setSuggestions();
    }

    private void showDateTimeRow() {
        vArriveDepartContainer.setVisibility(View.VISIBLE);
    }

    private void hideDateTimeRow() {
        vArriveDepartContainer.setVisibility(View.GONE);
    }

    @Override
    public void onRecyclerItemLongClick(RecyclerView.Adapter sender, RouteQuery object) {
        mLastSelectedQuery = object;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getActivity().getMenuInflater().inflate(R.menu.context_history, menu);
        if (mLastSelectedQuery != null) {
            menu.setHeaderTitle(mLastSelectedQuery.fromName + " - " + mLastSelectedQuery.toName);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_delete && mLastSelectedQuery != null) {
            if (mLastSelectedQuery.type == RouteQuery.RouteQueryType.FAVORITE_ROUTE) {
                persistentQueryProvider.removeFavoriteRoute(mLastSelectedQuery.from, mLastSelectedQuery.to);
                Snackbar.make(mSuggestionsRecyclerView, R.string.unmarked_route_favorite, Snackbar.LENGTH_LONG).show();
            } else {
                persistentQueryProvider.removeRecentRoute(mLastSelectedQuery.from, mLastSelectedQuery.to);
            }
            mSuggestionsAdapter.updateHistory(persistentQueryProvider.getAllRoutes());
        }

        // handle menu here - get item index or ID from info
        return super.onContextItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        setSuggestions();
    }

    private void setSuggestions() {
        RecyclerView suggestions = this.getActivity().findViewById(R.id.recyclerview_primary);
        // TODO pixel on Android O requires this much validation. There should be a better way for this.
        if (suggestions != null && suggestions.getAdapter() != null && suggestions.getAdapter() instanceof RouteSuggestionsCardAdapter) {
            RouteSuggestionsCardAdapter suggestionAdapter = (RouteSuggestionsCardAdapter) suggestions.getAdapter();
            suggestionAdapter.updateHistory(persistentQueryProvider.getAllRoutes());
        }
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
        View currentView = this.getView();

        if (from == null || from.trim().length() == 0) {
            if (currentView != null) {
                Snackbar.make(currentView, R.string.error_route_from_missing, Snackbar.LENGTH_LONG)
                        .show();
            } else {
                ErrorDialogFactory.showInvalidDepartureStationError(this.getActivity(), false);
            }
            return;
        } else if (to == null || to.trim().length() == 0) {
            if (currentView != null) {
                Snackbar.make(currentView, R.string.error_route_to_missing, Snackbar.LENGTH_LONG)
                        .show();
            } else {
                ErrorDialogFactory.showInvalidDestinationStationError(this.getActivity(), false);
            }
            return;
        }

        IrailStationProvider p = IrailFactory.getStationsProviderInstance();
        Station station_from = p.getStationByName(from);

        Station station_to = p.getStationByName(to);

        doSearch(station_from, station_to);
    }

    private void doSearch(Station from, Station to) {
        if (from == null) {
            ErrorDialogFactory.showInvalidDepartureStationError(this.getActivity(), false);
            return;
        }

        if (to == null) {
            ErrorDialogFactory.showInvalidDestinationStationError(this.getActivity(), false);
            return;
        }

        if (from == to) {
            ErrorDialogFactory.showDepartureEqualsArrivalStationError(this.getActivity(), false);
            return;
        }

        persistentQueryProvider.addRecentRoute(from, to);
        RouteTimeDefinition arrivedepart;
        if (vArriveDepart.getSelectedItemPosition() == 0) {
            arrivedepart = RouteTimeDefinition.DEPART;
        } else {
            arrivedepart = RouteTimeDefinition.ARRIVE;
        }

        DateTime d = null;
        if (searchDateTime != null) {
            d = searchDateTime;
        }
        Intent i = RouteActivity.createIntent(getActivity(), from, to, d, arrivedepart);
        startActivity(i);
    }

    @Override
    public void onRecyclerItemClick(RecyclerView.Adapter sender, RouteQuery object) {
        doSearch(object.from, object.to);
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

