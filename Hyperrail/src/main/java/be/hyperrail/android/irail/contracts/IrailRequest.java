/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.irail.contracts;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * A request for API data
 */

public interface IrailRequest extends Serializable {

    /**
     * The date this search was created at
     *
     * @return
     */
    DateTime getCreatedAt();

    /**
     * A JSON String representation of this request
     *
     * @return
     */
    JSONObject toJson() throws JSONException;

}
