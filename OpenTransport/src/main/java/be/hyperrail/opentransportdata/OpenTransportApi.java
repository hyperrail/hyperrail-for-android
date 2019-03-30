/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata;

import android.content.Context;

import be.hyperrail.opentransportdata.common.contracts.TransportDataProvider;
import be.hyperrail.opentransportdata.common.contracts.TransportDataSource;
import be.hyperrail.opentransportdata.common.contracts.TransportStopFacilitiesDataSource;
import be.hyperrail.opentransportdata.common.contracts.TransportStopsDataSource;
import be.hyperrail.opentransportdata.logging.OpenTransportLog;
import be.hyperrail.opentransportdata.logging.OpenTransportLogger;

public class OpenTransportApi {

    private static TransportStopsDataSource stationProviderInstance;
    private static TransportDataSource dataProviderInstance;
    private static TransportStopFacilitiesDataSource stopFacilitiesDataSource;

    public static void init(Context appContext, String qualifiedName) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        init(appContext, getProviderFromQualifiedNames(qualifiedName));
    }

    public static void init(Context applicationContext, String qualifiedName, OpenTransportLogger logger) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        OpenTransportLog.init(logger);
        init(applicationContext, qualifiedName);
    }

    public static void init(Context appContext, TransportDataProvider dataProvider) {
        stationProviderInstance = dataProvider.getStopsDataSource(appContext);
        stopFacilitiesDataSource = dataProvider.getStopsFacilitiesDataSource(appContext, stationProviderInstance);
        dataProviderInstance = dataProvider.getTransportDataSource(appContext, stationProviderInstance);
    }

    public static void init(Context applicationContext, TransportDataProvider dataProvider, OpenTransportLogger logger) {
        OpenTransportLog.init(logger);
        init(applicationContext, dataProvider);
    }

    private static TransportDataProvider getProviderFromQualifiedNames(String qualifiedName) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        return
                (TransportDataProvider) TransportDataProvider.class
                        .getClassLoader()
                        .loadClass(qualifiedName)
                        .newInstance();
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

    public static TransportStopFacilitiesDataSource getFacilitiesProviderInstance() {
        if (stopFacilitiesDataSource == null) {
            throw new IllegalStateException("Initialize OpenTransportApi using init() before trying to access the facilities provider!");
        }
        return stopFacilitiesDataSource;
    }
}
