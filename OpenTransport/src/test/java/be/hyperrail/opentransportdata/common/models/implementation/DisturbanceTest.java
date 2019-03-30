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

import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import be.hyperrail.opentransportdata.common.models.Disturbance;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DisturbanceTest {

    private Disturbance instance;
    private DateTime mDateTime = DateTime.now();

    @BeforeEach
    void setUp() {
        instance = new DisturbanceImpl(1, mDateTime, "title", "description", "link");
    }

    @Test
    void getId() {
        assertEquals(1, instance.getId());
    }

    @Test
    void getTitle() {
        assertEquals("title", instance.getTitle());
    }

    @Test
    void getDescription() {
        assertEquals("description", instance.getDescription());
    }

    @Test
    void getLink() {
        assertEquals("link", instance.getLink());
    }

    @Test
    void getTime() {
        assertEquals(mDateTime, instance.getTime());
    }
}