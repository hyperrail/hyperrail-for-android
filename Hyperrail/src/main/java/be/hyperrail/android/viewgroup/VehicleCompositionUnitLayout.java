/*
 *  This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 *  If a copy of the MPL was not distributed with this file,
 *  You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.viewgroup;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import be.hyperrail.android.R;
import be.hyperrail.opentransportdata.common.models.VehicleComposition;
import be.hyperrail.opentransportdata.common.models.VehicleCompositionUnit;


public class VehicleCompositionUnitLayout extends LinearLayout implements RecyclerViewItemViewGroup<VehicleComposition, VehicleCompositionUnit> {

    protected ImageView vImage;
    protected TextView vUnitType;
    protected TextView vUnitNumber;
    protected TextView vSecondClassLabel;
    protected TextView vFirstClassLabel;

    public VehicleCompositionUnitLayout(Context context) {
        super(context);
    }

    public VehicleCompositionUnitLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public VehicleCompositionUnitLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        vImage = findViewById(R.id.image_unit);
        vUnitType = findViewById(R.id.label_traincomp_type);
        vUnitNumber = findViewById(R.id.label_traincomp_number);
        vFirstClassLabel = findViewById(R.id.label_traincomp_firstclass);
        vSecondClassLabel = findViewById(R.id.label_traincomp_secondclass);
    }


    @SuppressLint("SetTextI18n")
    @Override
    public void bind(Context context, VehicleCompositionUnit itemData, VehicleComposition listData, int position) {
        vImage.setImageResource(itemData.getDrawableResourceId());
        if (itemData.getPublicFacingNumber() != null) {
            vUnitNumber.setText(itemData.getPublicFacingNumber().toString());
        }
        vUnitType.setText(itemData.getPublicTypeName());

        vFirstClassLabel.setVisibility(itemData.getNumberOfFirstClassSeats() > 0 ? VISIBLE : GONE);
        vSecondClassLabel.setVisibility(itemData.getNumberOfSecondClassSeats() > 0 ? VISIBLE : GONE);
    }
}
