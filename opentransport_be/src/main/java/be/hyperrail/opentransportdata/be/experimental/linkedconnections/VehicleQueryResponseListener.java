/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.be.experimental.linkedconnections;

import com.google.firebase.perf.metrics.AddTrace;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import be.hyperrail.opentransportdata.common.contracts.MeteredDataSource;
import be.hyperrail.opentransportdata.common.contracts.TransportDataErrorResponseListener;
import be.hyperrail.opentransportdata.common.contracts.TransportDataSuccessResponseListener;

/**
 * A query for linkedconnections related to a certain vehicle. Stop after the vehicle hasn't been seen for 2 hours, only return relevant connections.
 */
public class VehicleQueryResponseListener implements QueryResponseListener.LinkedConnectionsQuery {
    private final String mVehicleUri;
    private final TransportDataSuccessResponseListener<LinkedConnections> mSuccessListener;
    private final TransportDataErrorResponseListener mErrorListener;
    private final Object mTag;

    private List<LinkedConnection> result = new ArrayList<>();
    private String previous, current;

    private DateTime started;
    private DateTime lastSpotted;

    public VehicleQueryResponseListener(String vehicleUri, final TransportDataSuccessResponseListener<LinkedConnections> successListener, final TransportDataErrorResponseListener errorListener, Object tag) {
        mVehicleUri = vehicleUri;
        mSuccessListener = successListener;
        mErrorListener = errorListener;
        mTag = tag;
    }

    @Override
    @AddTrace(name="vehicleQuery.result")
    public int onQueryResult(LinkedConnections data) {
        if (current == null) {
            previous = data.previous;
            current = data.current;
        }

        if (data.connections.length < 1) {
            return 1;
        }

        if (started == null){
            started = data.connections[0].getDepartureTime();
        } else {
            if (new Duration(started,data.connections[0].getDepartureTime()).getStandardHours() > 48){
                mErrorListener.onErrorResponse(new IndexOutOfBoundsException("Requested page too far from original start"),mTag);
                return 0;
            }
        }

        DateTime lastDepartureTime = null;
        for (LinkedConnection connection : data.connections) {
            if (!connection.isNormal()){
                continue;
            }
            if (Objects.equals(connection.getRoute(), mVehicleUri)) {
                lastSpotted = connection.getArrivalTime();
                result.add(connection);
            }
            lastDepartureTime = connection.getDepartureTime();
        }

        if (lastSpotted != null && lastDepartureTime.isAfter(lastSpotted.plusHours(2))) {
            LinkedConnections resultObject = new LinkedConnections();
            LinkedConnection[] connections = new LinkedConnection[result.size()];
            connections = result.toArray(connections);
            resultObject.connections = connections;
            resultObject.current = current;
            resultObject.previous = previous;
            resultObject.next = data.next;
            mSuccessListener.onSuccessResponse(resultObject, mTag);
            ((MeteredDataSource.MeteredRequest) mTag).setMsecParsed(DateTime.now().getMillis());
            return 0;
        }

        return 1;
    }

    @Override
    public void onQueryFailed(Exception e, Object tag) {
        mErrorListener.onErrorResponse(e, tag);
        ((MeteredDataSource.MeteredRequest) tag).setMsecParsed(DateTime.now().getMillis());
        ((MeteredDataSource.MeteredRequest) tag).setResponseType(MeteredDataSource.RESPONSE_FAILED);
    }
}
