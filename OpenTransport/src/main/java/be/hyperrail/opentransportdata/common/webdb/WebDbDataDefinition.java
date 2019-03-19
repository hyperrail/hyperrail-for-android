/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.common.webdb;

import android.database.sqlite.SQLiteDatabase;

import org.joda.time.DateTime;

/**
 * This interface describes how the data structure for WebDb should be applied to the database.
 */
public interface WebDbDataDefinition {

    String getDataSourceUrl();
    String getDataSourceLocalName();
    DateTime getLastModifiedDate();

    void onCreate(SQLiteDatabase db);

    void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);

    void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion);

}
