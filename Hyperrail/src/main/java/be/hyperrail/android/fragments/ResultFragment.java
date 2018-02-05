/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.fragments;

import android.support.annotation.NonNull;

import be.hyperrail.android.irail.contracts.IrailRequest;
import be.hyperrail.android.util.OnDateTimeSetListener;

public interface ResultFragment<T extends IrailRequest> extends OnDateTimeSetListener {

    void setRequest(@NonNull T request);

    T getRequest();
}
