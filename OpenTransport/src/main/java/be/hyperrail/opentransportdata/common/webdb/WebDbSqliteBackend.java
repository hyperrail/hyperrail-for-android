/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */


package be.hyperrail.opentransportdata.common.webdb;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import be.hyperrail.opentransportdata.logging.OpenTransportLog;

/**
 * Database for querying stations
 */
class WebDbSqliteBackend extends SQLiteOpenHelper {

    private static final OpenTransportLog log = OpenTransportLog.getLogger(WebDbSqliteBackend.class);

    private final WebDbDataDefinition mDefinition;
    private final int version;
    private final WebDbConfig webDbConfig;
    private Object onlineUpdateData;

    WebDbSqliteBackend(Context context, int version, WebDbConfig webDbConfig, WebDbDataDefinition definition, Object onlineUpdateData) {
        // Calculate the version based on a code version and the last modified date, followed by a revision
        super(context, definition.getDatabaseName(), null, version);
        this.version = version;
        this.webDbConfig = webDbConfig;
        log.info("Creating a new WebDbSqliteBackend instance for " + definition.getDatabaseName());
        this.mDefinition = definition;
        this.onlineUpdateData = onlineUpdateData;
    }

    /**
     * Create the database.
     *
     * @param db Handle in which the database should be created.
     */
    @Override
    public synchronized void onCreate(SQLiteDatabase db) {
        recreateDb(db, "onCreate WebDbBackend instance ");
    }

    @Override
    public synchronized void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        recreateDb(db, "onUpgrade WebDbBackend instance");
    }

    private void recreateDb(SQLiteDatabase db, String actionNameForLogging) {
        log.info(actionNameForLogging + mDefinition.getDatabaseName());
        if (onlineUpdateData != null) {
            log.info("Online update data is provided.");
        }
        wipeDatabase(db);
        createAndFillDb(db);
        log.info(actionNameForLogging + mDefinition.getDatabaseName());
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Do nothing
    }

    private void createAndFillDb(SQLiteDatabase db) {
        mDefinition.createDatabaseStructure(db);
        if (onlineUpdateData != null) {
            // If online fetching failed
            if (!mDefinition.importDownloadedData(db, onlineUpdateData)) {
                log.warning("Failed to update WebDb instance using online data. " +
                        "Reverting to local data instead " + mDefinition.getDatabaseName());
                mDefinition.loadLocalData(db);
            } else {
                webDbConfig.setTimeOfLastOnlineUpdateToNow(mDefinition.getDatabaseName());
                webDbConfig.setCurrentDatabaseVersion(mDefinition.getDatabaseName(), version);
            }
        } else {
            mDefinition.loadLocalData(db);
        }
    }

    private void wipeDatabase(SQLiteDatabase db) {
        log.info("Wiping WebDbBackend " + mDefinition.getDatabaseName());
        mDefinition.deleteDatabase(db);
    }
}