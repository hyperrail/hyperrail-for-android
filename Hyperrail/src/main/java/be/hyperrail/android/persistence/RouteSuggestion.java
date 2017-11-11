/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package be.hyperrail.android.persistence;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import be.hyperrail.android.irail.db.Station;
import be.hyperrail.android.irail.factories.IrailFactory;

public class RouteSuggestion implements Suggestable {

    public Station from, to;
    Date created_at;

    RouteSuggestion() {
        created_at = new Date();
    }

    public RouteSuggestion(Station from, Station to) {
        this.from = from;
        this.to = to;
        this.created_at = new Date();
    }

    @Override
    public JSONObject serialize() throws JSONException {
        JSONObject json= new JSONObject();
        json.put("from",from.getId());
        json.put("to",to.getId());
        json.put("created_at",created_at.getTime());
                return json;
    }

    @Override
    public void deserialize(JSONObject json) throws JSONException {
        this.created_at = new Date(json.getLong("created_at"));
        this.from = IrailFactory.getStationsProviderInstance().getStationById(json.getString("from"));
        this.to = IrailFactory.getStationsProviderInstance().getStationById(json.getString("to"));
    }

    @Override
    public String getSortingName() {
        return from.getLocalizedName() + to.getLocalizedName();
    }

    @Override
    public Date getSortingDate() {
        return created_at;
    }

    @Override
    public boolean equals(JSONObject json) throws JSONException {
        return (json.getString("from").equals(from.getId()) && json.getString("to").equals(to.getId()));
    }

    public boolean equals(Object other){
        if (other == null){
        return false;
    }
        if (other instanceof  RouteSuggestion){
            return ((RouteSuggestion) other).from.getId().equals(this.from.getId()) && ((RouteSuggestion) other).to.getId().equals(this.to.getId());
        }
        return false;
    }
}