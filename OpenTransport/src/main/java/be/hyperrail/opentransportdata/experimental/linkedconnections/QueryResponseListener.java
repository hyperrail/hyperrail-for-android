/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.experimental.linkedconnections;

import android.support.annotation.NonNull;

import be.hyperrail.opentransportdata.common.contracts.TransportDataErrorResponseListener;
import be.hyperrail.opentransportdata.common.contracts.TransportDataSuccessResponseListener;

/**
 * Created in be.hyperrail.android.irail.implementation.linkedconnections on 18/04/2018.
 */
public class QueryResponseListener implements TransportDataErrorResponseListener, TransportDataSuccessResponseListener<LinkedConnections> {
    private final LinkedConnectionsProvider mProvider;
    private final LinkedConnectionsQuery mQueryFunction;

    public QueryResponseListener(LinkedConnectionsProvider provider, LinkedConnectionsQuery query) {
        mProvider = provider;
        mQueryFunction = query;
    }

    @Override
    public void onSuccessResponse(@NonNull LinkedConnections data, Object tag) {
        int status = mQueryFunction.onQueryResult(data);
        if (status < 0) {
            mProvider.getLinkedConnectionsByUrl(data.previous, this, this, tag);
        }
        if (status > 0) {
            mProvider.getLinkedConnectionsByUrl(data.next, this, this, tag);
        }
    }

    @Override
    public void onErrorResponse(@NonNull Exception e, Object tag) {
        mQueryFunction.onQueryFailed(e, tag);
    }

    public interface LinkedConnectionsQuery {
        int onQueryResult(LinkedConnections data);

        void onQueryFailed(Exception e, Object tag);
    }

}
