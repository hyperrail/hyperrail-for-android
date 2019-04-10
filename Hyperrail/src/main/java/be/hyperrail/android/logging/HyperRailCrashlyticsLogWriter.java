package be.hyperrail.android.logging;

import com.crashlytics.android.Crashlytics;

import java.util.logging.Level;

import be.hyperrail.opentransportdata.logging.OpenTransportLogWriter;

public class HyperRailCrashlyticsLogWriter extends HyperRailConsoleLogWriter implements HyperRailLogWriter, OpenTransportLogWriter {

    @Override
    public void logException(String tag, Throwable throwable) {
        super.logException(tag, throwable);
        Crashlytics.logException(throwable);
    }

    @Override
    public void log(Level priority, String tag, String msg) {
        super.log(priority,tag,msg);
        Crashlytics.log(priority.intValue(), tag, msg);
    }

    @Override
    public void setDebugVariable(String tag, String key, String value) {
        Crashlytics.setString(key, value);
    }

    @Override
    public void setDebugVariable(String tag, String key, int value) {
        Crashlytics.setInt(key, value);
    }
}
