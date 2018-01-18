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

package be.hyperrail.android.irail.implementation;

import org.joda.time.DateTime;

import be.hyperrail.android.irail.contracts.IrailDataResponse;

/**
 * @inheritDoc
 */
public class ApiResponse<T> implements IrailDataResponse<T> {

    private final T data;
    private final boolean isCached;
    private final boolean isOffline;
    private final Exception exception;
    private final DateTime time;

    public ApiResponse(T data) {
        this(data, null);
    }

    @Deprecated
    public ApiResponse(T data, Exception exception) {
        this.data = data;
        this.exception = exception;
        this.isCached = false;
        this.isOffline = false;
        this.time = new DateTime();
    }

    public ApiResponse(T data, boolean isCached, boolean isOffline, Exception exception) {
        this.data = data;
        this.isCached = isCached;
        this.isOffline = isOffline;
        this.exception = exception;
        this.time = new DateTime();
    }

    @Override
    public T getData() {
        return data;
    }

    @Override
    public boolean isSuccess() {
        return (exception == null);
    }

    @Override
    public Exception getException() {
        return exception;
    }

    @Override
    public DateTime getTime() {
        return time;
    }

    public boolean isOffline() {
        return isOffline;
    }

    public boolean isCached() {
        return isCached;
    }
}
