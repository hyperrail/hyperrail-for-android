/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.test;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import be.hyperrail.android.irail.contracts.IrailStationProvider;
import be.hyperrail.android.irail.contracts.StationNotResolvedException;
import be.hyperrail.android.irail.db.Station;
import be.hyperrail.android.irail.db.StationsDb;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class StationsDbInstrumentedTest {

    IrailStationProvider provider = new StationsDb(InstrumentationRegistry.getTargetContext());

    @Test
    public void testStationSearch() throws StationNotResolvedException {
        //008811437 	Bosvoorde/Boitsfort 	Boitsfort 	Bosvoorde 			be 	4.408112 	50.794698 	31.947976878613
        Station bosvoorde = provider.getStationByHID("008811437");
        assertNotNull(bosvoorde);
        assertEquals("008811437", bosvoorde.getHafasId());
        assertEquals("Bosvoorde/Boitsfort", bosvoorde.getName());
        assertEquals("Bosvoorde", bosvoorde.getAlternativeNl());
        assertEquals("Boitsfort", bosvoorde.getAlternativeFr());
        assertEquals("", bosvoorde.getAlternativeEn());
        assertEquals("", bosvoorde.getAlternativeDe());
        assertEquals("be", bosvoorde.getCountryCode());
        assertEquals(4.408112, bosvoorde.getLongitude(), 0.0000001);
        assertEquals(50.794698, bosvoorde.getLatitude(), 0.0000001);
        // This value changes frequently
        assertTrue(bosvoorde.getAvgStopTimes() > 1);

        // Test for caps variations, different languages, and diferent separator symbols
        String searchnames[] = new String[]{"Bosvoorde", "Boitsfort", "Bosvoorde/Boitsfort",
                "Bosvoorde Boitsfort", "Bosvoorde-Boitsfort", "BOSVOORDE"};
        for (String name : searchnames) {
            Station searchResult = provider.getStationByExactName(name);
            assertNotNull(searchResult);
            assertEquals(bosvoorde.getHafasId(), searchResult.getHafasId());
        }
        // 008866001 	Arlon 		Aarlen 	Arel
        Station arlon = provider.getStationByHID("008866001");
        assertNotNull(arlon);
        assertEquals("008866001", arlon.getHafasId());
        assertEquals("Arlon", arlon.getName());
        assertEquals("Aarlen", arlon.getAlternativeNl());
        assertEquals("", arlon.getAlternativeFr());
        assertEquals("", arlon.getAlternativeEn());
        assertEquals("Arel", arlon.getAlternativeDe());

        // http://irail.be/stations/NMBS/008815016 	Thurn en Taxis/Tour et Taxis 	Tour et Taxis 	Thurn en Taxis 	Tour et Taxis 	Thurn en Taxis
        Station thurnTaxis = provider.getStationByHID("008815016");
        assertNotNull(thurnTaxis);
        assertEquals(thurnTaxis, provider.getStationByExactName(thurnTaxis.getName()));
        assertEquals(thurnTaxis, provider.getStationByExactName(thurnTaxis.getAlternativeDe()));
        assertEquals(thurnTaxis, provider.getStationByExactName(thurnTaxis.getAlternativeEn()));
        assertEquals(thurnTaxis, provider.getStationByExactName(thurnTaxis.getAlternativeFr()));
        assertEquals(thurnTaxis, provider.getStationByExactName(thurnTaxis.getAlternativeNl()));

    }

    /**
     * Test station searches which caused crashes in earlier versions
     */
    @Test
    public void StationNamesRegressionTest() throws StationNotResolvedException {
        // 's gravenbrakel caused issues due to the '
        Station sGravenbrakel = provider.getStationByHID("008883006");
        assertNotNull(sGravenbrakel);
        assertEquals(sGravenbrakel, provider.getStationByExactName("'s Gravenbrakel"));
        assertEquals(sGravenbrakel, provider.getStationByExactName("'s Gravenbrakel (be)"));
        assertEquals(sGravenbrakel, provider.getStationByExactName("Braine-le-Comte"));
        assertEquals(sGravenbrakel, provider.getStationByExactName("Braine-le-Comte/s Gravenbrakel"));
        assertEquals(sGravenbrakel, provider.getStationByExactName("Braine-le-Comte/'s Gravenbrakel"));
        assertEquals(sGravenbrakel, provider.getStationByExactName("'s Gravenbrakel/Braine-le-Comte"));
        assertEquals(sGravenbrakel, provider.getStationByExactName("s Gravenbrakel/Braine-le-Comte"));
        assertEquals(sGravenbrakel, provider.getStationByExactName("s Gravenbrakel"));
        assertEquals(sGravenbrakel, provider.getStationByExactName("s-gravenbrakel"));
    }

    /**
     * Ensure all methods which query based on ID return the right result
     */
    @Test
    public void StationIdsTest() throws StationNotResolvedException {
        Station zuid = provider.getStationByHID("008814001");
        assertEquals(zuid, provider.getStationByUIC("8814001"));
        assertEquals(zuid, provider.getStationByUri("http://irail.be/stations/NMBS/008814001"));
    }
}
