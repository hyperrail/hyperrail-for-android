/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.irail.implementation;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DisturbanceTest {

    Disturbance instance;
    DateTime mDateTime = DateTime.now();

    @Before
    public void setUp() {
        instance = new Disturbance(1, mDateTime, "title", "description", "link");
    }

    @Test
    public void getId() {
        assertEquals(1, instance.getId());
    }

    @Test
    public void getTitle() {
        assertEquals("title", instance.getTitle());
    }

    @Test
    public void getDescription() {
        assertEquals("description", instance.getTitle());
    }

    @Test
    public void getLink() {
        assertEquals("link", instance.getTitle());
    }

    @Test
    public void getTime() {
        assertEquals(mDateTime, instance.getTime());
    }
}