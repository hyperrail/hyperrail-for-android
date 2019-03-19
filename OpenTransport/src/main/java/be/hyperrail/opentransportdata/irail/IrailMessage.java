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

package be.hyperrail.opentransportdata.irail;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

import be.hyperrail.opentransportdata.common.models.Message;

/**
 * An alert or remark message
 */
public class IrailMessage implements Message, Serializable {
    private String header;
    private String description;
    private String link;

    public IrailMessage(JSONObject json) {
        try {
            this.header = json.getString("header");
            this.description = json.getString("description");
            if (json.has("link")) {
                this.link = json.getString("link");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
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
