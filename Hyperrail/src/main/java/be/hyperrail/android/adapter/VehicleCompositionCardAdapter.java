/*
 *  This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 *  If a copy of the MPL was not distributed with this file,
 *  You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import be.hyperrail.android.R;
import be.hyperrail.android.viewgroup.VehicleCompositionUnitLayout;
import be.hyperrail.opentransportdata.common.models.VehicleComposition;
import be.hyperrail.opentransportdata.common.models.VehicleCompositionUnit;

public class VehicleCompositionCardAdapter extends RecyclerView.Adapter<VehicleCompositionCardAdapter.TrainCompositionUnitViewHolder> {

    private final Context context;
    private VehicleComposition mComposition;

    public VehicleCompositionCardAdapter(Context context, VehicleComposition train) {
        this.context = context;
        this.mComposition = train;
    }

    @NonNull
    @Override
    public VehicleCompositionCardAdapter.TrainCompositionUnitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_train_composition_unit, parent, false);
        return new VehicleCompositionCardAdapter.TrainCompositionUnitViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull VehicleCompositionCardAdapter.TrainCompositionUnitViewHolder holder, int position) {
        final VehicleCompositionUnit unit = mComposition.getVehicleCompositionUnits()[position];
        holder.mVehicleCompositionUnitLayout.bind(context, unit, mComposition, position);
    }

    @Override
    public int getItemCount() {
        if (mComposition == null || mComposition.getVehicleCompositionUnits() == null) {
            return 0;
        }
        return mComposition.getVehicleCompositionUnits().length;
    }

    public void updateComposition(VehicleComposition composition) {
        this.mComposition = composition;
        notifyDataSetChanged();
    }

    class TrainCompositionUnitViewHolder extends RecyclerView.ViewHolder {

        VehicleCompositionUnitLayout mVehicleCompositionUnitLayout;

        TrainCompositionUnitViewHolder(View view) {
            super(view);
            mVehicleCompositionUnitLayout = view.findViewById(R.id.binder);
        }
    }
}
