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


package be.hyperrail.android.fragments.searchResult;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.joda.time.DateTime;

import be.hyperrail.android.R;
import be.hyperrail.android.adapter.DisturbanceCardAdapter;
import be.hyperrail.android.adapter.OnRecyclerItemClickListener;
import be.hyperrail.android.irail.contracts.IRailErrorResponseListener;
import be.hyperrail.android.irail.contracts.IRailSuccessResponseListener;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.irail.implementation.Disturbance;
import be.hyperrail.android.irail.implementation.requests.IrailDisturbanceRequest;
import be.hyperrail.android.util.ErrorDialogFactory;

/**
 * A list with disturbances
 */
public class DisturbanceListFragment extends RecyclerViewFragment<Disturbance[]> implements ResultFragment<IrailDisturbanceRequest>, OnRecyclerItemClickListener<Disturbance> {

    private Disturbance[] disturbances;
    private DateTime lastUpdate;

    DisturbanceCardAdapter disturbanceCardAdapter;
    private IrailDisturbanceRequest mRequest;

    public static DisturbanceListFragment createInstance(){
        DisturbanceListFragment frg = new DisturbanceListFragment();
        frg.setRequest(new IrailDisturbanceRequest());
        return frg;
    }

    public DisturbanceListFragment() {
        // Required empty public constructor
        mRequest = new IrailDisturbanceRequest();
    }

    public static DisturbanceListFragment newInstance() {
        return new DisturbanceListFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_recyclerview_list, container, false);
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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

        IrailFactory.getDataProviderInstance().abortAllQueries();

        IrailDisturbanceRequest request = new IrailDisturbanceRequest();
        request.setCallback(new IRailSuccessResponseListener<Disturbance[]>() {
            @Override
            public void onSuccessResponse(@NonNull Disturbance[] data, Object tag) {
                vRefreshLayout.setRefreshing(false);
                lastUpdate = new DateTime();
                showData(data);
            }
        }, new IRailErrorResponseListener() {
            @Override
            public void onErrorResponse(@NonNull Exception e, Object tag) {
                vRefreshLayout.setRefreshing(false);
                // Don't finish, this is the main activity
                ErrorDialogFactory.showErrorDialog(e, DisturbanceListFragment.this.getActivity(), false);
            }
        }, null);
        IrailFactory.getDataProviderInstance().getDisturbances(request);
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (lastUpdate != null) {
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
    public void setRequest(@NonNull IrailDisturbanceRequest request) {
        this.mRequest = request;
    }

    @Override
    public IrailDisturbanceRequest getRequest() {
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
