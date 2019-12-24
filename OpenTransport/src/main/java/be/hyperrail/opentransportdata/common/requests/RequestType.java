/*
 *  This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 *  If a copy of the MPL was not distributed with this file,
 *  You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.common.requests;

public enum RequestType {
    LIVEBOARD(0),
    ROUTEPLANNING(1),
    VEHICLEJOURNEY(2),
    VEHICLECOMPOSITION(3),
    DISTURBANCES(4),
    POSTFEEDBACK(5),
    ROUTEDETAIL(6);

    private final int value;

    RequestType(int requestTagValue) {
        this.value = requestTagValue;
    }

    public int getRequestTypeTag() {
        return value;
    }
}
