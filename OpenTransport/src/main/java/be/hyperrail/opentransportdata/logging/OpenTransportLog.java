package be.hyperrail.opentransportdata.logging;

import java.util.logging.Level;

public class OpenTransportLog {

    private static OpenTransportLogWriter loggerInstance;
    private final Class callingClass;

    private OpenTransportLog(Class c) {
        this.callingClass = c;
    }

    public static void initLogWriter(OpenTransportLogWriter logger) {
        loggerInstance = logger;
    }

    private static OpenTransportLogWriter getLogWriter() {
        if (loggerInstance == null) {
            loggerInstance = new OpenTransportConsoleLogWriter();
        }

        return loggerInstance;
    }

    public static OpenTransportLog getLogger(Class c) {
        return new OpenTransportLog(c);
    }

    public void logException(Throwable throwable) {
        getLogWriter().logException(callingClass.getSimpleName(), throwable);
    }

    public void log(Level priority, String msg) {
        getLogWriter().log(priority, callingClass.getSimpleName(), msg);
    }

    public void setDebugVariable(String key, String value) {
        getLogWriter().setDebugVariable(callingClass.getSimpleName(), key, value);
    }

    public void setDebugVariable(String key, int value) {
        getLogWriter().setDebugVariable(callingClass.getSimpleName(), key, value);
    }

    public void info(String message) {
        log(Level.INFO, message);
    }

    public void warning(String message) {
        log(Level.WARNING, message);
    }

    public void severe(String message) {
        log(Level.SEVERE, message);
    }

    public void debug(String message) {
        log(Level.FINE, message);
    }

    public void info(String message, Throwable throwable) {
        info(message);
        logException(throwable);
    }

    public void warning(String message, Throwable throwable) {
        warning(message);
        logException(throwable);
    }

    public void severe(String message, Throwable throwable) {
        severe(message);
        logException(throwable);
    }

    public void debug(String message, Throwable throwable) {
        debug(message);
        logException(throwable);
    }
}
