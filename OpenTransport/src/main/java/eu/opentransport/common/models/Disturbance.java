/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */


package eu.opentransport.common.models;

import org.joda.time.DateTime;

import java.io.Serializable;

/**
 * A disturbance on the rail network
 */
public interface Disturbance extends Serializable {
    int getId();

    String getTitle();

    String getDescription();

    String getLink();

    DateTime getTime();
}
