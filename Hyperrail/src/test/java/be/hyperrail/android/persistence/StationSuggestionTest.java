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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StationSuggestionTest {

    Station station1 = new Station("BE.NMBS.1", "Brussels", "Brussel", "fr", "de", "en", "Brussel", "BE", 1, 2, 3);

    StationSuggestion s = new StationSuggestion(station1);

    @Test
    public void serialize() throws Exception {
        JSONObject serial = new JSONObject();
        serial.put("created_at", s.created_at.getMillis());
        serial.put("id", station1.getId());
        assertEquals(serial.toString(), s.serialize().toString());
    }

    @Test
    public void deserialize() throws Exception {
        // Can't be tested without context
    }

    @Test
    public void getSortingName() throws Exception {
        assertEquals(station1.getLocalizedName(), s.getSortingName());
    }

    @Test
    public void getSortingDate() throws Exception {
        assertTrue(DateTime.now().getMillis() - s.getSortingDate().getMillis() < 60000);
    }

}