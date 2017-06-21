/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package android.irail.be.hyperrail.irail.factories;

import android.content.Context;
import android.irail.be.hyperrail.irail.contracts.IrailDataProvider;
import android.irail.be.hyperrail.irail.contracts.IrailParser;
import android.irail.be.hyperrail.irail.contracts.IrailStationProvider;
import android.irail.be.hyperrail.irail.db.StationsDb;
import android.irail.be.hyperrail.irail.implementation.IrailApi;
import android.irail.be.hyperrail.irail.implementation.IrailApiParser;
import android.util.Log;

/**
 * Factory to provide singleton data providers (for stations and API calls) and a singleton parser.
 * This factory should be setup at application start. Once setup, context isn't required for calls which return an instance.
 */
public class IrailFactory {

    private static IrailStationProvider stationProviderInstance;
    private static IrailDataProvider dataProviderInstance;
    private static IrailParser parserInstance;

    public static void setup(Context applicationContext){
        stationProviderInstance = new StationsDb(applicationContext);
    }

    private static IrailParser getParserInstance(){
        if (IrailFactory.parserInstance == null){
            IrailFactory.parserInstance = new IrailApiParser(IrailFactory.stationProviderInstance);
        }
        return parserInstance;
    }

    public static IrailStationProvider getStationsProviderInstance(){
        if (IrailFactory.stationProviderInstance == null){
            Log.e("Irail16Factory","Failed to initialize station provider! Call setContext() before calling any factory method!");
        }
        return stationProviderInstance;
    }

    public static IrailDataProvider getDataProviderInstance(){
        if (IrailFactory.dataProviderInstance == null){
            IrailFactory.dataProviderInstance = new IrailApi(getParserInstance(),getStationsProviderInstance());
        }
        return dataProviderInstance;
    }
}
