/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.util;

import org.junit.Test;

import static be.hyperrail.android.util.ArrayUtils.concatenate;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotSame;

/**
 * Test type-safe array concatenation
 */
public class ArrayUtilsTest {

    @Test
    public void testConcatenate(){
        String[] mStrings = new String[]{"Test 1","Test 2"};
        String[] mStrings2 = new String[]{"Test 3","Test 4"};

        String[] concat = concatenate(mStrings,mStrings2);
        assertArrayEquals(new String[]{"Test 1","Test 2","Test 3","Test 4"},concat);

        assertNotSame(mStrings,concatenate(mStrings,null));
        assertNotSame(mStrings,concatenate(null,mStrings));
    }
}