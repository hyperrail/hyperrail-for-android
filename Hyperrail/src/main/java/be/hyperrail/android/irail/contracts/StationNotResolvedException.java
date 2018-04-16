/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.irail.contracts;

/**
 * Thrown when a station id could not be resolved to a station
 */
public class StationNotResolvedException extends Exception {
    public StationNotResolvedException(String id) {
        super("Id not found: " + id);
    }
}
