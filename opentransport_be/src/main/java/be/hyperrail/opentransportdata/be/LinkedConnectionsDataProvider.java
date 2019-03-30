package be.hyperrail.opentransportdata.be;

import android.content.Context;

import be.hyperrail.opentransportdata.be.experimental.linkedconnections.LinkedConnectionsDataSource;
import be.hyperrail.opentransportdata.be.irail.IrailFacilitiesDataProvider;
import be.hyperrail.opentransportdata.be.irail.IrailStationsDataProvider;
import be.hyperrail.opentransportdata.common.contracts.TransportDataProvider;
import be.hyperrail.opentransportdata.common.contracts.TransportDataSource;
import be.hyperrail.opentransportdata.common.contracts.TransportStopFacilitiesDataSource;
import be.hyperrail.opentransportdata.common.contracts.TransportStopsDataSource;

public class LinkedConnectionsDataProvider implements TransportDataProvider {
    @Override
    public TransportStopsDataSource getStopsDataSource(Context applicationContext) {
        return new IrailStationsDataProvider(applicationContext);
    }

    @Override
    public TransportDataSource getTransportDataSource(Context applicationContext, TransportStopsDataSource stationProviderInstance) {
        return new LinkedConnectionsDataSource(applicationContext, stationProviderInstance);
    }

    @Override
    public TransportStopFacilitiesDataSource getStopsFacilitiesDataSource(Context applicationContext, TransportStopsDataSource stationProviderInstance) {
        return new IrailFacilitiesDataProvider(applicationContext);
    }
}
