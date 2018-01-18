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

package be.hyperrail.android;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
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

import org.joda.time.DateTime;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import be.hyperrail.android.adapter.OnRecyclerItemClickListener;
import be.hyperrail.android.adapter.OnRecyclerItemLongClickListener;
import be.hyperrail.android.adapter.TrainSuggestionsCardAdapter;
import be.hyperrail.android.irail.implementation.TrainStub;
import be.hyperrail.android.persistence.PersistentQueryProvider;
import be.hyperrail.android.persistence.Suggestable;
import be.hyperrail.android.persistence.Suggestion;
import be.hyperrail.android.persistence.SuggestionType;
import be.hyperrail.android.persistence.TrainSuggestion;

/**
 * Fragment to let users search stations, and pick one to show its liveboard
 */
public class TrainSearchFragment extends Fragment implements OnRecyclerItemClickListener<Suggestion<TrainSuggestion>>, OnRecyclerItemLongClickListener<Suggestion<TrainSuggestion>> {

    private RecyclerView recentTrainsRecyclerView;
    private EditText vTrainSearchField;

    private PersistentQueryProvider persistentQueryProvider;
    private Suggestion<TrainSuggestion> mLastSelectedQuery;
    private TrainSuggestionsCardAdapter mTrainSuggestionAdapter;

    public TrainSearchFragment() {
        // Required empty public constructor
    }

    public static TrainSearchFragment newInstance() {
        return new TrainSearchFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_train_search, container, false);
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Create an instance of GoogleAPIClient.
        persistentQueryProvider = new PersistentQueryProvider(this.getActivity());

        recentTrainsRecyclerView = view.findViewById(R.id.recyclerview_primary);

        recentTrainsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recentTrainsRecyclerView.setItemAnimator(new DefaultItemAnimator());
        registerForContextMenu(recentTrainsRecyclerView);

        mTrainSuggestionAdapter = new TrainSuggestionsCardAdapter(this.getActivity());
        mTrainSuggestionAdapter.setOnItemClickListener(this);
        mTrainSuggestionAdapter.setOnLongItemClickListener(this);
        recentTrainsRecyclerView.setAdapter(mTrainSuggestionAdapter);
        setSuggestions();

        vTrainSearchField = view.findViewById(R.id.input_train);

        // Handle special keys in "from" text
        vTrainSearchField.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {

                    //noinspection StatementWithEmptyBody
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("station", vTrainSearchField.getText().toString());
    }

    private void setSuggestions() {
        if (recentTrainsRecyclerView != null && recentTrainsRecyclerView.getAdapter() != null && recentTrainsRecyclerView.getAdapter() instanceof TrainSuggestionsCardAdapter) {
            TrainSuggestionsCardAdapter suggestionAdapter = (TrainSuggestionsCardAdapter) recentTrainsRecyclerView.getAdapter();
            suggestionAdapter.setTrains(persistentQueryProvider.getAllTrains());
        }
    }

    /**
     * Open a trainActivity for a certain train id
     *
     * @param t The train which should be loaded
     */
    private void openTrain(TrainStub t) {
        persistentQueryProvider.store(new Suggestion<Suggestable>(new TrainSuggestion(t), SuggestionType.HISTORY));
        Intent i = TrainActivity.createIntent(getActivity(), t, DateTime.now());
        startActivity(i);
    }

    private void doSearch() {
        String searchQuery = vTrainSearchField.getText().toString();
        // Remove spaces
        searchQuery = searchQuery.replace(" ","");
        Pattern p = Pattern.compile("\\w{1,3}\\d{2,6}");
        Matcher m = p.matcher(searchQuery);
        if (m.matches()) {
            openTrain(new TrainStub(searchQuery.toUpperCase(), null, null));
            return;
        }

        if (this.getView() != null) {
            Snackbar.make(this.getView(), R.string.error_train_id_invalid, Snackbar.LENGTH_LONG)
                    .show();
        }
    }

    @Override
    public void onRecyclerItemClick(RecyclerView.Adapter sender, Suggestion<TrainSuggestion> object) {
        openTrain(object.getData());
    }

    @Override
    public void onRecyclerItemLongClick(RecyclerView.Adapter sender, Suggestion<TrainSuggestion> object) {
        mLastSelectedQuery = object;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (mLastSelectedQuery != null) {
            getActivity().getMenuInflater().inflate(R.menu.context_history, menu);
            menu.setHeaderTitle(mLastSelectedQuery.getData().getName());
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
            mTrainSuggestionAdapter.setTrains(persistentQueryProvider.getAllTrains());
        }

        // handle menu here - get item index or ID from info
        return super.onContextItemSelected(item);

    }
}
