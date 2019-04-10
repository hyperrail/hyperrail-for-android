package be.hyperrail.opentransportdata.common.contracts;

import android.content.Context;

public interface TransportDataProvider {

    TransportDataSource getTransportDataSource(Context applicationContext, TransportStopsDataSource stationProviderInstance);

    TransportStopFacilitiesDataSource getStopsFacilitiesDataSource(Context applicationContext, TransportStopsDataSource stationProviderInstance);

    TransportStopsDataSource getStopsDataSource(Context applicationContext);

}
