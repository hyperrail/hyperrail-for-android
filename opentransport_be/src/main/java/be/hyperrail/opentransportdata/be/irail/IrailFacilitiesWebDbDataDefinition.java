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

package be.hyperrail.opentransportdata.be.irail;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.RawRes;

import com.google.firebase.perf.metrics.AddTrace;

import org.joda.time.DateTime;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

import be.hyperrail.opentransportdata.be.R;
import be.hyperrail.opentransportdata.common.webdb.WebDbDataDefinition;
import be.hyperrail.opentransportdata.logging.OpenTransportLog;

import static be.hyperrail.opentransportdata.be.irail.IrailStationFacilitiesDataContract.SQL_CREATE_INDEX_FACILITIES_ID;
import static be.hyperrail.opentransportdata.be.irail.IrailStationFacilitiesDataContract.SQL_CREATE_TABLE_FACILITIES;
import static be.hyperrail.opentransportdata.be.irail.IrailStationFacilitiesDataContract.SQL_DELETE_TABLE_FACILITIES;
import static be.hyperrail.opentransportdata.be.irail.IrailStationFacilitiesDataContract.StationFacilityColumns;
import static java.util.logging.Level.INFO;

/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * (c) Bert Marcelis 2018
 */
class IrailFacilitiesWebDbDataDefinition implements WebDbDataDefinition {
    private static final String LOGTAG = "IrailFacilitiesWebDb";
    private final Context mContext;

    public IrailFacilitiesWebDbDataDefinition(Context context) {
        mContext = context;
    }

    @Override
    public boolean updateOnlyOnWifi() {
        return true;
    }

    @Override
    @RawRes
    public int getEmbeddedDataResourceId() {
        return R.raw.stationfacilities;
    }

    @Override
    public String getOnlineDataURL() {
        return "https://raw.githubusercontent.com/iRail/stations/master/facilities.csv";
    }

    @Override
    public String getDatabaseName() {
        return "irail-facilities.db";
    }

    @Override
    public DateTime getLastModifiedLocalDate() {
        return new DateTime(2019, 4, 1, 0, 0);

    }

    @NonNull
    private DateTime getSaturdayBeforeToday() {
        DateTime now = DateTime.now();
        // On a saturday (6) this will be now (+0), on sunday it will be saturday (+1), on monday it will be +2, ...
        // This way we update every saturday
        return now.minusDays(now.dayOfWeek().get() + 1 % 7);
    }

    @Override
    public DateTime getLastModifiedOnlineDate() {
        // Github doesn't send a proper last modified header. Instead we use the last saturday (can be today)
        return getSaturdayBeforeToday();

        /*
        URL url;
        try {
            url = new URL(getOnlineDataURL());
        } catch (MalformedURLException e) {
            return new DateTime(0);
        }
        HttpURLConnection httpCon;
        try {
            httpCon = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            return new DateTime(0);
        }

        return new DateTime(httpCon.getLastModified());
         */
    }

    private String getOnlineData() {
        URL url;
        try {
            url = new URL(getOnlineDataURL());
        } catch (MalformedURLException e) {
            return "";
        }
        HttpURLConnection httpCon;
        try {
            httpCon = (HttpURLConnection) url.openConnection();
            return httpCon.getResponseMessage();
        } catch (IOException e) {
            return "";
        }
    }

    private InputStream getLocalData() {
        return mContext.getResources().openRawResource(getEmbeddedDataResourceId());
    }

    /**
     * Create the database.
     *
     * @param db Handle in which the database should be created.
     */
    @Override
    @AddTrace(name = "StationsDb.onCreate")
    public void onCreate(SQLiteDatabase db, boolean useOnlineData) {
        OpenTransportLog.log(INFO.intValue(), LOGTAG, "Creating station facilities database");

        db.execSQL(SQL_CREATE_TABLE_FACILITIES);
        db.execSQL(SQL_CREATE_INDEX_FACILITIES_ID);

        OpenTransportLog.log(INFO.intValue(), LOGTAG, "Filling facilities database");
        fillFacilities(db, useOnlineData);
        OpenTransportLog.log(INFO.intValue(), LOGTAG, "Stations facilities table ready");
        OpenTransportLog.log(INFO.intValue(), LOGTAG, "Station facilities database ready");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion, boolean useOnlineData) {
        // This database is only a cache from a local file, so just recreate it
        db.execSQL(SQL_DELETE_TABLE_FACILITIES);
        onCreate(db, useOnlineData);
    }


    @AddTrace(name = "StationsDb.fillFacilities")
    private void fillFacilities(SQLiteDatabase db, boolean useOnlineData) {

        if (useOnlineData) {
            db.beginTransaction();
            try (Scanner lines = new Scanner(getOnlineData())) {
                importFacilities(db, lines);
                db.setTransactionSuccessful();
            } catch (Exception e) {
                OpenTransportLog.log("Failed to fill stations db with online data! Reverting to local.");
                OpenTransportLog.logException(e);
                // If we can't get online data, start a new transaction to load offline data instead.
                // This fallback should guarantee that everything keeps working should an online update wreck the schema.
                db.endTransaction();
                db.beginTransaction();
                try (Scanner lines = new Scanner(getLocalData())) {
                    importFacilities(db, lines);
                    db.setTransactionSuccessful();
                }
            } finally {
                db.endTransaction();
            }
        } else {
            // Load offline data
            db.beginTransaction();
            try (Scanner lines = new Scanner(getLocalData())) {
                importFacilities(db, lines);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

    }

    private void importFacilities(SQLiteDatabase db, Scanner lines) {
        lines.useDelimiter("\n");

        while (lines.hasNext()) {
            String line = lines.next();

            try (Scanner fields = new Scanner(line)) {
                fields.useDelimiter(",");

                ContentValues values = new ContentValues();

                String id = fields.next();

                // By default, the CSV contains ids in iRail URI format,
                // reformat them to 9 digit HAFAS ids
                if (id.startsWith("http")) {
                    id = id.replace("http://irail.be/stations/NMBS/", "");
                }

                // Store ID as 9 digit IDs
                values.put(StationFacilityColumns._ID, id);
                // Skip name
                fields.next();
                values.put(StationFacilityColumns.COLUMN_STREET, fields.next());
                values.put(StationFacilityColumns.COLUMN_ZIP, fields.next());
                values.put(StationFacilityColumns.COLUMN_CITY, fields.next());
                values.put(StationFacilityColumns.COLUMN_TICKET_VENDING_MACHINE, fields.next());
                values.put(StationFacilityColumns.COLUMN_LUGGAGE_LOCKERS, fields.next());
                values.put(StationFacilityColumns.COLUMN_FREE_PARKING, fields.next());
                values.put(StationFacilityColumns.COLUMN_TAXI, fields.next());
                values.put(StationFacilityColumns.COLUMN_BICYCLE_SPOTS, fields.next());
                values.put(StationFacilityColumns.COLUMN_BLUE_BIKE, fields.next());
                values.put(StationFacilityColumns.COLUMN_BUS, fields.next());
                values.put(StationFacilityColumns.COLUMN_TRAM, fields.next());
                values.put(StationFacilityColumns.COLUMN_METRO, fields.next());
                values.put(StationFacilityColumns.COLUMN_WHEELCHAIR_AVAILABLE, fields.next());
                values.put(StationFacilityColumns.COLUMN_RAMP, fields.next());
                values.put(StationFacilityColumns.COLUMN_DISABLED_PARKING_SPOTS, fields.next());
                values.put(StationFacilityColumns.COLUMN_ELEVATED_PLATFORM, fields.next());
                values.put(StationFacilityColumns.COLUMN_ESCALATOR_UP, fields.next());
                values.put(StationFacilityColumns.COLUMN_ESCALATOR_DOWN, fields.next());
                values.put(StationFacilityColumns.COLUMN_ELEVATOR_PLATFORM, fields.next());
                values.put(StationFacilityColumns.COLUMN_HEARING_AID_SIGNAL, fields.next());
                String field = fields.next();

                // If an opening time exists, a closing one also exists.
                if (field != null && !field.isEmpty()) {
                    values.put(StationFacilityColumns.COLUMN_SALES_OPEN_MONDAY, field);
                    values.put(StationFacilityColumns.COLUMN_SALES_CLOSE_MONDAY, fields.next());
                } else {
                    fields.next();
                }
                getOpeningHourValues(fields, values, StationFacilityColumns.COLUMN_SALES_OPEN_TUESDAY, StationFacilityColumns.COLUMN_SALES_CLOSE_TUESDAY);
                getOpeningHourValues(fields, values, StationFacilityColumns.COLUMN_SALES_OPEN_WEDNESDAY, StationFacilityColumns.COLUMN_SALES_CLOSE_WEDNESDAY);
                getOpeningHourValues(fields, values, StationFacilityColumns.COLUMN_SALES_OPEN_THURSDAY, StationFacilityColumns.COLUMN_SALES_CLOSE_THURSDAY);
                getOpeningHourValues(fields, values, StationFacilityColumns.COLUMN_SALES_OPEN_FRIDAY, StationFacilityColumns.COLUMN_SALES_CLOSE_FRIDAY);
                getOpeningHourValues(fields, values, StationFacilityColumns.COLUMN_SALES_OPEN_SATURDAY, StationFacilityColumns.COLUMN_SALES_CLOSE_SATURDAY);
                try {
                    getOpeningHourValues(fields, values, StationFacilityColumns.COLUMN_SALES_OPEN_SUNDAY, StationFacilityColumns.COLUMN_SALES_CLOSE_SUNDAY);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Insert row
                db.insert(StationFacilityColumns.TABLE_NAME, null, values);
            }
        }
    }

    private void getOpeningHourValues(Scanner fields, ContentValues values, String columnSalesOpen, String columnSalesClose) {
        String field;
        field = fields.next();
        if (field != null && !field.isEmpty()) {
            values.put(columnSalesOpen, field);
            values.put(columnSalesClose, fields.next());
        } else {
            fields.next();
        }
    }
}
