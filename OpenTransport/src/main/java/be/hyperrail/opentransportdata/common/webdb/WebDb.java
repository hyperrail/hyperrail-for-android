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
import android.util.ArrayMap;

import org.joda.time.DateTime;

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
    private WebDbDataDefinition dataDefinition;

    /**
     * Instantiate a new WebDb, according to the parameters defined in the WebDbDataDefinition.
     * Don't run this code on the main thread when requesting an update as it contains blocking I/O
     *
     * @param appContext     The Android application context
     * @param dataDefinition The data definition, containing both local and remote names as well as methods to create the database.
     */
    private WebDb(Context appContext, WebDbDataDefinition dataDefinition, boolean updateDataNow) {
        WebDbConfig webDbConfig = new WebDbConfig(appContext);
        this.dataDefinition = dataDefinition;

        log.info("Creating a new WebDb instance for " + dataDefinition.getDatabaseName());

        if (updateDataNow) {
            Object onlineUpdateData = dataDefinition.downloadOnlineData();
            synchronized (databaseModificationLock) {
                this.db = new WebDbSqliteBackend(appContext,
                        getVersionCodeForDateTime(DateTime.now()), webDbConfig, dataDefinition, onlineUpdateData);
            }

        } else {
            int currentVersion = webDbConfig.getCurrentDatabaseVersion(dataDefinition.getDatabaseName());
            int embeddedVersion = getVersionCodeForDateTime(dataDefinition.getLastModifiedLocalDate());

            // Determine if we can update with more recent embedded data
            int versionToLoad;
            if (currentVersion > embeddedVersion) {
                versionToLoad = currentVersion;
            } else {
                versionToLoad = embeddedVersion;
            }

            synchronized (databaseModificationLock) {
                this.db = new WebDbSqliteBackend(appContext, versionToLoad, webDbConfig, dataDefinition, null);
            }
        }


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
                instances.put(dataDefinition.getDatabaseName(), new WebDb(appContext, dataDefinition, false));
            }
            return instances.get(dataDefinition.getDatabaseName());
        }
    }

    public static WebDb getUpdatingInstance(Context appContext, WebDbDataDefinition dataDefinition) {
        synchronized (instanceGetterLock) {
            // Remove existing instances
            if (instances.containsKey(dataDefinition.getDatabaseName())) {
                log.severe("getUpdatingInstance should be called before getInstance! This database is already in use and won't be updated");
                return instances.get(dataDefinition.getDatabaseName());
            }

            instances.put(dataDefinition.getDatabaseName(), new WebDb(appContext, dataDefinition, true));
            return instances.get(dataDefinition.getDatabaseName());
        }
    }

    private static int getVersionCodeForDateTime(DateTime dateTime) {
        return Integer.valueOf(dateTime.toString("YYMMDD") + "00");
    }

    public SQLiteDatabase getReadableDatabase() {
        synchronized (databaseModificationLock) {
            // This will cause database creation
            log.info("A readable database was request for " + dataDefinition.getDatabaseName());
            return db.getReadableDatabase();
        }
    }
}