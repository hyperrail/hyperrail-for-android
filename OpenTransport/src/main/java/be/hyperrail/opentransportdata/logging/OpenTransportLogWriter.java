package be.hyperrail.opentransportdata.logging;

import java.util.logging.Level;

public interface OpenTransportLogWriter {

    void logException(String tag, Throwable throwable);

    void log(Level priority, String tag, String msg);

    void setDebugVariable(String tag, String key, String value);

    void setDebugVariable(String tag, String key, int value);

    void info(String tag, String message);

    void warning(String tag, String message);

    void severe(String tag, String message);

    void debug(String tag, String message);
}
