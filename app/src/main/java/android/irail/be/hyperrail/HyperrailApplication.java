/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package android.irail.be.hyperrail;

import android.app.Application;
import android.irail.be.hyperrail.irail.factories.IrailFactory;

/**
 * The application base class
 */
public class HyperrailApplication extends Application {

    public void onCreate() {
        // Setup the factory as soon as the app is created.
        IrailFactory.setup(getApplicationContext());
        super.onCreate();
    }

}