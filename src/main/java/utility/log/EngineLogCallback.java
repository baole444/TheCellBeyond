package utility.log;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * EngineLogCallback notify its listeners when a new log entry emerged from engine and editor activities.
 */
public final class EngineLogCallback {
    private static final CopyOnWriteArrayList<EngineLogListener> listeners = new CopyOnWriteArrayList<>();
    private EngineLogCallback() {}

    /**
     * Register a listener to start receiving notification.
     * @param listener the listener that needs register
     */
    public static void register(EngineLogListener listener) {
        if (listener == null || listeners.contains(listener)) return;
        listeners.add(listener);
    }

    /**
     * Unregister a listener to stop receiving notification.
     * @param listener the listener that needs register
     */
    public static void unregister(EngineLogListener listener) {
        if (listener == null) return;
        listeners.remove(listener);
    }

    /**
     * Notify all listener about a log entry.
     * @param entry the entry to notify with
     */
    static void emit(LogEntry entry) {
        if (entry == null) return;
        for (EngineLogListener listener : listeners) {
            try {
                listener.onNewLog(entry);
            } catch (Exception _) {}
        }
    }
}
