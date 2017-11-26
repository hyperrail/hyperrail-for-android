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

/**
 * Define the database structure for the stations database.
 */
class StationsDataContract {

    private StationsDataContract() {
        // don't instantiate
    }

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
    }

    public static final class StationFacilityColumns implements BaseColumns {

        protected static final String TABLE_NAME = "facilities";
        protected static final String _ID = "station_id";
        protected static final String COLUMN_STREET = "street";
        protected static final String COLUMN_ZIP = "zip";
        protected static final String COLUMN_CITY = "city";
        protected static final String COLUMN_TICKET_VENDING_MACHINE = "ticket_vending_machine";
        protected static final String COLUMN_LUGGAGE_LOCKERS = "luggage_lockers";
        protected static final String COLUMN_FREE_PARKING = "free_parking";
        protected static final String COLUMN_TAXI = "taxi";
        protected static final String COLUMN_BICYCLE_SPOTS = "bicycle_spots";
        protected static final String COLUMN_BLUE_BIKE = "blue_bike";
        protected static final String COLUMN_BUS = "bus";
        protected static final String COLUMN_TRAM = "tram";
        protected static final String COLUMN_METRO = "metro";
        protected static final String COLUMN_WHEELCHAIR_AVAILABLE = "wheelchair_available";
        protected static final String COLUMN_RAMP = "ramp";
        protected static final String COLUMN_DISABLED_PARKING_SPOTS = "disabled_parking_spots";
        protected static final String COLUMN_ELEVATED_PLATFORM = "elevated_platform";
        protected static final String COLUMN_ESCALATOR_UP = "escalator_up";
        protected static final String COLUMN_ESCALATOR_DOWN = "escalator_down";
        protected static final String COLUMN_ELEVATOR_PLATFORM = "elevator_platform";
        protected static final String COLUMN_HEARING_AID_SIGNAL = "hearing_aid_signal";
        protected static final String COLUMN_SALES_OPEN_MONDAY = "sales_open_monday";
        protected static final String COLUMN_SALES_CLOSE_MONDAY = "sales_close_monday";
        protected static final String COLUMN_SALES_OPEN_TUESDAY = "sales_open_tuesday";
        protected static final String COLUMN_SALES_CLOSE_TUESDAY = "sales_close_tuesday";
        protected static final String COLUMN_SALES_OPEN_WEDNESDAY = "sales_open_wednesday";
        protected static final String COLUMN_SALES_CLOSE_WEDNESDAY = "sales_close_wednesday";
        protected static final String COLUMN_SALES_OPEN_THURSDAY = "sales_open_thursday";
        protected static final String COLUMN_SALES_CLOSE_THURSDAY = "sales_close_thursday";
        protected static final String COLUMN_SALES_OPEN_FRIDAY = "sales_open_friday";
        protected static final String COLUMN_SALES_CLOSE_FRIDAY = "sales_close_friday";
        protected static final String COLUMN_SALES_OPEN_SATURDAY = "sales_open_saturday";
        protected static final String COLUMN_SALES_CLOSE_SATURDAY = "sales_close_saturday";
        protected static final String COLUMN_SALES_OPEN_SUNDAY = "sales_open_sunday";
        protected static final String COLUMN_SALES_CLOSE_SUNDAY = "sales_close_sunday";
    }

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
                    StationsDataColumns.COLUMN_NAME_AVG_STOP_TIMES + " REAL); " +
                    " CREATE INDEX stations_station_name ON " + StationsDataColumns.TABLE_NAME + " (" + StationsDataColumns.COLUMN_NAME_NAME + ");" +
                    " CREATE INDEX stations_station_id ON " + StationsDataColumns.TABLE_NAME + " (" + StationsDataColumns._ID + ");";

    static final String SQL_CREATE_TABLE_FACILITIES =   " CREATE TABLE " + StationFacilityColumns.TABLE_NAME + " (" +
                    StationFacilityColumns._ID + " TEXT PRIMARY KEY," +
                    StationFacilityColumns.COLUMN_STREET + " TEXT," +
                    StationFacilityColumns.COLUMN_ZIP + " TEXT," +
                    StationFacilityColumns.COLUMN_CITY + " TEXT," +
                    StationFacilityColumns.COLUMN_TICKET_VENDING_MACHINE + " NUMERIC," +
                    StationFacilityColumns.COLUMN_LUGGAGE_LOCKERS + " NUMERIC," +
                    StationFacilityColumns.COLUMN_FREE_PARKING + " NUMERIC," +
                    StationFacilityColumns.COLUMN_TAXI + " NUMERIC," +
                    StationFacilityColumns.COLUMN_BICYCLE_SPOTS + " NUMERIC," +
                    StationFacilityColumns.COLUMN_BLUE_BIKE + " NUMERIC," +
                    StationFacilityColumns.COLUMN_BUS + " NUMERIC," +
                    StationFacilityColumns.COLUMN_TRAM + " NUMERIC," +
                    StationFacilityColumns.COLUMN_METRO + " NUMERIC," +
                    StationFacilityColumns.COLUMN_WHEELCHAIR_AVAILABLE + " NUMERIC," +
                    StationFacilityColumns.COLUMN_RAMP + " NUMERIC," +
                    StationFacilityColumns.COLUMN_DISABLED_PARKING_SPOTS + " NUMERIC," +
                    StationFacilityColumns.COLUMN_ELEVATED_PLATFORM + " NUMERIC," +
                    StationFacilityColumns.COLUMN_ESCALATOR_UP + " NUMERIC," +
                    StationFacilityColumns.COLUMN_ESCALATOR_DOWN + " NUMERIC," +
                    StationFacilityColumns.COLUMN_ELEVATOR_PLATFORM + " NUMERIC," +
                    StationFacilityColumns.COLUMN_HEARING_AID_SIGNAL + " NUMERIC," +
                    StationFacilityColumns.COLUMN_SALES_OPEN_MONDAY + " TEXT," +
                    StationFacilityColumns.COLUMN_SALES_CLOSE_MONDAY + " TEXT," +
                    StationFacilityColumns.COLUMN_SALES_OPEN_TUESDAY + " TEXT," +
                    StationFacilityColumns.COLUMN_SALES_CLOSE_TUESDAY + " TEXT," +
                    StationFacilityColumns.COLUMN_SALES_OPEN_WEDNESDAY + " TEXT," +
                    StationFacilityColumns.COLUMN_SALES_CLOSE_WEDNESDAY + " TEXT," +
                    StationFacilityColumns.COLUMN_SALES_OPEN_THURSDAY + " TEXT," +
                    StationFacilityColumns.COLUMN_SALES_CLOSE_THURSDAY + " TEXT," +
                    StationFacilityColumns.COLUMN_SALES_OPEN_FRIDAY + " TEXT," +
                    StationFacilityColumns.COLUMN_SALES_CLOSE_FRIDAY + " TEXT," +
                    StationFacilityColumns.COLUMN_SALES_OPEN_SATURDAY + " TEXT," +
                    StationFacilityColumns.COLUMN_SALES_CLOSE_SATURDAY + " TEXT," +
                    StationFacilityColumns.COLUMN_SALES_OPEN_SUNDAY + " TEXT," +
                    StationFacilityColumns.COLUMN_SALES_CLOSE_SUNDAY + " TEXT); ";

    static final String SQL_DELETE_TABLE_STATIONS =
            "DROP TABLE IF EXISTS " + StationsDataColumns.TABLE_NAME + ";" ;

    static final String SQL_DELETE_TABLE_FACILITIES ="DROP TABLE IF EXISTS " + StationFacilityColumns.TABLE_NAME;

}
