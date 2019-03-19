/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata;

import android.content.Context;

import be.hyperrail.opentransportdata.common.contracts.TransportDataSource;
import be.hyperrail.opentransportdata.common.contracts.TransportStopsDataSource;
import be.hyperrail.opentransportdata.irail.IrailApi;
import be.hyperrail.opentransportdata.irail.IrailFacilitiesDataProvider;
import be.hyperrail.opentransportdata.irail.IrailStationsDataProvider;
import be.hyperrail.opentransportdata.experimental.lc2Irail.Lc2IrailDataSource;
import be.hyperrail.opentransportdata.experimental.linkedconnections.LinkedConnectionsDataSource;

public class OpenTransportApi {

    private static TransportStopsDataSource stationProviderInstance;
    private static TransportDataSource dataProviderInstance;

    public enum DataProvider {
        BE_IRAIL_GRAPH,
        BE_IRAIL_API,
        BE_IRAIL_LC2IRAIL,
        SE_GRAPH,
        SE_LC2IRAIL
    }

    public static void init(Context appContext, DataProvider provider) {
        switch (provider) {
            case BE_IRAIL_API:
                stationProviderInstance = new IrailStationsDataProvider(appContext);
                dataProviderInstance = new IrailApi(appContext);
                break;
            case BE_IRAIL_GRAPH:
                stationProviderInstance = new IrailStationsDataProvider(appContext);
                dataProviderInstance = new LinkedConnectionsDataSource(appContext);
                break;
            case BE_IRAIL_LC2IRAIL:
                stationProviderInstance = new IrailStationsDataProvider(appContext);
                dataProviderInstance = new Lc2IrailDataSource(appContext);
                break;
            case SE_GRAPH:
                stationProviderInstance = new IrailStationsDataProvider(appContext);
                dataProviderInstance = new IrailApi(appContext);
                break;
            case SE_LC2IRAIL:
                stationProviderInstance = new IrailStationsDataProvider(appContext);
                dataProviderInstance = new IrailApi(appContext);
                break;
        }
    }

    public static TransportStopsDataSource getStationsProviderInstance() {
        if (stationProviderInstance == null) {
            throw new IllegalStateException("Initialize OpenTransportApi using init() before trying to access the stations provider!");
        }
        return stationProviderInstance;
    }

    public static TransportDataSource getDataProviderInstance() {
        if (dataProviderInstance == null) {
            throw new IllegalStateException("Initialize OpenTransportApi using init() before trying to access the data provider!");
        }
        return dataProviderInstance;
    }

    public static IrailFacilitiesDataProvider getFacilitiesProviderInstance() {
        // TODO: implement
        return null;
    }
}
