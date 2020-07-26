/*
 *  This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 *  If a copy of the MPL was not distributed with this file,
 *  You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.be.irail;

public class NmbsTrainType {
    String parentType;
    String subType;
    String orientation;

    NmbsTrainType(String parentType, String subType, String orientation) {
        this.parentType = parentType;
        this.subType = subType;
        this.orientation = orientation;
    }
}

