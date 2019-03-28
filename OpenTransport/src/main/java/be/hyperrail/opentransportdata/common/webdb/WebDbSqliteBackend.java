/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */


package be.hyperrail.opentransportdata.common.webdb;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.joda.time.DateTime;

import be.hyperrail.opentransportdata.logging.OpenTransportLog;

/**
 * Database for querying stations
 */
class WebDbSqliteBackend extends SQLiteOpenHelper {

    // Logtag for logging purpose
    private static final String LOGTAG = "WebDbSqliteBackend";

    private final DateTime mLastModified;
    private final WebDbDataDefinition mDefinition;
    private boolean shouldUseOnlineData;

    WebDbSqliteBackend(Context context, DateTime lastModified, WebDbDataDefinition definition, boolean shouldUseOnlineData) {
        // Calculate the version based on a code version and the last modified date, followed by a revision
        super(context, definition.getDatabaseName(), null, Integer.valueOf("0" + lastModified.toString("YMd") + "00"));
        this.mLastModified = lastModified;
        this.mDefinition = definition;
        this.shouldUseOnlineData = shouldUseOnlineData;
    }

    /**
     * Create the database.
     *
     * @param db Handle in which the database should be created.
     */
    public void onCreate(SQLiteDatabase db) {
        OpenTransportLog.log("Creating WebDb instance " + mDefinition.getDatabaseName());
        mDefinition.onCreate(db, shouldUseOnlineData);
        OpenTransportLog.log("Created WebDb instance " + mDefinition.getDatabaseName());
    }


    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        OpenTransportLog.log("Upgrading WebDb instance " + mDefinition.getDatabaseName());
        mDefinition.onUpgrade(db, oldVersion, newVersion, shouldUseOnlineData);
        OpenTransportLog.log("Upgraded WebDb instance " + mDefinition.getDatabaseName());
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Do nothing
    }

    public DateTime getLastModified() {
        return mLastModified;
    }
}