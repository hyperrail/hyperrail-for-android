/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.be.irail;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import be.hyperrail.opentransportdata.common.contracts.TransportStopsDataSource;
import be.hyperrail.opentransportdata.common.exceptions.StopLocationNotResolvedException;
import be.hyperrail.opentransportdata.common.models.StopLocation;
import be.hyperrail.opentransportdata.common.models.implementation.StopLocationImpl;
import be.hyperrail.opentransportdata.logging.OpenTransportLog;
import be.hyperrail.opentransportdata.util.StringUtils;

import static be.hyperrail.opentransportdata.be.irail.IrailStationsDataContract.StationsDataColumns;

/**
 * Database for querying stations
 */
public class IrailStationsDataProvider implements TransportStopsDataSource {

    private static final String ISO2_DE = "de";
    private static final String ISO2_EN = "en";
    private static final String ISO2_FR = "fr";
    private static final String ISO2_NL = "nl";
    private static final String SQL_SELECT_NAME_LIKE = StationsDataColumns.COLUMN_NAME_NAME + " LIKE ? OR " + StationsDataColumns.COLUMN_NAME_ALTERNATIVE_FR + " LIKE ? OR " + StationsDataColumns.COLUMN_NAME_ALTERNATIVE_NL +
            " LIKE ? OR " + StationsDataColumns.COLUMN_NAME_ALTERNATIVE_DE + " LIKE ? OR " + StationsDataColumns.COLUMN_NAME_ALTERNATIVE_EN + " LIKE ?";
    private static final OpenTransportLog log = OpenTransportLog.getLogger(IrailStationsDataProvider.class);
    private final Context context;
    private final Object getStoplocationsOrderedBySizeLock = new Object();
    // The underlying webDb instance, ensuring that the local SQLite database stays up-to-date with the online data
    private SQLiteOpenHelper mDbInstance;
    private HashMap<String, StopLocation> mStationIdCache = new HashMap<>();
    private HashMap<String, StopLocation> mStationNameCache = new HashMap<>();
    private StopLocation[] stationsOrderedBySizeCache;

    public IrailStationsDataProvider(Context appContext) {
        this.context = appContext;
        this.mDbInstance = new IrailStopsDatabase(appContext);
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
    @NonNull
    public StopLocation[] getStoplocationsOrderedBySize() {
        // Synchronized method to make better use of the cache
        synchronized (getStoplocationsOrderedBySizeLock) {
            log.debug("getStoplocationsOrderedBySize");
            if (stationsOrderedBySizeCache != null) {
                return stationsOrderedBySizeCache;
            }

            SQLiteDatabase db = mDbInstance.getReadableDatabase();
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
    }

    /**
     * @inheritDoc
     */

    @NonNull
    @Override
    public StopLocation[] getStoplocationsOrderedByLocation(Location location) {
        return this.getStoplocationsByNameOrderByLocation("", location);
    }

    /**
     * @inheritDoc
     */
    @Override
    @NonNull
    public StopLocation[] getStoplocationsOrderedByLocationAndSize(Location location, int limit) {
        SQLiteDatabase db = mDbInstance.getReadableDatabase();
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

    @Override
    public void preloadDatabase() {
        // Getting a readable database ensures onCreate and onUpgrade are called as needed
        mDbInstance.getReadableDatabase();
    }

    /**
     * @inheritDoc
     */
    @NonNull
    @Override
    public String[] getStoplocationsNames(@NonNull StopLocation[] stopLocations, boolean includeTranslations) {
        if (stopLocations.length == 0) {
            log.warning("Tried to load station names on empty station list!");
            return new String[0];
        }

        Set<String> results = new HashSet<>();
        for (StopLocation stopLocation : stopLocations) {
            results.add(stopLocation.getName());
            if (includeTranslations) {
                results.addAll(stopLocation.getTranslations().values());
            }
        }
        return results.toArray(new String[0]);
    }

    @Nullable
    @Override
    public StopLocation getStoplocationByHafasId(String id) throws StopLocationNotResolvedException {
        if (mStationIdCache.containsKey(id)) {
            return mStationIdCache.get(id);
        }

        if (id.startsWith("BE.NMBS.")) {
            // TODO: remove in the future when it is 100% sure this is no longer used
            id = id.substring(8);
            log.info("Incorrect call to getStopLocationByHafasId");
            log.info(Arrays.toString(Thread.currentThread().getStackTrace()));
            log.logException(new IllegalStateException("getStopLocationByHAfasId should not be used with a BE.NMBS.* id"));
        }

        try {
            return getStoplocationBySemanticId("http://irail.be/stations/NMBS/" + id);
        } catch (StopLocationNotResolvedException exception) {
            // This retry makes it easier to migrate away from Hafas IDs
            return getStoplocationBySemanticId(id);
        }
    }

    @Nullable
    @Override
    public StopLocation getStoplocationBySemanticId(String uri) throws StopLocationNotResolvedException {
        SQLiteDatabase db = mDbInstance.getReadableDatabase();
        Cursor c = db.query(
                StationsDataColumns.TABLE_NAME,
                getDefaultQueryColumns(),
                StationsDataColumns._ID + "=?",
                new String[]{uri},
                null,
                null,
                null,
                "1"
        );

        StopLocation[] results = loadStationCursor(c);

        c.close();

        if (results.length == 0) {
            log.logException(new IllegalStateException("URI not found in station database! " + uri));
            throw new StopLocationNotResolvedException(uri);
        }
        mStationIdCache.put(uri, results[0]);
        return results[0];
    }

    /**
     * @inheritDoc
     */
    @Override
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
    @NonNull
    public StopLocation[] getStoplocationsByNameOrderBySize(@NonNull String name) {
        return getStationsByNameOrderBySize(name, false);
    }

    @NonNull
    private StopLocation[] getStationsByNameOrderBySize(@NonNull String name, boolean exact) {
        SQLiteDatabase db = mDbInstance.getReadableDatabase();
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
    public StopLocation[] getStoplocationsByNameOrderByLocation(String name, Location location) {
        SQLiteDatabase db = mDbInstance.getReadableDatabase();

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

            String uri = c.getString(c.getColumnIndex(StationsDataColumns._ID));

            // Handle URIs
            String id = uri.substring(uri.lastIndexOf('/') + 1);


            StopLocation s = new StopLocationImpl(
                    id,
                    uri,
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