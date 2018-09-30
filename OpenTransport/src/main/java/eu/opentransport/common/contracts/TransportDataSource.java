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

package eu.opentransport.common.contracts;

import eu.opentransport.common.requests.ExtendLiveboardRequest;
import eu.opentransport.common.requests.ExtendRoutesRequest;
import eu.opentransport.common.requests.IrailDisturbanceRequest;
import eu.opentransport.common.requests.IrailLiveboardRequest;
import eu.opentransport.common.requests.IrailPostOccupancyRequest;
import eu.opentransport.common.requests.IrailRouteRequest;
import eu.opentransport.common.requests.IrailRoutesRequest;
import eu.opentransport.common.requests.IrailVehicleRequest;
import eu.opentransport.common.requests.VehicleStopRequest;

/**
 * Retrieve (realtime) data according from the iRail API, or any API which provides similar data.
 * Requests can contain additional data fields which are not supported by all supported data sources. Data fields should be ignored when they are not supported by the API.
 * See http://docs.irail.be/
 */
public interface TransportDataSource {

    void getDisturbances( IrailDisturbanceRequest... requests);

    void getLiveboard( IrailLiveboardRequest... requests);

    void extendLiveboard( ExtendLiveboardRequest... requests);

    void getRoutes( IrailRoutesRequest... requests);

    void extendRoutes( ExtendRoutesRequest... requests);

    void getRoute( IrailRouteRequest... requests);

    void getStop( VehicleStopRequest... requests);
    
    void getVehicle( IrailVehicleRequest... requests);

    void postOccupancy( IrailPostOccupancyRequest... requests);

    void abortAllQueries();

}
