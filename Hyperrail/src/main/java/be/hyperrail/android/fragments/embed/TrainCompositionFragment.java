/*
 *  This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 *  If a copy of the MPL was not distributed with this file,
 *  You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.fragments.embed;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.joda.time.DateTime;

import be.hyperrail.android.R;
import be.hyperrail.android.adapter.VehicleCompositionCardAdapter;
import be.hyperrail.android.fragments.searchresult.ResultFragment;
import be.hyperrail.opentransportdata.OpenTransportApi;
import be.hyperrail.opentransportdata.common.models.VehicleComposition;
import be.hyperrail.opentransportdata.common.requests.RequestType;
import be.hyperrail.opentransportdata.common.requests.VehicleCompositionRequest;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link TrainCompositionFragment#createInstance(String)} factory method to
 * create an instance of this fragment.
 */
public class TrainCompositionFragment extends Fragment implements ResultFragment<VehicleCompositionRequest> {

    private static final String ARG_VEHICLE_ID = "vehicleId";
    private static final String SAVEDINSTANCESTATE_COMPOSITION = "savedResult";

    private String mVehicleId;
    private VehicleCompositionRequest mRequest;

    private LinearLayout rootElement;
    private RecyclerView vRecyclerView;
    private VehicleCompositionCardAdapter recyclerViewAdapter;
    private TextView vWarningUnconfirmed;
    private TextView vErrorUnavailable;
    private ProgressBar vLoadingProgBar;

    public TrainCompositionFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param vehicleId The id of the vehicle to show;
     * @return A new instance of fragment TrainCompositionFragment.
     */
    public static TrainCompositionFragment createInstance(String vehicleId) {
        TrainCompositionFragment fragment = new TrainCompositionFragment();
        Bundle args = new Bundle();
        args.putString(ARG_VEHICLE_ID, vehicleId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mVehicleId = getArguments().getString(ARG_VEHICLE_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_vehicle_composition, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Restore a previous instance state
        VehicleComposition storedInstanceState = getStoredInstanceStateData(savedInstanceState);
        rootElement = view.findViewById(R.id.root);
        vLoadingProgBar = view.findViewById(R.id.progressBar);
        vRecyclerView = view.findViewById(R.id.recyclerview_primary);
        vErrorUnavailable = view.findViewById(R.id.text_status_unavailable);
        vWarningUnconfirmed = view.findViewById(R.id.text_status_unconfirmed);
        recyclerViewAdapter = new VehicleCompositionCardAdapter(view.getContext(), null);

        // Only a progressbar to start
        vRecyclerView.setVisibility(View.GONE);
        vErrorUnavailable.setVisibility(View.GONE);
        vWarningUnconfirmed.setVisibility(View.GONE);

        // Set-up recyclerview
        vRecyclerView.setItemAnimator(new DefaultItemAnimator());
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity(), RecyclerView.HORIZONTAL, false);
        mLayoutManager.setSmoothScrollbarEnabled(true);
        vRecyclerView.setLayoutManager(mLayoutManager);
        vRecyclerView.setAdapter(recyclerViewAdapter);

        if (storedInstanceState == null) {
            getData();
        } else {
            showData(storedInstanceState);
        }
    }

    private VehicleComposition getStoredInstanceStateData(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey(SAVEDINSTANCESTATE_COMPOSITION)) {
            return (VehicleComposition) savedInstanceState.getSerializable(SAVEDINSTANCESTATE_COMPOSITION);
        } else {
            return null;
        }
    }

    private void showData(VehicleComposition data) {
        vLoadingProgBar.setVisibility(View.GONE);
        vRecyclerView.setVisibility(View.VISIBLE);
        if (data.isConfirmed()) {
            vWarningUnconfirmed.setVisibility(View.GONE);
        } else {
            vWarningUnconfirmed.setVisibility(View.VISIBLE);
        }

        vErrorUnavailable.setVisibility(View.GONE);
        recyclerViewAdapter.updateComposition(data);
    }

    private void getData() {
        OpenTransportApi.getDataProviderInstance().abortQueries(RequestType.VEHICLECOMPOSITION);

        VehicleCompositionRequest request = new VehicleCompositionRequest(mVehicleId);
        request.setCallback((data, tag) -> {
            showData(data);
        }, (e, tag) -> showError(), null);
        OpenTransportApi.getDataProviderInstance().getVehicleComposition(request);
    }

    private void showError() {
        vLoadingProgBar.setVisibility(View.GONE);
        vRecyclerView.setVisibility(View.GONE);
        vWarningUnconfirmed.setVisibility(View.GONE);
        vErrorUnavailable.setVisibility(View.VISIBLE);
    }

    @Override
    public VehicleCompositionRequest getRequest() {
        return mRequest;
    }

    @Override
    public void setRequest(@NonNull VehicleCompositionRequest request) {
        mRequest = request;
    }

    @Override
    public void onDateTimePicked(DateTime d) {
        throw new java.lang.UnsupportedOperationException();
    }
}
