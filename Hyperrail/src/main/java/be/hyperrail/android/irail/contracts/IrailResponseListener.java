/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.irail.contracts;

public interface IrailResponseListener<T> {

    void onIrailSuccessResponse(T data, int tag);

    void onIrailErrorResponse(Exception e, int tag);
}
