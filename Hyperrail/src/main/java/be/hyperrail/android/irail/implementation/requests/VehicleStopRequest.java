/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.irail.implementation.requests;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import be.hyperrail.android.irail.contracts.IrailRequest;
import be.hyperrail.android.irail.implementation.VehicleStop;

/**
 * Request to update a certain vehiclestop
 */

public class VehicleStopRequest extends IrailBaseRequest<VehicleStop> implements IrailRequest<VehicleStop> {

    private final VehicleStop mStop;

    /**
     * Create a request to update a certain vehiclestop
     *
     * @param stop
     */

    public VehicleStopRequest(VehicleStop stop) {
        mStop = stop;
    }

    public VehicleStopRequest(@NonNull JSONObject jsonObject) throws JSONException {
        super(jsonObject);
        throw new UnsupportedOperationException("VehicleStopRequests can't be serialized");
    }

    @NonNull
    @Override
    public JSONObject toJson() throws JSONException {
        throw new UnsupportedOperationException("VehicleStopRequests can't be serialized");
    }

    @Override
    public boolean equalsIgnoringTime(IrailRequest other) {
        return (other instanceof VehicleStopRequest) && mStop.equals(((VehicleStopRequest) other).getStop());
    }

    @Override
    public int compareTo(@NonNull IrailRequest o) {
        return 0;
    }

    public VehicleStop getStop() {
        return mStop;
    }
}
