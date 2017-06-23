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

package be.hyperrail.android.util;

import java.util.Arrays;

/**
 * Utility class for array operations
 */
public class ArrayUtils {

    /**
     * Concatenate 2 arrays
     * @param firstArray The first array
     * @param secondArray The second array
     * @param <T> The type of the array elements
     * @return The concatenated array
     */
    public static <T> T[] concatenate(T[] firstArray, T[] secondArray){
        if (firstArray == null){
            return secondArray;
        }
        if (secondArray == null){
            return firstArray;
        }

        T[] result = Arrays.copyOf(firstArray, firstArray.length + secondArray.length);
        System.arraycopy(secondArray, 0, result, firstArray.length, secondArray.length);
        return result;
    }
}
