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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import com.google.firebase.perf.metrics.AddTrace;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import be.hyperrail.opentransportdata.common.contracts.TransportStopsDataSource;
import be.hyperrail.opentransportdata.common.exceptions.StopLocationNotResolvedException;
import be.hyperrail.opentransportdata.common.models.StopLocation;
import be.hyperrail.opentransportdata.common.models.implementation.StopLocationImpl;
import be.hyperrail.opentransportdata.common.webdb.WebDb;
import be.hyperrail.opentransportdata.logging.OpenTransportLog;
import be.hyperrail.opentransportdata.util.StringUtils;

import static be.hyperrail.opentransportdata.be.irail.IrailStationsDataContract.StationsDataColumns;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

/**
 * Database for querying stations
 */
public class IrailStationsDataProvider implements TransportStopsDataSource {

    // Logtag for logging purpose
    private static final String LOGTAG = "database";

    private final Context context;

    // The underlying webDb instance, ensuring that the local SQLite database stays up-to-date with the online data
    private WebDb mWebDb;

    HashMap<String, StopLocation> mStationIdCache = new HashMap<>();
    HashMap<String, StopLocation> mStationNameCache = new HashMap<>();

    StopLocation[] stationsOrderedBySizeCache;

    public IrailStationsDataProvider(Context appContext) {
        this.context = appContext;
        this.mWebDb = new WebDb(appContext, new IrailStopsWebDbDataDefinition(context));
    }

    private String[] getLocationQueryColumns(double longitude, double latitude) {
        return new String[]{
                StationsDataColumns._ID,
                StationsDataColumns.COLUMN_NAME_NAME,
                StationsDataColumns.COLUMN_NAME_ALTERNATIVE_NL,
                StationsDataColumns.COLUMN_NAME_ALTERNATIVE_FR,
                StationsDataColumns.COLUMN_NAME_ALTERNATIVE_DE,
                StationsDataColumns.COLUMN_NAME_ALTERNATIVE_EN,
                StationsDataColumns.COLUMN_NAME_COUNTRY_CODE,
                StationsDataColumns.COLUMN_NAME_LATITUDE,
                StationsDataColumns.COLUMN_NAME_LONGITUDE,
                StationsDataColumns.COLUMN_NAME_AVG_STOP_TIMES,
                "(" + StationsDataColumns.COLUMN_NAME_LATITUDE + " - " + latitude + ")*(" + StationsDataColumns.COLUMN_NAME_LATITUDE + " - " + latitude
                        + ")+(" + StationsDataColumns.COLUMN_NAME_LONGITUDE + " - " + longitude + ")*(" + StationsDataColumns.COLUMN_NAME_LONGITUDE + " - " + longitude
                        + ") AS distance"
        };
    }

    private String[] getDefaultQueryColumns() {
        return new String[]{
                StationsDataColumns._ID,
                StationsDataColumns.COLUMN_NAME_NAME,
                StationsDataColumns.COLUMN_NAME_ALTERNATIVE_NL,
                StationsDataColumns.COLUMN_NAME_ALTERNATIVE_FR,
                StationsDataColumns.COLUMN_NAME_ALTERNATIVE_DE,
                StationsDataColumns.COLUMN_NAME_ALTERNATIVE_EN,
                StationsDataColumns.COLUMN_NAME_COUNTRY_CODE,
                StationsDataColumns.COLUMN_NAME_LATITUDE,
                StationsDataColumns.COLUMN_NAME_LONGITUDE,
                StationsDataColumns.COLUMN_NAME_AVG_STOP_TIMES
        };
    }


    @Override
    @AddTrace(name = "StationsDb.getStationsOrderBySize")
    public StopLocation[] getStationsOrderBySize() {

        if (stationsOrderedBySizeCache != null) {
            return stationsOrderedBySizeCache;
        }

        SQLiteDatabase db = mWebDb.getReadableDatabase();
        Cursor c = db.query(
                StationsDataColumns.TABLE_NAME,
                getDefaultQueryColumns(),
                null,
                null,
                null,
                null,
                StationsDataColumns.COLUMN_NAME_AVG_STOP_TIMES + " DESC"
        );

        StopLocation[] stations = loadStationCursor(c);
        c.close();

        if (stations == null) {
            return new StopLocation[0];
        }

        stationsOrderedBySizeCache = stations;

        return stations;
    }

    /**
     * @inheritDoc
     */

    @Override
    @AddTrace(name = "StationsDb.getStationsOrderByLocation")
    public StopLocation[] getStationsOrderByLocation(Location location) {
        StopLocation[] results = this.getStationsByNameOrderByLocation("", location);
        if (results == null) {
            return new StopLocation[0];
        }
        return results;
    }


    /**
     * @inheritDoc
     */

    @Override
    @AddTrace(name = "StationsDb.getStationsOrderByLocationAndSize")
    public StopLocation[] getStationsOrderByLocationAndSize(Location location, int limit) {
        SQLiteDatabase db = mWebDb.getReadableDatabase();
        double longitude = Math.round(location.getLongitude() * 1000000.0) / 1000000.0;
        double latitude = Math.round(location.getLatitude() * 1000000.0) / 1000000.0;

        Cursor c = db.query(
                StationsDataColumns.TABLE_NAME,
                getLocationQueryColumns(longitude, latitude),
                null,
                null,
                null,
                null,
                "distance ASC, " + StationsDataColumns.COLUMN_NAME_AVG_STOP_TIMES + " DESC",
                String.valueOf(limit)
        );

        StopLocation[] stations = loadStationCursor(c);

        c.close();


        if (stations == null) {
            return new StopLocation[0];
        }

        Arrays.sort(stations, new Comparator<StopLocation>() {
            @Override
            public int compare(StopLocation o1, StopLocation o2) {
                return Float.compare(o2.getAvgStopTimes(), o1.getAvgStopTimes());
            }
        });

        return stations;
    }

    /**
     * @inheritDoc
     */
    @Override
    @AddTrace(name = "StationsDb.getStationNames")
    public String[] getStationNames(StopLocation[] Stations) {

        if (Stations == null || Stations.length == 0) {
            OpenTransportLog.log(
                    WARNING.intValue(), LOGTAG,
                    "Tried to load station names on empty station list!"
            );
            return new String[0];
        }

        String[] results = new String[Stations.length];
        for (int i = 0; i < Stations.length; i++) {
            results[i] = Stations[i].getLocalizedName();
        }
        return results;
    }


    @Override
    public StopLocation getStationByUIC(String id) throws StopLocationNotResolvedException {
        return getStationByUIC(id, false);
    }


    @Override
    public StopLocation getStationByUIC(String id, boolean suppressErrors) throws StopLocationNotResolvedException {
        return getStationByHID("00" + id, false);
    }


    @Override
    public StopLocation getStationByHID(String id) throws StopLocationNotResolvedException {
        return getStationByHID(id, false);
    }


    @Override
    public StopLocation getStationByHID(String id, boolean suppressErrors) throws StopLocationNotResolvedException {
        if (mStationIdCache.containsKey(id)) {
            return mStationIdCache.get(id);
        }

        if (id.startsWith("BE.NMBS.")) {
            id = id.substring(8);
        }

        SQLiteDatabase db = mWebDb.getReadableDatabase();
        Cursor c = db.query(
                StationsDataColumns.TABLE_NAME,
                getDefaultQueryColumns(),
                StationsDataColumns._ID + "=?",
                new String[]{id},
                null,
                null,
                null,
                "1"
        );

        StopLocation[] results = loadStationCursor(c);

        c.close();

        if (results == null) {
            if (!suppressErrors) {
                OpenTransportLog.logException(
                        new IllegalStateException("ID Not found in station database! " + id));
            }
            throw new StopLocationNotResolvedException(id);
        }
        mStationIdCache.put(id, results[0]);
        return results[0];
    }


    @Override
    public StopLocation getStationByIrailApiId(String id) throws StopLocationNotResolvedException {
        if (id.startsWith("BE.NMBS.")) {
            id = id.substring(8);
        }
        return getStationByHID(id);
    }


    @Override
    public StopLocation getStationByUri(String uri) throws StopLocationNotResolvedException {
        return getStationByUri(uri, false);
    }


    @Override
    public StopLocation getStationByUri(String uri, boolean suppressErrors) throws StopLocationNotResolvedException {
        return getStationByHID(uri.substring(uri.lastIndexOf('/') + 1), suppressErrors);
    }

    /**
     * @inheritDoc
     */
    @Override
    @AddTrace(name = "StationsDb.getStationByExactName")
    @Nullable
    public StopLocation getStationByExactName(String name) {
        if (mStationNameCache.containsKey(name)) {
            return mStationNameCache.get(name);
        }
        StopLocation[] results = getStationsByNameOrderBySize(name, true);
        if (results == null) {
            return null;
        }

        mStationNameCache.put(name, results[0]);
        return results[0];
    }

    /**
     * @inheritDoc
     */
    @Override
    @Nullable
    @AddTrace(name = "StationsDb.getStationsByNameOrderBySize")
    public StopLocation[] getStationsByNameOrderBySize(String name) {
        return getStationsByNameOrderBySize(name, false);
    }

    private final static String stationNameSelection = StationsDataColumns.COLUMN_NAME_NAME + " LIKE ? OR " + StationsDataColumns.COLUMN_NAME_ALTERNATIVE_FR + " LIKE ? OR " + StationsDataColumns.COLUMN_NAME_ALTERNATIVE_NL +
            " LIKE ? OR " + StationsDataColumns.COLUMN_NAME_ALTERNATIVE_DE + " LIKE ? OR " + StationsDataColumns.COLUMN_NAME_ALTERNATIVE_EN + " LIKE ?";

    @Nullable
    public StopLocation[] getStationsByNameOrderBySize(String name, boolean exact) {
        SQLiteDatabase db = mWebDb.getReadableDatabase();
        name = StringUtils.cleanAccents(name);
        name = name.replaceAll("\\(\\w\\)", "");

        String cleanedName = name.replaceAll("[^A-Za-z]", "%");
        if (!exact) {
            cleanedName = "%" + cleanedName + "%";
        }

        Cursor c = db.query(
                StationsDataColumns.TABLE_NAME,
                getDefaultQueryColumns(),
                stationNameSelection,
                new String[]{cleanedName, cleanedName, cleanedName, cleanedName, cleanedName},
                null,
                null,
                StationsDataColumns.COLUMN_NAME_AVG_STOP_TIMES + " DESC",
                null
        );

        if (c.getCount() < 1) {
            c.close();

            if (name.contains("/")) {
                String newname = name.substring(0, name.indexOf("/"));
                OpenTransportLog.log(
                        WARNING.intValue(), "SQLiteStationProvider",
                        "Station not found: " + name + ", replacement search " + newname
                );
                return getStationsByNameOrderBySize(newname);
            } else if (name.contains("(")) {
                String newname = name.substring(0, name.indexOf("("));
                OpenTransportLog.log(
                        WARNING.intValue(), "SQLiteStationProvider",
                        "Station not found: " + name + ", replacement search " + newname
                );
                return getStationsByNameOrderBySize(newname);
            } else if (name.toLowerCase().startsWith("s ") || cleanedName.toLowerCase().startsWith("s%")) {
                String newname = "'" + name;
                OpenTransportLog.log(
                        WARNING.intValue(), "SQLiteStationProvider",
                        "Station not found: " + name + ", replacement search " + newname
                );
                return getStationsByNameOrderBySize(newname);
            } else {
                OpenTransportLog.log(
                        SEVERE.intValue(), "SQLiteStationProvider",
                        "Station not found: " + name + ", cleaned search " + cleanedName
                );
                return null;
            }
        }

        StopLocation[] results = loadStationCursor(c);

        c.close();


        if (results == null) {
            return null;
        }
        return results;
    }

    /**
     * @inheritDoc
     */
    @Override
    @AddTrace(name = "StationsDb.getStationsByNameOrderByLocation")
    public StopLocation[] getStationsByNameOrderByLocation(String name, Location location) {
        SQLiteDatabase db = mWebDb.getReadableDatabase();

        double longitude = Math.round(location.getLongitude() * 1000000.0) / 1000000.0;
        double latitude = Math.round(location.getLatitude() * 1000000.0) / 1000000.0;
        name = StringUtils.cleanAccents(name);
        name = name.replaceAll("\\(\\w\\)", "");
        Cursor c = db.query(
                StationsDataColumns.TABLE_NAME,
                getLocationQueryColumns(longitude, latitude),
                StationsDataColumns.COLUMN_NAME_NAME + " LIKE ? OR " + StationsDataColumns.COLUMN_NAME_ALTERNATIVE_FR + " LIKE ? OR " + StationsDataColumns.COLUMN_NAME_ALTERNATIVE_NL +
                        " LIKE ? OR " + StationsDataColumns.COLUMN_NAME_ALTERNATIVE_DE + " LIKE ? OR " + StationsDataColumns.COLUMN_NAME_ALTERNATIVE_EN + " LIKE ?",
                new String[]{"%" + name + "%", "%" + name + "%", "%" + name + "%", "%" + name + "%",
                        "%" + name + "%"},
                null,
                null,
                "distance ASC, " + StationsDataColumns.COLUMN_NAME_AVG_STOP_TIMES + " DESC"
        );

        StopLocation[] stations = loadStationCursor(c);
        c.close();

        return stations;
    }

    /**
     * Load stations from a cursor. This method <strong>does not close the cursor afterwards</strong>.
     *
     * @param c The cursor from which stations should be loaded.
     * @return The array of loaded stations
     */
    private StopLocation[] loadStationCursor(Cursor c) {
        if (c.isClosed()) {
            OpenTransportLog.log(SEVERE.intValue(), LOGTAG, "Tried to load closed cursor");
            return null;
        }

        if (c.getCount() == 0) {
            OpenTransportLog.log(SEVERE.intValue(), LOGTAG, "Tried to load cursor with 0 results!");
            return null;
        }

        c.moveToFirst();
        StopLocation[] result = new StopLocation[c.getCount()];
        int i = 0;
        while (!c.isAfterLast()) {

            String locale = PreferenceManager.getDefaultSharedPreferences(context).getString(
                    "pref_stations_language", "");
            if (locale.isEmpty()) {
                // Only get locale when needed
                locale = Locale.getDefault().getISO3Language();
                PreferenceManager.getDefaultSharedPreferences(context).edit().putString(
                        "pref_stations_language", locale).apply();
            }

            String name = c.getString(c.getColumnIndex(StationsDataColumns.COLUMN_NAME_NAME));
            String localizedName = null;

            Map<String, String> localizedNames = new HashMap<>();
            localizedNames.put("nl_BE", c.getString(
                    c.getColumnIndex(StationsDataColumns.COLUMN_NAME_ALTERNATIVE_NL)));
            localizedNames.put("fr_FR", c.getString(
                    c.getColumnIndex(StationsDataColumns.COLUMN_NAME_ALTERNATIVE_FR)));
            localizedNames.put("de_DE", c.getString(
                    c.getColumnIndex(StationsDataColumns.COLUMN_NAME_ALTERNATIVE_DE)));
            localizedNames.put("en_UK", c.getString(
                    c.getColumnIndex(StationsDataColumns.COLUMN_NAME_ALTERNATIVE_EN)));

            switch (locale) {
                case "nld":
                    localizedName = localizedNames.get("nl_BE");
                    break;
                case "fra":
                    localizedName = localizedNames.get("fr_FR");
                    break;
                case "deu":
                    localizedName = localizedNames.get("de_DE");
                    break;
                case "eng":
                    localizedName = localizedNames.get("en_UK");
                    break;
            }

            if (localizedName == null || localizedName.isEmpty()) {
                localizedName = name;
            }

            StopLocation s = new StopLocationImpl(
                    c.getString(c.getColumnIndex(StationsDataColumns._ID)),
                    name,
                    localizedNames,
                    localizedName,
                    c.getString(c.getColumnIndex(StationsDataColumns.COLUMN_NAME_COUNTRY_CODE)),
                    c.getDouble(c.getColumnIndex(StationsDataColumns.COLUMN_NAME_LATITUDE)),
                    c.getDouble(c.getColumnIndex(StationsDataColumns.COLUMN_NAME_LONGITUDE)),
                    c.getFloat(c.getColumnIndex(StationsDataColumns.COLUMN_NAME_AVG_STOP_TIMES))
            );

            c.moveToNext();
            result[i] = s;
            i++;
        }
        return result;
    }
}