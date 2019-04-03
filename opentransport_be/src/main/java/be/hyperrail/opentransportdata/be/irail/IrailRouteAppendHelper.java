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

package be.hyperrail.opentransportdata.be.irail;

import org.joda.time.DateTime;

import be.hyperrail.opentransportdata.OpenTransportApi;
import be.hyperrail.opentransportdata.common.contracts.QueryTimeDefinition;
import be.hyperrail.opentransportdata.common.contracts.TransportDataErrorResponseListener;
import be.hyperrail.opentransportdata.common.contracts.TransportDataSource;
import be.hyperrail.opentransportdata.common.contracts.TransportDataSuccessResponseListener;
import be.hyperrail.opentransportdata.common.models.Route;
import be.hyperrail.opentransportdata.common.models.RoutesList;
import be.hyperrail.opentransportdata.common.models.implementation.RoutesListImpl;
import be.hyperrail.opentransportdata.common.requests.ExtendRoutePlanningRequest;
import be.hyperrail.opentransportdata.common.requests.RoutePlanningRequest;
import be.hyperrail.opentransportdata.util.ArrayUtils;

/**
 * A class which allows to withStopsAppended route results.
 */
public class IrailRouteAppendHelper implements TransportDataSuccessResponseListener<RoutesList>, TransportDataErrorResponseListener {

    private final int TAG_APPEND = 0;
    private final int TAG_PREPEND = 1;

    private int attempt = 0;
    private DateTime lastSearchTime;
    private RoutesList originalRouteResult;

    private TransportDataSource api = OpenTransportApi.getDataProviderInstance();
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
        RoutePlanningRequest request = new RoutePlanningRequest(originalRouteResult.getOrigin(), originalRouteResult.getDestination(), QueryTimeDefinition.EQUAL_OR_LATER, lastSearchTime);
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

        RoutePlanningRequest request = new RoutePlanningRequest(originalRouteResult.getOrigin(), originalRouteResult.getDestination(), QueryTimeDefinition.EQUAL_OR_EARLIER, lastSearchTime);
        request.setCallback(this, this, TAG_PREPEND);
        api.getRoutePlanning(request);
    }

    @Override
    public void onSuccessResponse(RoutesList data, Object tag) {
        switch ((int) tag) {
            case TAG_APPEND:
                handleAppendSuccessResponse((RoutesListImpl) data);
                break;
            case TAG_PREPEND:
                handlePrependSuccessResponse((RoutesListImpl) data);
                break;
        }
    }

    /**
     * Handle a successful response when prepending a route result
     *
     * @param data The newly received data
     */
    private void handlePrependSuccessResponse(RoutesListImpl data) {
        if (data.getRoutes().length > 0) {
            Route[] mergedRoutes = ArrayUtils.concatenate(data.getRoutes(), originalRouteResult.getRoutes());
            RoutesListImpl merged = new RoutesListImpl(originalRouteResult.getOrigin(), originalRouteResult.getDestination(), originalRouteResult.getSearchTime(), originalRouteResult.getTimeDefinition(), mergedRoutes);
            mExtendRoutePlanningRequest.notifySuccessListeners(merged);
        } else {
            attempt++;
            lastSearchTime = lastSearchTime.minusHours(2);
            if (attempt < 12) {
                RoutePlanningRequest request = new RoutePlanningRequest(originalRouteResult.getOrigin(), originalRouteResult.getDestination(), QueryTimeDefinition.EQUAL_OR_EARLIER, lastSearchTime);
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
    private void handleAppendSuccessResponse(RoutesListImpl data) {
        if (data.getRoutes().length > 0) {
            Route[] mergedRoutes = ArrayUtils.concatenate(originalRouteResult.getRoutes(), data.getRoutes());
            RoutesListImpl merged = new RoutesListImpl(originalRouteResult.getOrigin(), originalRouteResult.getDestination(), originalRouteResult.getSearchTime(), originalRouteResult.getTimeDefinition(), mergedRoutes);
            mExtendRoutePlanningRequest.notifySuccessListeners(merged);
        } else {
            attempt++;
            lastSearchTime = lastSearchTime.plusHours(2);
            if (attempt < 12) {
                RoutePlanningRequest request = new RoutePlanningRequest(originalRouteResult.getOrigin(), originalRouteResult.getDestination(), QueryTimeDefinition.EQUAL_OR_LATER, lastSearchTime);
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
