/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.persistence;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import be.hyperrail.android.irail.implementation.TrainStub;

public class TrainSuggestion extends TrainStub implements Suggestable {

    Date created_at;

    TrainSuggestion() {
        super(null,null);
        created_at = new Date();
    }

    public TrainSuggestion(TrainStub trainStub) {
        super(trainStub.getId(),null);
        this.created_at = new Date();
    }

    @Override
    public JSONObject serialize() throws JSONException {
        JSONObject json= new JSONObject();
        json.put("id",this.id);
        json.put("created_at",created_at.getTime());
                return json;
    }

    @Override
    public void deserialize(JSONObject json) throws JSONException {
        this.created_at = new Date(json.getLong("created_at"));
        this.id = json.getString("id");
    }

    @Override
    public String getSortingName() {
        return getId();
    }

    @Override
    public Date getSortingDate() {
        return created_at;
    }

    @Override
    public boolean equals(JSONObject json) throws JSONException {
        return (json.getString("id").equals(this.id));
    }

    public boolean equals(Object o){
        if (o instanceof TrainStub){
            return ((TrainStub) o).getId().equals(this.getId());
        }
        return false;
    }
}