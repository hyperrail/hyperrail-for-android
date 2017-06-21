/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package android.irail.be.hyperrail.util;

import android.view.View;

/**
 * Callback interface for {@link SwipeDetector}
 */
public interface Swipable {

    /**
     * Called when a swipe occured
     *
     * @param v   The view in which the swipe ended
     * @param tag The tag associated with the {@link SwipeDetector}
     */
    void swipedBottomToTop(View v, int tag);

    /**
     * Called when a swipe occured
     *
     * @param v   The view in which the swipe ended
     * @param tag The tag associated with the {@link SwipeDetector}
     */
    void swipedLeftToRight(View v, int tag);

    /**
     * Called when a swipe occured
     *
     * @param v   The view in which the swipe ended
     * @param tag The tag associated with the {@link SwipeDetector}
     */
    void swipedRightToLeft(View v, int tag);

    /**
     * Called when a swipe occured
     *
     * @param v   The view in which the swipe ended
     * @param tag The tag associated with the {@link SwipeDetector}
     */
    void swipedTopToBottom(View v, int tag);

}
