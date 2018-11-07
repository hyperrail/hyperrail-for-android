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

package eu.opentransport.irail;

import org.joda.time.LocalTime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created in be.hyperrail.android.irail.db on 22/04/2018.
 */
public class StationFacilitiesTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void getOpeningHours() {
        LocalTime[][] hours = new LocalTime[7][2];
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 2; j++) {
                hours[i][j] = new LocalTime(i, j);
            }
        }
        StationFacilities facilities = new StationFacilities(hours, "street", "zip", "city",
                                                             false, false, false,
                                                             false, false, false, false, false,
                                                             false, false, false, 0,
                                                             false, false, false,
                                                             false, false);
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 2; j++) {
                assertEquals(hours[i][j], facilities.getOpeningHours(i)[j]);
            }
        }
    }

    @Test
    public void getStreetZipCity() {
        StationFacilities facilities = new StationFacilities(new LocalTime[7][0], "street", "zip", "city",
                                                             false, false, false,
                                                             false, false, false, false, false,
                                                             false, false, false, 0,
                                                             false, false, false,
                                                             false, false);
        assertEquals("street", facilities.getStreet());
        assertEquals("zip", facilities.getZip());
        assertEquals("city", facilities.getCity());
    }

    @Test
    public void hasTicketVendingMachines() {
    }

    @Test
    public void hasLuggageLockers() {
    }

    @Test
    public void hasFreeParking() {
    }

    @Test
    public void hasBlue_bike() {
        StationFacilities facilitiesTrue = new StationFacilities(new LocalTime[7][0], "street", "zip", "city",
                                                                 false, false, false,
                                                                 true, false, false, false, false,
                                                                 false, false, false, 0,
                                                                 false, false, false,
                                                                 false, false);
        Assert.assertTrue(facilitiesTrue.hasBlue_bike());
        StationFacilities facilitiesFalse = new StationFacilities(new LocalTime[7][0], "street", "zip", "city",
                                                                  true, true, true,
                                                                  false, true, true, true, true,
                                                                  true, true, true, 0,
                                                                  true, true, true,
                                                                  true, true);
        Assert.assertFalse(facilitiesFalse.hasBlue_bike());
    }

    @Test
    public void hasBike() {
        StationFacilities facilitiesTrue = new StationFacilities(new LocalTime[7][0], "street", "zip", "city",
                                                                 false, false, false,
                                                                 false, true, false, false, false,
                                                                 false, false, false, 0,
                                                                 false, false, false,
                                                                 false, false);
        Assert.assertTrue(facilitiesTrue.hasBike());
        StationFacilities facilitiesFalse = new StationFacilities(new LocalTime[7][0], "street", "zip", "city",
                                                                  true, true, true,
                                                                  true, false, true, true, true,
                                                                  true, true, true, 0,
                                                                  true, true, true,
                                                                  true, true);
        Assert.assertFalse(facilitiesFalse.hasBike());
    }

    @Test
    public void hasTaxi() {
    }

    @Test
    public void hasBus() {
    }

    @Test
    public void hasTram() {
    }

    @Test
    public void hasMetro() {
    }

    @Test
    public void isWheelchair_available() {
    }

    @Test
    public void hasRamp() {
    }

    @Test
    public void isElevated_platform() {
        StationFacilities facilitiesTrue = new StationFacilities(new LocalTime[7][0], "street", "zip", "city",
                                                                 false, false, false,
                                                                 false, false, false, false, false,
                                                                 false, false, false, 0,
                                                                 true, false, false,
                                                                 false, false);
        Assert.assertTrue(facilitiesTrue.isElevated_platform());
        StationFacilities facilitiesFalse = new StationFacilities(new LocalTime[7][0], "street", "zip", "city",
                                                                  true, true, true,
                                                                  true, true, true, true, true,
                                                                  true, true, true, 0,
                                                                  false, true, true,
                                                                  true, true);
        Assert.assertFalse(facilitiesFalse.isElevated_platform());
    }

    @Test
    public void hasEscalator_up() {
        StationFacilities facilitiesTrue = new StationFacilities(new LocalTime[7][0], "street", "zip", "city",
                                                                 false, false, false,
                                                                 false, false, false, false, false,
                                                                 false, false, false, 0,
                                                                 false, true, false,
                                                                 false, false);
        Assert.assertTrue(facilitiesTrue.hasEscalator_up());
        StationFacilities facilitiesFalse = new StationFacilities(new LocalTime[7][0], "street", "zip", "city",
                                                                  true, true, true,
                                                                  true, true, true, true, true,
                                                                  true, true, true, 0,
                                                                  true, false, true,
                                                                  true, true);
        Assert.assertFalse(facilitiesFalse.hasEscalator_up());
    }

    @Test
    public void hasEscalator_down() {
        StationFacilities facilitiesTrue = new StationFacilities(new LocalTime[7][0], "street", "zip", "city",
                                                                 false, false, false,
                                                                 false, false, false, false, false,
                                                                 false, false, false, 0,
                                                                 false, false, true,
                                                                 false, false);
        Assert.assertTrue(facilitiesTrue.hasEscalator_down());
        StationFacilities facilitiesFalse = new StationFacilities(new LocalTime[7][0], "street", "zip", "city",
                                                                  true, true, true,
                                                                  true, true, true, true, true,
                                                                  true, true, true, 0,
                                                                  true, true, false,
                                                                  true, true);
        Assert.assertFalse(facilitiesFalse.hasEscalator_down());
    }

    @Test
    public void hasElevator_platform() {
    }

    @Test
    public void hasHearing_aid_signal() {
    }

    @Test
    public void getDisabled_parking_spots() {
        StationFacilities facilitiesFalse = new StationFacilities(new LocalTime[7][0], "street", "zip", "city",
                                                                  true, true, true,
                                                                  true, true, true, true, true,
                                                                  true, true, true, 5,
                                                                  true, true, true,
                                                                  true, true);
        assertEquals(5, facilitiesFalse.getDisabled_parking_spots());
    }
}