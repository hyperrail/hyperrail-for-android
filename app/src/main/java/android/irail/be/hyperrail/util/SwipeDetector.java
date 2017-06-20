/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package android.irail.be.hyperrail.util;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

/**
 * Class to detect swipes.
 * Requires one class to which a callback will be made, but can be associated as an OnTouchListener to many views.
 */
public class SwipeDetector implements View.OnTouchListener {

    private static final String logTag = "ActivitySwipeDetector";

    // Tag for callback
    private final int tag;

    // Callback activity
    private Swipable activity;

    // Defaults, device agnostic
    private int SWIPE_MIN_DISTANCE = 100;
    private int SWIPE_MAX_OFF_PATH = 100;
    private int SWIPE_THRESHOLD_VELOCITY = 100;

    // Will be overridden on init by values relative to the device
    private int REL_SWIPE_MIN_DISTANCE = SWIPE_MIN_DISTANCE;
    private int REL_SWIPE_MAX_OFF_PATH = SWIPE_MAX_OFF_PATH;
    private int REL_SWIPE_THRESHOLD_VELOCITY = SWIPE_THRESHOLD_VELOCITY;

    private float downX, downY, upX, upY;

    public SwipeDetector(Context context, Swipable callback, int tag) {
        this.activity = callback;
        this.tag = tag;

        final ViewConfiguration vc = ViewConfiguration.get(context);
        this.REL_SWIPE_MIN_DISTANCE = vc.getScaledPagingTouchSlop();
        this.REL_SWIPE_THRESHOLD_VELOCITY = vc.getScaledMinimumFlingVelocity();
        this.REL_SWIPE_MAX_OFF_PATH = vc.getScaledTouchSlop();
    }

    private void onRightToLeftSwipe(View v) {
        activity.swipedRightToLeft(v, tag);
    }

    private void onLeftToRightSwipe(View v) {
        activity.swipedLeftToRight(v, tag);
    }

    private void onTopToBottomSwipe(View v) {
        activity.swipedTopToBottom(v, tag);
    }

    private void onBottomToTopSwipe(View v) {
        activity.swipedBottomToTop(v, tag);
    }

    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                downX = event.getX();
                downY = event.getY();
                return true;
            }
            case MotionEvent.ACTION_UP: {
                upX = event.getX();
                upY = event.getY();

                float deltaX = downX - upX;
                float deltaY = downY - upY;

                // swipe horizontal?
                if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > REL_SWIPE_MIN_DISTANCE) {
                    // left or right
                    if (deltaX < 0) {
                        this.onLeftToRightSwipe(v);
                        return true;
                    }
                    if (deltaX > 0) {
                        this.onRightToLeftSwipe(v);
                        return true;
                    }
                } else if (Math.abs(deltaX) < Math.abs(deltaY) && Math.abs(deltaY) > REL_SWIPE_MIN_DISTANCE) {
                    // top or down
                    if (deltaY < 0) {
                        this.onTopToBottomSwipe(v);
                        return true;
                    }
                    if (deltaY > 0) {
                        this.onBottomToTopSwipe(v);
                        return true;
                    }
                }
            }
        }
        return false;
    }

}