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
    private boolean shouldUseOnlineData;

    WebDbSqliteBackend(Context context, int version, WebDbDataDefinition definition, boolean shouldUseOnlineData) {
        // Calculate the version based on a code version and the last modified date, followed by a revision
        super(context, definition.getDatabaseName(), null, version);
        this.mDefinition = definition;
        this.shouldUseOnlineData = shouldUseOnlineData;
    }

    /**
     * Create the database.
     *
     * @param db Handle in which the database should be created.
     */
    public void onCreate(SQLiteDatabase db) {
        log.info("Creating WebDb instance " + mDefinition.getDatabaseName());
        createAndFillDb(db);
        log.info("Created WebDb instance " + mDefinition.getDatabaseName());
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        log.info("Upgrading WebDb instance " + mDefinition.getDatabaseName());
        wipeDatabase(db);
        createAndFillDb(db);
        log.info("Upgraded WebDb instance " + mDefinition.getDatabaseName());
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Do nothing
    }

    private void createAndFillDb(SQLiteDatabase db) {
        mDefinition.createDatabaseStructure(db);
        if (shouldUseOnlineData) {
            // If online fetching failed
            if (!mDefinition.loadOnlineData(db)) {
                log.warning("Failed to update WebDb instance using online data. " +
                        "Reverting to local data instead " + mDefinition.getDatabaseName());
                mDefinition.loadLocalData(db);
            }
        } else {
            mDefinition.loadLocalData(db);
        }
    }

    private void wipeDatabase(SQLiteDatabase db) {
        mDefinition.clearDatabase(db);
    }
}