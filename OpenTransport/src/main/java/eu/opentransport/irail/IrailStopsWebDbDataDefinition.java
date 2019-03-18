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

import static eu.opentransport.irail.IrailStationsDataContract.SQL_CREATE_INDEX_ID;
import static eu.opentransport.irail.IrailStationsDataContract.SQL_CREATE_INDEX_NAME;
import static eu.opentransport.irail.IrailStationsDataContract.SQL_CREATE_TABLE_STATIONS;
import static eu.opentransport.irail.IrailStationsDataContract.SQL_DELETE_TABLE_STATIONS;
import static eu.opentransport.irail.IrailStationsDataContract.StationsDataColumns;
import static eu.opentransport.util.StringUtils.cleanAccents;
import static java.util.logging.Level.INFO;

/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * (c) Bert Marcelis 2018
 */
class IrailStopsWebDbDataDefinition implements WebDbDataDefinition {
    private static final String LOGTAG = "IrailWebDb";
    private final Context mContext;

    @Override
    public String getDataSourceUrl() {
        return "https://api.irail.be/stations.format=json";
    }

    @Override
    public String getDataSourceLocalName() {
        return "irail-stations.db";
    }

    @Override
    public DateTime getLastModifiedDate() {
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


    public String getOnlineData() {
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

    public IrailStopsWebDbDataDefinition(Context context) {
        mContext = context;
    }

    /**
     * Create the database.
     *
     * @param db Handle in which the database should be created.
     */
    @AddTrace(name = "StationsDb.onCreate")
    public void onCreate(SQLiteDatabase db) {
        Crashlytics.log(INFO.intValue(), LOGTAG, "Creating stations database");

        db.execSQL(SQL_CREATE_TABLE_STATIONS);
        db.execSQL(SQL_CREATE_INDEX_ID);
        db.execSQL(SQL_CREATE_INDEX_NAME);

        Crashlytics.log(INFO.intValue(), LOGTAG, "Filling stations database");
        fillStations(db);
        Crashlytics.log(INFO.intValue(), LOGTAG, "Stations table ready");
        Crashlytics.log(INFO.intValue(), LOGTAG, "Stations facilities table ready");
        Crashlytics.log(INFO.intValue(), LOGTAG, "Stations database ready");
    }

    /**
     * Fill the database with data from the embedded CSV file (raw resource).
     *
     * @param db The database to fillStations
     */
    @AddTrace(name = "StationsDb.fillStations")
    private void fillStations(SQLiteDatabase db) {

        db.beginTransaction();
        try (Scanner lines = new Scanner(getOnlineData())) {

            lines.useDelimiter("\n");

            while (lines.hasNext()) {
                String line = lines.next();
                if (line.startsWith("URI,name")) {
                    // Header line
                    continue;
                }

                try (Scanner fields = new Scanner(line)) {
                    fields.useDelimiter(",");

                    ContentValues values = new ContentValues();

                    String id = fields.next();

                    // By default, the CSV contains ids in iRail URI format,
                    // reformat them to 9-digit HAFAS IDs, as HAFAS IDs are an extension upon UIC station codes.
                    if (id.startsWith("http")) {
                        id = id.replace("http://irail.be/stations/NMBS/", "");
                    }

                    // Store ID as XXXXXXXX
                    values.put(StationsDataColumns._ID, id);
                    // Replace special characters (for search purposes)
                    values.put(
                            StationsDataColumns.COLUMN_NAME_NAME,
                            cleanAccents(fields.next())
                    );
                    values.put(
                            StationsDataColumns.COLUMN_NAME_ALTERNATIVE_FR,
                            cleanAccents(fields.next())
                    );
                    values.put(
                            StationsDataColumns.COLUMN_NAME_ALTERNATIVE_NL,
                            cleanAccents(fields.next())
                    );
                    values.put(
                            StationsDataColumns.COLUMN_NAME_ALTERNATIVE_DE,
                            cleanAccents(fields.next())
                    );
                    values.put(
                            StationsDataColumns.COLUMN_NAME_ALTERNATIVE_EN,
                            cleanAccents(fields.next())
                    );
                    values.put(StationsDataColumns.COLUMN_NAME_COUNTRY_CODE, fields.next());

                    String field = fields.next();
                    if (field != null && !field.isEmpty()) {

                        values.put(
                                StationsDataColumns.COLUMN_NAME_LONGITUDE,
                                Double.parseDouble(field)
                        );

                    } else {
                        values.put(StationsDataColumns.COLUMN_NAME_LONGITUDE, 0);
                    }

                    field = fields.next();
                    if (field != null && !field.isEmpty()) {
                        values.put(
                                StationsDataColumns.COLUMN_NAME_LATITUDE,
                                Double.parseDouble(field)
                        );

                    } else {
                        values.put(StationsDataColumns.COLUMN_NAME_LATITUDE, 0);
                    }

                    field = fields.next();
                    if (field != null && !field.isEmpty()) {
                        try {
                            values.put(
                                    StationsDataColumns.COLUMN_NAME_AVG_STOP_TIMES,
                                    Double.parseDouble(field)
                            );
                        } catch (NumberFormatException e) {
                            values.put(StationsDataColumns.COLUMN_NAME_AVG_STOP_TIMES, 0);
                        }
                    } else {
                        values.put(StationsDataColumns.COLUMN_NAME_AVG_STOP_TIMES, 0);
                    }

                    field = fields.next();
                    if (field != null && !field.isEmpty()) {
                        try {
                            values.put(
                                    StationsDataColumns.COLUMN_NAME_OFFICIAL_TRANSFER_TIME,
                                    Double.parseDouble(field)
                            );
                        } catch (NumberFormatException e) {
                            values.put(StationsDataColumns.COLUMN_NAME_OFFICIAL_TRANSFER_TIME, 0);
                        }
                    } else {
                        values.put(StationsDataColumns.COLUMN_NAME_OFFICIAL_TRANSFER_TIME, 0);
                    }
                    // Insert row
                    db.insert(StationsDataColumns.TABLE_NAME, null, values);
                }
            }
        }

        db.setTransactionSuccessful();
        db.endTransaction();

    }


    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache from a local file, so just recreate it
        db.execSQL(SQL_DELETE_TABLE_STATIONS);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache from a local file, so just recreate it
        onUpgrade(db, oldVersion, newVersion);
    }

}
