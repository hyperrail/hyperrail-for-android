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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.firebase.perf.metrics.AddTrace;

import java.util.Arrays;
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

/**
 * Database for querying stations
 */
public class IrailStationsDataProvider implements TransportStopsDataSource {

    private static final OpenTransportLog log = OpenTransportLog.getLogger(IrailStationsDataProvider.class);
    private static final String ISO2_NL = "nl";
    private static final String ISO2_FR = "fr";
    private static final String ISO2_DE = "de";
    private static final String ISO2_EN = "en";

    private static final String SQL_SELECT_NAME_LIKE = StationsDataColumns.COLUMN_NAME_NAME + " LIKE ? OR " + StationsDataColumns.COLUMN_NAME_ALTERNATIVE_FR + " LIKE ? OR " + StationsDataColumns.COLUMN_NAME_ALTERNATIVE_NL +
            " LIKE ? OR " + StationsDataColumns.COLUMN_NAME_ALTERNATIVE_DE + " LIKE ? OR " + StationsDataColumns.COLUMN_NAME_ALTERNATIVE_EN + " LIKE ?";

    private final Context context;

    // The underlying webDb instance, ensuring that the local SQLite database stays up-to-date with the online data
    private WebDb mWebDb;

    private HashMap<String, StopLocation> mStationIdCache = new HashMap<>();
    private HashMap<String, StopLocation> mStationNameCache = new HashMap<>();

    private StopLocation[] stationsOrderedBySizeCache;

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
    @AddTrace(name = "StationsDb.getStoplocationsOrderedBySize")
    @NonNull
    public StopLocation[] getStoplocationsOrderedBySize() {

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

        stationsOrderedBySizeCache = stations;
        return stations;
    }

    /**
     * @inheritDoc
     */

    @NonNull
    @Override
    @AddTrace(name = "StationsDb.getStoplocationsOrderedByLocation")
    public StopLocation[] getStoplocationsOrderedByLocation(Location location) {
        return this.getStoplocationsByNameOrderByLocation("", location);
    }

    /**
     * @inheritDoc
     */
    @Override
    @AddTrace(name = "StationsDb.getStoplocationsOrderedByLocationAndSize")
    @NonNull
    public StopLocation[] getStoplocationsOrderedByLocationAndSize(Location location, int limit) {
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

        Arrays.sort(stations, (o1, o2) -> Float.compare(o2.getAvgStopTimes(), o1.getAvgStopTimes()));

        return stations;
    }

    /**
     * @inheritDoc
     */
    @NonNull
    @Override
    @AddTrace(name = "StationsDb.getStoplocationsNames")
    public String[] getStoplocationsNames(@NonNull StopLocation[] stopLocations) {
        if (stopLocations.length == 0) {
            log.warning("Tried to load station names on empty station list!");
            return new String[0];
        }

        String[] results = new String[stopLocations.length];
        for (int i = 0; i < stopLocations.length; i++) {
            results[i] = stopLocations[i].getLocalizedName();
        }
        return results;
    }

    @Nullable
    @Override
    public StopLocation getStoplocationByHafasId(String id) throws StopLocationNotResolvedException {
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

        if (results.length == 0) {
            log.logException(new IllegalStateException("ID Not found in station database! " + id));
            throw new StopLocationNotResolvedException(id);
        }
        mStationIdCache.put(id, results[0]);
        return results[0];
    }

    @Nullable
    @Override
    public StopLocation getStoplocationBySemanticId(String id) throws StopLocationNotResolvedException {
        if (id.startsWith("BE.NMBS.")) {
            // Handle old iRail ids
            id = id.substring(8);
        }
        if (id.contains("/")){
            // Handle URIs
            id = id.substring(id.lastIndexOf('/') + 1);
        }
        return getStoplocationByHafasId(id);
    }

    /**
     * @inheritDoc
     */
    @Override
    @AddTrace(name = "StationsDb.getStoplocationByExactName")
    @Nullable
    public StopLocation getStoplocationByExactName(@NonNull String name) {
        if (mStationNameCache.containsKey(name)) {
            return mStationNameCache.get(name);
        }
        StopLocation[] results = getStationsByNameOrderBySize(name, true);

        if (results.length < 1) {
            return null;
        }

        mStationNameCache.put(name, results[0]);
        return results[0];
    }

    /**
     * @inheritDoc
     */
    @Override
    @AddTrace(name = "StationsDb.getStoplocationsByNameOrderBySize")
    @NonNull
    public StopLocation[] getStoplocationsByNameOrderBySize(@NonNull String name) {
        return getStationsByNameOrderBySize(name, false);
    }

    @NonNull
    private StopLocation[] getStationsByNameOrderBySize(@NonNull String name, boolean exact) {
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
                SQL_SELECT_NAME_LIKE,
                new String[]{cleanedName, cleanedName, cleanedName, cleanedName, cleanedName},
                null,
                null,
                StationsDataColumns.COLUMN_NAME_AVG_STOP_TIMES + " DESC",
                null
        );

        if (c.getCount() < 1) {
            c.close();

            if (name.contains("/")) {
                String newname = name.substring(0, name.indexOf('/'));
                log.warning(String.format("Station not found: %s, replacement search %s", name, newname));
                return getStoplocationsByNameOrderBySize(newname);
            } else if (name.contains("(")) {
                String newname = name.substring(0, name.indexOf('('));
                log.warning(String.format("Station not found: %s, replacement search %s", name, newname));
                return getStoplocationsByNameOrderBySize(newname);
            } else if (name.toLowerCase().startsWith("s ") || cleanedName.toLowerCase().startsWith("s%")) {
                String newname = "'" + name;
                log.warning(String.format("Station not found: %s, replacement search %s", name, newname));
                return getStoplocationsByNameOrderBySize(newname);
            } else {
                log.severe(String.format("Station not found: %s, cleaned search %s", name, cleanedName));
                return new StopLocation[0];
            }
        }

        StopLocation[] results = loadStationCursor(c);
        c.close();
        return results;
    }

    /**
     * @inheritDoc
     */
    @NonNull
    @Override
    @AddTrace(name = "StationsDb.getStoplocationsByNameOrderByLocation")
    public StopLocation[] getStoplocationsByNameOrderByLocation(String name, Location location) {
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
    @NonNull
    private StopLocation[] loadStationCursor(@NonNull Cursor c) {
        if (c.isClosed()) {
            log.severe("Tried to load closed cursor");
            return new StopLocation[0];
        }

        if (c.getCount() == 0) {
            log.warning("Tried to load cursor with 0 results!");
            return new StopLocation[0];
        }

        c.moveToFirst();
        StopLocation[] result = new StopLocation[c.getCount()];
        int i = 0;
        while (!c.isAfterLast()) {

            String locale = PreferenceManager.getDefaultSharedPreferences(context).getString(
                    "pref_stations_language", "");

            if (locale == null || locale.isEmpty()) {
                // Only get locale when needed
                locale = Locale.getDefault().getISO3Language();
                PreferenceManager.getDefaultSharedPreferences(context).edit().putString(
                        "pref_stations_language", locale).apply();
            }

            String name = c.getString(c.getColumnIndex(StationsDataColumns.COLUMN_NAME_NAME));
            String localizedName;

            Map<String, String> localizedNames = new HashMap<>();
            localizedNames.put(ISO2_NL, c.getString(
                    c.getColumnIndex(StationsDataColumns.COLUMN_NAME_ALTERNATIVE_NL)));
            localizedNames.put(ISO2_FR, c.getString(
                    c.getColumnIndex(StationsDataColumns.COLUMN_NAME_ALTERNATIVE_FR)));
            localizedNames.put(ISO2_DE, c.getString(
                    c.getColumnIndex(StationsDataColumns.COLUMN_NAME_ALTERNATIVE_DE)));
            localizedNames.put(ISO2_EN, c.getString(
                    c.getColumnIndex(StationsDataColumns.COLUMN_NAME_ALTERNATIVE_EN)));

            switch (locale) {
                case "nld":
                    localizedName = localizedNames.get(ISO2_NL);
                    break;
                case "fra":
                    localizedName = localizedNames.get(ISO2_FR);
                    break;
                case "deu":
                    localizedName = localizedNames.get(ISO2_DE);
                    break;
                case "eng":
                default:
                    localizedName = localizedNames.get(ISO2_EN);
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