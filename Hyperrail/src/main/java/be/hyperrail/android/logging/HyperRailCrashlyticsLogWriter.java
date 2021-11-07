package be.hyperrail.android.logging;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.logging.Level;

public class HyperRailCrashlyticsLogWriter extends HyperRailConsoleLogWriter implements HyperRailLogWriter {

    @Override
    public void logException(String tag, Throwable throwable) {
        FirebaseCrashlytics.getInstance().recordException(throwable);
    }

    @Override
    public void log(Level priority, String tag, String msg) {
        FirebaseCrashlytics.getInstance().log("[" + priority.getName() + "] [" + tag + "] " + msg);
    }

    @Override
    public void setDebugVariable(String tag, String key, String value) {
        FirebaseCrashlytics.getInstance().setCustomKey(key, value);
    }

    @Override
    public void setDebugVariable(String tag, String key, int value) {
        FirebaseCrashlytics.getInstance().setCustomKey(key, value);
    }
}
