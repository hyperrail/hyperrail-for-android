/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.common.requests;

import org.json.JSONException;
import org.json.JSONObject;

import be.hyperrail.opentransportdata.common.contracts.TransportDataRequest;
import be.hyperrail.opentransportdata.common.models.Disturbance;


/**
 * A request for disturbance information
 */
public class ActualDisturbancesRequest extends OpenTransportBaseRequest<Disturbance[]> implements TransportDataRequest<Disturbance[]> {

    public ActualDisturbancesRequest() {
    }

    public ActualDisturbancesRequest(JSONObject json) throws JSONException {
        super(json);
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof ActualDisturbancesRequest);
    }

    @Override
    public int compareTo(TransportDataRequest o) {
        if (!(o instanceof ActualDisturbancesRequest)) {
            return -1;
        }

        return 0;
    }

    @Override
    public boolean equalsIgnoringTime(TransportDataRequest other) {
        return other instanceof ActualDisturbancesRequest;
    }

    @Override
    public int getRequestTypeTag() {
        return RequestType.DISTURBANCES.getRequestTypeTag();
    }
}
