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
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;
import androidx.annotation.RawRes;

import org.joda.time.DateTime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.NoSuchElementException;
import java.util.Scanner;

import be.hyperrail.opentransportdata.be.R;
import be.hyperrail.opentransportdata.common.webdb.WebDbDataDefinition;
import be.hyperrail.opentransportdata.logging.OpenTransportLog;

import static be.hyperrail.opentransportdata.be.irail.IrailStationFacilitiesDataContract.SQL_CREATE_INDEX_FACILITIES_ID;
import static be.hyperrail.opentransportdata.be.irail.IrailStationFacilitiesDataContract.SQL_CREATE_TABLE_FACILITIES;
import static be.hyperrail.opentransportdata.be.irail.IrailStationFacilitiesDataContract.SQL_DELETE_TABLE_FACILITIES;
import static be.hyperrail.opentransportdata.be.irail.IrailStationFacilitiesDataContract.StationFacilityColumns;

/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * (c) Bert Marcelis 2018
 */
class IrailFacilitiesWebDbDataDefinition implements WebDbDataDefinition {
    private final static OpenTransportLog log = OpenTransportLog.getLogger(IrailFacilitiesWebDbDataDefinition.class);
    private static IrailFacilitiesWebDbDataDefinition instance;
    private final Resources mResources;

    private IrailFacilitiesWebDbDataDefinition(Context context) {
        mResources = context.getResources();
    }

    public static IrailFacilitiesWebDbDataDefinition getInstance(Context context) {
        if (instance == null) {
            instance = new IrailFacilitiesWebDbDataDefinition(context);
        }
        return instance;
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

    @Override
    public boolean loadLocalData(SQLiteDatabase db) {
        db.beginTransaction();
        try (Scanner lines = new Scanner(getLocalData())) {
            importFacilities(db, lines);
            db.setTransactionSuccessful();
            db.endTransaction();
        } catch (Exception e) {
            log.severe("Failed to fill facilities db with offline data!", e);
            db.endTransaction();
            return false;
        }
        return true;
    }

    @Override
    public boolean importDownloadedData(SQLiteDatabase db, Object onlineUpdateData) {
        db.beginTransaction();
        try (Scanner lines = new Scanner((String) onlineUpdateData)) {
            importFacilities(db, lines);
            db.setTransactionSuccessful();
            db.endTransaction();
            log.info("Filled station facilities database with online data");
            return true;
        } catch (Exception e) {
            log.severe("Failed to fill facilities db with online data!", e);
            db.endTransaction();
            return false;
        }
    }

    @Override
    public String downloadOnlineData() {
        URL url;
        try {
            url = new URL(getOnlineDataURL());
        } catch (MalformedURLException e) {
            log.warning("Failed to get data URL for database " + getDatabaseName());
            return null;
        }
        HttpURLConnection httpCon;
        try {
            httpCon = (HttpURLConnection) url.openConnection();
            return dataStreamToString(httpCon.getInputStream());
        } catch (IOException e) {
            log.warning("Failed to get online for database " + getDatabaseName() + " from " + url.toString());
            return null;
        }
    }

    private String dataStreamToString(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder stringBuilder = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
        }
        return stringBuilder.toString();
    }

    @Override
    public void clearDatabase(SQLiteDatabase db) {
        db.execSQL(SQL_DELETE_TABLE_FACILITIES);
    }

    private InputStream getLocalData() {
        return mResources.openRawResource(getEmbeddedDataResourceId());
    }

    @Override
    public void createDatabaseStructure(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE_FACILITIES);
        db.execSQL(SQL_CREATE_INDEX_FACILITIES_ID);
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

                try {
                    getOpeningHourValues(fields, values, StationFacilityColumns.COLUMN_SALES_OPEN_TUESDAY, StationFacilityColumns.COLUMN_SALES_CLOSE_TUESDAY);
                    getOpeningHourValues(fields, values, StationFacilityColumns.COLUMN_SALES_OPEN_WEDNESDAY, StationFacilityColumns.COLUMN_SALES_CLOSE_WEDNESDAY);
                    getOpeningHourValues(fields, values, StationFacilityColumns.COLUMN_SALES_OPEN_THURSDAY, StationFacilityColumns.COLUMN_SALES_CLOSE_THURSDAY);
                    getOpeningHourValues(fields, values, StationFacilityColumns.COLUMN_SALES_OPEN_FRIDAY, StationFacilityColumns.COLUMN_SALES_CLOSE_FRIDAY);
                    getOpeningHourValues(fields, values, StationFacilityColumns.COLUMN_SALES_OPEN_SATURDAY, StationFacilityColumns.COLUMN_SALES_CLOSE_SATURDAY);
                    getOpeningHourValues(fields, values, StationFacilityColumns.COLUMN_SALES_OPEN_SUNDAY, StationFacilityColumns.COLUMN_SALES_CLOSE_SUNDAY);
                } catch (NoSuchElementException e) {
                    // Ignored
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
        }
        field = fields.next();
        if (field != null && !field.isEmpty()) {
            values.put(columnSalesClose, field);
        }
    }
}
