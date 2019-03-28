/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.common.webdb;

import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.RawRes;

import org.joda.time.DateTime;

/**
 * This interface describes how the data structure for WebDb should be applied to the database.
 */
public interface WebDbDataDefinition {

    /**
     * Whether or not updating the data should be limited to moments when the user is connected to Wi-Fi.
     *
     * @return True if updates should only happen when connected to Wi-Fi.
     */
    boolean updateOnlyOnWifi();

    /**
     * Get the default data embedded with the application.
     *
     * @return The raw resource holding the data to be used when no internet connectivity is available.
     */
    @RawRes
    int getEmbeddedDataResourceId();

    /**
     * Get the online location of the data.
     *
     * @return The URL pointing to the data on the internet.
     */
    String getOnlineDataURL();

    /**
     * Get the local name for the database.
     *
     * @return The local name for the database.
     */
    String getDatabaseName();

    /**
     * Get the last modified timestamp for the embedded data.
     *
     * @return The datetime at which the embedded data was last modified.
     */
    DateTime getLastModifiedLocalDate();

    /**
     * Get the last modified timestamp for the online data.
     *
     * @return The datetime at which the online data was last modified.
     */
    DateTime getLastModifiedOnlineDate();


    void onCreate(SQLiteDatabase db, boolean useOnlineData);

    void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion, boolean useOnlineData);


}
