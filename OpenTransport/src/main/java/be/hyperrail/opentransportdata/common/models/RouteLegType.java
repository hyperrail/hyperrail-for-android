/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.common.models;

/**
 * The type of transport for a leg on a route
 */
public enum RouteLegType {
    WALK,
    CYCLE,
    BUS,
    TRAM,
    METRO,
    TRAIN
}
