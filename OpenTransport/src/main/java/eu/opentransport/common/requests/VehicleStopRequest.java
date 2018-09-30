/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package eu.opentransport.common.requests;

import org.json.JSONException;
import org.json.JSONObject;

import eu.opentransport.common.contracts.TransportDataRequest;
import eu.opentransport.common.models.VehicleStop;
import eu.opentransport.irail.IrailVehicleStop;

/**
 * Request to update a certain vehiclestop
 */

public class VehicleStopRequest extends IrailBaseRequest<IrailVehicleStop> implements TransportDataRequest<IrailVehicleStop> {

    private final VehicleStop mStop;

    /**
     * Create a request to update a certain vehiclestop
     *
     * @param stop the stop for which information should be obtained
     */
    public VehicleStopRequest( IrailVehicleStop stop) {
        mStop = stop;
    }

    public VehicleStopRequest( JSONObject jsonObject) throws JSONException {
        super(jsonObject);
        throw new UnsupportedOperationException("VehicleStopRequests can't be serialized");
    }


    @Override
    public JSONObject toJson() {
        throw new UnsupportedOperationException("VehicleStopRequests can't be serialized");
    }

    @Override
    public boolean equalsIgnoringTime(TransportDataRequest other) {
        return (other instanceof VehicleStopRequest) && mStop.equals(((VehicleStopRequest) other).getStop());
    }

    @Override
    public int compareTo( TransportDataRequest o) {
        return 0;
    }

    public VehicleStop getStop() {
        return mStop;
    }
}
