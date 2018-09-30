/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package eu.opentransport.linkedconnections;

import android.support.annotation.NonNull;

import org.joda.time.DateTime;

import java.io.FileNotFoundException;

import eu.opentransport.common.contracts.MeteredDataSource;
import eu.opentransport.common.contracts.PagedDataResourceDescriptor;
import eu.opentransport.common.contracts.QueryTimeDefinition;
import eu.opentransport.common.contracts.TransportDataErrorResponseListener;
import eu.opentransport.common.contracts.TransportDataSuccessResponseListener;
import eu.opentransport.common.contracts.TransportStopsDataSource;
import eu.opentransport.common.models.RouteResult;
import eu.opentransport.common.requests.ExtendRoutesRequest;
import eu.opentransport.common.requests.IrailRoutesRequest;

/**
 * Created in be.hyperrail.android.irail.implementation.linkedconnections on 17/04/2018.
 */
public class RouteExtendHelper implements TransportDataSuccessResponseListener<RouteResult>, TransportDataErrorResponseListener {

    private final LinkedConnectionsProvider mLinkedConnectionsProvider;
    private final TransportStopsDataSource mStationProvider;
    private final ExtendRoutesRequest mRequest;
    private final MeteredDataSource.MeteredRequest mMeteredRequest;
    private RouteResult mRoutes;
    int attempts = 0;

    public RouteExtendHelper(LinkedConnectionsProvider linkedConnectionsProvider, TransportStopsDataSource stationProvider, ExtendRoutesRequest request, MeteredDataSource.MeteredRequest meteredRequest) {
        mLinkedConnectionsProvider = linkedConnectionsProvider;
        mStationProvider = stationProvider;
        mRequest = request;
        mMeteredRequest = meteredRequest;
    }

    public void extend() {
        extend(mRequest.getRoutes());
    }

    private void extend(RouteResult routes) {
        attempts++;

        if (attempts > 12) {
            mRequest.notifyErrorListeners(new FileNotFoundException());
            return;
        }

        mRoutes = routes;
        String start, stop = null;
        DateTime departureLimit;
        if (mRequest.getRoutes().getTimeDefinition() == QueryTimeDefinition.DEPART_AT) {
            departureLimit = mRequest.getRoutes().getSearchTime();
            if (mRequest.getRoutes().getRoutes().length > 0) {
                departureLimit = mRequest.getRoutes().getRoutes()[mRequest.getRoutes().getRoutes().length - 1].getDepartureTime();
            }
            if (mRequest.getAction() == ExtendRoutesRequest.Action.PREPEND) {
                start = (String) mRequest.getRoutes().getPagedResourceDescriptor().getCurrentPointer();
            } else {
                start = (String) mRequest.getRoutes().getPagedResourceDescriptor().getNextPointer();
            }
        } else {
            departureLimit = null;
            if (mRequest.getAction() == ExtendRoutesRequest.Action.PREPEND) {
                start = (String) mRequest.getRoutes().getPagedResourceDescriptor().getPreviousPointer();
            } else {
                start = (String) mRequest.getRoutes().getPagedResourceDescriptor().getNextPointer();
            }
        }


        final IrailRoutesRequest routesRequest = new IrailRoutesRequest(mRoutes.getOrigin(),
                                                                        mRoutes.getDestination(),
                                                                        mRoutes.getTimeDefinition(),
                                                                        mRoutes.getSearchTime());

        routesRequest.setCallback(this, this, mMeteredRequest);

        RouteResponseListener listener;
        if (mRequest.getAction() == ExtendRoutesRequest.Action.PREPEND) {
            listener = new RouteResponseListener(mLinkedConnectionsProvider, mStationProvider, routesRequest, null);
        } else {
            listener = new RouteResponseListener(mLinkedConnectionsProvider, mStationProvider, routesRequest, departureLimit, 12 * 60);
        }

        if (mRequest.getRoutes().getTimeDefinition() == QueryTimeDefinition.DEPART_AT) {
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
    public void onSuccessResponse(@NonNull RouteResult data, Object tag) {
        RouteResult appended = mRoutes.withRoutesAppended(data);
        appended.setPageInfo(new PagedDataResourceDescriptor(
                data.getPagedResourceDescriptor().getPreviousPointer(),
                data.getPagedResourceDescriptor().getCurrentPointer(),
                data.getPagedResourceDescriptor().getNextPointer()
        ));
        if (mRequest.getAction() == ExtendRoutesRequest.Action.APPEND) {
            if (appended.getRoutes().length > mRoutes.getRoutes().length) {
                mRequest.notifySuccessListeners(appended);
            } else {
                mRequest.getRoutes().setPageInfo(new PagedDataResourceDescriptor(
                        mRequest.getRoutes().getPagedResourceDescriptor().getPreviousPointer(),
                        mRequest.getRoutes().getPagedResourceDescriptor().getCurrentPointer(),
                        data.getPagedResourceDescriptor().getNextPointer()
                ));
                extend();
            }
        } else {
            if (appended.getRoutes().length > mRoutes.getRoutes().length) {
                mRequest.notifySuccessListeners(appended);
            } else {
                mRequest.getRoutes().setPageInfo(new PagedDataResourceDescriptor(
                        data.getPagedResourceDescriptor().getPreviousPointer(),
                        data.getPagedResourceDescriptor().getCurrentPointer(),
                        mRequest.getRoutes().getPagedResourceDescriptor().getNextPointer()
                ));
                extend();
            }
        }
    }

    @Override
    public void onErrorResponse(@NonNull Exception e, Object tag) {
        mRequest.notifyErrorListeners(e);
    }
}
