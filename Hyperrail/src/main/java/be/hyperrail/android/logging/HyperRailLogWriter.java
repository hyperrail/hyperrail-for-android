package be.hyperrail.android.logging;

import java.util.logging.Level;

import be.hyperrail.opentransportdata.logging.OpenTransportLogWriter;

public interface HyperRailLogWriter extends OpenTransportLogWriter {

    void logException(String tag, Throwable throwable);

    void log(Level priority, String tag, String msg);

    void setDebugVariable(String tag, String key, String value);

    void setDebugVariable(String tag, String key, int value);

    void info(String tag, String message);

    void warning(String tag, String message);

    void severe(String tag, String message);

    void debug(String tag, String message);
}
