/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */


package be.hyperrail.opentransportdata.common.models;

/**
 * How the arrival or departure of a vehicle at a vehiclestop is coordinated
 */
public enum VehicleStopCoordinationType {
    /**
     * The vehicle always stops
     */
    ALWAYS,
    /**
     * Passengers need to coordinate with the driver
     */
    COORDINATE_WITH_DRIVER,
    /**
     * The vehicle never stops
     */
    NEVER,
}
