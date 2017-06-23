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
 * Error thrown when no result can be loaded due to lack of results
 */
public class NotFoundException extends Exception {
    public NotFoundException(String url, String data) {
        super("The query could not be found.\n" +
                "URL: " + url + "\n" +
                "Data:" + data);
    }
}
