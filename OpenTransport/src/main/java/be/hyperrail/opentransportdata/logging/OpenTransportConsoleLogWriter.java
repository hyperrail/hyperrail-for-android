package be.hyperrail.opentransportdata.logging;

import android.util.Log;

import java.util.logging.Level;

class OpenTransportConsoleLogWriter implements OpenTransportLogWriter {

    @Override
    public void logException(String tag, Throwable throwable) {
        Log.e(tag, "An exception was logged", throwable);
    }

    @Override
    public void log(Level priority, String tag, String msg) {
        Log.println(priority.intValue(), tag, msg);
    }

    @Override
    public void setDebugVariable(String tag, String key, String value) {
        Log.i(tag, "DEBUG VALUE: " + key + " = " + value);
    }

    @Override
    public void setDebugVariable(String tag, String key, int value) {
        Log.i(tag, "DEBUG VALUE: " + key + " = " + value);
    }

    @Override
    public void info(String tag, String message) {
        log(Level.INFO, tag, message);
    }

    @Override
    public void warning(String tag, String message) {
        log(Level.WARNING, tag, message);
    }

    @Override
    public void severe(String tag, String message) {
        log(Level.SEVERE, tag, message);
    }

    @Override
    public void debug(String tag, String message) {
        log(Level.FINE, tag, message);
    }
}
