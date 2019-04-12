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


package be.hyperrail.opentransportdata.common.webdb;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.ArrayMap;

import org.joda.time.DateTime;

import java.util.Arrays;
import java.util.Map;

import be.hyperrail.opentransportdata.logging.OpenTransportLog;

/**
 * A database which can automatically update from a predefined web page.
 * Using this database removes the need for application updates every time something changes
 */
public class WebDb {

    private static final Object instanceGetterLock = new Object();
    private static final Map<String, WebDb> instances = new ArrayMap<>();
    private static final OpenTransportLog log = OpenTransportLog.getLogger(WebDb.class);
    private final Object databaseModificationLock = new Object();
    private volatile WebDbSqliteBackend db;
    private Context context;
    private WebDbDataDefinition dataDefinition;
    private WebDbConfig webDbConfig;

    /**
     * Instantiate a new WebDb, according to the parameters defined in the WebDbDataDefinition.
     * Don't run this code on the main thread as it contains blocking I/O
     *
     * @param appContext     The Android application context
     * @param dataDefinition The data definition, containing both local and remote names as well as methods to create the database.
     */
    private WebDb(Context appContext, WebDbDataDefinition dataDefinition) {
        this.context = appContext;
        this.webDbConfig = new WebDbConfig(appContext);
        this.dataDefinition = dataDefinition;

        log.info("Creating a new WebDb instance for " + dataDefinition.getDatabaseName());


        int currentVersion = webDbConfig.getCurrentDatabaseVersion(dataDefinition.getDatabaseName());
        int embeddedVersion = getVersionCodeForDateTime(dataDefinition.getLastModifiedLocalDate());
        if (currentVersion < embeddedVersion) {
            currentVersion = embeddedVersion;
        }

        synchronized (databaseModificationLock) {
            this.db = new WebDbSqliteBackend(appContext, currentVersion, dataDefinition, null);
        }

        //updateDatabaseIfConnected();
        log.debug("Created a new WebDb instance for " + dataDefinition.getDatabaseName());
    }

    /**
     * Instantiate a new WebDb, according to the parameters defined in the WebDbDataDefinition.
     * Don't run this code on the main thread as it contains blocking I/O
     *
     * @param appContext     The Android application context
     * @param dataDefinition The data definition, containing both local and remote names as well as methods to create the database.
     */
    public static WebDb getInstance(Context appContext, WebDbDataDefinition dataDefinition) {
        synchronized (instanceGetterLock) {
            if (!instances.containsKey(dataDefinition.getDatabaseName())) {
                instances.put(dataDefinition.getDatabaseName(), new WebDb(appContext, dataDefinition));
            }
            return instances.get(dataDefinition.getDatabaseName());
        }
    }

    private static int getVersionCodeForDateTime(DateTime dateTime) {
        return Integer.valueOf(dateTime.toString("YYMMDD") + "00");
    }

    private void updateDatabaseIfConnected() {
        // If the last check was more than 2 days ago, try to check
        if (webDbConfig.getTimeOfLastCheck(dataDefinition.getDatabaseName()).isBefore(DateTime.now().minusDays(2))) {
            // If not restricted to Wifi, or if connecected to wifi, check for updates
            boolean isConnectedToWifi = isConnectedToWifi();
            log.info("WLAN: " + isConnectedToWifi);
            if (!dataDefinition.updateOnlyOnWifi() || isConnectedToWifi) {
                log.info("Starting update check for " + dataDefinition.getDatabaseName());
                UpdateDatabaseIfNeeded updateTask = new UpdateDatabaseIfNeeded(this, dataDefinition);
                updateTask.execute(dataDefinition);
            } else {
                log.info("Not starting update check for " + dataDefinition.getDatabaseName());
            }
        }
    }

    private boolean isConnectedToWifi() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Network wifiNetwork = connectivityManager.getActiveNetwork();
            final NetworkCapabilities capabilities = connectivityManager
                    .getNetworkCapabilities(wifiNetwork);
            return capabilities != null
                    && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) && !capabilities.hasCapability(NetworkCapabilities.TRANSPORT_CELLULAR);
        } else {
            // For older devices
            NetworkInfo wifiNetwork = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            return wifiNetwork.isConnected();
        }

    }

    public SQLiteDatabase getReadableDatabase() {
        log.debug("GETTING READABLE DATABASE " + dataDefinition.getDatabaseName());
        log.debug(Arrays.toString(Thread.currentThread().getStackTrace()));
        synchronized (databaseModificationLock) {
            // This will cause database creation
            return db.getReadableDatabase();
        }
    }

    private static class UpdateDatabaseIfNeeded extends AsyncTask<WebDbDataDefinition, Void, Boolean> {
        private WebDb webDbRef;
        private WebDbDataDefinition definition;

        // only retain a weak reference to the activity
        UpdateDatabaseIfNeeded(WebDb webDb, WebDbDataDefinition definition) {
            webDbRef = webDb;
            this.definition = definition;
        }

        @Override
        protected Boolean doInBackground(WebDbDataDefinition... definitions) {
            DateTime lastModifiedOnline = definitions[0].getLastModifiedOnlineDate();

            log.info("Online last modified date check for " +
                    definition.getDatabaseName() + " resulted in " + lastModifiedOnline.toString("YYYY-MM-HH HH:mm"));
            String databaseName = definition.getDatabaseName();

            WebDb webDb = webDbRef;

            if (webDb == null) {
                return false;
            }

            if (!lastModifiedOnline.isAfter(webDb.webDbConfig.getTimeOfLastOnlineUpdate(databaseName))) {
                log.info("No newer data available: " + databaseName);
                return false;
            }

            Object newData = definition.downloadOnlineData();
            if (newData == null) {
                log.warning("Failed to get updated data from internet for database " + definition.getDatabaseName() + ", aborting update");
                return false;
            }

            log.info("Re-creating database using online data: " + databaseName);
            synchronized (webDb.databaseModificationLock) {
                try {
                    // Allow other threads to finish their database queries which happen outside synchronized blocks
                    wait(1000);
                } catch (Exception e) {
                    // Ignored
                }
                webDb.db.close();
                webDb.db = new WebDbSqliteBackend(webDb.context, getVersionCodeForDateTime(lastModifiedOnline), definition, newData);
                webDb.db.getReadableDatabase(); // Ensure we populate the database as well
            }
            log.info("Re-created database using online data: " + databaseName);
            webDb.webDbConfig.setTimeOfLastOnlineUpdateToNow(databaseName);
            webDb.webDbConfig.setTimeOfLastCheckToNow(databaseName);
            return true;
        }
    }
}