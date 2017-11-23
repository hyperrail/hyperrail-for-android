/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.viewgroup;

import android.content.Context;

/**
 * Created by Bert on 23-11-2017.
 */

public interface ListDataViewGroup<T> {

    /**
     * Bind data to this view
     * @param data
     */
    void bind(Context context, T data);
}
