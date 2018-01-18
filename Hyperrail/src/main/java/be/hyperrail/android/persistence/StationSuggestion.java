/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.persistence;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import be.hyperrail.android.irail.db.Station;
import be.hyperrail.android.irail.factories.IrailFactory;

public class StationSuggestion extends Station implements Suggestable {

    DateTime created_at;

    // Empty constructor required!
    StationSuggestion() {

    }

    public StationSuggestion(Station s) {
        created_at = new DateTime();
        copy(s);
    }

    @Override
    public JSONObject serialize() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", this.id);
        json.put("created_at", this.created_at.getMillis());
        return json;
    }

    @Override
    public void deserialize(JSONObject json) throws JSONException {
        this.created_at = new DateTime(json.getLong("created_at"));
        Station s = IrailFactory.getStationsProviderInstance().getStationById(json.getString("id"));
        copy(s);
    }

    @Override
    public String getSortingName() {
        return localizedName;
    }

    @Override
    public DateTime getSortingDate() {
        return created_at;
    }

    @Override
    public boolean equals(JSONObject json) throws JSONException {
        return json.getString("id").equals(this.id);
    }

    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof Station) {
            return (((Station) o).getId().equals(this.getId()));
        }
        return false;
    }

}
