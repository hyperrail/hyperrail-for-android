/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.common.exceptions;

import androidx.annotation.NonNull;

/**
 * Thrown when a stop location identifier could not be resolved to a stop location
 */
public class StopLocationNotResolvedException extends Exception {
    public StopLocationNotResolvedException(@NonNull String id) {
        super("Id not found: " + id);
    }
}
