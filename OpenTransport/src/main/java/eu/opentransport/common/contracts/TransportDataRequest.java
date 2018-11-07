/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package eu.opentransport.common.contracts;

import android.support.annotation.Nullable;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * A request for API data from an API providing IRail-like data.
 * <p>
 * Requests can contain additional data fields which are not supported by all supported data sources. Data fields should be ignored when they are not supported by the API.
 */

public interface TransportDataRequest<T> extends Serializable, Comparable<TransportDataRequest> {

    /**
     * The date this search was created at
     *
     * @return The datetime at which this request was created
     */

    DateTime getCreatedAt();

    /**
     * A JSON representation of this request. This object only contains fields relevant for storage,
     * to always get actual results when re-submitting a query (from favorites, recents, widgets, notifications, ..)
     *
     * @return A JSON object representing this object
     */

    JSONObject toJson() throws JSONException;

    @Nullable
    TransportDataSuccessResponseListener<T> getOnSuccessListener();

    /**
     * Notify possible listeners of a successful request
     *
     * @param data The request result
     */
    void notifySuccessListeners(T data);

    @Nullable
    TransportDataErrorResponseListener getOnErrorListener();

    /**
     * Notify possible listeners of a failed request
     *
     * @param e The exception which occurred while making this request
     */
    void notifyErrorListeners( Exception e);

    /**
     * Set the callbacks for responses
     *
     * @param successResponseListener Listener for successful responses
     * @param errorResponseListener   Listener for error responses
     * @param tag                     A tag which will be passed on to the listeners, to be able to distinguish requests
     */
    void setCallback(@Nullable TransportDataSuccessResponseListener<T> successResponseListener, @Nullable TransportDataErrorResponseListener errorResponseListener, @Nullable Object tag);

    @Nullable
    Object getTag();

    /**
     * Check if this object equals another when ignoring all extra and time related fields
     *
     * @param other The request which should be compared to this request
     * @return True when fields are equal
     */
    boolean equalsIgnoringTime(TransportDataRequest other);
}
