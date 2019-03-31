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

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;

import org.osmdroid.config.Configuration;

import java.io.File;

import be.hyperrail.android.logging.HyperRailCrashlyticsLogger;
import be.hyperrail.android.logging.HyperRailLog;
import be.hyperrail.android.util.ReviewDialogProvider;
import be.hyperrail.opentransportdata.OpenTransportApi;
import be.hyperrail.opentransportdata.be.IrailDataProvider;
import io.fabric.sdk.android.Fabric;

/**
 * The application base class
 */
public class Launcher extends android.app.Application {

    public void onCreate() {

        // Crashlytics abstraction layer
        HyperRailCrashlyticsLogger logger = new HyperRailCrashlyticsLogger();
        HyperRailLog.init(logger);
        // Setup the factory as soon as the app is created.
        OpenTransportApi.init(getApplicationContext(), new IrailDataProvider(), logger);
        // Set up Crashlytics, disabled for debug builds
        Crashlytics crashlyticsKit = new Crashlytics.Builder()
                .core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
                .build();

        // Initialize Fabric with the debug-disabled crashlytics.
        Fabric.with(this, crashlyticsKit);
        ReviewDialogProvider.init(this);

        Configuration.getInstance().setOsmdroidTileCache(new File(this.getExternalCacheDir(), this.getPackageName() +
                "/osmdroid"));
        Configuration.getInstance().setUserAgentValue("HyperRailBelgium");

        super.onCreate();
    }

}