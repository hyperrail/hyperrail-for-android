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

import java.io.Serializable;
import java.util.Date;

/**
 * Listener for {@link DateTimePicker}
 */
public interface OnDateTimeSetListener extends Serializable {

    /**
     * Callback which is called when a user filled both a date and time in a DateTimePicker
     * @param d the resulting date object
     */
    void onDateTimePicked(Date d);
}
