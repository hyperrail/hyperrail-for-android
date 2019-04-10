/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.be.experimental.linkedconnections;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

/**
 * A list of LinkedConnection objects, which can be parsed using the LoganSquare parser.
 */

@JsonObject
public class LinkedConnections {
    @JsonField(name = "@id")
    String current;
    @JsonField(name = "hydra:previous")
    String previous;
    @JsonField(name = "hydra:next")
    String next;
    @JsonField(name = "@graph")
    LinkedConnection[] connections;
}
