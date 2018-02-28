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

import be.hyperrail.android.irail.implementation.requests.IrailDisturbanceRequest;
import be.hyperrail.android.irail.implementation.requests.IrailLiveboardRequest;
import be.hyperrail.android.irail.implementation.requests.IrailPostOccupancyRequest;
import be.hyperrail.android.irail.implementation.requests.IrailRouteRequest;
import be.hyperrail.android.irail.implementation.requests.IrailRoutesRequest;
import be.hyperrail.android.irail.implementation.requests.IrailVehicleRequest;

/**
 * Retrieve (realtime) data according from the iRail API, or any API which provides similar data.
 * Requests can contain additional data fields which are not supported by all supported data sources. Data fields should be ignored when they are not supported by the API.
 * See http://docs.irail.be/
 */
public interface IrailDataProvider {

    void getDisturbances(@NonNull IrailDisturbanceRequest... request);

    void getLiveboard(@NonNull IrailLiveboardRequest... request);

    void getLiveboardBefore(@NonNull IrailLiveboardRequest... request);

    void getRoutes(@NonNull IrailRoutesRequest... request);

    void getRoute(@NonNull IrailRouteRequest... request);

    void getTrain(@NonNull IrailVehicleRequest... request);

    void postOccupancy(@NonNull IrailPostOccupancyRequest... request);

    void abortAllQueries();

}
