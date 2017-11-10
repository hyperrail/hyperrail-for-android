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

import be.hyperrail.android.irail.contracts.IRailErrorResponseListener;
import be.hyperrail.android.irail.contracts.IRailSuccessResponseListener;
import be.hyperrail.android.irail.contracts.IrailDataProvider;
import be.hyperrail.android.irail.contracts.RouteTimeDefinition;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.util.ArrayUtils;

public class RouteAppendHelper implements IRailSuccessResponseListener<RouteResult>, IRailErrorResponseListener<RouteResult> {

    private final int TAG_APPEND = 0;
    private final int TAG_PREPEND = 1;

    private int attempt = 0;
    private DateTime lastSearchTime;
    private RouteResult originalRouteResult;

    private IRailSuccessResponseListener<RouteResult> successResponseListener;
    private IRailErrorResponseListener<RouteResult> errorResponseListener;

    IrailDataProvider api = IrailFactory.getDataProviderInstance();

    public void appendRouteResult(final RouteResult routes, final IRailSuccessResponseListener<RouteResult> successResponseListener,
                                  final IRailErrorResponseListener<RouteResult> errorResponseListener) {
        this.successResponseListener = successResponseListener;
        this.errorResponseListener = errorResponseListener;

        this.originalRouteResult = routes;

        if (routes.getRoutes().length > 0) {
            lastSearchTime = routes.getRoutes()[routes.getRoutes().length - 1].getDepartureTime().plusMinutes(1);
        } else {
            lastSearchTime = routes.getSearchTime().plusHours(1);
        }

        api.getRoute(routes.getOrigin(), routes.getDestination(), lastSearchTime, RouteTimeDefinition.DEPART, this, this, TAG_APPEND);
    }

    public void prependRouteResult(final RouteResult routes, final IRailSuccessResponseListener<RouteResult> successResponseListener,
                                   final IRailErrorResponseListener<RouteResult> errorResponseListener, final Object tag) {
        this.successResponseListener = successResponseListener;
        this.errorResponseListener = errorResponseListener;

        this.originalRouteResult = routes;

        if (routes.getRoutes().length > 0) {
            lastSearchTime = routes.getRoutes()[0].getDepartureTime();
        } else {
            lastSearchTime = routes.getSearchTime();
        }

        api.getRoute(routes.getOrigin(), routes.getDestination(), lastSearchTime, RouteTimeDefinition.ARRIVE, this, this, TAG_PREPEND);
    }

    @Override
    public void onSuccessResponse(RouteResult data, Object tag) {
        switch ((int) tag) {
            case TAG_APPEND:
                if (data.getRoutes().length > 0) {
                    Route[] mergedRoutes = ArrayUtils.concatenate(originalRouteResult.getRoutes(), data.getRoutes());
                    RouteResult merged = new RouteResult(originalRouteResult.getOrigin(), originalRouteResult.getDestination(), originalRouteResult.getSearchTime(), originalRouteResult.getTimeDefinition(), mergedRoutes);
                    successResponseListener.onSuccessResponse(merged, tag);
                } else {
                    attempt++;
                    lastSearchTime = lastSearchTime.plusHours(2);
                    if (attempt < 12) {
                        api.getRoute(originalRouteResult.getOrigin(), originalRouteResult.getDestination(), lastSearchTime, RouteTimeDefinition.DEPART, this, this, tag);
                    } else {
                        this.successResponseListener.onSuccessResponse(originalRouteResult, this);
                    }
                }
                break;
            case TAG_PREPEND:
                if (data.getRoutes().length > 0) {
                    Route[] mergedRoutes = ArrayUtils.concatenate(data.getRoutes(),originalRouteResult.getRoutes());
                    RouteResult merged = new RouteResult(originalRouteResult.getOrigin(), originalRouteResult.getDestination(), originalRouteResult.getSearchTime(), originalRouteResult.getTimeDefinition(), mergedRoutes);
                    successResponseListener.onSuccessResponse(merged, tag);
                } else {
                    attempt++;
                    lastSearchTime = lastSearchTime.minusHours(2);
                    if (attempt < 12) {
                        api.getRoute(originalRouteResult.getOrigin(), originalRouteResult.getDestination(), lastSearchTime, RouteTimeDefinition.ARRIVE, this, this, tag);
                    } else {
                        this.successResponseListener.onSuccessResponse(originalRouteResult, this);
                    }
                }
                break;
        }
    }

    @Override
    public void onErrorResponse(Exception e, Object tag) {
        errorResponseListener.onErrorResponse(e, this);
    }
}
