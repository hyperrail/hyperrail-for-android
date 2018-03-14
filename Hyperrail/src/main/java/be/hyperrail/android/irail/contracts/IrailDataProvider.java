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

package be.hyperrail.android.irail.contracts;

import android.support.annotation.NonNull;

import be.hyperrail.android.irail.implementation.requests.ExtendLiveboardRequest;
import be.hyperrail.android.irail.implementation.requests.ExtendRoutesRequest;
import be.hyperrail.android.irail.implementation.requests.IrailDisturbanceRequest;
import be.hyperrail.android.irail.implementation.requests.IrailLiveboardRequest;
import be.hyperrail.android.irail.implementation.requests.IrailPostOccupancyRequest;
import be.hyperrail.android.irail.implementation.requests.IrailRouteRequest;
import be.hyperrail.android.irail.implementation.requests.IrailRoutesRequest;
import be.hyperrail.android.irail.implementation.requests.IrailVehicleRequest;
import be.hyperrail.android.irail.implementation.requests.VehicleStopRequest;

/**
 * Retrieve (realtime) data according from the iRail API, or any API which provides similar data.
 * Requests can contain additional data fields which are not supported by all supported data sources. Data fields should be ignored when they are not supported by the API.
 * See http://docs.irail.be/
 */
public interface IrailDataProvider {

    void getDisturbances(@NonNull IrailDisturbanceRequest... requests);

    void getLiveboard(@NonNull IrailLiveboardRequest... requests);

    void extendLiveboard(@NonNull ExtendLiveboardRequest... requests);

    void getLiveboardBefore(@NonNull IrailLiveboardRequest... requests);

    void getRoutes(@NonNull IrailRoutesRequest... requests);

    void extendRoutes(@NonNull ExtendRoutesRequest... requests);

    void getRoute(@NonNull IrailRouteRequest... requests);

    void getStop(@NonNull VehicleStopRequest... requests);
    
    void getVehicle(@NonNull IrailVehicleRequest... requests);

    void postOccupancy(@NonNull IrailPostOccupancyRequest... requests);

    void abortAllQueries();

}
