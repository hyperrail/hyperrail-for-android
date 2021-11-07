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

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import be.hyperrail.android.logging.HyperRailConsoleLogWriter;
import be.hyperrail.android.logging.HyperRailCrashlyticsLogWriter;
import be.hyperrail.android.logging.HyperRailLog;
import be.hyperrail.android.logging.HyperRailLogWriter;
import be.hyperrail.android.util.ReviewDialogProvider;
import be.hyperrail.opentransportdata.OpenTransportApi;
import be.hyperrail.opentransportdata.be.IrailDataProvider;

/**
 * The application base class
 */
public class Launcher extends android.app.Application {

    public void onCreate() {

        // Crashlytics abstraction layer
        HyperRailLogWriter logger;
        if (BuildConfig.DEBUG) {
            logger = new HyperRailConsoleLogWriter();
        } else {
            logger = new HyperRailCrashlyticsLogWriter();
        }
        HyperRailLog.initLogWriter(logger);
        // Setup the factory as soon as the app is created.
        OpenTransportApi.init(getApplicationContext(), new IrailDataProvider(), logger);
        // Set up Crashlytics, disabled for debug builds
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
        ReviewDialogProvider.init(this);
        super.onCreate();
    }

}