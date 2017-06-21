/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package android.irail.be.hyperrail.persistence;

import java.util.Date;

public class RouteQuery {
    public String from, to;
    public RouteQueryType type;

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

    public RouteQuery(String from, String to) {
        this.from = from;
        this.to = to;
        this.created_at = new Date();
    }
}