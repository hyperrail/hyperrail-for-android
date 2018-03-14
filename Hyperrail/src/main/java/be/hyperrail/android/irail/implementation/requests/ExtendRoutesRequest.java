/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.irail.implementation.requests;

import android.support.annotation.NonNull;

import be.hyperrail.android.irail.contracts.IrailRequest;
import be.hyperrail.android.irail.implementation.RouteResult;

/**
 * Request to append or prepend a routes result
 */

public class ExtendRoutesRequest extends IrailBaseRequest<RouteResult> implements IrailRequest<RouteResult> {

    @NonNull
    private final RouteResult routes;

    @NonNull
    private final Action mAction;

    public ExtendRoutesRequest(@NonNull RouteResult routes, @NonNull Action action) {
        this.routes = routes;
        mAction = action;
    }

    @Override
    public boolean equalsIgnoringTime(IrailRequest other) {
        return (other instanceof ExtendRoutesRequest) && routes.equals(((ExtendRoutesRequest) other).getRoutes());
    }

    @Override
    public int compareTo(@NonNull IrailRequest o) {
        return 0;
    }

    @NonNull
    public RouteResult getRoutes() {
        return routes;
    }

    @NonNull
    public Action getAction() {
        return mAction;
    }

    public enum Action {
        APPEND,
        PREPEND
    }
}
