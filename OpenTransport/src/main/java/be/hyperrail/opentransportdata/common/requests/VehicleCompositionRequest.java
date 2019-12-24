/*
 *  This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 *  If a copy of the MPL was not distributed with this file,
 *  You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.common.requests;

import be.hyperrail.opentransportdata.common.contracts.TransportDataRequest;
import be.hyperrail.opentransportdata.common.models.VehicleComposition;

/**
 * A request for train data
 */
public class VehicleCompositionRequest extends OpenTransportBaseRequest<VehicleComposition> implements TransportDataRequest<VehicleComposition> {


    private final String mVehicleId;

    /**
     * Create a request for train departures or arrivals in a given station
     *
     * @param vehicleId The train for which data should be retrieved
     */
    public VehicleCompositionRequest(String vehicleId) {
        this.mVehicleId = vehicleId;
    }

    public String getVehicleId() {
        return mVehicleId;
    }

    @Override
    public boolean equalsIgnoringTime(TransportDataRequest other) {
        return false;
    }

    @Override
    public int compareTo(TransportDataRequest o) {
        if (!(o instanceof VehicleCompositionRequest)) {
            return 1;
        }
        return mVehicleId.compareTo(((VehicleCompositionRequest) o).mVehicleId);
    }

    @Override
    public int getRequestTypeTag() {
        return RequestType.VEHICLECOMPOSITION.getRequestTypeTag();
    }
}
