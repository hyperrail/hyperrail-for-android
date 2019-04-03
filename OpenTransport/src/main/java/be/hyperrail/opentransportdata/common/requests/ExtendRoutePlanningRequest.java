/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.common.requests;

import be.hyperrail.opentransportdata.common.contracts.TransportDataRequest;
import be.hyperrail.opentransportdata.common.models.RoutesList;

/**
 * Request to withStopsAppended or prepend a routes result
 */

public class ExtendRoutePlanningRequest extends OpenTransportBaseRequest<RoutesList> implements TransportDataRequest<RoutesList> {

    private final RoutesList routes;

    private final ResultExtensionType mAction;

    public ExtendRoutePlanningRequest(RoutesList routes, ResultExtensionType action) {
        this.routes = routes;
        mAction = action;
    }

    @Override
    public boolean equalsIgnoringTime(TransportDataRequest other) {
        return (other instanceof ExtendRoutePlanningRequest) && routes.equals(((ExtendRoutePlanningRequest) other).getRoutes());
    }

    @Override
    public int compareTo(TransportDataRequest o) {
        return 0;
    }


    public RoutesList getRoutes() {
        return routes;
    }


    public ResultExtensionType getAction() {
        return mAction;
    }
}
