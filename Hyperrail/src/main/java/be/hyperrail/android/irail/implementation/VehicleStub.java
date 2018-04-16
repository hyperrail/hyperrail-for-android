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
import android.support.annotation.Nullable;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Vehicle information, except its stops.
 * This data is typically present in the API without requiring a second API call.
 */
public class VehicleStub implements Serializable {

    /**
     * The URI which uniquely identifies this train across time and transport providers.
     */
    @Nullable
    private final String uri;

    /**
     * The ID of the train, relative to the public transport provider. For example IC538.
     */
    @NonNull
    protected String id;

    /**
     * The headsign of the train, which indicates the destination of the train to end users.
     * This is the preferred way to communicate the train in an understandable way to the end user.
     */
    @NonNull
    protected String headsign;

    // Direction is required, since we need to display something
    public VehicleStub(@NonNull String id, @NonNull String headSign, @Nullable String uri) {
        this.id = id.toUpperCase();
        this.headsign = headSign;
        this.uri = uri;
    }

    /**
     * The ID, for example IC4516
     *
     * @return ID, for example IC4516
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
    public String getHeadsign() {
        return headsign;
    }

    /**
     * Human-readable name, for example IC 4516
     *
     * @return Human-readable name
     */
    @NonNull
    public String getName() {
        return getVehicleName(id);
    }

    @NonNull
    public static String getVehicleName(@NonNull String id) {
        return getVehicleClass(id) + " " + getVehicleNumber(id);
    }

    /**
     * Semantic ID, for example http://irail.be/vehicle/IC4516
     *
     * @return Semantic ID
     */
    @NonNull
    public String getSemanticId() {
        if (uri != null) {
            return uri;
        }
        // Calculate if unknown
        return "http://irail.be/vehicle/" + getId();
    }

    /**
     * Vehicle type, for example S, IC, L, P
     *
     * @return The type of this train
     */
    @NonNull
    public String getType() {
        return getVehicleClass(getId());
    }

    /**
     * Route/trip class, for example S, IC, L, P
     *
     * @return The route/trip type for this vehicle
     */
    @NonNull
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
    @NonNull
    public String getNumber() {
        return getVehicleNumber(getId());
    }

    /**
     * Deduct the number of a vehicle from its ID
     *
     * @param vehicleId The ID of a vehicle, e.g. IC538
     * @return The number of a vehicle, e.g. 538
     */
    @NonNull
    public static String getVehicleNumber(@NonNull String vehicleId) {
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
