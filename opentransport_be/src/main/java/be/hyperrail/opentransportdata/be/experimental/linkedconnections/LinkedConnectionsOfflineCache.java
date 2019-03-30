/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.be.experimental.linkedconnections;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.firebase.perf.metrics.AddTrace;

import org.joda.time.DateTime;

/**
 * Created in be.hyperrail.android.irail.implementation.LinkedConnections on 08/03/2018.
 */

public class LinkedConnectionsOfflineCache extends SQLiteOpenHelper {

    // If you change the database schema, you must increment the database version.
    // year/month/day/increment
    private static final int DATABASE_VERSION = 18043000;

    // Name of the database file
    private static final String DATABASE_NAME = "linkedconnections.db";

    // Logtag for logging purpose
    private static final String LOGTAG = "LinkedConnectionsCache";

    private static final String SQL_CREATE_TABLE = "CREATE TABLE cache (_id INTEGER PRIMARY KEY AUTOINCREMENT, url TEXT NOT NULL UNIQUE, next TEXT NOT NULL, data TEXT NOT NULL, datetime INTEGER)";
    private static final String SQL_CREATE_INDEX = "CREATE INDEX cache_index ON cache (url);";
    private static final String TABLE = "cache";

    LinkedConnectionsOfflineCache(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE);
        db.execSQL(SQL_CREATE_INDEX);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS cache;");
        onCreate(db);
    }

    public void store(LinkedConnections connections, String data) {
        ContentValues values = new ContentValues();
        values.put("url", connections.current);
        values.put("next", connections.next);
        values.put("data", data);
        values.put("datetime", DateTime.now().getMillis());
        SQLiteDatabase db = getWritableDatabase();
        int id = (int) db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    @AddTrace(name = "LinkedConnectionsOfflineCache.load")
    public CachedLinkedConnections load(String url) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE, new String[]{"url", "data", "datetime"}, "url=?", new String[]{url}, null, null, null);

        if (c.getCount() == 0) {
            c.close();
            return loadApproximate(url);
        }

        CachedLinkedConnections result = new CachedLinkedConnections();
        c.moveToFirst();
        result.createdAt = new DateTime(c.getLong(c.getColumnIndex("datetime")));
        result.data = c.getString(c.getColumnIndex("data"));
        result.url = c.getString(c.getColumnIndex("url"));
        c.close();
        return result;
    }

    @AddTrace(name = "LinkedConnectionsOfflineCache.loadApproximate")
    private CachedLinkedConnections loadApproximate(String url) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE, new String[]{"url", "data", "datetime"}, "url<=? AND next>?", new String[]{url,url}, null, null, "url DESC");

        if (c.getCount() == 0) {
            c.close();
            return null;
        }

        CachedLinkedConnections result = new CachedLinkedConnections();
        c.moveToFirst();
        result.createdAt = new DateTime(c.getLong(c.getColumnIndex("datetime")));
        result.data = c.getString(c.getColumnIndex("data"));
        result.url = c.getString(c.getColumnIndex("url"));
        c.close();
        return result;
    }

    class CachedLinkedConnections {
        String url, data;
        DateTime createdAt;
    }
}