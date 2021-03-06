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

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.common.models.implementation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import be.hyperrail.opentransportdata.common.models.StopLocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class StopLocationTest {

    @Test
    void testStopLocations() {
        StopLocation station1 = new StopLocationImpl("1","http://irail.be/stations/NMBS/1", "Brussels", getTranslationMap("Brussel", "fr", "de", "en"), "Brussel", "BE", 1, 2, 3);
        StopLocation station2 = new StopLocationImpl("1","http://irail.be/stations/NMBS/1", "Brussels-south", getTranslationMap("Brussel-zuid", "fr", "de", "en"), "Brussel-zuid", "BE", 1, 2, 3);
        StopLocationImpl station3 = new StopLocationImpl("2","http://irail.be/stations/NMBS/2", "Ghent", getTranslationMap("Gent", "Gand", "Gent", "Ghent"), "Gent", "BE", 1, 2, 3);
        assertEquals(station1, station2);
        assertNotEquals(station1, station3);
        assertNotEquals(station2, station3);

        assertEquals("1", station1.getHafasId());
        assertEquals("Brussels", station1.getName());
        assertEquals("Brussel", station1.getLocalizedName());
        assertEquals("Brussels-south", station2.getName());
        assertEquals("Brussel-zuid", station2.getLocalizedName());
        assertEquals(1, station2.getLatitude(), 0.00000001);
        assertEquals(2, station2.getLongitude(), 0.00000001);
        assertEquals(3, station2.getAvgStopTimes(), 0.00000001);

        assertEquals("http://irail.be/stations/NMBS/1", station1.getSemanticId());

        station3 = new StopLocationImpl(station1);
        assertEquals(station1.getHafasId(), station3.getHafasId());
        assertEquals(station1.getName(), station3.getName());
        assertEquals(station1.getTranslations().get("nl_BE"), station3.getTranslations().get("nl_BE"));
        assertEquals(station1.getTranslations().size(), station3.getTranslations().size());
        assertEquals(station1.getLocalizedName(), station3.getLocalizedName());
        assertEquals(station1.getCountryCode(), station3.getCountryCode());
        assertEquals(station1.getLongitude(), station3.getLongitude(), 0.00000001);
        assertEquals(station1.getLatitude(), station3.getLatitude(), 0.00000001);
        assertEquals(station1.getAvgStopTimes(), station3.getAvgStopTimes(), 0.00000001);
        assertEquals(station1.getSemanticId(), station3.getSemanticId());
    }

    private Map<String, String> getTranslationMap(String nl, String fr, String de, String en) {
        Map<String, String> map = new HashMap<>();
        map.put("nl_BE", nl);
        map.put("fr_BE", fr);
        map.put("de_DE", de);
        map.put("en_UK", en);
        return map;
    }

    @Test
    void testStopLocationInvalidId() {
        // With BE.NMBS. prefix
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> {
                    new StopLocationImpl("BE.NMBS.000000001", "http://irail.be/stations/NMBS/000000001","Brussels", getTranslationMap("Brussel", "fr", "de", "en"), "Brussel", "BE", 1, 2, 3);
                }
        );

    }
}