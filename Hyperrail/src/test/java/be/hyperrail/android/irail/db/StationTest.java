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

package be.hyperrail.android.irail.db;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class StationTest {

    @Test
    public void testStations() throws Exception {
        Station station1 = new Station("BE.NMBS.1", "Brussels", "Brussel", "fr", "de", "en", "Brussel", "BE", 1, 2, 3);
        Station station2 = new Station("BE.NMBS.1", "Brussels-south", "Brussel-zuid", "fr", "de", "en", "Brussel-zuid", "BE", 1, 2, 3);
        Station station3 = new Station("BE.NMBS.2", "Ghent", "Gent", "Gand", "Gent", "Ghent", "Gent", "BE", 1, 2, 3);
        assertEquals(station1, station2);
        assertNotEquals(station1, station3);
        assertNotEquals(station2, station3);

        assertEquals("BE.NMBS.1", station1.getId());
        assertEquals("Brussels", station1.getName());
        assertEquals("Brussel", station1.getLocalizedName());
        assertEquals("Brussels-south", station2.getName());
        assertEquals("Brussel-zuid", station2.getLocalizedName());
        assertEquals(1, station2.getLatitude(), 0);
        assertEquals(2, station2.getLongitude(), 0);
        assertEquals(3, station2.getAvgStopTimes(), 0);

        assertEquals("http://irail.be/stations/NMBS/1", station1.getSemanticId());

        station3.copy(station1);
        assertEquals(station1.getId(),station3.getId());
        assertEquals(station1.getName(),station3.getName());
        assertEquals(station1.getAlternativeEn(),station3.getAlternativeEn());
        assertEquals(station1.getAlternativeNl(),station3.getAlternativeNl());
        assertEquals(station1.getAlternativeFr(),station3.getAlternativeFr());
        assertEquals(station1.getAlternativeDe(),station3.getAlternativeDe());
        assertEquals(station1.getLocalizedName(),station3.getLocalizedName());
        assertEquals(station1.getCountryCode(),station3.getCountryCode());
        assertEquals(station1.getLongitude(),station3.getLongitude(),0);
        assertEquals(station1.getLatitude(),station3.getLatitude(),0);
        assertEquals(station1.getAvgStopTimes(),station3.getAvgStopTimes(),0);
        assertEquals(station1.getSemanticId(),station3.getSemanticId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStationInvalidId() throws Exception {
        // Without BE.NMBS. prefix
        Station station1 = new Station("1", "Brussels", "Brussel", "fr", "de", "en", "Brussel", "BE", 1, 2, 3);
    }
}