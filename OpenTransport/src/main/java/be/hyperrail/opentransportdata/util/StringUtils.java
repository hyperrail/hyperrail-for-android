/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.util;

public class StringUtils {
    public static String cleanAccents(String s) {

        if (s == null || s.isEmpty()) {
            return s;
        }

        return s.replaceAll("[ÉÈÊË]", "E")
                .replaceAll("[éèêë]", "e")
                .replaceAll("[ÂÄÅ]", "A")
                .replaceAll("[âåä]", "a")
                .replaceAll("[ÖØ]", "O")
                .replaceAll("[öø]", "o")
                .replaceAll("[Ü]", "U")
                .replaceAll("[ü]", "u");
    }
}
