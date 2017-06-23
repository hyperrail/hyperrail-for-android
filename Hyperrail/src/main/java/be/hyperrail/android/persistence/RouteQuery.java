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

package be.hyperrail.android.persistence;

import java.util.Date;

import be.hyperrail.android.irail.db.Station;

public class RouteQuery {

    public Station from, to;
    public RouteQueryType type;
    public String fromName, toName;
    Date created_at;

    public enum RouteQueryType {
        UNSET,
        RECENT_ROUTE,
        FAVORITE_ROUTE,
        RECENT_STATION,
        FAVORITE_STATION
    }

    public RouteQuery() {
        created_at = new Date();
    }

    public RouteQuery(Station from, Station to) {
        this.from = from;
        this.to = to;
        this.created_at = new Date();
        this.fromName = from.getLocalizedName();
        if (to == null) {
            toName = "";
        } else {
            this.toName = to.getLocalizedName();
        }
    }
}