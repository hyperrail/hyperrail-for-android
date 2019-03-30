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
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.AsyncTask;

import org.joda.time.DateTime;

import java.lang.ref.WeakReference;

import be.hyperrail.opentransportdata.logging.OpenTransportLog;

/**
 * A database which can automatically update from a predefined web page.
 * Using this database removes the need for application updates every time something changes
 */
public class WebDb {

    private WebDbSqliteBackend db;
    private Context context;
    private WebDbDataDefinition dataDefinition;
    private WebDbConfig webDbConfig;

    /**
     * Instantiate a new WebDb, according to the parameters defined in the WebDbDataDefinition.
     * Don't run this code on the main thread as it contains blocking I/O
     *
     * @param appContext     The Android application context
     * @param dataDefinition The data definition, containing both local and remote names as well as methods to create the database.
     */
    public WebDb(Context appContext, WebDbDataDefinition dataDefinition) {
        this.context = appContext;
        this.webDbConfig = new WebDbConfig(appContext);
        this.dataDefinition = dataDefinition;

        int currentVersion = webDbConfig.getCurrentDatabaseVersion(dataDefinition.getDatabaseName());
        int embeddedVersion = getVersionCodeForDateTime(dataDefinition.getLastModifiedLocalDate());
        if (currentVersion <embeddedVersion ) {
            currentVersion = embeddedVersion;
        }
        this.db = new WebDbSqliteBackend(appContext, currentVersion, dataDefinition, false);

        // If the last check was more than 2 days ago, try to check
        if (webDbConfig.getTimeOfLastCheck(dataDefinition.getDatabaseName()).isBefore(DateTime.now().minusDays(2))) {
            // If not restricted to Wifi, or if connecected to wifi, check for updates
            boolean isConnectedToWifi = isConnectedToWifi();
            OpenTransportLog.log("WLAN: " + isConnectedToWifi);
            if (!dataDefinition.updateOnlyOnWifi() || isConnectedToWifi) {
                OpenTransportLog.log("Starting update check for " + dataDefinition.getDatabaseName());
                GetLastModifiedTask getLastModifiedTask = new GetLastModifiedTask(this, dataDefinition);
                getLastModifiedTask.execute(dataDefinition);
            } else {
                OpenTransportLog.log("Not starting update check for " + dataDefinition.getDatabaseName());
            }
        }
    }

    private boolean isConnectedToWifi() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Network wifiNetwork = connectivityManager.getActiveNetwork();
            final NetworkCapabilities capabilities = connectivityManager
                    .getNetworkCapabilities(wifiNetwork);
            return capabilities != null
                    && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) && !capabilities.hasCapability(NetworkCapabilities.TRANSPORT_CELLULAR);
        } else {
            // For older devices
            NetworkInfo wifiNetwork = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            return wifiNetwork.isConnected();
        }

    }

    public SQLiteDatabase getReadableDatabase() {
        return db.getReadableDatabase();
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
            return definitions[0].getLastModifiedOnlineDate();
        }

        @Override
        protected void onPostExecute(DateTime lastModifiedOnline) {
            if (webDbRef.get() == null) {
                return;
            }

            OpenTransportLog.log("Online last modified date check for " +
                    webDbRef.get().dataDefinition.getDatabaseName() + " resulted in " + lastModifiedOnline.toString("YYYY-MM-HH HH:mm"));
            String databaseName = definition.getDatabaseName();

            if (lastModifiedOnline.isAfter(webDbRef.get().webDbConfig.getTimeOfLastOnlineUpdate(databaseName))) {
                OpenTransportLog.log("Re-creating database using online data: " + databaseName);
                webDbRef.get().db = new WebDbSqliteBackend(webDbRef.get().context, getVersionCodeForDateTime(lastModifiedOnline), definition, true);
                OpenTransportLog.log("Re-created database using online data: " + databaseName);
                webDbRef.get().webDbConfig.setTimeOfLastOnlineUpdateToNow(databaseName);
            } else {
                OpenTransportLog.log("No newer data available: " + databaseName);
            }
            webDbRef.get().webDbConfig.setTimeOfLastCheckToNow(databaseName);
        }
    }

    private static int getVersionCodeForDateTime(DateTime dateTime) {
        return Integer.valueOf(dateTime.toString("YYMMDD") + "00");
    }
}