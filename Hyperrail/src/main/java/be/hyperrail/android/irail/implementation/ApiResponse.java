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

/**
 * @inheritDoc
 */
public class ApiResponse<T> implements IrailDataResponse<T> {

    @Nullable
    private final T data;

    private final boolean isCached;
    private final boolean isOffline;

    @NonNull
    private final DateTime time;

    public ApiResponse(T data) {
        this(data, false, false);
    }



    public ApiResponse(@Nullable T data, boolean isCached, boolean isOffline) {
        this.data = data;
        this.isCached = isCached;
        this.isOffline = isOffline;
        this.time = new DateTime();
    }

    @Nullable
    @Override
    public T getData() {
        return data;
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
