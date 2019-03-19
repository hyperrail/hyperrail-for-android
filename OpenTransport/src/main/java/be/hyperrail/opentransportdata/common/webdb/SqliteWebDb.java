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

/**
 * Database for querying stations
 */
class SqliteWebDb extends SQLiteOpenHelper {

    // Logtag for logging purpose
    private static final String LOGTAG = "SqliteWebDb";

    private final Context mContext;
    private final DateTime mLastModified;
    private final WebDbDataDefinition mDefinition;

    public SqliteWebDb(Context context, DateTime lastModified, WebDbDataDefinition definition) {
        // Calculate the version based on a code version and the last modified date, followed by a revision
        super(context, definition.getDataSourceLocalName(), null, Integer.valueOf("0" + lastModified.toString("YMd") + "00"));
        this.mContext = context;
        mLastModified = lastModified;
        mDefinition = definition;
    }

    /**
     * Create the database.
     *
     * @param db Handle in which the database should be created.
     */
    public void onCreate(SQLiteDatabase db) {
        mDefinition.onCreate(db);
    }


    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        mDefinition.onUpgrade(db, oldVersion, newVersion);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache from a local file, so just recreate it
        mDefinition.onDowngrade(db, oldVersion, newVersion);
    }
}