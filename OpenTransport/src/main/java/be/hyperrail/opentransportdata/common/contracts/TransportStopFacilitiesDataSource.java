package be.hyperrail.opentransportdata.common.contracts;

import be.hyperrail.opentransportdata.common.models.StopLocation;
import be.hyperrail.opentransportdata.common.models.StopLocationFacilities;

public interface TransportStopFacilitiesDataSource {
    StopLocationFacilities getStationFacilitiesByUri(String id);

    StopLocationFacilities getStationFacilities(StopLocation stopLocation);
}
