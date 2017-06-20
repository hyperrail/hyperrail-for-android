/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package android.irail.be.hyperrail.irail.implementation;

import android.irail.be.hyperrail.irail.contracts.IrailDataResponse;

import java.util.Date;

/**
 * @inheritDoc
 */
public class ApiResponse<T> implements IrailDataResponse<T> {

    private T data;
    private Exception exception;
    private Date time;

    public ApiResponse(T data) {
        this(data, null);
    }

    public ApiResponse(T data, Exception exception) {
        this.data = data;
        this.exception = exception;
        this.time = new Date();
    }

    @Override
    public T getData() {
        return data;
    }

    @Override
    public boolean isSucces() {
        return (exception == null);
    }

    @Override
    public Exception getException() {
        return exception;
    }

    @Override
    public Date getTime() {
        return time;
    }
}
