/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package eu.opentransport.common.requests;

import org.json.JSONException;
import org.json.JSONObject;

import eu.opentransport.common.contracts.TransportDataRequest;
import eu.opentransport.common.models.Disturbance;


/**
 * A request for disturbance information
 */
public class IrailDisturbanceRequest extends IrailBaseRequest<Disturbance[]> implements TransportDataRequest<Disturbance[]> {

    public IrailDisturbanceRequest(){
    }

    public IrailDisturbanceRequest( JSONObject json) throws JSONException {
        super(json);
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof IrailDisturbanceRequest);
    }

    @Override
    public int compareTo( TransportDataRequest o) {
        if (! (o instanceof  IrailDisturbanceRequest)){
            return -1;
        }

        return 0;
    }

    @Override
    public boolean equalsIgnoringTime(TransportDataRequest other) {
        return other instanceof IrailDisturbanceRequest;
    }
}
