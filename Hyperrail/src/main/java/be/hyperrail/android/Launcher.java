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

package be.hyperrail.android;

import com.google.firebase.crash.FirebaseCrash;

import be.hyperrail.android.irail.factories.IrailFactory;

/**
 * The application base class
 */
public class Launcher extends android.app.Application {

    public void onCreate() {
        // Setup the factory as soon as the app is created.
        IrailFactory.setup(getApplicationContext());
        if (BuildConfig.DEBUG) {
            FirebaseCrash.setCrashCollectionEnabled(false);
        }
        super.onCreate();
    }

}