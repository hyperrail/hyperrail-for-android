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

package eu.opentransport.common.requests;

import android.support.annotation.Nullable;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import eu.opentransport.common.contracts.TransportDataErrorResponseListener;
import eu.opentransport.common.contracts.TransportDataRequest;
import eu.opentransport.common.contracts.TransportDataSuccessResponseListener;


/**
 * A base class for TransportDataRequest implementations
 */

public abstract class OpenTransportBaseRequest<T> implements TransportDataRequest<T> {

    protected DateTime createdAt;
    protected transient Object tag;
    protected transient TransportDataErrorResponseListener errorResponseListener;
    protected transient TransportDataSuccessResponseListener<T> successResponseListener;

    protected OpenTransportBaseRequest() {
        this.createdAt = new DateTime();
    }

    protected OpenTransportBaseRequest(JSONObject json) throws JSONException {
        this.createdAt = new DateTime(json.getLong("created_at"));
    }


    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("created_at", createdAt.getMillis());
        return json;
    }


    public DateTime getCreatedAt() {
        return createdAt;
    }


    public void setCallback(@Nullable TransportDataSuccessResponseListener<T> successResponseListener, @Nullable TransportDataErrorResponseListener errorResponseListener, @Nullable Object tag) {
        this.errorResponseListener = errorResponseListener;
        this.successResponseListener = successResponseListener;
        this.tag = tag;
    }

    public TransportDataErrorResponseListener getOnErrorListener() {
        return errorResponseListener;
    }

    @Override
    public TransportDataSuccessResponseListener<T> getOnSuccessListener() {
        return successResponseListener;
    }
    /**
     * This is a safe way to broadcast a result without risking NullPointerExceptions
     */
    @Override
    public void notifySuccessListeners( T data) {
        if (successResponseListener != null) {
            successResponseListener.onSuccessResponse(data, this.tag);
        }
    }

    /**
     * This is a safe way to broadcast an exception without risking NullPointerExceptions
     */
    @Override
    public void notifyErrorListeners( Exception e) {
        if (errorResponseListener != null) {
            errorResponseListener.onErrorResponse(e, this.tag);
        }
    }

    public Object getTag() {
        return tag;
    }
}
