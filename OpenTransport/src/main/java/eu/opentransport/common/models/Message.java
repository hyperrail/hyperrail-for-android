/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package eu.opentransport.common.models;

import java.io.Serializable;

/**
 * An alert or remark message
 */
public interface Message extends Serializable {

    String getHeader();

    String getDescription();

    String getLink();
}