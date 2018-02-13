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

import android.support.annotation.NonNull;

import org.joda.time.DateTime;

import be.hyperrail.android.irail.contracts.IRailErrorResponseListener;
import be.hyperrail.android.irail.contracts.IRailSuccessResponseListener;
import be.hyperrail.android.irail.contracts.IrailDataProvider;
import be.hyperrail.android.irail.contracts.RouteTimeDefinition;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.irail.implementation.requests.IrailRoutesRequest;
import be.hyperrail.android.util.ArrayUtils;

/**
 * A class which allows to append route results.
 * TODO: move to IrailApi API implementation as this is API specific
 */
public class RouteAppendHelper implements IRailSuccessResponseListener<RouteResult>, IRailErrorResponseListener {

    private final int TAG_APPEND = 0;
    private final int TAG_PREPEND = 1;

    private int attempt = 0;
    private DateTime lastSearchTime;
    private RouteResult originalRouteResult;

    private IRailSuccessResponseListener<RouteResult> successResponseListener;
    private IRailErrorResponseListener errorResponseListener;

    IrailDataProvider api = IrailFactory.getDataProviderInstance();

    public void appendRouteResult(@NonNull final RouteResult routes, final IRailSuccessResponseListener<RouteResult> successResponseListener,
                                  final IRailErrorResponseListener errorResponseListener) {
        this.successResponseListener = successResponseListener;
        this.errorResponseListener = errorResponseListener;

        this.originalRouteResult = routes;

        if (routes.getRoutes().length > 0) {
            lastSearchTime = routes.getRoutes()[routes.getRoutes().length - 1].getDepartureTime().plusMinutes(1);
        } else {
            lastSearchTime = routes.getSearchTime().plusHours(1);
        }
        IrailRoutesRequest request = new IrailRoutesRequest(routes.getOrigin(), routes.getDestination(), RouteTimeDefinition.DEPART, lastSearchTime);
        request.setCallback(this, this, TAG_APPEND);
        api.getRoutes(request);
    }

    public void prependRouteResult(@NonNull final RouteResult routes, final IRailSuccessResponseListener<RouteResult> successResponseListener,
                                   final IRailErrorResponseListener errorResponseListener) {
        this.successResponseListener = successResponseListener;
        this.errorResponseListener = errorResponseListener;

        this.originalRouteResult = routes;

        if (routes.getRoutes().length > 0) {
            lastSearchTime = routes.getRoutes()[0].getArrivalTime().minusMinutes(1);
        } else {
            lastSearchTime = routes.getSearchTime();
        }

        IrailRoutesRequest request = new IrailRoutesRequest(routes.getOrigin(), routes.getDestination(), RouteTimeDefinition.ARRIVE, lastSearchTime);
        request.setCallback(this, this, TAG_PREPEND);
        api.getRoutes(request);
    }

    @Override
    public void onSuccessResponse(@NonNull RouteResult data, Object tag) {
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
                        IrailRoutesRequest request = new IrailRoutesRequest(originalRouteResult.getOrigin(), originalRouteResult.getDestination(), RouteTimeDefinition.DEPART, lastSearchTime);
                        request.setCallback(this, this, TAG_APPEND);
                        api.getRoutes(request);
                    } else {
                        this.successResponseListener.onSuccessResponse(originalRouteResult, this);
                    }
                }
                break;
            case TAG_PREPEND:
                if (data.getRoutes().length > 0) {
                    Route[] mergedRoutes = ArrayUtils.concatenate(data.getRoutes(), originalRouteResult.getRoutes());
                    RouteResult merged = new RouteResult(originalRouteResult.getOrigin(), originalRouteResult.getDestination(), originalRouteResult.getSearchTime(), originalRouteResult.getTimeDefinition(), mergedRoutes);
                    successResponseListener.onSuccessResponse(merged, tag);
                } else {
                    attempt++;
                    lastSearchTime = lastSearchTime.minusHours(2);
                    if (attempt < 12) {
                        IrailRoutesRequest request = new IrailRoutesRequest(originalRouteResult.getOrigin(), originalRouteResult.getDestination(), RouteTimeDefinition.ARRIVE, lastSearchTime);
                        request.setCallback(this, this, TAG_PREPEND);
                        api.getRoutes(request);
                    } else {
                        this.successResponseListener.onSuccessResponse(originalRouteResult, this);
                    }
                }
                break;
        }
    }

    @Override
    public void onErrorResponse(@NonNull Exception e, Object tag) {
        errorResponseListener.onErrorResponse(e, this);
    }
}
