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

import be.hyperrail.android.util.ReviewDialogProvider;
import eu.opentransport.OpenTransportApi;
import io.fabric.sdk.android.Fabric;

/**
 * The application base class
 */
public class Launcher extends android.app.Application {

    public void onCreate() {
        // Setup the factory as soon as the app is created.
        OpenTransportApi.init(getApplicationContext(),OpenTransportApi.DataProvider.BE_IRAIL_API);

        CrashlyticsCore core = new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build();
        Fabric.with(this, new Crashlytics.Builder().core(core).build());

        ReviewDialogProvider.init(this);

        super.onCreate();
    }

}