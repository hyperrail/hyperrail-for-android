/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.irail.implementation;

import org.joda.time.DateTime;

import be.hyperrail.android.irail.contracts.IrailDataProvider;
import be.hyperrail.android.irail.contracts.IrailResponseListener;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.util.ArrayUtils;

public class RouteAppendHelper implements IrailResponseListener<RouteResult> {

    private final static int APPEND_ROUTE = 0;

    private int attempt = 0;
    private DateTime lastSearchTime;
    private RouteResult originalRouteResult;
    private IrailResponseListener<RouteResult> callback;
    private int tag;
    IrailDataProvider api = IrailFactory.getDataProviderInstance();

    public void appendRouteResult(final IrailResponseListener<RouteResult> callback, final int tag, final RouteResult routes) {
        this.callback = callback;
        this.tag = tag;
        this.originalRouteResult = routes;

        if (routes.getRoutes().length > 0) {
            lastSearchTime = routes.getRoutes()[routes.getRoutes().length - 1].getDepartureTime().plusMinutes(1);
        } else {
            lastSearchTime = routes.getSearchTime().plusHours(1);
        }
        api.getRoute(this, APPEND_ROUTE, routes.getOrigin(), routes.getDestination(), lastSearchTime);
    }

    @Override
    public void onIrailSuccessResponse(RouteResult data, int tag) {
        switch (tag) {
            case APPEND_ROUTE:
                if (data.getRoutes().length > 0) {
                    Route[] mergedRoutes = ArrayUtils.concatenate(originalRouteResult.getRoutes(), data.getRoutes());
                    RouteResult merged = new RouteResult(originalRouteResult.getOrigin(), originalRouteResult.getDestination(), originalRouteResult.getSearchTime(), originalRouteResult.getTimeDefinition(), mergedRoutes);
                    callback.onIrailSuccessResponse(merged, tag);
                } else {
                    attempt++;
                    lastSearchTime = lastSearchTime.plusHours(2);
                    if (attempt < 12) {
                        api.getRoute(this, APPEND_ROUTE, originalRouteResult.getOrigin(), originalRouteResult.getDestination(), lastSearchTime);
                    } else {
                        this.callback.onIrailSuccessResponse(originalRouteResult, this.tag);
                    }
                }
        }
    }

    @Override
    public void onIrailErrorResponse(Exception e, int tag) {
        switch (tag) {
            case APPEND_ROUTE:
                callback.onIrailErrorResponse(e, this.tag);
                break;
        }
    }
}
