/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.common.requests;

import be.hyperrail.opentransportdata.common.contracts.TransportDataRequest;
import be.hyperrail.opentransportdata.common.models.Liveboard;

/**
 * Request to withStopsAppended or prepend a mLiveboard
 */

public class ExtendLiveboardRequest extends OpenTransportBaseRequest<Liveboard> implements TransportDataRequest<Liveboard> {


    private final Liveboard mLiveboard;

    private final ResultExtensionType mAction;

    public ExtendLiveboardRequest(Liveboard liveboard, ResultExtensionType action) {
        this.mLiveboard = liveboard;
        this.mAction = action;
    }

    @Override
    public boolean equalsIgnoringTime(TransportDataRequest other) {
        return (other instanceof ExtendLiveboardRequest) && mLiveboard.equals(((ExtendLiveboardRequest) other).getLiveboard());
    }

    @Override
    public int compareTo(TransportDataRequest o) {
        return 0;
    }


    public Liveboard getLiveboard() {
        return mLiveboard;
    }


    public ResultExtensionType getAction() {
        return mAction;
    }

}
