/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package android.irail.be.hyperrail.irail.exception;

/**
 * Error thrown when no data can be retrieved
 */
public class NetworkDisconnectedException extends Exception {

    public NetworkDisconnectedException(String url) {
        super("No or empty response received from the network.\n" +
                "URL: " + url);
    }
}
