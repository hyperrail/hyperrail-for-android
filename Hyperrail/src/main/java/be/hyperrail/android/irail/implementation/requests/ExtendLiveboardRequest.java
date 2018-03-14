/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.irail.implementation.requests;

import android.support.annotation.NonNull;

import be.hyperrail.android.irail.contracts.IrailRequest;
import be.hyperrail.android.irail.implementation.LiveBoard;

/**
 * Request to append or prepend a mLiveBoard
 */

public class ExtendLiveboardRequest extends IrailBaseRequest<LiveBoard> implements IrailRequest<LiveBoard> {

    @NonNull
    private final LiveBoard mLiveBoard;
    @NonNull
    private final Action mAction;

    public ExtendLiveboardRequest(@NonNull LiveBoard liveboard, @NonNull Action action) {
        this.mLiveBoard = liveboard;
        this.mAction = action;
    }

    @Override
    public boolean equalsIgnoringTime(IrailRequest other) {
        return (other instanceof ExtendLiveboardRequest) && mLiveBoard.equals(((ExtendLiveboardRequest) other).getLiveboard());
    }

    @Override
    public int compareTo(@NonNull IrailRequest o) {
        return 0;
    }

    @NonNull
    public LiveBoard getLiveboard() {
        return mLiveBoard;
    }

    @NonNull
    public Action getAction() {
        return mAction;
    }

    public enum Action {
        APPEND,
        PREPEND
    }
}
