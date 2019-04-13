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
import java.util.Scanner;

import be.hyperrail.opentransportdata.be.R;
import be.hyperrail.opentransportdata.common.webdb.WebDbDataDefinition;
import be.hyperrail.opentransportdata.logging.OpenTransportLog;
import be.hyperrail.opentransportdata.util.StringUtils;

import static be.hyperrail.opentransportdata.be.irail.IrailStationsDataContract.SQL_CREATE_INDEX_ID;
import static be.hyperrail.opentransportdata.be.irail.IrailStationsDataContract.SQL_CREATE_INDEX_NAME;
import static be.hyperrail.opentransportdata.be.irail.IrailStationsDataContract.SQL_CREATE_TABLE_STATIONS;
import static be.hyperrail.opentransportdata.be.irail.IrailStationsDataContract.SQL_DELETE_TABLE_STATIONS;
import static be.hyperrail.opentransportdata.be.irail.IrailStationsDataContract.StationsDataColumns;

/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * (c) Bert Marcelis 2018
 */
class IrailStopsWebDbDataDefinition implements WebDbDataDefinition {
    private static final OpenTransportLog log = OpenTransportLog.getLogger(IrailStopsWebDbDataDefinition.class);
    private static IrailStopsWebDbDataDefinition instance;
    private final Resources mResources;

    private IrailStopsWebDbDataDefinition(Context context) {
        mResources = context.getResources();
    }

    public static IrailStopsWebDbDataDefinition getInstance(Context context) {
        if (instance == null) {
            instance = new IrailStopsWebDbDataDefinition(context);
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
        return R.raw.stations;
    }

    @Override
    public String getOnlineDataURL() {
        return "https://raw.githubusercontent.com/iRail/stations/master/stations.csv";
    }

    @Override
    public String getDatabaseName() {
        return "irail-stations.db";
    }

    @Override
    public DateTime getLastModifiedLocalDate() {
        return new DateTime(2019, 4, 1, 0, 0);
    }

    @Override
    public DateTime getLastModifiedOnlineDate() {
        // Github doesn't send a proper last modified header. Instead we use the last saturday (can be today)
        return getSaturdayBeforeToday();
    }

    @Override
    public void createDatabaseStructure(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE_STATIONS);
        db.execSQL(SQL_CREATE_INDEX_ID);
        db.execSQL(SQL_CREATE_INDEX_NAME);
    }

    @Override
    public boolean loadLocalData(SQLiteDatabase db) {
        db.beginTransaction();
        try (Scanner lines = new Scanner(getLocalData())) {
            importData(db, lines);
            db.setTransactionSuccessful();
            db.endTransaction();
        } catch (Exception e) {
            log.severe("Failed to fill stations db with local data!", e);
            db.endTransaction();
            return false;
        }
        return true;
    }

    @Override
    public boolean importDownloadedData(SQLiteDatabase db, Object onlineUpdateData) {
        db.beginTransaction();
        try (Scanner lines = new Scanner((String) onlineUpdateData)) {
            importData(db, lines);
            db.setTransactionSuccessful();
            log.info("Filled stations database with online data");
            return true;
        } catch (Exception e) {
            db.endTransaction();
            log.severe("Failed to fill stations db with online data!", e);
            return false;
        }
    }

    @Override
    public void clearDatabase(SQLiteDatabase db) {
        db.execSQL(SQL_DELETE_TABLE_STATIONS);
    }

    @NonNull
    private DateTime getSaturdayBeforeToday() {
        DateTime now = DateTime.now();
        // On a saturday (6) this will be now (+0), on sunday it will be saturday (+1), on monday it will be +2, ...
        // This way we update every saturday
        return now.minusDays(now.dayOfWeek().get() + 1 % 7);
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
        while ((line = reader.readLine()) != null)
        {
            stringBuilder.append(line).append("\n");
        }
        return stringBuilder.toString();
    }

    private InputStream getLocalData() {
        return mResources.openRawResource(getEmbeddedDataResourceId());
    }

    private void importData(SQLiteDatabase db, Scanner lines) {
        lines.useDelimiter("\n");

        while (lines.hasNext()) {
            String line = lines.next();
            if (line.startsWith("URI,name")) {
                // Header line
                continue;
            }

            try (Scanner fields = new Scanner(line)) {
                importRow(db, fields);
            }
        }
    }

    private void importRow(SQLiteDatabase db, Scanner fields) {
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
                StringUtils.cleanAccents(fields.next())
        );
        values.put(
                StationsDataColumns.COLUMN_NAME_ALTERNATIVE_FR,
                StringUtils.cleanAccents(fields.next())
        );
        values.put(
                StationsDataColumns.COLUMN_NAME_ALTERNATIVE_NL,
                StringUtils.cleanAccents(fields.next())
        );
        values.put(
                StationsDataColumns.COLUMN_NAME_ALTERNATIVE_DE,
                StringUtils.cleanAccents(fields.next())
        );
        values.put(
                StationsDataColumns.COLUMN_NAME_ALTERNATIVE_EN,
                StringUtils.cleanAccents(fields.next())
        );
        values.put(StationsDataColumns.COLUMN_NAME_COUNTRY_CODE, fields.next());

        String field = fields.next();
        insertDoubleSafely(values, field, StationsDataColumns.COLUMN_NAME_LONGITUDE);

        field = fields.next();
        insertDoubleSafely(values, field, StationsDataColumns.COLUMN_NAME_LATITUDE);

        field = fields.next();
        insertDoubleSafely(values, field, StationsDataColumns.COLUMN_NAME_AVG_STOP_TIMES);

        field = fields.next();
        insertDoubleSafely(values, field, StationsDataColumns.COLUMN_NAME_OFFICIAL_TRANSFER_TIME);
        // Insert row
        db.insert(StationsDataColumns.TABLE_NAME, null, values);
    }

    private void insertDoubleSafely(ContentValues values, String field, String columnName) {
        if (field != null && !field.isEmpty()) {
            try {
                values.put(
                        columnName,
                        Double.parseDouble(field)
                );

            } catch (NumberFormatException e) {
                values.put(columnName, 0);
            }
        } else {
            values.put(columnName, 0);
        }
    }
}
