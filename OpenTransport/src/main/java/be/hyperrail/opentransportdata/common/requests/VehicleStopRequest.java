/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.common.requests;

import org.json.JSONException;
import org.json.JSONObject;

import be.hyperrail.opentransportdata.common.contracts.TransportDataRequest;
import be.hyperrail.opentransportdata.common.models.VehicleStop;
import be.hyperrail.opentransportdata.common.models.implementation.VehicleStopImpl;

/**
 * Request to update a certain vehiclestop
 */

public class VehicleStopRequest extends OpenTransportBaseRequest<VehicleStop> implements TransportDataRequest<VehicleStop> {

    private final VehicleStop mStop;

    /**
     * Create a request to update a certain vehiclestop
     *
     * @param stop the stop for which information should be obtained
     */
    public VehicleStopRequest(VehicleStopImpl stop) {
        mStop = stop;
    }

    public VehicleStopRequest(JSONObject jsonObject) throws JSONException {
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
    public int compareTo(TransportDataRequest o) {
        return 0;
    }

    public VehicleStop getStop() {
        return mStop;
    }

    @Override
    public int getRequestTypeTag() {
        // TODO: implement when needed
        throw new UnsupportedOperationException("Not supported for this type");
    }
}
