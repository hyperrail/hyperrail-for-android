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
import android.os.AsyncTask;

import org.joda.time.DateTime;

import java.lang.ref.WeakReference;

/**
 * A database which can automatically update from a predefined web page.
 * Using this database removes the need for application updates every time something changes
 */
public class WebDb {

    private SqliteWebDb db;
    private Context context;

    /**
     * Instantiate a new WebDb, according to the parameters defined in the WebDbDataDefinition.
     * Don't run this code on the main thread as it contains blocking I/O
     *
     * @param appContext     The Android application context
     * @param dataDefinition The data definition, containing both local and remote names as well as methods to create the database.
     */
    public WebDb(Context appContext, WebDbDataDefinition dataDefinition) {
        context = appContext;
        db = new SqliteWebDb(appContext, new DateTime(2000, 1, 1, 0, 0), dataDefinition);

        GetLastModifiedTask getLastModifiedTask = new GetLastModifiedTask(this, dataDefinition);
        getLastModifiedTask.execute(dataDefinition);

    }

    public SQLiteDatabase getReadableDatabase() {
        return db.getReadableDatabase();
    }

    public SQLiteDatabase getWriteableDatabase() {
        return db.getWritableDatabase();
    }

    private static class GetLastModifiedTask extends AsyncTask<WebDbDataDefinition, Void, DateTime> {
        private WeakReference<WebDb> webDbRef;
        private WebDbDataDefinition definition;

        // only retain a weak reference to the activity
        GetLastModifiedTask(WebDb webDb, WebDbDataDefinition definition) {
            webDbRef = new WeakReference<>(webDb);
            this.definition = definition;
        }

        @Override
        protected DateTime doInBackground(WebDbDataDefinition... definitions) {
            return definitions[0].getLastModifiedDate();
        }

        @Override
        protected void onPostExecute(DateTime dateTime) {
            if (webDbRef.get() == null) {
                return;
            }
            webDbRef.get().db = new SqliteWebDb(webDbRef.get().context, dateTime, definition);
        }
    }
}