package be.hyperrail.android.logging;

import java.util.logging.Level;

public class HyperRailLog {

    private static HyperRailLogWriter loggerInstance;
    private final Class callingClass;

    private HyperRailLog(Class c) {
        this.callingClass = c;
    }

    public static void initLogWriter(HyperRailLogWriter logger) {
        loggerInstance = logger;
        logger.info(logger.getClass().getName(), "Using Console logger");
    }

    private static HyperRailLogWriter getLogWriter() {
        if (loggerInstance == null) {
            loggerInstance = new HyperRailConsoleLogWriter();
        }

        return loggerInstance;
    }

    public static HyperRailLog getLogger(Class c) {
        return new HyperRailLog(c);
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

}
