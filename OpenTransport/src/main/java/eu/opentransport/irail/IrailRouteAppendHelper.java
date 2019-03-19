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

package eu.opentransport.irail;

import org.joda.time.DateTime;

import eu.opentransport.OpenTransportApi;
import eu.opentransport.common.contracts.QueryTimeDefinition;
import eu.opentransport.common.contracts.TransportDataErrorResponseListener;
import eu.opentransport.common.contracts.TransportDataSource;
import eu.opentransport.common.contracts.TransportDataSuccessResponseListener;
import eu.opentransport.common.models.Route;
import eu.opentransport.common.models.RoutesList;
import eu.opentransport.common.requests.ExtendRoutePlanningRequest;
import eu.opentransport.common.requests.RoutePlanningRequest;
import eu.opentransport.util.ArrayUtils;

/**
 * A class which allows to withStopsAppended route results.
 */
public class IrailRouteAppendHelper implements TransportDataSuccessResponseListener<RoutesList>, TransportDataErrorResponseListener {

    private final int TAG_APPEND = 0;
    private final int TAG_PREPEND = 1;

    private int attempt = 0;
    private DateTime lastSearchTime;
    private RoutesList originalRouteResult;

    TransportDataSource api = OpenTransportApi.getDataProviderInstance();
    private ExtendRoutePlanningRequest mExtendRoutePlanningRequest;

    public void extendRoutesRequest(ExtendRoutePlanningRequest extendRoutePlanningRequest) {
        switch (extendRoutePlanningRequest.getAction()) {
            default:
            case APPEND:
                appendRouteResult(extendRoutePlanningRequest);
                break;
            case PREPEND:
                prependRouteResult(extendRoutePlanningRequest);
                break;
        }
    }

    private void appendRouteResult(ExtendRoutePlanningRequest extendRoutePlanningRequest) {
        mExtendRoutePlanningRequest = extendRoutePlanningRequest;

        this.originalRouteResult = extendRoutePlanningRequest.getRoutes();

        if (originalRouteResult.getRoutes().length > 0) {
            lastSearchTime = originalRouteResult.getRoutes()[originalRouteResult.getRoutes().length - 1].getDepartureTime().plusMinutes(1);
        } else {
            lastSearchTime = originalRouteResult.getSearchTime().plusHours(1);
        }
        RoutePlanningRequest request = new RoutePlanningRequest(originalRouteResult.getOrigin(), originalRouteResult.getDestination(), QueryTimeDefinition.DEPART_AT, lastSearchTime);
        request.setCallback(this, this, TAG_APPEND);
        api.getRoutePlanning(request);
    }

    private void prependRouteResult(ExtendRoutePlanningRequest extendRoutePlanningRequest) {
        mExtendRoutePlanningRequest = extendRoutePlanningRequest;

        this.originalRouteResult = extendRoutePlanningRequest.getRoutes();

        if (originalRouteResult.getRoutes().length > 0) {
            lastSearchTime = originalRouteResult.getRoutes()[0].getArrivalTime().minusMinutes(1);
        } else {
            lastSearchTime = originalRouteResult.getSearchTime();
        }

        RoutePlanningRequest request = new RoutePlanningRequest(originalRouteResult.getOrigin(), originalRouteResult.getDestination(), QueryTimeDefinition.ARRIVE_AT, lastSearchTime);
        request.setCallback(this, this, TAG_PREPEND);
        api.getRoutePlanning(request);
    }

    @Override
    public void onSuccessResponse(RoutesList data, Object tag) {
        switch ((int) tag) {
            case TAG_APPEND:
                handleAppendSuccessResponse((IrailRoutesList) data);
                break;
            case TAG_PREPEND:
                handlePrependSuccessResponse((IrailRoutesList) data);
                break;
        }
    }

    /**
     * Handle a successful response when prepending a route result
     *
     * @param data The newly received data
     */
    private void handlePrependSuccessResponse(IrailRoutesList data) {
        if (data.getRoutes().length > 0) {
            Route[] mergedRoutes = ArrayUtils.concatenate(data.getRoutes(), originalRouteResult.getRoutes());
            IrailRoutesList merged = new IrailRoutesList(originalRouteResult.getOrigin(), originalRouteResult.getDestination(), originalRouteResult.getSearchTime(), originalRouteResult.getTimeDefinition(), mergedRoutes);
            mExtendRoutePlanningRequest.notifySuccessListeners(merged);
        } else {
            attempt++;
            lastSearchTime = lastSearchTime.minusHours(2);
            if (attempt < 12) {
                RoutePlanningRequest request = new RoutePlanningRequest(originalRouteResult.getOrigin(), originalRouteResult.getDestination(), QueryTimeDefinition.ARRIVE_AT, lastSearchTime);
                request.setCallback(this, this, TAG_PREPEND);
                api.getRoutePlanning(request);
            } else {
                mExtendRoutePlanningRequest.notifySuccessListeners(originalRouteResult);
            }
        }
    }

    /**
     * Handle a successful response when appending a route result
     *
     * @param data The newly received data
     */
    private void handleAppendSuccessResponse(IrailRoutesList data) {
        if (data.getRoutes().length > 0) {
            Route[] mergedRoutes = ArrayUtils.concatenate(originalRouteResult.getRoutes(), data.getRoutes());
            IrailRoutesList merged = new IrailRoutesList(originalRouteResult.getOrigin(), originalRouteResult.getDestination(), originalRouteResult.getSearchTime(), originalRouteResult.getTimeDefinition(), mergedRoutes);
            mExtendRoutePlanningRequest.notifySuccessListeners(merged);
        } else {
            attempt++;
            lastSearchTime = lastSearchTime.plusHours(2);
            if (attempt < 12) {
                RoutePlanningRequest request = new RoutePlanningRequest(originalRouteResult.getOrigin(), originalRouteResult.getDestination(), QueryTimeDefinition.DEPART_AT, lastSearchTime);
                request.setCallback(this, this, TAG_APPEND);
                api.getRoutePlanning(request);
            } else {
                mExtendRoutePlanningRequest.notifySuccessListeners(originalRouteResult);
            }
        }
    }

    @Override
    public void onErrorResponse(Exception e, Object tag) {
        mExtendRoutePlanningRequest.notifyErrorListeners(e);
    }
}
