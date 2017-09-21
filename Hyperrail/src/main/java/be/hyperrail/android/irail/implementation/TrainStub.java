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

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import be.hyperrail.android.irail.db.Station;

/**
 * Train information, except its stops.
 * This data is typically present in the API without requiring a second API call.
 */
public class TrainStub implements Serializable {

    protected final String id;
    private final Station direction;

    // Direction is required, since we need to display something
    TrainStub(String id, Station direction) {
        this.id = id;
        this.direction = direction;
    }

    /**
     * The ID, for example BE.NMBS.IC4516
     * @return
     */
    public String getId() {
        return id;
    }

    /**
     * The direction (final stop) of this train
     * @return
     */
    public Station getDirection() {
        return direction;
    }

    /**
     * Human-readable name, for example IC 4516
     * @return
     */
    public String getName() {
        return getType() + " " + getNumber();
    }

    /**
     * ID without leading BE.NMBS, for example IC4516
     * @return
     */
    private String getReducedId() {
        return this.id.substring(8);
    }

    /**
     * Semantic ID, for example http://irail.be/vehicle/IC4516
     * @return
     */
    public String getSemanticId() {
        return "http://irail.be/vehicle/" + getReducedId();
    }

    /**
     * Train type, for example S, IC, L, P
     * @return
     */
    public String getType() {
        String rid = getReducedId();
        // S trains are special
        if (rid.startsWith("S")) {
            return rid.substring(0, rid.length() - 4);
        }

        String pattern = "(\\w+?)(\\d+)";

        // Create a Pattern object
        Pattern r = Pattern.compile(pattern);

        // Now create matcher object.
        Matcher m = r.matcher(rid);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }

    /**
     * Train number, for example 4516
     * @return
     */
    public String getNumber() {
        String rid = getReducedId();
        // S trains are special
        if (rid.startsWith("S")) {
            return rid.substring(rid.length() - 4);
        }

        String pattern = "(\\w+?)(\\d+)";

        // Create a Pattern object
        Pattern r = Pattern.compile(pattern);

        // Now create matcher object.
        Matcher m = r.matcher(rid);
        if (m.find()) {
            return m.group(2);
        }
        return "";
    }
}
