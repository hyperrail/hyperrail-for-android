/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.viewgroup;

import android.content.Context;

/**
 * Interface for RecyclerView items
 * @param <C>
 * @param <T>
 */
public interface RecyclerViewItemViewGroup<C, T> {

    /**
     * Bind data to this view
     *
     * @param itemData The data to show in this item
     * @param listData Contextual data, giving information on the larger object of which data is part
     * @param position The position of this item in the list
     */
    void bind(Context context, T itemData, C listData, int position);
}
