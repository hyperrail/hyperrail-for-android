/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package eu.opentransport;

import android.content.Context;

import com.crashlytics.android.Crashlytics;

import eu.opentransport.common.contracts.TransportDataSource;
import eu.opentransport.common.contracts.TransportStopsDataSource;
import eu.opentransport.common.models.IrailApi;
import eu.opentransport.irail.StationsDataProvider;
import eu.opentransport.lc2Irail.Lc2IrailDataSource;
import eu.opentransport.linkedconnections.LinkedConnectionsDataSource;

import static java.util.logging.Level.SEVERE;

public class OpenTransport {

    private static TransportStopsDataSource stationProviderInstance;
    private static TransportDataSource dataProviderInstance;

    enum DataProvider {
        BE_IRAIL_GRAPH,
        BE_IRAIL_API,
        BE_IRAIL_LC2IRAIL,
        SE_GRAPH,
        SE_LC2IRAIL
    }

    public static void initPreset(Context context, DataProvider provider) {
        switch (provider) {
            case BE_IRAIL_API:
                initClass(new IrailApi(context), new StationsDataProvider(context));
                break;
            case BE_IRAIL_GRAPH:
                initClass(new LinkedConnectionsDataSource(context), new StationsDataProvider(context));
                break;
            case BE_IRAIL_LC2IRAIL:
                initClass(new Lc2IrailDataSource(context), new StationsDataProvider(context));
                break;
            case SE_GRAPH:
                initClass(new LinkedConnectionsDataSource(context), new StationsDataProvider(context));
                break;
            case SE_LC2IRAIL:
                initClass(new Lc2IrailDataSource(context), new StationsDataProvider(context));
                break;
        }
    }

    public static void initClass(TransportDataSource source, TransportStopsDataSource stops) {
        stationProviderInstance = stops;
        dataProviderInstance = source;
    }

    public static TransportStopsDataSource getStationsProviderInstance() {
        if (stationProviderInstance == null) {
            Crashlytics.log(SEVERE.intValue(), "Irail16Factory", "Failed to provide station provider! Call setup() before calling any factory method!");
            Crashlytics.logException(new Exception("TransportStopsDataSource was requested before the factory was initialized"));
            throw new IllegalStateException();
        }
        return stationProviderInstance;
    }

    public static TransportDataSource getDataProviderInstance() {
        if (dataProviderInstance == null) {
            Crashlytics.log(SEVERE.intValue(), "Irail16Factory", "Failed to provide data provider! Call setup() before calling any factory method!");
            Crashlytics.logException(new Exception("TransportDataSource was requested before the factory was initialized"));
            throw new IllegalStateException();
        }
        return dataProviderInstance;
    }
}
