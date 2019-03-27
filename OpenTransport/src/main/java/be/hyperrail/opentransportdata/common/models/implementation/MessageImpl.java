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

package be.hyperrail.opentransportdata.common.models.implementation;

import java.io.Serializable;

import be.hyperrail.opentransportdata.common.models.Message;

/**
 * An alert or remark message
 */
public class MessageImpl implements Message, Serializable {
    private final String header;
    private final String description;
    private final String link;

    public MessageImpl(String header, String description, String link) {
        this.header = header;
        this.description = description;
        this.link = link;
    }

    public String getHeader() {
        return header;
    }

    public String getDescription() {
        return description;
    }

    public String getLink() {
        return link;
    }
}
