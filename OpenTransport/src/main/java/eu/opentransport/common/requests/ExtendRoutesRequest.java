/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package eu.opentransport.common.requests;

import eu.opentransport.common.contracts.TransportDataRequest;
import eu.opentransport.common.models.RouteResult;

/**
 * Request to withStopsAppended or prepend a routes result
 */

public class ExtendRoutesRequest extends IrailBaseRequest<RouteResult> implements TransportDataRequest<RouteResult> {


    private final RouteResult routes;


    private final Action mAction;

    public ExtendRoutesRequest( RouteResult routes,  Action action) {
        this.routes = routes;
        mAction = action;
    }

    @Override
    public boolean equalsIgnoringTime(TransportDataRequest other) {
        return (other instanceof ExtendRoutesRequest) && routes.equals(((ExtendRoutesRequest) other).getRoutes());
    }

    @Override
    public int compareTo( TransportDataRequest o) {
        return 0;
    }


    public RouteResult getRoutes() {
        return routes;
    }


    public Action getAction() {
        return mAction;
    }

    public enum Action {
        APPEND,
        PREPEND
    }
}
