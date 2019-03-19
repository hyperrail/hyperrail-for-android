/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.common.contracts;

/**
 * Define search times as either time of departure, or time of arrival.
 */
public enum QueryTimeDefinition {
    /**
     * DEPART_AT indicates that a query is made to find results where the departure is at or after the given time.
     */
    DEPART_AT,

    /**
     * ARRIVE_AT indicates that a query is made to find results where the departure is at or before the given time.
     */
    ARRIVE_AT
}

