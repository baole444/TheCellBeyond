package utility.log;

import java.util.concurrent.CopyOnWriteArrayList;

public class EngineLogCallback {
    private static final CopyOnWriteArrayList<EngineLogListener> listeners = new CopyOnWriteArrayList<>();

    public static void register(EngineLogListener listener) {
        if (listener == null || listeners.contains(listener)) return;

        listeners.add(listener);
    }

    public static void unregister(EngineLogListener listener) {
        if (listener == null) return;

        listeners.remove(listener);
    }

    static void emit(LogEntry entry) {
        for (EngineLogListener listener : listeners) {
            try {
                listener.onNewLog(entry);
            } catch (Exception ignore) {}
        }
    }
}
