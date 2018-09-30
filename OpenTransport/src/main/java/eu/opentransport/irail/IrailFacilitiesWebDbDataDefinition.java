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

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.perf.metrics.AddTrace;

import org.joda.time.DateTime;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

import eu.opentransport.common.webdb.WebDbDataDefinition;

import static eu.opentransport.irail.StationFacilitiesDataContract.SQL_CREATE_INDEX_FACILITIES_ID;
import static eu.opentransport.irail.StationFacilitiesDataContract.SQL_CREATE_TABLE_FACILITIES;
import static eu.opentransport.irail.StationFacilitiesDataContract.SQL_DELETE_TABLE_FACILITIES;
import static eu.opentransport.irail.StationFacilitiesDataContract.StationFacilityColumns;
import static java.util.logging.Level.INFO;

/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * (c) Bert Marcelis 2018
 */
class IrailFacilitiesWebDbDataDefinition implements WebDbDataDefinition {
    private static final String LOGTAG = "IrailFacilitiesWebDb";
    private final Context mContext;

    public IrailFacilitiesWebDbDataDefinition(Context context){
        mContext = context;
    }

    @Override
    public String getDataSourceUrl() {
        return null;
    }

    @Override
    public String getDataSourceLocalName() {
        return "irail-facilities.db";
    }

    @Override
    public DateTime getLastModifiedDate(){
        URL url;
        try {
            url = new URL(getDataSourceUrl());
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
    }

    public String getOnlineData(){
        URL url;
        try {
            url = new URL(getDataSourceUrl());
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

    /**
     * Create the database.
     *
     * @param db Handle in which the database should be created.
     */
    @AddTrace(name = "StationsDb.onCreate")
    public void onCreate(SQLiteDatabase db) {
        Crashlytics.log(INFO.intValue(), LOGTAG, "Creating station facilities database");

        db.execSQL(SQL_CREATE_TABLE_FACILITIES);
        db.execSQL(SQL_CREATE_INDEX_FACILITIES_ID);

        Crashlytics.log(INFO.intValue(), LOGTAG, "Filling facilities database");
        fillFacilities(db);
        Crashlytics.log(INFO.intValue(), LOGTAG, "Stations facilities table ready");
        Crashlytics.log(INFO.intValue(), LOGTAG, "Station facilities database ready");
    }


    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache from a local file, so just recreate it
        db.execSQL(SQL_DELETE_TABLE_FACILITIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache from a local file, so just recreate it
        onUpgrade(db, oldVersion, newVersion);
    }

    @AddTrace(name = "StationsDb.fillFacilities")
    public void fillFacilities(SQLiteDatabase db) {

        db.beginTransaction();

        try (Scanner lines = new Scanner(getOnlineData())) {
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
                    field = fields.next();
                    if (field != null && !field.isEmpty()) {
                        values.put(StationFacilityColumns.COLUMN_SALES_OPEN_TUESDAY, field);
                        values.put(
                                StationFacilityColumns.COLUMN_SALES_CLOSE_TUESDAY, fields.next());
                    } else {
                        fields.next();
                    }
                    field = fields.next();
                    if (field != null && !field.isEmpty()) {
                        values.put(StationFacilityColumns.COLUMN_SALES_OPEN_WEDNESDAY, field);
                        values.put(
                                StationFacilityColumns.COLUMN_SALES_CLOSE_WEDNESDAY, fields.next());
                    } else {
                        fields.next();
                    }
                    field = fields.next();
                    if (field != null && !field.isEmpty()) {
                        values.put(StationFacilityColumns.COLUMN_SALES_OPEN_THURSDAY, field);
                        values.put(
                                StationFacilityColumns.COLUMN_SALES_CLOSE_THURSDAY, fields.next());
                    } else {
                        fields.next();
                    }
                    field = fields.next();
                    if (field != null && !field.isEmpty()) {
                        values.put(StationFacilityColumns.COLUMN_SALES_OPEN_FRIDAY, field);
                        values.put(StationFacilityColumns.COLUMN_SALES_CLOSE_FRIDAY, fields.next());
                    } else {
                        fields.next();
                    }
                    field = fields.next();
                    if (field != null && !field.isEmpty()) {
                        values.put(StationFacilityColumns.COLUMN_SALES_OPEN_SATURDAY, field);
                        values.put(
                                StationFacilityColumns.COLUMN_SALES_CLOSE_SATURDAY, fields.next());
                    } else {
                        fields.next();
                    }
                    field = fields.next();
                    if (field != null && !field.isEmpty()) {
                        values.put(StationFacilityColumns.COLUMN_SALES_OPEN_SUNDAY, field);
                        values.put(StationFacilityColumns.COLUMN_SALES_CLOSE_SUNDAY, fields.next());
                    } else {
                        try {
                            fields.next();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    // Insert row
                    db.insert(StationFacilityColumns.TABLE_NAME, null, values);
                }
            }
        }

        db.setTransactionSuccessful();
        db.endTransaction();
    }
}
