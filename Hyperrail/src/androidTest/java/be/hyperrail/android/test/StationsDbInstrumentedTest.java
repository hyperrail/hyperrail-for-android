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
import be.hyperrail.android.irail.db.Station;
import be.hyperrail.android.irail.db.StationsDb;

import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class StationsDbInstrumentedTest {

    IrailStationProvider provider = new StationsDb(InstrumentationRegistry.getTargetContext());

    @Test
    public void testStationSearch(){
        //BE.NMBS.008811437 	Bosvoorde/Boitsfort 	Boitsfort 	Bosvoorde 			be 	4.408112 	50.794698 	31.947976878613
        Station bosvoorde = provider.getStationByIrailId("BE.NMBS.008811437");
        assertEquals("BE.NMBS.008811437", bosvoorde.getId());
        assertEquals("Bosvoorde/Boitsfort", bosvoorde.getName());
        assertEquals("Bosvoorde", bosvoorde.getAlternativeNl());
        assertEquals("Boitsfort", bosvoorde.getAlternativeFr());
        assertEquals("", bosvoorde.getAlternativeEn());
        assertEquals("", bosvoorde.getAlternativeDe());
        assertEquals("be", bosvoorde.getCountryCode());
        assertEquals(4.408112, bosvoorde.getLongitude(),0.0000001);
        assertEquals(50.794698, bosvoorde.getLatitude(),0.0000001);
        assertEquals(	31.947976878613, bosvoorde.getAvgStopTimes(),0.0001);

        // Test for caps variations, different languages, and diferent separator symbols
        String searchnames[] = new String[]{"Bosvoorde","Boitsfort","Bosvoorde/Boitsfort","Bosvoorde Boitsfort","Bosvoorde-Boitsfort","BOSVOORDE"};
        for (String name :searchnames) {
            assertEquals(bosvoorde.getId(),provider.getStationByName(name).getId());
        }
        // BE.NMBS.008866001 	Arlon 		Aarlen 	Arel
        Station arlon = provider.getStationByIrailId("BE.NMBS.008866001");
        assertEquals("BE.NMBS.008866001", arlon.getId());
        assertEquals("Arlon", arlon.getName());
        assertEquals("Aarlen", arlon.getAlternativeNl());
        assertEquals("", arlon.getAlternativeFr());
        assertEquals("", arlon.getAlternativeEn());
        assertEquals("Arel", arlon.getAlternativeDe());

    }
}
