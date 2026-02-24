package utility.log;

/**
 * Interface for receiving engine's log.
 */
public interface EngineLogListener {
    /**
     * Callback on new incoming log entry.
     * @param entry the new log entry
     */
    void onNewLog(LogEntry entry);
}
