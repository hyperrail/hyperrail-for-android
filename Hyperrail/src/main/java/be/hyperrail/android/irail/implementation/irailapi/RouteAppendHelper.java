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

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.irail.implementation.irailapi;

import android.support.annotation.NonNull;

import org.joda.time.DateTime;

import be.hyperrail.android.irail.contracts.IRailErrorResponseListener;
import be.hyperrail.android.irail.contracts.IRailSuccessResponseListener;
import be.hyperrail.android.irail.contracts.IrailDataProvider;
import be.hyperrail.android.irail.contracts.RouteTimeDefinition;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.irail.implementation.Route;
import be.hyperrail.android.irail.implementation.RouteResult;
import be.hyperrail.android.irail.implementation.requests.ExtendRoutesRequest;
import be.hyperrail.android.irail.implementation.requests.IrailRoutesRequest;
import be.hyperrail.android.util.ArrayUtils;

/**
 * A class which allows to append route results.
 */
public class RouteAppendHelper implements IRailSuccessResponseListener<RouteResult>, IRailErrorResponseListener {

    private final int TAG_APPEND = 0;
    private final int TAG_PREPEND = 1;

    private int attempt = 0;
    private DateTime lastSearchTime;
    private RouteResult originalRouteResult;

    IrailDataProvider api = IrailFactory.getDataProviderInstance();
    private ExtendRoutesRequest mExtendRoutesRequest;

    public void extendRoutesRequest(@NonNull ExtendRoutesRequest extendRoutesRequest) {
        switch (extendRoutesRequest.getAction()) {
            default:
            case APPEND:
                appendRouteResult(extendRoutesRequest);
                break;
            case PREPEND:
                prependRouteResult(extendRoutesRequest);
                break;
        }
    }

    private void appendRouteResult(@NonNull ExtendRoutesRequest extendRoutesRequest) {
        mExtendRoutesRequest = extendRoutesRequest;

        this.originalRouteResult = extendRoutesRequest.getRoutes();

        if (originalRouteResult.getRoutes().length > 0) {
            lastSearchTime = originalRouteResult.getRoutes()[originalRouteResult.getRoutes().length - 1].getDepartureTime().plusMinutes(1);
        } else {
            lastSearchTime = originalRouteResult.getSearchTime().plusHours(1);
        }
        IrailRoutesRequest request = new IrailRoutesRequest(originalRouteResult.getOrigin(), originalRouteResult.getDestination(), RouteTimeDefinition.DEPART, lastSearchTime);
        request.setCallback(this, this, TAG_APPEND);
        api.getRoutes(request);
    }

    private void prependRouteResult(@NonNull ExtendRoutesRequest extendRoutesRequest) {
        mExtendRoutesRequest = extendRoutesRequest;

        this.originalRouteResult = extendRoutesRequest.getRoutes();

        if (originalRouteResult.getRoutes().length > 0) {
            lastSearchTime = originalRouteResult.getRoutes()[0].getArrivalTime().minusMinutes(1);
        } else {
            lastSearchTime = originalRouteResult.getSearchTime();
        }

        IrailRoutesRequest request = new IrailRoutesRequest(originalRouteResult.getOrigin(), originalRouteResult.getDestination(), RouteTimeDefinition.ARRIVE, lastSearchTime);
        request.setCallback(this, this, TAG_PREPEND);
        api.getRoutes(request);
    }

    @Override
    public void onSuccessResponse(@NonNull RouteResult data, Object tag) {
        switch ((int) tag) {
            case TAG_APPEND:
                handleAppendSuccessResponse(data);
                break;
            case TAG_PREPEND:
                handlePrependSuccessResponse(data);
                break;
        }
    }

    /**
     * Handle a successful response when prepending a route result
     *
     * @param data The newly received data
     */
    private void handlePrependSuccessResponse(@NonNull RouteResult data) {
        if (data.getRoutes().length > 0) {
            Route[] mergedRoutes = ArrayUtils.concatenate(data.getRoutes(), originalRouteResult.getRoutes());
            RouteResult merged = new RouteResult(originalRouteResult.getOrigin(), originalRouteResult.getDestination(), originalRouteResult.getSearchTime(), originalRouteResult.getTimeDefinition(), mergedRoutes);
            mExtendRoutesRequest.notifySuccessListeners(merged);
        } else {
            attempt++;
            lastSearchTime = lastSearchTime.minusHours(2);
            if (attempt < 12) {
                IrailRoutesRequest request = new IrailRoutesRequest(originalRouteResult.getOrigin(), originalRouteResult.getDestination(), RouteTimeDefinition.ARRIVE, lastSearchTime);
                request.setCallback(this, this, TAG_PREPEND);
                api.getRoutes(request);
            } else {
                mExtendRoutesRequest.notifySuccessListeners(originalRouteResult);
            }
        }
    }

    /**
     * Handle a successful response when appending a route result
     *
     * @param data The newly received data
     */
    private void handleAppendSuccessResponse(@NonNull RouteResult data) {
        if (data.getRoutes().length > 0) {
            Route[] mergedRoutes = ArrayUtils.concatenate(originalRouteResult.getRoutes(), data.getRoutes());
            RouteResult merged = new RouteResult(originalRouteResult.getOrigin(), originalRouteResult.getDestination(), originalRouteResult.getSearchTime(), originalRouteResult.getTimeDefinition(), mergedRoutes);
            mExtendRoutesRequest.notifySuccessListeners(merged);
        } else {
            attempt++;
            lastSearchTime = lastSearchTime.plusHours(2);
            if (attempt < 12) {
                IrailRoutesRequest request = new IrailRoutesRequest(originalRouteResult.getOrigin(), originalRouteResult.getDestination(), RouteTimeDefinition.DEPART, lastSearchTime);
                request.setCallback(this, this, TAG_APPEND);
                api.getRoutes(request);
            } else {
                mExtendRoutesRequest.notifySuccessListeners(originalRouteResult);
            }
        }
    }

    @Override
    public void onErrorResponse(@NonNull Exception e, Object tag) {
        mExtendRoutesRequest.notifyErrorListeners(e);
    }
}
