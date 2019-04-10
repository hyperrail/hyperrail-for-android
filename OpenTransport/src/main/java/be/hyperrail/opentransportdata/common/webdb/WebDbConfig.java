package be.hyperrail.opentransportdata.common.webdb;

import android.content.Context;
import android.content.SharedPreferences;

import org.joda.time.DateTime;

public class WebDbConfig {

    private final SharedPreferences preferences;

    WebDbConfig(Context applicationContext) {
        this.preferences = applicationContext.getSharedPreferences("webdb", Context.MODE_PRIVATE);
    }

    void setCurrentDatabaseVersion(String databaseName, int version) {
        preferences.edit().putInt(databaseName + "_version", version).apply();
    }

    int getCurrentDatabaseVersion(String databaseName) {
        return preferences.getInt(databaseName + "_version", 0);
    }

    DateTime getTimeOfLastCheck(String databaseName) {
        return new DateTime(preferences.getLong(databaseName + "_lastupdatecheck", 0));
    }

    DateTime getTimeOfLastOnlineUpdate(String databaseName) {
        return new DateTime(preferences.getLong(databaseName + "_lastonlineupdate", 0));
    }

    DateTime getTimeOfLastOfflineUpdate(String databaseName) {
        return new DateTime(preferences.getLong(databaseName + "_lastofflineupdate", 0));
    }

    void setTimeOfLastCheckToNow(String databaseName) {
        preferences.edit().putLong(databaseName + "_lastupdatecheck", DateTime.now().getMillis()).apply();
    }

    void setTimeOfLastOnlineUpdateToNow(String databaseName) {
        preferences.edit().putLong(databaseName + "_lastonlineupdate", DateTime.now().getMillis()).apply();
    }

    void setTimeOfLastOfflineUpdateToNow(String databaseName) {
        preferences.edit().putLong(databaseName + "_lastofflineupdate", DateTime.now().getMillis()).apply();
    }
}
