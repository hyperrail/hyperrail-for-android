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

package be.hyperrail.android.irail.implementation;

import android.support.annotation.NonNull;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import be.hyperrail.android.irail.db.Station;

/**
 * Vehicle information, except its stops.
 * This data is typically present in the API without requiring a second API call.
 */
public class VehicleStub implements Serializable {

    private final String uri;

    @NonNull
    protected String id;
    protected Station direction;

    // Direction is required, since we need to display something
    public VehicleStub(@NonNull String id, @NonNull Station direction, String uri) {

        // TODO: all ids should have a correct prefix already, should not be tightly coupled to iRail
        if (!id.startsWith("BE.NMBS.")) {
            id = "BE.NMBS." + id;
        }

        this.id = id.toUpperCase();

        this.direction = direction;
        this.uri = uri;
    }

    /**
     * The ID, for example BE.NMBS.IC4516
     *
     * @return ID, for example BE.NMBS.IC4516
     */
    @NonNull
    public String getId() {
        return id;
    }

    /**
     * The direction (final stop) of this train
     *
     * @return direction (final stop) of this train
     */
    public Station getDirection() {
        return direction;
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
        id = getReducedVehicleId(id);
        return getVehicleClass(id) + " " + getVehicleNumber(id);
    }

    /**
     * ID without leading BE.NMBS, for example IC4516
     *
     * @return ID without leading BE.NMBS
     */
    public static String getReducedVehicleId(String id) {
        if (id.startsWith("BE.NMBS.")) {
            return id.substring(8);
        } else {
            return id;
        }
    }

    public String getReducedId() {
        return getReducedVehicleId(this.id);
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
        return "http://irail.be/vehicle/" + getReducedId();
    }

    /**
     * Vehicle type, for example S, IC, L, P
     *
     * @return The type of this train
     */
    public String getType() {
        return getVehicleClass(getReducedId());
    }

    /**
     * Route/trip class, for example S, IC, L, P
     *
     * @return The route/trip type for this vehicle
     */
    public static String getVehicleClass(String id) {
        // S trains are special
        if (id.startsWith("S")) {
            return id.substring(0, id.length() - 4);
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
     * Vehicle number, for example 4516
     *
     * @return The number of this train
     */
    public String getNumber() {
        return getVehicleNumber(getReducedId());
    }

    /**
     * Deduct the number of a vehicle from its ID
     *
     * @param vehicleId The ID of a vehicle, e.g. IC538
     * @return The number of a vehicle, e.g. 538
     */
    public static String getVehicleNumber(String vehicleId) {
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
