/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.persistence;

import org.joda.time.DateTime;
import org.json.JSONObject;
import org.junit.Test;

import be.hyperrail.android.irail.db.Station;
import be.hyperrail.android.irail.implementation.VehicleStub;

import static org.junit.Assert.assertTrue;

public class TrainSuggestionTest {

    Station station1 = new Station("BE.NMBS.1", "Brussels", "Brussel", "fr", "de", "en", "Brussel", "BE", 1, 2, 3);
    VehicleStub train = new VehicleStub("IC537",station1,"http://uri");
    TrainSuggestion s = new TrainSuggestion(train);

    @Test
    public void serialize() throws Exception {
        JSONObject serial = new JSONObject();
        serial.put("created_at", s.created_at.getMillis());
        serial.put("id", train.getId());
        serial.put("direction", station1.getId());
        assertEquals(serial.toString(), s.serialize().toString());
    }

    @Test
    public void deserialize() throws Exception {
        // Can't be tested without context
    }

    @Test
    public void getSortingName() throws Exception {
    }

    @Test
    public void getSortingDate() throws Exception {
        assertTrue(DateTime.now().getMillis() - s.getSortingDate().getMillis() < 60000);
    }

    @Test
    public void equals() throws Exception {
    }

    @Test
    public void equals1() throws Exception {
    }

}