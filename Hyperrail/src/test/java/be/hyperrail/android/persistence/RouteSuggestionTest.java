/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.persistence;

import org.json.JSONObject;
import org.junit.Test;

import be.hyperrail.android.irail.db.Station;

import static org.junit.Assert.assertEquals;

/**
 * Created by Bert on 28-11-2017.
 */
public class RouteSuggestionTest {
    Station station1 = new Station("BE.NMBS.1", "Brussels-south", "Brussel-zuid", "fr", "de", "en", "Brussel-zuid", "BE", 1, 2, 3);
    Station station2 = new Station("BE.NMBS.2", "Ghent", "Gent", "Gand", "Gent", "Ghent", "Gent", "BE", 1, 2, 3);

    @Test
    public void serialize() throws Exception {
        RouteSuggestion s = new RouteSuggestion(station1,station2);
        JSONObject serial = new JSONObject();
        serial.put("created_at", s.created_at.getTime());
        serial.put("from", station1.getId());
        serial.put("to", station2.getId());
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
    }

    @Test
    public void equals() throws Exception {
    }

    @Test
    public void equals1() throws Exception {
    }

}