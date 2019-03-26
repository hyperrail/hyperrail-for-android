package be.hyperrail.android.logging;

public interface HyperRailLogger {

    void logException(Throwable throwable);

    void log(String msg);

    void log(int priority, String tag, String msg);

    void setDebugVariable(String key, String value);

    void setDebugVariable(String key, int value);

}
