/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.irail.implementation;

import org.junit.Before;
import org.junit.Test;

import be.hyperrail.android.irail.contracts.IrailDataResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test API responses to ensure all fields are correct
 */
public class ApiResponseTest {

    private IrailDataResponse<String> correct;
    private IrailDataResponse<String> cached;
    private IrailDataResponse<String> offline;

    @Before
    public void setup() {
        correct = new ApiResponse<>("ok", false, false);
        cached = new ApiResponse<>("cached", true, false);
        offline = new ApiResponse<>("offline", false, true);
    }

    @Test
    public void getData() throws Exception {
        assertEquals("ok", correct.getData());
        assertEquals("cached", cached.getData());
        assertEquals("offline", offline.getData());
    }

    @Test
    public void getTime() throws Exception {
        assertTrue(correct.getTime().isBeforeNow() || correct.getTime().isEqualNow());
        assertTrue(cached.getTime().isBeforeNow() || cached.getTime().isEqualNow());
        assertTrue(offline.getTime().isBeforeNow() || offline.getTime().isEqualNow());
    }

    @Test
    public void isOffline() throws Exception {
        assertEquals(false, correct.isOffline());
        assertEquals(false, cached.isOffline());
        assertEquals(true, offline.isOffline());
    }

    @Test
    public void isCached() throws Exception {
        assertEquals(false, correct.isCached());
        assertEquals(true, cached.isCached());
        assertEquals(false, offline.isCached());
    }

}