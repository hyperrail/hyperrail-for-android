/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package eu.opentransport.linkedconnections;

import android.support.annotation.NonNull;

import eu.opentransport.common.contracts.MeteredDataSource;
import eu.opentransport.common.contracts.NextDataPointer;
import eu.opentransport.common.contracts.PagedDataResource;
import eu.opentransport.common.contracts.TransportDataErrorResponseListener;
import eu.opentransport.common.contracts.TransportDataSuccessResponseListener;
import eu.opentransport.common.contracts.TransportStopsDataSource;
import eu.opentransport.common.models.Liveboard;
import eu.opentransport.common.requests.ExtendLiveboardRequest;
import eu.opentransport.common.requests.IrailLiveboardRequest;
import eu.opentransport.irail.IrailLiveboard;

/**
 * Created in be.hyperrail.android.irail.implementation.linkedconnections on 17/04/2018.
 */
public class LiveboardExtendHelper implements TransportDataSuccessResponseListener<Liveboard>, TransportDataErrorResponseListener {

    private final LinkedConnectionsProvider mLinkedConnectionsProvider;
    private final TransportStopsDataSource mStationProvider;
    private final ExtendLiveboardRequest mRequest;
    private final MeteredDataSource.MeteredRequest mMeteredRequest;
    private IrailLiveboard mLiveboard;

    public LiveboardExtendHelper(LinkedConnectionsProvider linkedConnectionsProvider, TransportStopsDataSource stationProvider, ExtendLiveboardRequest request, MeteredDataSource.MeteredRequest meteredRequest) {
        mLinkedConnectionsProvider = linkedConnectionsProvider;
        mStationProvider = stationProvider;

        if (!(request.getLiveboard() instanceof IrailLiveboard)) {
            throw new IllegalArgumentException("Liveboard should be of type irailLiveboard!");
        }

        mRequest = request;
        mMeteredRequest = meteredRequest;
    }

    public void extend() {
        extend((IrailLiveboard) mRequest.getLiveboard());
    }

    private void extend(IrailLiveboard liveboard) {
        mLiveboard = liveboard;
        String url;

        if (mRequest.getAction() == ExtendLiveboardRequest.Action.PREPEND) {
            url = (String) mLiveboard.getPreviousResultsPointer().getPointer();
        } else {
            url = (String) mLiveboard.getNextResultsPointer().getPointer();
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
    public void onSuccessResponse(@NonNull Liveboard data, Object tag) {

        if (!(data instanceof IrailLiveboard)) {
            throw new IllegalArgumentException("Liveboard should be of type irailLiveboard!");
        }


        int originalLength = mLiveboard.getStops().length;
        mLiveboard = mLiveboard.withStopsAppended((IrailLiveboard) data);

        NextDataPointer previous =  mRequest.getLiveboard().getPreviousResultsPointer();
        NextDataPointer current =  mRequest.getLiveboard().getPreviousResultsPointer();
        NextDataPointer next =  mRequest.getLiveboard().getPreviousResultsPointer();

        if (mRequest.getAction() == ExtendLiveboardRequest.Action.APPEND) {
            next =  data.getNextResultsPointer();
        } else {
            previous =  data.getPreviousResultsPointer();
            current =  data.getCurrentResultsPointer();
        }
        mLiveboard.setPageInfo(previous, current, next);

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
