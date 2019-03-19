/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.experimental.linkedconnections;

import android.support.annotation.Nullable;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import be.hyperrail.opentransportdata.common.contracts.TransportDataErrorResponseListener;
import be.hyperrail.opentransportdata.common.contracts.TransportDataSuccessResponseListener;

/**
 * Created in be.hyperrail.android.irail.implementation.linkedconnections on 18/04/2018.
 */
public class TimespanQueryResponseListener implements QueryResponseListener.LinkedConnectionsQuery {
    private final DateTime mEndTime;
    private final TransportDataSuccessResponseListener<LinkedConnections> mSuccessListener;
    private final TransportDataErrorResponseListener mErrorListener;
    private final Object mTag;
    public final static int DIRECTION_FORWARD = 1;
    public final static int DIRECTION_BACKWARD = -1;

    private final int mDirection;

    private List<LinkedConnection> result = new ArrayList<>();
    private String previous, current, next;

    TimespanQueryResponseListener(final DateTime endTime, int direction, @Nullable final TransportDataSuccessResponseListener<LinkedConnections> successListener, @Nullable final TransportDataErrorResponseListener errorListener, @Nullable Object tag) {
        mEndTime = endTime;
        mDirection = direction;
        mSuccessListener = successListener;
        mErrorListener = errorListener;
        mTag = tag;
    }

    @Override
    public int onQueryResult(LinkedConnections data) {
        if (current == null) {
            if (mDirection == DIRECTION_FORWARD) {
                previous = data.previous;
            } else {
                next = data.next;
            }
            current = data.current;
        }
        if (mDirection == DIRECTION_BACKWARD) {
            previous = data.previous;
            current = data.current;
        } else {
            next = data.next;
        }
        Collections.addAll(result, data.connections);

        if ((mDirection > 0 && data.connections[data.connections.length - 1].getDepartureTime().isBefore(mEndTime)) ||
                (mDirection < 0 && data.connections[0].getDepartureTime().isAfter(mEndTime))) {
            return mDirection;
        } else {
            LinkedConnections resultObject = new LinkedConnections();
            LinkedConnection[] connections = new LinkedConnection[result.size()];
            connections = result.toArray(connections);
            resultObject.connections = connections;
            resultObject.current = current;
            resultObject.previous = previous;
            resultObject.next = next;
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
