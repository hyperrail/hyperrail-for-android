/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.irail.contracts;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * A request for API data from an API providing IRail-like data.
 *
 * Requests can contain additional data fields which are not supported by all supported data sources. Data fields should be ignored when they are not supported by the API.
 */

public interface IrailRequest<T> extends Serializable, Comparable<IrailRequest> {

    /**
     * The date this search was created at
     *
     * @return
     */
    DateTime getCreatedAt();

    /**
     * A JSON String representation of this request
     *
     * @return
     */
    JSONObject toJson() throws JSONException;

    IRailSuccessResponseListener<T> getOnSuccessListener();
    void notifySuccessListeners(T data);

    IRailErrorResponseListener getOnErrorListener();
    void notifyErrorListeners(Exception e);

    void setCallback(IRailSuccessResponseListener<T> successResponseListener, IRailErrorResponseListener errorResponseListener, Object tag);

    Object getTag();
}
