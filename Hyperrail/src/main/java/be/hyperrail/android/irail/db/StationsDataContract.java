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

package be.hyperrail.android.irail.db;

import android.provider.BaseColumns;

import static be.hyperrail.android.irail.db.StationsDataContract.StationsDataColumns.COLUMN_NAME_ALTERNATIVE_DE;
import static be.hyperrail.android.irail.db.StationsDataContract.StationsDataColumns.COLUMN_NAME_ALTERNATIVE_EN;
import static be.hyperrail.android.irail.db.StationsDataContract.StationsDataColumns.COLUMN_NAME_ALTERNATIVE_FR;
import static be.hyperrail.android.irail.db.StationsDataContract.StationsDataColumns.COLUMN_NAME_ALTERNATIVE_NL;
import static be.hyperrail.android.irail.db.StationsDataContract.StationsDataColumns.COLUMN_NAME_AVG_STOP_TIMES;
import static be.hyperrail.android.irail.db.StationsDataContract.StationsDataColumns.COLUMN_NAME_COUNTRY_CODE;
import static be.hyperrail.android.irail.db.StationsDataContract.StationsDataColumns.COLUMN_NAME_LATITUDE;
import static be.hyperrail.android.irail.db.StationsDataContract.StationsDataColumns.COLUMN_NAME_LONGITUDE;
import static be.hyperrail.android.irail.db.StationsDataContract.StationsDataColumns.COLUMN_NAME_NAME;
import static be.hyperrail.android.irail.db.StationsDataContract.StationsDataColumns.TABLE_NAME;
import static be.hyperrail.android.irail.db.StationsDataContract.StationsDataColumns._ID;

/**
 * Define the database structure for the stations database.
 */
class StationsDataContract {

    private StationsDataContract() {
        // don't instantiate
    }

    public static final class StationsDataColumns implements BaseColumns {
        static final String TABLE_NAME = "stations";
        static final String _ID = "station_id";
        static final String COLUMN_NAME_NAME = "name";
        static final String COLUMN_NAME_ALTERNATIVE_NL = "alternative_nl";
        static final String COLUMN_NAME_ALTERNATIVE_FR = "alternative_fr";
        static final String COLUMN_NAME_ALTERNATIVE_DE = "alternative_de";
        static final String COLUMN_NAME_ALTERNATIVE_EN = "alternative_en";
        static final String COLUMN_NAME_COUNTRY_CODE = "country_code";
        static final String COLUMN_NAME_LONGITUDE = "longitude";
        static final String COLUMN_NAME_LATITUDE = "latitude";
        static final String COLUMN_NAME_AVG_STOP_TIMES = "avg_stop_times";
    }

    /**
     * Create table + index for lookup by name. No index for sort since little performance gains.
     */
    static final String SQL_CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
            _ID + " TEXT PRIMARY KEY," +
            COLUMN_NAME_NAME + " TEXT COLLATE NOCASE," +
            COLUMN_NAME_ALTERNATIVE_NL + " TEXT COLLATE NOCASE," +
            COLUMN_NAME_ALTERNATIVE_FR + " TEXT COLLATE NOCASE," +
            COLUMN_NAME_ALTERNATIVE_DE + " TEXT COLLATE NOCASE," +
            COLUMN_NAME_ALTERNATIVE_EN + " TEXT COLLATE NOCASE," +
            COLUMN_NAME_COUNTRY_CODE + " TEXT," +
            COLUMN_NAME_LONGITUDE + " REAL," +
            COLUMN_NAME_LATITUDE + " REAL," +
            COLUMN_NAME_AVG_STOP_TIMES + " REAL); " +
            "CREATE INDEX index_name ON " + TABLE_NAME + " (" + COLUMN_NAME_NAME + ");";

    static final String SQL_DELETE_TABLE =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

}
