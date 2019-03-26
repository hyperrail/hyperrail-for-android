package be.hyperrail.android.logging;

import com.crashlytics.android.Crashlytics;

import be.hyperrail.opentransportdata.logging.OpenTransportLogger;

public class HyperRailCrashlyticsLogger implements HyperRailLogger, OpenTransportLogger {

    @Override
    public void logException(Throwable throwable) {
        Crashlytics.logException(throwable);
    }

    @Override
    public void log(String msg) {
        Crashlytics.log(msg);
    }

    @Override
    public void log(int priority, String tag, String msg) {
        Crashlytics.log(priority, tag, msg);
    }

    @Override
    public void setDebugVariable(String key, String value) {
        Crashlytics.setString(key, value);
    }

    @Override
    public void setDebugVariable(String key, int value) {
        Crashlytics.setInt(key, value);
    }
}
