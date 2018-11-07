/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package eu.opentransport.util;

public class StringUtils {
    public static String cleanAccents(String s) {

        if (s == null || s.isEmpty()) {
            return s;
        }

        return s.replaceAll("[ÉÈÊË]", "E")
                .replaceAll("[éÉèÈêÊëË]", "e")
                .replaceAll("[ÂÄ]", "A")
                .replaceAll("[âÂåäÄ]", "a")
                .replaceAll("[Ö]", "O")
                .replaceAll("[öÖø]", "o")
                .replaceAll("[Ü]", "U")
                .replaceAll("[üÜ]", "u");
    }
}
