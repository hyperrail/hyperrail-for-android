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

package be.hyperrail.android.adapter;

import android.support.v7.widget.RecyclerView;

/**
 * Interface for item click callbacks
 * @param <T>
 */
public interface onRecyclerItemClickListener<T> {
    void onRecyclerItemClick(RecyclerView.Adapter sender, T object);
}
