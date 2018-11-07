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

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package eu.opentransport.irail;

import android.provider.BaseColumns;

/**
 * Define the database structure for the stations database.
 */
class IrailStationsDataContract {

    static final String SQL_CREATE_INDEX_NAME = " CREATE INDEX stations_station_name ON " + StationsDataColumns.TABLE_NAME + " (" + StationsDataColumns.COLUMN_NAME_NAME + ");";
    static final String SQL_CREATE_INDEX_ID = " CREATE INDEX stations_station_id ON " + StationsDataColumns.TABLE_NAME + " (" + StationsDataColumns._ID + ");";
    static final String SQL_DELETE_TABLE_STATIONS =
            "DROP TABLE IF EXISTS " + StationsDataColumns.TABLE_NAME + ";";

    /**
     * Create table + index for lookup by name. No index for sort since little performance gains.
     */
    static final String SQL_CREATE_TABLE_STATIONS =
            // Stations table
            "CREATE TABLE " + StationsDataColumns.TABLE_NAME + " (" +
                    StationsDataColumns._ID + " TEXT PRIMARY KEY," +
                    StationsDataColumns.COLUMN_NAME_NAME + " TEXT COLLATE NOCASE," +
                    StationsDataColumns.COLUMN_NAME_ALTERNATIVE_NL + " TEXT COLLATE NOCASE," +
                    StationsDataColumns.COLUMN_NAME_ALTERNATIVE_FR + " TEXT COLLATE NOCASE," +
                    StationsDataColumns.COLUMN_NAME_ALTERNATIVE_DE + " TEXT COLLATE NOCASE," +
                    StationsDataColumns.COLUMN_NAME_ALTERNATIVE_EN + " TEXT COLLATE NOCASE," +
                    StationsDataColumns.COLUMN_NAME_COUNTRY_CODE + " TEXT," +
                    StationsDataColumns.COLUMN_NAME_LONGITUDE + " REAL," +
                    StationsDataColumns.COLUMN_NAME_LATITUDE + " REAL," +
                    StationsDataColumns.COLUMN_NAME_AVG_STOP_TIMES + " REAL," +
                    StationsDataColumns.COLUMN_NAME_OFFICIAL_TRANSFER_TIME + " REAL);";

    public static final class StationsDataColumns implements BaseColumns {

        protected static final String TABLE_NAME = "stations";
        protected static final String _ID = "station_id";
        protected static final String COLUMN_NAME_NAME = "name";
        protected static final String COLUMN_NAME_ALTERNATIVE_NL = "alternative_nl";
        protected static final String COLUMN_NAME_ALTERNATIVE_FR = "alternative_fr";
        protected static final String COLUMN_NAME_ALTERNATIVE_DE = "alternative_de";
        protected static final String COLUMN_NAME_ALTERNATIVE_EN = "alternative_en";
        protected static final String COLUMN_NAME_COUNTRY_CODE = "country_code";
        protected static final String COLUMN_NAME_LONGITUDE = "longitude";
        protected static final String COLUMN_NAME_LATITUDE = "latitude";
        protected static final String COLUMN_NAME_AVG_STOP_TIMES = "avg_stop_times";
        protected static final String COLUMN_NAME_OFFICIAL_TRANSFER_TIME = "official_transfer_time";
    }

    private IrailStationsDataContract() {
        // don't instantiate
    }


}
