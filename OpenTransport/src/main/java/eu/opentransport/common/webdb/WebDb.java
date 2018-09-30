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


package eu.opentransport.common.webdb;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.joda.time.DateTime;

/**
 * A database which can automatically update from a predefined web page.
 * Using this database removes the need for application updates every time something changes
 */
public class WebDb {

    SqliteWebDb db;

    /**
     * Instantiate a new WebDb, according to the parameters defined in the WebDbDataDefinition.
     * Don't run this code on the main thread as it contains blocking I/O
     * @param context The Android context
     * @param dataDefinition The data definition, containing both local and remote names as well as methods to create the database.
     */
    public WebDb(Context context, WebDbDataDefinition dataDefinition) {
        DateTime lastModified = dataDefinition.getLastModifiedDate();
        db = new SqliteWebDb(context, lastModified, dataDefinition);
    }

    public SQLiteDatabase getReadableDatabase(){
        return db.getReadableDatabase();
    }


   public SQLiteDatabase getWriteableDatabase(){
        return db.getWritableDatabase();
    }
}