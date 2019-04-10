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

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.be.irail;

import android.support.annotation.Nullable;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import be.hyperrail.opentransportdata.common.models.VehicleJourneyStub;
import be.hyperrail.opentransportdata.logging.OpenTransportLog;

/**
 * VehicleJourney information, except its stops.
 * This data is typically present in the API without requiring a second API call.
 */
public class IrailVehicleJourneyStub implements VehicleJourneyStub, Serializable {

    private final static OpenTransportLog log = OpenTransportLog.getLogger(IrailVehicleJourneyStub.class);

    /**
     * The URI which uniquely identifies this train across time and transport providers.
     */
    @Nullable
    private final String uri;

    /**
     * The ID of the train, relative to the public transport provider. For example IC538.
     */
    private String id;

    /**
     * The headsign of the train, which indicates the destination of the train to end users.
     * This is the preferred way to communicate the train in an understandable way to the end user.
     */
    private String headsign;

    // Direction is required, since we need to display something
    public IrailVehicleJourneyStub(String id, String headSign, @Nullable String uri) {
        this.id = id.toUpperCase();
        this.headsign = headSign;
        this.uri = uri;
    }

    /**
     * The ID, for example IC4516
     *
     * @return ID, for example IC4516
     */

    public String getId() {
        return id;
    }

    /**
     * The direction (final stop) of this train
     *
     * @return direction (final stop) of this train
     */
    public String getHeadsign() {
        return headsign;
    }

    /**
     * Human-readable name, for example IC 4516
     *
     * @return Human-readable name
     */

    public String getName() {
        return getVehicleName(id);
    }


    public static String getVehicleName(String id) {
        return getVehicleClass(id) + " " + getVehicleNumber(id);
    }

    /**
     * Semantic ID, for example http://irail.be/vehicle/IC4516
     *
     * @return Semantic ID
     */

    public String getSemanticId() {
        if (uri != null) {
            return uri;
        }
        // Calculate if unknown
        return "http://irail.be/vehicle/" + getId();
    }

    /**
     * VehicleJourney type, for example S, IC, L, P
     *
     * @return The type of this train
     */

    public String getType() {
        return getVehicleClass(getId());
    }

    /**
     * Route/trip class, for example S, IC, L, P
     *
     * @return The route/trip type for this vehicle
     */

    private static String getVehicleClass(String id) {
        // S trains are special
        if (id.startsWith("S")) {
            if (id.length() > 5) {
                return id.substring(0, id.length() - 4);
            } else {
                // Logging an exception will make it more visible, allowing possible new formats to be detected more easily.
                log.logException(new IllegalArgumentException("Failed to get vehicle class for id " + id));
            }
        }

        String pattern = "(\\w+?)(\\d+)";

        // Create a Pattern object
        Pattern r = Pattern.compile(pattern);

        // Now create matcher object.
        Matcher m = r.matcher(id);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }

    /**
     * VehicleJourney number, for example 4516
     *
     * @return The number of this train
     */

    public String getNumber() {
        return getVehicleNumber(getId());
    }

    /**
     * Deduct the number of a vehicle from its ID
     *
     * @param vehicleId The ID of a vehicle, e.g. IC538
     * @return The number of a vehicle, e.g. 538
     */

    private static String getVehicleNumber(String vehicleId) {
        // S trains are special
        if (vehicleId.startsWith("S")) {
            return vehicleId.substring(vehicleId.length() - 4);
        }

        String pattern = "(\\w+?)(\\d+)";

        // Create a Pattern object
        Pattern r = Pattern.compile(pattern);

        // Now create matcher object.
        Matcher m = r.matcher(vehicleId);
        if (m.find()) {
            return m.group(2);
        }
        return "";
    }
}
