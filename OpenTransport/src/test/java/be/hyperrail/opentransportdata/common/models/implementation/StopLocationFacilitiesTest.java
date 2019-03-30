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

import org.joda.time.LocalTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import be.hyperrail.opentransportdata.common.models.StopLocationFacilities;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created in be.hyperrail.android.irail.db on 22/04/2018.
 */
class StopLocationFacilitiesTest {

    @Test
    void getOpeningHours_shouldReturnCorrectValues() {
        LocalTime[][] hours = new LocalTime[7][2];
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 2; j++) {
                hours[i][j] = new LocalTime(i, j);
            }
        }
        StopLocationFacilities facilities = new StopLocationFacilitiesImpl(hours, "street", "zip", "city",
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
    void getStreetZipCity_shouldReturnCorrectValues() {
        StopLocationFacilities facilities = new StopLocationFacilitiesImpl(new LocalTime[7][0], "street", "zip", "city",
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
    void hasTicketVendingMachines_shouldReturnCorrectValues() {
    }

    @Test
    void hasLuggageLockers_shouldReturnCorrectValues() {
    }

    @Test
    void hasFreeParking_shouldReturnCorrectValues() {
    }

    @Test
    void hasBlueBike_shouldReturnCorrectValues_shouldReturnCorrectValues() {
        StopLocationFacilities facilitiesTrue = new StopLocationFacilitiesImpl(new LocalTime[7][0], "street", "zip", "city",
                false, false, false,
                true, false, false, false, false,
                false, false, false, 0,
                false, false, false,
                false, false);
        Assertions.assertTrue(facilitiesTrue.hasBlue_bike());
        StopLocationFacilitiesImpl facilitiesFalse = new StopLocationFacilitiesImpl(new LocalTime[7][0], "street", "zip", "city",
                true, true, true,
                false, true, true, true, true,
                true, true, true, 0,
                true, true, true,
                true, true);
        Assertions.assertFalse(facilitiesFalse.hasBlue_bike());
    }

    @Test
    void hasBike_shouldReturnCorrectValues() {
        StopLocationFacilitiesImpl facilitiesTrue = new StopLocationFacilitiesImpl(new LocalTime[7][0], "street", "zip", "city",
                false, false, false,
                false, true, false, false, false,
                false, false, false, 0,
                false, false, false,
                false, false);
        Assertions.assertTrue(facilitiesTrue.hasBike());
        StopLocationFacilitiesImpl facilitiesFalse = new StopLocationFacilitiesImpl(new LocalTime[7][0], "street", "zip", "city",
                true, true, true,
                true, false, true, true, true,
                true, true, true, 0,
                true, true, true,
                true, true);
        Assertions.assertFalse(facilitiesFalse.hasBike());
    }

    @Test
    void hasTaxi_shouldReturnCorrectValues() {
    }

    @Test
    void hasBus_shouldReturnCorrectValues() {
    }

    @Test
    void hasTram_shouldReturnCorrectValues() {
    }

    @Test
    void hasMetro_shouldReturnCorrectValues() {
    }

    @Test
    void isWheelchair_available_shouldReturnCorrectValues() {
    }

    @Test
    void hasRamp_shouldReturnCorrectValues() {
    }

    @Test
    void isElevated_platform_shouldReturnCorrectValues() {
        StopLocationFacilitiesImpl facilitiesTrue = new StopLocationFacilitiesImpl(new LocalTime[7][0], "street", "zip", "city",
                false, false, false,
                false, false, false, false, false,
                false, false, false, 0,
                true, false, false,
                false, false);
        Assertions.assertTrue(facilitiesTrue.isElevated_platform());
        StopLocationFacilitiesImpl facilitiesFalse = new StopLocationFacilitiesImpl(new LocalTime[7][0], "street", "zip", "city",
                true, true, true,
                true, true, true, true, true,
                true, true, true, 0,
                false, true, true,
                true, true);
        Assertions.assertFalse(facilitiesFalse.isElevated_platform());
    }

    @Test
    void hasEscalator_up_shouldReturnCorrectValues() {
        StopLocationFacilitiesImpl facilitiesTrue = new StopLocationFacilitiesImpl(new LocalTime[7][0], "street", "zip", "city",
                false, false, false,
                false, false, false, false, false,
                false, false, false, 0,
                false, true, false,
                false, false);
        Assertions.assertTrue(facilitiesTrue.hasEscalator_up());
        StopLocationFacilitiesImpl facilitiesFalse = new StopLocationFacilitiesImpl(new LocalTime[7][0], "street", "zip", "city",
                true, true, true,
                true, true, true, true, true,
                true, true, true, 0,
                true, false, true,
                true, true);
        Assertions.assertFalse(facilitiesFalse.hasEscalator_up());
    }

    @Test
    void hasEscalator_down_shouldReturnCorrectValues() {
        StopLocationFacilitiesImpl facilitiesTrue = new StopLocationFacilitiesImpl(new LocalTime[7][0], "street", "zip", "city",
                false, false, false,
                false, false, false, false, false,
                false, false, false, 0,
                false, false, true,
                false, false);
        Assertions.assertTrue(facilitiesTrue.hasEscalator_down());
        StopLocationFacilitiesImpl facilitiesFalse = new StopLocationFacilitiesImpl(new LocalTime[7][0], "street", "zip", "city",
                true, true, true,
                true, true, true, true, true,
                true, true, true, 0,
                true, true, false,
                true, true);
        Assertions.assertFalse(facilitiesFalse.hasEscalator_down());
    }

    @Test
    void hasElevator_platform_shouldReturnCorrectValues() {
    }

    @Test
    void hasHearing_aid_signal_shouldReturnCorrectValues() {
    }

    @Test
    void getDisabled_parking_spots_shouldReturnCorrectValues() {
        StopLocationFacilitiesImpl facilitiesFalse = new StopLocationFacilitiesImpl(new LocalTime[7][0], "street", "zip", "city",
                true, true, true,
                true, true, true, true, true,
                true, true, true, 5,
                true, true, true,
                true, true);
        assertEquals(5, facilitiesFalse.getDisabled_parking_spots());
    }
}