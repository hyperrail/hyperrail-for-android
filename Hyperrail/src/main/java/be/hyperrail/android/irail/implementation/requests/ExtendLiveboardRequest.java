/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.irail.implementation.requests;

import android.support.annotation.NonNull;

import be.hyperrail.android.irail.contracts.IrailRequest;
import be.hyperrail.android.irail.implementation.Liveboard;

/**
 * Request to withStopsAppended or prepend a mLiveboard
 */

public class ExtendLiveboardRequest extends IrailBaseRequest<Liveboard> implements IrailRequest<Liveboard> {

    @NonNull
    private final Liveboard mLiveboard;
    @NonNull
    private final Action mAction;

    public ExtendLiveboardRequest(@NonNull Liveboard liveboard, @NonNull Action action) {
        this.mLiveboard = liveboard;
        this.mAction = action;
    }

    @Override
    public boolean equalsIgnoringTime(IrailRequest other) {
        return (other instanceof ExtendLiveboardRequest) && mLiveboard.equals(((ExtendLiveboardRequest) other).getLiveboard());
    }

    @Override
    public int compareTo(@NonNull IrailRequest o) {
        return 0;
    }

    @NonNull
    public Liveboard getLiveboard() {
        return mLiveboard;
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
