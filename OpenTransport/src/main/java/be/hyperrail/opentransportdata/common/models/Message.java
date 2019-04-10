/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.common.models;

import java.io.Serializable;

/**
 * An alert or remark message
 */
public interface Message extends Serializable {

    /**
     * The header, caption of the message.
     *
     * @return The header, caption of the message.
     */
    String getHeader();

    /**
     * The description, detailled text of the message.
     *
     * @return The description, detailled text of the message
     */
    String getDescription();

    /**
     * A link leading to a webpage which contains more information.
     *
     * @return A link leading to a webpage which contains more information.
     */
    String getLink();
}
