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

package be.hyperrail.opentransportdata.be.irail;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import be.hyperrail.opentransportdata.common.exceptions.StopLocationNotResolvedException;
import be.hyperrail.opentransportdata.common.models.StopLocation;

import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class StationsDbInstrumentedTest {

    IrailStationsDataProvider provider = new IrailStationsDataProvider(InstrumentationRegistry.getTargetContext());

    @Test
    public void testStationSearch() throws StopLocationNotResolvedException {
        //008811437 	Bosvoorde/Boitsfort 	Boitsfort 	Bosvoorde 			be 	4.408112 	50.794698 	31.947976878613
        StopLocation bosvoorde = provider.getStoplocationByHafasId("008811437");
        Assert.assertNotNull(bosvoorde);
        assertEquals("008811437", bosvoorde.getHafasId());
        assertEquals("Bosvoorde/Boitsfort", bosvoorde.getName());
        assertEquals("Bosvoorde", bosvoorde.getTranslations().get("nl_BE"));
        assertEquals("Boitsfort", bosvoorde.getTranslations().get("fr_BE"));
        assertEquals("", bosvoorde.getTranslations().get("en_UK"));
        assertEquals("", bosvoorde.getTranslations().get("de_DE"));
        assertEquals("be", bosvoorde.getCountryCode());
        assertEquals(4.408112, bosvoorde.getLongitude(), 0.0000001);
        assertEquals(50.794698, bosvoorde.getLatitude(), 0.0000001);
        // This value changes frequently
        Assert.assertTrue(bosvoorde.getAvgStopTimes() > 1);

        // Test for caps variations, different languages, and diferent separator symbols
        String searchnames[] = new String[]{"Bosvoorde", "Boitsfort", "Bosvoorde/Boitsfort",
                "Bosvoorde Boitsfort", "Bosvoorde-Boitsfort", "BOSVOORDE"};
        for (String name : searchnames) {
            StopLocation searchResult = provider.getStoplocationByExactName(name);
            Assert.assertNotNull(searchResult);
            assertEquals(bosvoorde.getHafasId(), searchResult.getHafasId());
        }
        // 008866001 	Arlon 		Aarlen 	Arel
        StopLocation arlon = provider.getStoplocationByHafasId("008866001");
        Assert.assertNotNull(arlon);
        assertEquals("008866001", arlon.getHafasId());
        assertEquals("Arlon", arlon.getName());

        // http://irail.be/stations/NMBS/008815016 	Thurn en Taxis/Tour et Taxis 	Tour et Taxis 	Thurn en Taxis 	Tour et Taxis 	Thurn en Taxis
        StopLocation thurnTaxis = provider.getStoplocationByHafasId("008815016");
        Assert.assertNotNull(thurnTaxis);
        assertEquals(thurnTaxis, provider.getStoplocationByExactName(thurnTaxis.getName()));
        assertEquals(thurnTaxis, provider.getStoplocationByExactName(thurnTaxis.getTranslations().get("de_DE")));
        assertEquals(thurnTaxis, provider.getStoplocationByExactName(thurnTaxis.getTranslations().get("en_UK")));
        assertEquals(thurnTaxis, provider.getStoplocationByExactName(thurnTaxis.getTranslations().get("fr_BE")));
        assertEquals(thurnTaxis, provider.getStoplocationByExactName(thurnTaxis.getTranslations().get("nl_BE")));

    }

    /**
     * Test station searches which caused crashes in earlier versions
     */
    @Test
    public void StationNamesRegressionTest() throws StopLocationNotResolvedException {
        // 's gravenbrakel caused issues due to the '
        StopLocation sGravenbrakel = provider.getStoplocationByHafasId("008883006");
        Assert.assertNotNull(sGravenbrakel);
        assertEquals(sGravenbrakel, provider.getStoplocationByExactName("'s Gravenbrakel"));
        assertEquals(sGravenbrakel, provider.getStoplocationByExactName("'s Gravenbrakel (be)"));
        assertEquals(sGravenbrakel, provider.getStoplocationByExactName("Braine-le-Comte"));
        assertEquals(sGravenbrakel, provider.getStoplocationByExactName("Braine-le-Comte/s Gravenbrakel"));
        assertEquals(sGravenbrakel, provider.getStoplocationByExactName("Braine-le-Comte/'s Gravenbrakel"));
        assertEquals(sGravenbrakel, provider.getStoplocationByExactName("'s Gravenbrakel/Braine-le-Comte"));
        assertEquals(sGravenbrakel, provider.getStoplocationByExactName("s Gravenbrakel/Braine-le-Comte"));
        assertEquals(sGravenbrakel, provider.getStoplocationByExactName("s Gravenbrakel"));
        assertEquals(sGravenbrakel, provider.getStoplocationByExactName("s-gravenbrakel"));
    }

    /**
     * Ensure all methods which query based on ID return the right result
     */
    @Test
    public void StationIdsTest() throws StopLocationNotResolvedException {
        StopLocation zuid = provider.getStoplocationByHafasId("008814001");
        assertEquals(zuid, provider.getStoplocationBySemanticId("http://irail.be/stations/NMBS/008814001"));
    }
}
