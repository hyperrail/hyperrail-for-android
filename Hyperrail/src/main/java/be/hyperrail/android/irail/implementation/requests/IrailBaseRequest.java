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

package be.hyperrail.android.irail.implementation.requests;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import be.hyperrail.android.irail.contracts.IrailRequest;


/**
 * A base class for IrailRequest implementations
 */

public abstract class IrailBaseRequest implements IrailRequest {

    private DateTime createdAt;

    protected IrailBaseRequest() {
        this.createdAt = new DateTime();
    }

    protected IrailBaseRequest(JSONObject json) throws JSONException {
        this.createdAt = new DateTime(json.getLong("created_at"));
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("created_at", createdAt.getMillis());
        return json;
    }

    public DateTime getCreatedAt() {
        return createdAt;
    }
}
