/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package android.irail.be.hyperrail.irail.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.irail.be.hyperrail.R;
import android.irail.be.hyperrail.irail.contracts.IrailStationProvider;
import android.location.Location;
import android.util.Log;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Scanner;

import static android.irail.be.hyperrail.irail.db.StationsDataContract.SQL_CREATE_TABLE;
import static android.irail.be.hyperrail.irail.db.StationsDataContract.SQL_DELETE_TABLE;
import static android.irail.be.hyperrail.irail.db.StationsDataContract.StationsDataColumns.COLUMN_NAME_ALTERNATIVE_DE;
import static android.irail.be.hyperrail.irail.db.StationsDataContract.StationsDataColumns.COLUMN_NAME_ALTERNATIVE_EN;
import static android.irail.be.hyperrail.irail.db.StationsDataContract.StationsDataColumns.COLUMN_NAME_ALTERNATIVE_FR;
import static android.irail.be.hyperrail.irail.db.StationsDataContract.StationsDataColumns.COLUMN_NAME_ALTERNATIVE_NL;
import static android.irail.be.hyperrail.irail.db.StationsDataContract.StationsDataColumns.COLUMN_NAME_AVG_STOP_TIMES;
import static android.irail.be.hyperrail.irail.db.StationsDataContract.StationsDataColumns.COLUMN_NAME_COUNTRY_CODE;
import static android.irail.be.hyperrail.irail.db.StationsDataContract.StationsDataColumns.COLUMN_NAME_LATITUDE;
import static android.irail.be.hyperrail.irail.db.StationsDataContract.StationsDataColumns.COLUMN_NAME_LONGITUDE;
import static android.irail.be.hyperrail.irail.db.StationsDataContract.StationsDataColumns.COLUMN_NAME_NAME;
import static android.irail.be.hyperrail.irail.db.StationsDataContract.StationsDataColumns.TABLE_NAME;
import static android.irail.be.hyperrail.irail.db.StationsDataContract.StationsDataColumns._ID;

/**
 * Database for querying stations
 */
public class StationsDb extends SQLiteOpenHelper implements IrailStationProvider {

    // If you change the database schema, you must increment the database version.
    // year/month/day/increment
    private static final int DATABASE_VERSION = 17061200;

    // Name of the database file
    private static final String DATABASE_NAME = "stations.db";

    // Logtag for logging purpose
    private static final String LOGTAG = "database";

    private final Context context;

    public StationsDb(Context applicationContext) {
        super(applicationContext, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = applicationContext;
    }

    /**
     * Create the database.
     *
     * @param db Handle in which the database should be created.
     */
    public void onCreate(SQLiteDatabase db) {
        Log.d(LOGTAG, "Creating database ...");
        db.execSQL(SQL_CREATE_TABLE);
        Log.d(LOGTAG, "Filling database ...");
        fill(db);
        Log.d(LOGTAG, "Database ready");
    }

    /**
     * Fill the database with data from the embedded CSV file (raw resource).
     *
     * @param db
     */
    private void fill(SQLiteDatabase db) {

        try (Scanner lines = new Scanner(context.getResources().openRawResource(R.raw.stations))) {
            lines.useDelimiter("\n");

            while (lines.hasNext()) {
                String line = lines.next();

                try (Scanner fields = new Scanner(line)) {
                    fields.useDelimiter(",");

                    ContentValues values = new ContentValues();

                    String id = fields.next();

                    // By default, the CSV contains ids in iRail URI format,
                    // reformat them to BE.NMBS.XXXXXXXX (which is also iRail compliant)
                    if (id.startsWith("http")) {
                        id = id.replace("http://irail.be/stations/NMBS/", "BE.NMBS.");
                    }

                    // Store ID as BE.NMBS.XXXXXXXX
                    values.put(_ID, id);
                    // Replace special characters (for search purposes)
                    values.put(COLUMN_NAME_NAME, fields.next()
                            .replaceAll("(é|É|è|È|ê|Ê|ë|Ë)", "e")
                            .replaceAll("(â|Â|å|ä|Ä)", "a")
                            .replaceAll("(ö|Ö)", "o")
                    );
                    values.put(COLUMN_NAME_ALTERNATIVE_FR, fields.next());
                    values.put(COLUMN_NAME_ALTERNATIVE_NL, fields.next());
                    values.put(COLUMN_NAME_ALTERNATIVE_DE, fields.next());
                    values.put(COLUMN_NAME_ALTERNATIVE_EN, fields.next());
                    values.put(COLUMN_NAME_COUNTRY_CODE, fields.next());

                    String field = fields.next();
                    if (field != null && !field.isEmpty()) {
                        values.put(COLUMN_NAME_LONGITUDE, Double.parseDouble(field));
                    } else {
                        values.put(COLUMN_NAME_LONGITUDE, 0);
                    }

                    field = fields.next();
                    if (field != null && !field.isEmpty()) {
                        values.put(COLUMN_NAME_LATITUDE, Double.parseDouble(field));
                    } else {
                        values.put(COLUMN_NAME_LATITUDE, 0);
                    }

                    field = fields.next();
                    if (field != null && !field.isEmpty()) {
                        try {
                            values.put(COLUMN_NAME_AVG_STOP_TIMES, Double.parseDouble(field));
                        } catch (NumberFormatException e) {
                            values.put(COLUMN_NAME_AVG_STOP_TIMES, 0);
                        }
                    } else {
                        values.put(COLUMN_NAME_AVG_STOP_TIMES, 0);
                    }

                    // Insert row
                    db.insert(TABLE_NAME, null, values);
                }
            }
        }
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache from a local file, so just recreate it
        db.execSQL(SQL_DELETE_TABLE);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache from a local file, so just recreate it
        onUpgrade(db, oldVersion, newVersion);
    }

    public Station[] getStationsOrderBySize() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(
                TABLE_NAME,
                new String[]{
                        _ID,
                        COLUMN_NAME_NAME,
                        COLUMN_NAME_ALTERNATIVE_NL,
                        COLUMN_NAME_ALTERNATIVE_FR,
                        COLUMN_NAME_ALTERNATIVE_DE,
                        COLUMN_NAME_ALTERNATIVE_EN,
                        COLUMN_NAME_COUNTRY_CODE,
                        COLUMN_NAME_LATITUDE,
                        COLUMN_NAME_LONGITUDE,
                        COLUMN_NAME_AVG_STOP_TIMES
                },
                null,
                null,
                null,
                null,
                COLUMN_NAME_AVG_STOP_TIMES + " DESC"
        );

        Station[] stations = loadStationCursor(c);
        c.close();
        db.close();
        return stations;
    }

    /**
     * @inheritDoc
     */
    @Override
    public Station[] getStationsByNameOrderBySize(String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(
                TABLE_NAME,
                new String[]{
                        _ID,
                        COLUMN_NAME_NAME,
                        COLUMN_NAME_ALTERNATIVE_NL,
                        COLUMN_NAME_ALTERNATIVE_FR,
                        COLUMN_NAME_ALTERNATIVE_DE,
                        COLUMN_NAME_ALTERNATIVE_EN,
                        COLUMN_NAME_COUNTRY_CODE,
                        COLUMN_NAME_LATITUDE,
                        COLUMN_NAME_LONGITUDE,
                        COLUMN_NAME_AVG_STOP_TIMES
                },
                COLUMN_NAME_NAME + " LIKE ?",
                new String[]{"%" + name + "%"},
                null,
                null,
                COLUMN_NAME_AVG_STOP_TIMES + " DESC"
        );

        Station[] stations = loadStationCursor(c);
        c.close();
        db.close();
        return stations;
    }

    /**
     * @inheritDoc
     */
    @Override
    public Station[] getStationsOrderByLocation(Location location) {
        return this.getStationsByNameOrderByLocation("", location);
    }

    /**
     * @inheritDoc
     */
    @Override
    public Station[] getStationsByNameOrderByLocation(String name, Location location) {
        SQLiteDatabase db = this.getReadableDatabase();
        name = name.replaceAll("\\(\\w\\)", "");
        Cursor c = db.query(
                TABLE_NAME,
                new String[]{
                        _ID,
                        COLUMN_NAME_NAME,
                        COLUMN_NAME_ALTERNATIVE_NL,
                        COLUMN_NAME_ALTERNATIVE_FR,
                        COLUMN_NAME_ALTERNATIVE_DE,
                        COLUMN_NAME_ALTERNATIVE_EN,
                        COLUMN_NAME_COUNTRY_CODE,
                        COLUMN_NAME_LATITUDE,
                        COLUMN_NAME_LONGITUDE,
                        COLUMN_NAME_AVG_STOP_TIMES,
                        "(" + COLUMN_NAME_LATITUDE + "-" + location.getLatitude() + ")*(" + COLUMN_NAME_LATITUDE + "-" + location.getLatitude() + ")+(" + COLUMN_NAME_LONGITUDE + "-" + location.getLongitude() + ")*(" + COLUMN_NAME_LONGITUDE + "-" + location.getLongitude() + ") AS distance"
                },
                COLUMN_NAME_NAME + " LIKE ?",
                new String[]{"%" + name + "%"},
                null,
                null,
                "distance ASC, " + COLUMN_NAME_AVG_STOP_TIMES + " DESC"
        );

        Station[] stations = loadStationCursor(c);
        c.close();
        db.close();
        return stations;
    }
    /**
     * @inheritDoc
     */
    @Override
    public Station[] getStationsOrderByLocationAndSize(Location location, int limit) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(
                TABLE_NAME,
                new String[]{
                        _ID,
                        COLUMN_NAME_NAME,
                        COLUMN_NAME_ALTERNATIVE_NL,
                        COLUMN_NAME_ALTERNATIVE_FR,
                        COLUMN_NAME_ALTERNATIVE_DE,
                        COLUMN_NAME_ALTERNATIVE_EN,
                        COLUMN_NAME_COUNTRY_CODE,
                        COLUMN_NAME_LATITUDE,
                        COLUMN_NAME_LONGITUDE,
                        COLUMN_NAME_AVG_STOP_TIMES,
                        "(" + COLUMN_NAME_LATITUDE + "-" + location.getLatitude() + ")*(" + COLUMN_NAME_LATITUDE + "-" + location.getLatitude() + ")+(" + COLUMN_NAME_LONGITUDE + "-" + location.getLongitude() + ")*(" + COLUMN_NAME_LONGITUDE + "-" + location.getLongitude() + ") AS distance"
                },
                null,
                null,
                null,
                null,
                "distance ASC, " + COLUMN_NAME_AVG_STOP_TIMES + " DESC",
                String.valueOf(limit)
        );

        Station[] stations = loadStationCursor(c);

        Arrays.sort(stations, new Comparator<Station>() {
            @Override
            public int compare(Station o1, Station o2) {
                return Float.compare(o2.getAvgStopTimes(), o1.getAvgStopTimes());
            }
        });

        c.close();
        db.close();
        return stations;
    }

    /**
     * @inheritDoc
     */
    @Override
    public String[] getStationNames(Station[] Stations) {
        String[] results = new String[Stations.length];
        for (int i = 0; i < Stations.length; i++) {
            results[i] = Stations[i].getLocalizedName();
        }
        return results;
    }

    /**
     * @inheritDoc
     */
    @Override
    public Station getStationById(String id) {

        SQLiteOpenHelper StationsDbHelper = new StationsDb(context);
        SQLiteDatabase db = StationsDbHelper.getReadableDatabase();
        Cursor c = db.query(
                TABLE_NAME,
                new String[]{
                        _ID,
                        COLUMN_NAME_NAME,
                        COLUMN_NAME_ALTERNATIVE_NL,
                        COLUMN_NAME_ALTERNATIVE_FR,
                        COLUMN_NAME_ALTERNATIVE_DE,
                        COLUMN_NAME_ALTERNATIVE_EN,
                        COLUMN_NAME_COUNTRY_CODE,
                        COLUMN_NAME_LATITUDE,
                        COLUMN_NAME_LONGITUDE,
                        COLUMN_NAME_AVG_STOP_TIMES
                },
                _ID + "=?",
                new String[]{id},
                null,
                null,
                null,
                "1");

        Station[] results = loadStationCursor(c);
        if (results == null) {
            return null;
        }

        Station result = results[0];
        c.close();
        db.close();
        return result;
    }

    /**
     * @inheritDoc
     */
    @Override
    public Station getStationByName(String name) {
        SQLiteOpenHelper StationsDbHelper = new StationsDb(context);
        SQLiteDatabase db = StationsDbHelper.getReadableDatabase();
        name = name.replaceAll("\\(\\w\\)", "");
        String wcName = name.replaceAll("[^A-Za-z]", "%");
        Cursor c = db.query(
                TABLE_NAME,
                new String[]{
                        _ID,
                        COLUMN_NAME_NAME,
                        COLUMN_NAME_ALTERNATIVE_NL,
                        COLUMN_NAME_ALTERNATIVE_FR,
                        COLUMN_NAME_ALTERNATIVE_DE,
                        COLUMN_NAME_ALTERNATIVE_EN,
                        COLUMN_NAME_COUNTRY_CODE,
                        COLUMN_NAME_LATITUDE,
                        COLUMN_NAME_LONGITUDE,
                        COLUMN_NAME_AVG_STOP_TIMES
                },
                COLUMN_NAME_NAME + " LIKE ? OR " + COLUMN_NAME_ALTERNATIVE_FR + " LIKE ? OR " + COLUMN_NAME_ALTERNATIVE_NL +
                        " LIKE ? OR " + COLUMN_NAME_ALTERNATIVE_DE + " LIKE ? OR " + COLUMN_NAME_ALTERNATIVE_EN + " LIKE ?",
                new String[]{wcName, wcName, wcName, wcName, wcName},
                null,
                null,
                COLUMN_NAME_AVG_STOP_TIMES + " DESC",
                "1");
        if (c.getCount() < 1) {

            c.close();
            db.close();

            if (name.contains("/")) {
                String newname = name.substring(0, name.indexOf("/") - 1);
                Log.w("SQLiteStationProvider", "Station not found: " + name + " replacement search " + newname);
                return getStationByName(newname);
            } else if (name.contains("(")) {
                String newname = name.substring(0, name.indexOf("(") - 1);
                Log.w("SQLiteStationProvider", "Station not found: " + name + " replacement search " + newname);
                return getStationByName(newname);
            } else {
                Log.e("SQLiteStationProvider", "Station not found: " + name + " search " + name.replaceAll("[^A-Za-z]", "/"));
                return null;
            }
        }

        Station[] results = loadStationCursor(c);
        if (results == null) {
            return null;
        }

        Station result = results[0];

        c.close();
        db.close();
        return result;
    }

    /**
     * Load stations from a cursor. This method <strong>does not close the cursor afterwards</strong>.
     * @param c The cursor from which stations should be loaded.
     * @return The array of loaded stations
     */
    private Station[] loadStationCursor(Cursor c) {
        if (c.isClosed()) {
            return null;
        }

        c.moveToFirst();
        Station[] result = new Station[c.getCount()];
        int i = 0;
        while (!c.isAfterLast()) {
            Station s = new Station(
                    c.getString(c.getColumnIndex(_ID)),
                    c.getString(c.getColumnIndex(COLUMN_NAME_NAME)),
                    c.getString(c.getColumnIndex(COLUMN_NAME_ALTERNATIVE_NL)),
                    c.getString(c.getColumnIndex(COLUMN_NAME_ALTERNATIVE_FR)),
                    c.getString(c.getColumnIndex(COLUMN_NAME_ALTERNATIVE_DE)),
                    c.getString(c.getColumnIndex(COLUMN_NAME_ALTERNATIVE_EN)),
                    c.getString(c.getColumnIndex(COLUMN_NAME_COUNTRY_CODE)),
                    c.getDouble(c.getColumnIndex(COLUMN_NAME_LATITUDE)),
                    c.getDouble(c.getColumnIndex(COLUMN_NAME_LONGITUDE)),
                    c.getFloat(c.getColumnIndex(COLUMN_NAME_AVG_STOP_TIMES)));

            c.moveToNext();
            result[i] = s;
            i++;
        }
        return result;
    }
}