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


package be.hyperrail.android.fragments.searchresult;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.joda.time.DateTime;

import be.hyperrail.android.R;
import be.hyperrail.android.adapter.DisturbanceCardAdapter;
import be.hyperrail.android.adapter.OnRecyclerItemClickListener;
import be.hyperrail.opentransportdata.OpenTransportApi;
import be.hyperrail.opentransportdata.common.models.Disturbance;
import be.hyperrail.opentransportdata.common.requests.ActualDisturbancesRequest;
import be.hyperrail.opentransportdata.common.requests.RequestType;

/**
 * A list with disturbances
 */
public class DisturbanceListFragment extends RecyclerViewFragment<Disturbance[]> implements ResultFragment<ActualDisturbancesRequest>, OnRecyclerItemClickListener<Disturbance> {

    public static final String INSTANCESTATE_KEY_REQUEST = "request";
    private Disturbance[] disturbances;
    private DateTime lastUpdate;

    DisturbanceCardAdapter disturbanceCardAdapter;
    private ActualDisturbancesRequest mRequest;

    public DisturbanceListFragment() {
        // Required empty public constructor
        mRequest = new ActualDisturbancesRequest();
    }

    public static DisturbanceListFragment newInstance() {
        DisturbanceListFragment frg = new DisturbanceListFragment();
        frg.setRequest(new ActualDisturbancesRequest());
        return frg;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey(INSTANCESTATE_KEY_REQUEST)) {
            mRequest = (ActualDisturbancesRequest) savedInstanceState.getSerializable(INSTANCESTATE_KEY_REQUEST);
        }
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_recyclerview_list, container, false);
    }

    @Override
    protected RecyclerView.Adapter getAdapter() {
        if (disturbanceCardAdapter == null) {
            disturbanceCardAdapter = new DisturbanceCardAdapter(getActivity(), null);
        }
        return disturbanceCardAdapter;
    }

    @Override
    protected void getData() {
        vRefreshLayout.setRefreshing(true);

        OpenTransportApi.getDataProviderInstance().abortQueries(RequestType.DISTURBANCES);

        ActualDisturbancesRequest request = new ActualDisturbancesRequest();
        request.setCallback((data, tag) -> {
            resetErrorState();
            vRefreshLayout.setRefreshing(false);
            lastUpdate = new DateTime();
            showData(data);
        }, (e, tag) -> {
            vRefreshLayout.setRefreshing(false);
            // Don't finish, this is the main activity
           showError(e);
        }, null);
        OpenTransportApi.getDataProviderInstance().getActualDisturbances(request);
    }

    @Override
    protected void showData(Disturbance[] data) {
        if (data == null) {
            return;
        }

        this.disturbances = data;
        disturbanceCardAdapter.updateDisturbances(disturbances);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (lastUpdate != null) {
            outState.putSerializable(INSTANCESTATE_KEY_REQUEST, mRequest);
            outState.putSerializable("disturbances", disturbances);
            outState.putLong("updated", lastUpdate.getMillis());
        }
    }

    @Override
    public void onRecyclerItemClick(RecyclerView.Adapter sender, Disturbance disturbance) {
        if (disturbance.getLink() != null && !disturbance.getLink().isEmpty()) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(disturbance.getLink()));
            startActivity(browserIntent);
        }
    }

    @Override
    public void setRequest(@NonNull ActualDisturbancesRequest request) {
        this.mRequest = request;
    }

    @Override
    public ActualDisturbancesRequest getRequest() {
        return mRequest;
    }

    @Override
    public void loadNextRecyclerviewItems() {
        // Nothing to do - not supported
    }

    @Override
    public void loadPreviousRecyclerviewItems() {
        // Nothing to do - not supported
    }

    @Override
    public void onDateTimePicked(DateTime d) {
        // Nothing to do - not supported
    }
}
