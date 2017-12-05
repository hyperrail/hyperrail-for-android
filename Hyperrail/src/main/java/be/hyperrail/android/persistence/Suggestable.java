/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.persistence;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

public interface Suggestable {

    JSONObject serialize() throws JSONException;

    void deserialize(JSONObject json) throws JSONException;

    String getSortingName();

    DateTime getSortingDate();

    boolean equals(JSONObject json) throws JSONException;
}
