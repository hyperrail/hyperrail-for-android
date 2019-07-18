/*
 *  This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 *  If a copy of the MPL was not distributed with this file,
 *  You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.be.irail;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.RawRes;

import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.Scanner;

import be.hyperrail.opentransportdata.be.R;
import be.hyperrail.opentransportdata.logging.OpenTransportLog;

import static be.hyperrail.opentransportdata.be.irail.IrailStationFacilitiesDataContract.SQL_CREATE_INDEX_FACILITIES_ID;
import static be.hyperrail.opentransportdata.be.irail.IrailStationFacilitiesDataContract.SQL_CREATE_TABLE_FACILITIES;
import static be.hyperrail.opentransportdata.be.irail.IrailStationFacilitiesDataContract.SQL_DELETE_TABLE_FACILITIES;
import static be.hyperrail.opentransportdata.be.irail.IrailStationFacilitiesDataContract.StationFacilityColumns;

class IrailFacilitiesDatabase extends SQLiteOpenHelper {
    private static final OpenTransportLog log = OpenTransportLog.getLogger(IrailFacilitiesDatabase.class);
    private static IrailFacilitiesDatabase instance;
    private final Resources mResources;

    IrailFacilitiesDatabase(Context context) {
        super(context, "irail-facilities.db", null, 2019060900);
        this.mResources = context.getResources();
    }

    public static IrailFacilitiesDatabase getInstance(Context context) {
        if (instance == null) {
            instance = new IrailFacilitiesDatabase(context);
        }
        return instance;
    }

    @RawRes
    private int getEmbeddedDataResourceId() {
        return R.raw.stationfacilities;
    }


    @Override
    public String getDatabaseName() {
        return "irail-facilities.db";
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createDatabaseStructure(db);
        loadLocalData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        deleteDatabase(db);
        createDatabaseStructure(db);
        loadLocalData(db);
    }

    private void loadLocalData(SQLiteDatabase db) {
        db.beginTransaction();
        try (Scanner lines = new Scanner(getLocalData())) {
            importFacilities(db, lines);
            db.setTransactionSuccessful();
            db.endTransaction();
        } catch (Exception e) {
            log.severe("Failed to fill facilities db with offline data!", e);
            db.endTransaction();
        }
    }

    private void deleteDatabase(SQLiteDatabase db) {
        db.execSQL(SQL_DELETE_TABLE_FACILITIES);
    }

    private InputStream getLocalData() {
        return mResources.openRawResource(getEmbeddedDataResourceId());
    }

    private void createDatabaseStructure(SQLiteDatabase db) {
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

                // Store ID as URIs
                values.put(StationFacilityColumns._ID, id);
                // Skip name, already in stations.csv
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
            values.put(columnSalesClose, field.trim());
        }
    }
}
