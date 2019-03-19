/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.linkedconnections;

import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import be.hyperrail.opentransportdata.common.contracts.TransportDataErrorResponseListener;
import be.hyperrail.opentransportdata.common.contracts.TransportDataSuccessResponseListener;

/**
 * Created in be.hyperrail.android.irail.implementation.linkedconnections on 18/04/2018.
 */
public class UrlSpanQueryResponseListener implements QueryResponseListener.LinkedConnectionsQuery {
    private final String mLastUrl;
    private final TransportDataSuccessResponseListener<LinkedConnections> mSuccessListener;
    private final TransportDataErrorResponseListener mErrorListener;
    private final Object mTag;

    private List<LinkedConnection> result = new ArrayList<>();
    private String previous, current;

    UrlSpanQueryResponseListener(final String lastUrl,  @Nullable final TransportDataSuccessResponseListener<LinkedConnections> successListener, @Nullable final TransportDataErrorResponseListener errorListener, @Nullable Object tag) {
        mLastUrl = lastUrl;
        mSuccessListener = successListener;
        mErrorListener = errorListener;
        mTag = tag;
    }

    @Override
    public int onQueryResult(LinkedConnections data) {
        if (current == null) {
            previous = data.previous;
            current = data.current;
        }

        Collections.addAll(result, data.connections);

        if ( mLastUrl.compareTo(data.current) > 0) {
            return 1;
        } else {
            LinkedConnections resultObject = new LinkedConnections();
            LinkedConnection[] connections = new LinkedConnection[result.size()];
            connections = result.toArray(connections);
            resultObject.connections = connections;
            resultObject.current = current;
            resultObject.previous = previous;
            resultObject.next = data.next;
            if (mSuccessListener != null) {
                mSuccessListener.onSuccessResponse(resultObject, mTag);
            }
            return 0;
        }
    }

    @Override
    public void onQueryFailed(Exception e, Object tag) {
        if (mErrorListener != null) {
            mErrorListener.onErrorResponse(e, tag);
        }
    }
}
