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

package be.hyperrail.android.irail.exception;

/**
 * Exception thrown when an invalid response if received, e.g. when fields are missing or in an unexpected format.
 */
public class InvalidResponseException extends Exception {
    public InvalidResponseException(String url, String data) {
        super("The received response was invalid and could not be parsed.\n" +
                "URL: " + url + "\n" +
                "Data:" + data);
    }
}
