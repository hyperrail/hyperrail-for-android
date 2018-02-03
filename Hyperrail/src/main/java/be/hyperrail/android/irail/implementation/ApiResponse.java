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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.joda.time.DateTime;

import be.hyperrail.android.irail.contracts.IrailDataResponse;
import be.hyperrail.android.irail.contracts.IrailRequest;

/**
 * @inheritDoc
 */
public class ApiResponse<T> implements IrailDataResponse<T> {

    @Nullable
    private final T data;

    private final boolean isCached;
    private final boolean isOffline;

    @Nullable
    private final Exception exception;

    @NonNull
    private final DateTime time;

    @NonNull
    private IrailRequest request;

    public ApiResponse(T data, @NonNull IrailRequest request) {
        this(data, false, false, null, request);
        this.request = request;
    }

    @Deprecated
    public ApiResponse(@Nullable T data, @Nullable Exception exception, @NonNull IrailRequest request) {
        this.data = data;
        this.exception = exception;
        this.request = request;
        this.isCached = false;
        this.isOffline = false;
        this.time = new DateTime();
    }

    public ApiResponse(@Nullable T data, boolean isCached, boolean isOffline, @Nullable Exception exception, @NonNull IrailRequest request) {
        this.data = data;
        this.isCached = isCached;
        this.isOffline = isOffline;
        this.exception = exception;
        this.request = request;
        this.time = new DateTime();
    }

    @Nullable
    @Override
    public T getData() {
        return data;
    }


    @NonNull
    @Override
    public IrailRequest getRequest() {
        return request;
    }

    @Override
    public boolean isSuccess() {
        return (exception == null);
    }

    @Nullable
    @Override
    public Exception getException() {
        return exception;
    }

    @NonNull
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
