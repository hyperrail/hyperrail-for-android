/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package eu.opentransport.irail;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.crashlytics.android.Crashlytics;

import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import eu.opentransport.common.webdb.WebDb;
import eu.opentransport.irail.StationFacilitiesDataContract.StationFacilityColumns;

import static java.util.logging.Level.SEVERE;

/**
 * Database for querying stations
 */
public class FacilitiesDataProvider {

    // Logtag for logging purpose
    private static final String LOGTAG = "database";

    private final Context context;
    private WebDb mWebDb;

    public FacilitiesDataProvider(Context context) {
        this.context = context;
        this.mWebDb = new WebDb(context, new IrailFacilitiesWebDbDataDefinition(context));
    }

    static String cleanAccents(String s) {

        if (s == null || s.isEmpty()) {
            return s;
        }

        return s.replaceAll("[éÉèÈêÊëË]", "e")
                .replaceAll("[âÂåäÄ]", "a")
                .replaceAll("[öÖø]", "o")
                .replaceAll("[üÜ]", "u");
    }


    public StationFacilities getStationFacilitiesById(String id) {
        SQLiteDatabase db = mWebDb.getReadableDatabase();
        Cursor c = db.query(
                StationFacilityColumns.TABLE_NAME,
                new String[]{
                        StationFacilityColumns._ID,
                        StationFacilityColumns.COLUMN_STREET,
                        StationFacilityColumns.COLUMN_ZIP,
                        StationFacilityColumns.COLUMN_CITY,
                        StationFacilityColumns.COLUMN_TICKET_VENDING_MACHINE,
                        StationFacilityColumns.COLUMN_LUGGAGE_LOCKERS,
                        StationFacilityColumns.COLUMN_FREE_PARKING,
                        StationFacilityColumns.COLUMN_TAXI,
                        StationFacilityColumns.COLUMN_BICYCLE_SPOTS,
                        StationFacilityColumns.COLUMN_BLUE_BIKE,
                        StationFacilityColumns.COLUMN_BUS,
                        StationFacilityColumns.COLUMN_TRAM,
                        StationFacilityColumns.COLUMN_METRO,
                        StationFacilityColumns.COLUMN_WHEELCHAIR_AVAILABLE,
                        StationFacilityColumns.COLUMN_RAMP,
                        StationFacilityColumns.COLUMN_DISABLED_PARKING_SPOTS,
                        StationFacilityColumns.COLUMN_ELEVATED_PLATFORM,
                        StationFacilityColumns.COLUMN_ESCALATOR_UP,
                        StationFacilityColumns.COLUMN_ESCALATOR_DOWN,
                        StationFacilityColumns.COLUMN_ELEVATOR_PLATFORM,
                        StationFacilityColumns.COLUMN_HEARING_AID_SIGNAL,
                        StationFacilityColumns.COLUMN_SALES_OPEN_MONDAY,
                        StationFacilityColumns.COLUMN_SALES_CLOSE_MONDAY,
                        StationFacilityColumns.COLUMN_SALES_OPEN_TUESDAY,
                        StationFacilityColumns.COLUMN_SALES_CLOSE_TUESDAY,
                        StationFacilityColumns.COLUMN_SALES_OPEN_WEDNESDAY,
                        StationFacilityColumns.COLUMN_SALES_CLOSE_WEDNESDAY,
                        StationFacilityColumns.COLUMN_SALES_OPEN_THURSDAY,
                        StationFacilityColumns.COLUMN_SALES_CLOSE_THURSDAY,
                        StationFacilityColumns.COLUMN_SALES_OPEN_FRIDAY,
                        StationFacilityColumns.COLUMN_SALES_CLOSE_FRIDAY,
                        StationFacilityColumns.COLUMN_SALES_OPEN_SATURDAY,
                        StationFacilityColumns.COLUMN_SALES_CLOSE_SATURDAY,
                        StationFacilityColumns.COLUMN_SALES_OPEN_SUNDAY,
                        StationFacilityColumns.COLUMN_SALES_CLOSE_SUNDAY,
                },
                StationFacilityColumns._ID + "=?",
                new String[]{id},
                null,
                null,
                null,
                "1"
        );

        if (c.getCount() == 0) {
            c.close();

            return null;
        }

        StationFacilities result = loadFacilitiesCursor(c);
        c.close();

        return result;

    }


    /**
     * Load stations from a cursor. This method <strong>does not close the cursor afterwards</strong>.
     *
     * @param c The cursor from which stations should be loaded.
     * @return The array of loaded stations
     */
    private StationFacilities loadFacilitiesCursor(Cursor c) {
        if (c.isClosed()) {
            Crashlytics.log(SEVERE.intValue(), LOGTAG, "Tried to load closed cursor");
            return null;
        }

        if (c.getCount() == 0) {
            Crashlytics.log(SEVERE.intValue(), LOGTAG, "Tried to load cursor with 0 results!");
            return null;
        }

        c.moveToFirst();

        int[][] indices = new int[][]{
                new int[]{c.getColumnIndex(
                        StationFacilityColumns.COLUMN_SALES_OPEN_MONDAY), c.getColumnIndex(
                        StationFacilityColumns.COLUMN_SALES_CLOSE_MONDAY)},
                new int[]{c.getColumnIndex(
                        StationFacilityColumns.COLUMN_SALES_OPEN_TUESDAY), c.getColumnIndex(
                        StationFacilityColumns.COLUMN_SALES_CLOSE_MONDAY)},
                new int[]{c.getColumnIndex(
                        StationFacilityColumns.COLUMN_SALES_OPEN_WEDNESDAY), c.getColumnIndex(
                        StationFacilityColumns.COLUMN_SALES_CLOSE_WEDNESDAY)},
                new int[]{c.getColumnIndex(
                        StationFacilityColumns.COLUMN_SALES_OPEN_THURSDAY), c.getColumnIndex(
                        StationFacilityColumns.COLUMN_SALES_CLOSE_THURSDAY)},
                new int[]{c.getColumnIndex(
                        StationFacilityColumns.COLUMN_SALES_OPEN_FRIDAY), c.getColumnIndex(
                        StationFacilityColumns.COLUMN_SALES_CLOSE_FRIDAY)},
                new int[]{c.getColumnIndex(
                        StationFacilityColumns.COLUMN_SALES_OPEN_SATURDAY), c.getColumnIndex(
                        StationFacilityColumns.COLUMN_SALES_CLOSE_SATURDAY)},
                new int[]{c.getColumnIndex(
                        StationFacilityColumns.COLUMN_SALES_OPEN_SUNDAY), c.getColumnIndex(
                        StationFacilityColumns.COLUMN_SALES_CLOSE_SUNDAY)},
        };

        LocalTime[][] openingHours = new LocalTime[7][];
        DateTimeFormatter localTimeFormatter = DateTimeFormat.forPattern("HH:mm");
        for (int i = 0; i < 7; i++) {
            if (c.getString(indices[i][0]) == null) {
                openingHours[i] = null;
            } else {
                openingHours[i] = new LocalTime[2];
                openingHours[i][0] = LocalTime.parse(
                        c.getString(indices[i][0]), localTimeFormatter);
                openingHours[i][1] = LocalTime.parse(
                        c.getString(indices[i][1]), localTimeFormatter);
            }
        }

        return new StationFacilities(
                openingHours,
                c.getString(c.getColumnIndex(StationFacilityColumns.COLUMN_STREET)),
                c.getString(c.getColumnIndex(StationFacilityColumns.COLUMN_ZIP)),
                c.getString(c.getColumnIndex(StationFacilityColumns.COLUMN_CITY)),
                c.getInt(c.getColumnIndex(
                        StationFacilityColumns.COLUMN_TICKET_VENDING_MACHINE)) == 1,
                c.getInt(c.getColumnIndex(StationFacilityColumns.COLUMN_LUGGAGE_LOCKERS)) == 1,
                c.getInt(c.getColumnIndex(StationFacilityColumns.COLUMN_FREE_PARKING)) == 1,
                c.getInt(c.getColumnIndex(StationFacilityColumns.COLUMN_TAXI)) == 1,
                c.getInt(c.getColumnIndex(StationFacilityColumns.COLUMN_BICYCLE_SPOTS)) == 1,
                c.getInt(c.getColumnIndex(StationFacilityColumns.COLUMN_BLUE_BIKE)) == 1,
                c.getInt(c.getColumnIndex(StationFacilityColumns.COLUMN_BUS)) == 1,
                c.getInt(c.getColumnIndex(StationFacilityColumns.COLUMN_TRAM)) == 1,
                c.getInt(c.getColumnIndex(StationFacilityColumns.COLUMN_METRO)) == 1,
                c.getInt(c.getColumnIndex(StationFacilityColumns.COLUMN_WHEELCHAIR_AVAILABLE)) == 1,
                c.getInt(c.getColumnIndex(StationFacilityColumns.COLUMN_RAMP)) == 1,
                c.getInt(c.getColumnIndex(StationFacilityColumns.COLUMN_DISABLED_PARKING_SPOTS)),
                c.getInt(c.getColumnIndex(StationFacilityColumns.COLUMN_ELEVATED_PLATFORM)) == 1,
                c.getInt(c.getColumnIndex(StationFacilityColumns.COLUMN_ESCALATOR_UP)) == 1,
                c.getInt(c.getColumnIndex(StationFacilityColumns.COLUMN_ESCALATOR_DOWN)) == 1,
                c.getInt(c.getColumnIndex(StationFacilityColumns.COLUMN_ELEVATOR_PLATFORM)) == 1,
                c.getInt(c.getColumnIndex(StationFacilityColumns.COLUMN_HEARING_AID_SIGNAL)) == 1
        );

    }
}