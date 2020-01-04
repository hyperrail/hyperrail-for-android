/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */


package be.hyperrail.opentransportdata.common.models;

import org.joda.time.DateTime;

import java.io.Serializable;

/**
 * A disturbance on the rail network
 */
public interface Disturbance extends Serializable {
    /**
     * Get a unique id for this disturbance.
     *
     * @return A unique id for this disturbance.
     */
    int getId();

    /**
     * Get the title for this disturbance.
     *
     * @return The title for this disturbance.
     */
    String getTitle();

    /**
     * Get the description for this disturbance.
     *
     * @return The description for this disturbance.
     */
    String getDescription();

    /**
     * Get a link leading to a webpage with more information about the disturbance.
     *
     * @return A link leading to a webpage with more information about the disturbance.
     */
    String getLink();

    /**
     * Get the time at which this disturbance was reported.
     *
     * @return The time at which this disturbance was reported.
     */
    DateTime getTime();

    Type getType();

    public enum Type {
        PLANNED,
        DISTURBANCE
    }
}
