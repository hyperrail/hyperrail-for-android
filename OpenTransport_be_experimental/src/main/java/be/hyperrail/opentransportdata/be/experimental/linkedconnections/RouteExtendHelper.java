/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.be.experimental.linkedconnections;

import androidx.annotation.NonNull;

import org.joda.time.DateTime;

import java.io.FileNotFoundException;

import be.hyperrail.opentransportdata.common.contracts.MeteredDataSource;
import be.hyperrail.opentransportdata.common.contracts.QueryTimeDefinition;
import be.hyperrail.opentransportdata.common.contracts.TransportDataErrorResponseListener;
import be.hyperrail.opentransportdata.common.contracts.TransportDataSuccessResponseListener;
import be.hyperrail.opentransportdata.common.contracts.TransportStopsDataSource;
import be.hyperrail.opentransportdata.common.models.RoutesList;
import be.hyperrail.opentransportdata.common.requests.ExtendRoutePlanningRequest;
import be.hyperrail.opentransportdata.common.requests.ResultExtensionType;
import be.hyperrail.opentransportdata.common.requests.RoutePlanningRequest;

/**
 * Created in be.hyperrail.android.irail.implementation.linkedconnections on 17/04/2018.
 */
public class RouteExtendHelper implements TransportDataSuccessResponseListener<RoutesList>, TransportDataErrorResponseListener {

    private final LinkedConnectionsProvider mLinkedConnectionsProvider;
    private final TransportStopsDataSource mStationProvider;
    private final ExtendRoutePlanningRequest mRequest;
    private final MeteredDataSource.MeteredRequest mMeteredRequest;
    private LinkedConnectionsRoutesList mRoutes;
    private int attempts = 0;

    RouteExtendHelper(LinkedConnectionsProvider linkedConnectionsProvider, TransportStopsDataSource stationProvider, ExtendRoutePlanningRequest request, MeteredDataSource.MeteredRequest meteredRequest) {
        mLinkedConnectionsProvider = linkedConnectionsProvider;
        mStationProvider = stationProvider;
        mRequest = request;
        mMeteredRequest = meteredRequest;
    }

    void extend() {
        if (!(mRequest.getRoutes() instanceof LinkedConnectionsRoutesList)) {
            throw new IllegalArgumentException("Routeslist should be of type LinkedConnectionsRoutesList!");
        }
        extend((LinkedConnectionsRoutesList) mRequest.getRoutes());
    }

    private void extend(LinkedConnectionsRoutesList routes) {
        attempts++;

        if (attempts > 12) {
            mRequest.notifyErrorListeners(new FileNotFoundException());
            return;
        }

        mRoutes = routes;
        String start;
        DateTime departureLimit;
        if (mRequest.getRoutes().getTimeDefinition() == QueryTimeDefinition.EQUAL_OR_LATER) {
            departureLimit = mRequest.getRoutes().getSearchTime();
            if (mRequest.getRoutes().getRoutes().length > 0) {
                departureLimit = mRequest.getRoutes().getRoutes()[mRequest.getRoutes().getRoutes().length - 1].getDepartureTime();
            }
            if (mRequest.getAction() == ResultExtensionType.PREPEND) {
                start = (String) mRequest.getRoutes().getCurrentResultsPointer().getPointer();
            } else {
                start = (String) mRequest.getRoutes().getNextResultsPointer().getPointer();
            }
        } else {
            departureLimit = null;
            if (mRequest.getAction() == ResultExtensionType.PREPEND) {
                start = (String) mRequest.getRoutes().getPreviousResultsPointer().getPointer();
            } else {
                start = (String) mRequest.getRoutes().getNextResultsPointer().getPointer();
            }
        }


        final RoutePlanningRequest routesRequest = new RoutePlanningRequest(mRoutes.getOrigin(),
                mRoutes.getDestination(),
                mRoutes.getTimeDefinition(),
                mRoutes.getSearchTime());

        routesRequest.setCallback(this, this, mMeteredRequest);

        RouteResponseListener listener;
        if (mRequest.getAction() == ResultExtensionType.PREPEND) {
            listener = new RouteResponseListener(mLinkedConnectionsProvider, mStationProvider, routesRequest, null);
        } else {
            listener = new RouteResponseListener(mLinkedConnectionsProvider, mStationProvider, routesRequest, departureLimit, 12 * 60);
        }

        if (mRequest.getRoutes().getTimeDefinition() == QueryTimeDefinition.EQUAL_OR_LATER) {
            DateTime limit;
            if (mRoutes.getRoutes() != null && mRoutes.getRoutes().length > 0) {
                limit = mRoutes.getRoutes()[mRoutes.getRoutes().length - 1].getDepartureTime();
            } else {
                limit = mRoutes.getSearchTime();
            }
            mLinkedConnectionsProvider.getLinkedConnectionsByUrlForTimeSpanBackwards(start, limit,
                    listener,
                    listener,
                    mMeteredRequest);
        } else {
            mLinkedConnectionsProvider.getLinkedConnectionsByUrl(start,
                    listener,
                    listener,
                    mMeteredRequest);
        }
    }

    @Override
    public void onSuccessResponse(@NonNull RoutesList data, Object tag) {
        if (!(data instanceof LinkedConnectionsRoutesList)) {
            throw new IllegalArgumentException("data should be of type LinkedConnectionsRoutesList!");
        }
        LinkedConnectionsRoutesList newData = (LinkedConnectionsRoutesList) data;
        LinkedConnectionsRoutesList originalWithAppended = mRoutes.withRoutesAppended(newData);

        originalWithAppended.setPageInfo(
                newData.getPreviousResultsPointer(),
                newData.getCurrentResultsPointer(),
                newData.getNextResultsPointer()
        );
        if (mRequest.getAction() == ResultExtensionType.APPEND) {
            if (originalWithAppended.getRoutes().length > mRoutes.getRoutes().length) {
                mRequest.notifySuccessListeners(originalWithAppended);
            } else {
                ((LinkedConnectionsRoutesList) mRequest.getRoutes()).setPageInfo(
                        mRequest.getRoutes().getPreviousResultsPointer(),
                        mRequest.getRoutes().getCurrentResultsPointer(),
                        data.getNextResultsPointer()
                );
                extend();
            }
        } else {
            if (originalWithAppended.getRoutes().length > mRoutes.getRoutes().length) {
                mRequest.notifySuccessListeners(originalWithAppended);
            } else {
                ((LinkedConnectionsRoutesList) mRequest.getRoutes()).setPageInfo(
                        data.getPreviousResultsPointer(),
                        data.getCurrentResultsPointer(),
                        mRequest.getRoutes().getNextResultsPointer()
                );
                extend();
            }
        }
    }

    @Override
    public void onErrorResponse(@NonNull Exception e, Object tag) {
        mRequest.notifyErrorListeners(e);
    }
}
