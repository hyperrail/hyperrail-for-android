/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package eu.opentransport.linkedconnections;

import android.support.annotation.NonNull;

import eu.opentransport.common.contracts.MeteredDataSource;
import eu.opentransport.common.contracts.PagedDataResourceDescriptor;
import eu.opentransport.common.contracts.TransportDataErrorResponseListener;
import eu.opentransport.common.contracts.TransportDataSuccessResponseListener;
import eu.opentransport.common.contracts.TransportStopsDataSource;
import eu.opentransport.common.requests.ExtendLiveboardRequest;
import eu.opentransport.common.requests.IrailLiveboardRequest;
import eu.opentransport.irail.IrailLiveboard;

/**
 * Created in be.hyperrail.android.irail.implementation.linkedconnections on 17/04/2018.
 */
public class LiveboardExtendHelper implements TransportDataSuccessResponseListener<IrailLiveboard>, TransportDataErrorResponseListener {

    private final LinkedConnectionsProvider mLinkedConnectionsProvider;
    private final TransportStopsDataSource mStationProvider;
    private final ExtendLiveboardRequest mRequest;
    private final MeteredDataSource.MeteredRequest mMeteredRequest;
    private IrailLiveboard mLiveboard;

    public LiveboardExtendHelper(LinkedConnectionsProvider linkedConnectionsProvider, TransportStopsDataSource stationProvider, ExtendLiveboardRequest request, MeteredDataSource.MeteredRequest meteredRequest) {
        mLinkedConnectionsProvider = linkedConnectionsProvider;
        mStationProvider = stationProvider;
        mRequest = request;
        mMeteredRequest = meteredRequest;
    }

    public void extend() {
        extend(mRequest.getLiveboard());
    }

    private void extend(IrailLiveboard liveboard) {
        mLiveboard = liveboard;
        String url;

        if (mRequest.getAction() == ExtendLiveboardRequest.Action.PREPEND) {
            url = (String) mLiveboard.getPagedResourceDescriptor().getPreviousPointer();
        } else {
            url = (String) mLiveboard.getPagedResourceDescriptor().getNextPointer();
        }

        final IrailLiveboardRequest liveboardRequest = new IrailLiveboardRequest(mLiveboard,
                                                                                 mLiveboard.getTimeDefinition(),
                                                                                 mLiveboard.getLiveboardType(),
                                                                                 mLiveboard.getSearchTime());

        liveboardRequest.setCallback(this, this, mMeteredRequest);
        LiveboardResponseListener listener = new LiveboardResponseListener(mLinkedConnectionsProvider, mStationProvider, liveboardRequest);

        mLinkedConnectionsProvider.getLinkedConnectionsByUrl(url,
                                                             listener,
                                                             listener,
                                                             mMeteredRequest);

    }

    @Override
    public void onSuccessResponse(@NonNull IrailLiveboard data, Object tag) {
        int originalLength = mLiveboard.getStops().length;
        mLiveboard = mLiveboard.withStopsAppended(data);

        String previous = (String) mRequest.getLiveboard().getPagedResourceDescriptor().getPreviousPointer();
        String current = (String) mRequest.getLiveboard().getPagedResourceDescriptor().getPreviousPointer();
        String next = (String) mRequest.getLiveboard().getPagedResourceDescriptor().getPreviousPointer();

        if (mRequest.getAction() == ExtendLiveboardRequest.Action.APPEND) {
            next = (String) data.getPagedResourceDescriptor().getNextPointer();
        } else {
            previous = (String) data.getPagedResourceDescriptor().getPreviousPointer();
            current = (String) data.getPagedResourceDescriptor().getCurrentPointer();
        }
        mLiveboard.setPageInfo(new PagedDataResourceDescriptor(previous, current, next));

        if (mLiveboard.getStops().length == originalLength) {
            // Didn't find anything new
            extend(mLiveboard);
        } else {
            mRequest.notifySuccessListeners(mLiveboard);
        }
    }

    @Override
    public void onErrorResponse(@NonNull Exception e, Object tag) {
        mRequest.notifyErrorListeners(e);
    }
}
