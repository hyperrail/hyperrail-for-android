/*
 *  This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 *  If a copy of the MPL was not distributed with this file,
 *  You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.viewgroup;

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
        vUnitType = findViewById(R.id.text_unit_type);
        vUnitNumber = findViewById(R.id.text_unit_number);
    }


    @Override
    public void bind(Context context, VehicleCompositionUnit itemData, VehicleComposition listData, int position) {
        vImage.setImageResource(itemData.getDrawableResourceId());
        if (itemData.getPublicFacingNumber() != null) {
            vUnitNumber.setText(itemData.getPublicFacingNumber().toString());
        }
        vUnitType.setText(itemData.getPublicTypeName());
    }
}
