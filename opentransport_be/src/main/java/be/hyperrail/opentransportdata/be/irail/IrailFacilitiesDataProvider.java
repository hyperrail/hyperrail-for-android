/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.be.irail;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import be.hyperrail.opentransportdata.be.irail.IrailStationFacilitiesDataContract.StationFacilityColumns;
import be.hyperrail.opentransportdata.common.contracts.TransportStopFacilitiesDataSource;
import be.hyperrail.opentransportdata.common.models.StopLocation;
import be.hyperrail.opentransportdata.common.models.StopLocationFacilities;
import be.hyperrail.opentransportdata.common.models.implementation.StopLocationFacilitiesImpl;
import be.hyperrail.opentransportdata.common.webdb.WebDb;
import be.hyperrail.opentransportdata.logging.OpenTransportLog;

/**
 * Database for querying stations
 */
public class IrailFacilitiesDataProvider implements TransportStopFacilitiesDataSource {

    private final static OpenTransportLog log = OpenTransportLog.getLogger(IrailFacilitiesDataProvider.class);

    private final Context context;
    private WebDb mWebDb;

    public IrailFacilitiesDataProvider(Context appContext) {
        this.context = appContext;
        this.mWebDb = new WebDb(context, new IrailFacilitiesWebDbDataDefinition(context));
    }

    @Override
    public StopLocationFacilities getStationFacilitiesByUri(String id) {
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

        StopLocationFacilitiesImpl result = loadFacilitiesCursor(c);
        c.close();

        return result;

    }

    @Override
    public StopLocationFacilities getStationFacilities(StopLocation stopLocation) {
        return getStationFacilitiesByUri(stopLocation.getSemanticId());
    }

    /**
     * Load stations from a cursor. This method <strong>does not close the cursor afterwards</strong>.
     *
     * @param c The cursor from which stations should be loaded.
     * @return The array of loaded stations
     */
    private StopLocationFacilitiesImpl loadFacilitiesCursor(Cursor c) {
        if (c.isClosed()) {
            log.severe("Tried to load closed cursor");
            return null;
        }

        if (c.getCount() == 0) {
            log.severe("Tried to load cursor with 0 results!");
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

        return new StopLocationFacilitiesImpl(
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