/*
 *  This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 *  If a copy of the MPL was not distributed with this file,
 *  You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.common.models.implementation;

import be.hyperrail.opentransportdata.common.models.VehicleComposition;
import be.hyperrail.opentransportdata.common.models.VehicleCompositionUnit;

public class VehicleCompositionImpl implements VehicleComposition {
    private VehicleCompositionUnit[] vehicleCompositionUnits;
    private boolean isConfirmed;

    public VehicleCompositionImpl(VehicleCompositionUnit[] vehicleCompositionUnits, boolean isConfirmed) {
        this.vehicleCompositionUnits = vehicleCompositionUnits;
        this.isConfirmed = isConfirmed;
    }

    @Override
    public VehicleCompositionUnit[] getVehicleCompositionUnits() {
        return vehicleCompositionUnits;
    }

    @Override
    public boolean isConfirmed() {
        return isConfirmed;
    }
}
