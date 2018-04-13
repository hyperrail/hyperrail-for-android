/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.activities;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import be.hyperrail.android.util.UriToIntentMapper;

/**
 * Verify all incoming deep-links and open them in the correct activity
 */
public class LinkDispatcherActivity extends Activity {
    private final UriToIntentMapper mMapper = new UriToIntentMapper(this);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            mMapper.dispatchIntent(getIntent());
        } catch (IllegalArgumentException iae) {
            // Malformed URL
            Log.e("Deep links", "Invalid URI", iae);
        } finally {
            // Always finish the activity so that it doesn't stay in our history
            finish();
        }
    }
}
