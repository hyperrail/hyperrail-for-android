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

package be.hyperrail.android.irail.contracts;

import org.joda.time.DateTime;

/**
 * A server response, containing a result (if any), an exception object (if any),
 * the time at which this data was retrieved and a boolean indicating its success
 *
 * @param <T> The data type which is contained in this response
 */
public interface IrailDataResponse<T> {

    /**
     * Get the result data.
     *
     * @return The returned data object
     */
    T getData();

    /**
     * Whether or not the request was successful.
     *
     * @return Whether or not the request was successful.
     */
    boolean isSuccess();

    /**
     * Get the exception object that occurred during the request
     *
     * @return The exception thrown by the data provider
     */
    Exception getException();

    /**
     * Get the time at which the request was fulfilled.
     *
     * @return The time when the response was received
     */
    DateTime getTime();

    boolean isOffline();

    boolean isCached();
}
