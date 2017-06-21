/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package android.irail.be.hyperrail.infiniteScrolling;

public interface InfiniteScrollingDataSource {

    /**
     * This method is called when an InfiniteScrollingAdapter reached the end of the available data.
     * More items should be retrieved, after which the adapter should be updated with the enlarged data set.
     */
    void loadMoreRecyclerviewItems();
}
