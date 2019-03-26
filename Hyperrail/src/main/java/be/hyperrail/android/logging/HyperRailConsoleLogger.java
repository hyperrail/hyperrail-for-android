package be.hyperrail.android.logging;

import android.util.Log;

class HyperRailConsoleLogger implements HyperRailLogger {

    public static final String LOGTAG = "ConsoleLogger";

    @Override
    public void logException(Throwable throwable) {
        Log.e(LOGTAG, "An exception was logged", throwable);
    }

    @Override
    public void log(String msg) {
        Log.i(LOGTAG, msg);
    }

    @Override
    public void log(int priority, String tag, String msg) {
        Log.println(priority, tag, msg);
    }

    @Override
    public void setDebugVariable(String key, String value) {
        Log.i(LOGTAG, "DEBUG VALUE: " + key + " = " + value);
    }

    @Override
    public void setDebugVariable(String key, int value) {
        Log.i(LOGTAG, "DEBUG VALUE: " + key + " = " + value);
    }
}
