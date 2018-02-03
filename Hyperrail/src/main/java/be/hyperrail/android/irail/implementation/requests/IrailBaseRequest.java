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

package be.hyperrail.android.irail.implementation.requests;

import android.support.annotation.NonNull;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import be.hyperrail.android.irail.contracts.IRailErrorResponseListener;
import be.hyperrail.android.irail.contracts.IRailSuccessResponseListener;
import be.hyperrail.android.irail.contracts.IrailRequest;


/**
 * A base class for IrailRequest implementations
 */

public abstract class IrailBaseRequest<T> implements IrailRequest<T> {

    protected DateTime createdAt;
    protected Object tag;
    protected IRailErrorResponseListener errorResponseListener;
    protected IRailSuccessResponseListener<T> successResponseListener;

    protected IrailBaseRequest() {
        this.createdAt = new DateTime();
    }

    protected IrailBaseRequest(@NonNull JSONObject json) throws JSONException {
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


    public void setCallback(IRailSuccessResponseListener<T> successResponseListener, IRailErrorResponseListener errorResponseListener, Object tag) {
        this.errorResponseListener = errorResponseListener;
        this.successResponseListener = successResponseListener;
        this.tag = tag;
    }

    public IRailErrorResponseListener getOnErrorListener() {
        return errorResponseListener;
    }

    @Override
    public IRailSuccessResponseListener<T> getOnSuccessListener() {
        return successResponseListener;
    }
    /**
     * This is a safe way to broadcast a result without risking NullPointerExceptions
     */
    @Override
    public void notifySuccessListeners(@NonNull T data) {
        if (successResponseListener != null) {
            successResponseListener.onSuccessResponse(data, this.tag);
        }
    }

    /**
     * This is a safe way to broadcast an exception without risking NullPointerExceptions
     */
    @Override
    public void notifyErrorListeners(@NonNull Exception e) {
        if (errorResponseListener != null) {
            errorResponseListener.onErrorResponse(e, this.tag);
        }
    }

    public Object getTag() {
        return tag;
    }
}
