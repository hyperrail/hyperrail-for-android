package be.hyperrail.android.logging;

public class HyperRailLog {

    private static HyperRailLogger loggerInstance;

    public static void init(HyperRailLogger logger) {
        loggerInstance = logger;
    }

    private static HyperRailLogger getLoggerInstance() {
        if (loggerInstance == null) {
            loggerInstance = new HyperRailConsoleLogger();
        }

        return loggerInstance;
    }

    public static void logException(Throwable throwable) {
        getLoggerInstance().logException(throwable);
    }

    public static void log(String msg) {
        getLoggerInstance().log(msg);
    }

    public static void log(int priority, String tag, String msg) {
        getLoggerInstance().log(priority, tag, msg);
    }

    public static void setDebugVariable(String key, String value) {
        getLoggerInstance().setDebugVariable(key, value);
    }

    public static void setDebugVariable(String key, int value) {
        getLoggerInstance().setDebugVariable(key, value);
    }
}
