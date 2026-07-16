package utility.log;

import scripting.API;
import utility.RingBuffer;

import java.time.LocalDateTime;
import java.util.List;

/**
 * EngineLog is a logging utility class use across the engine. It can be use directly with static logging methods,
 * or as an instance with prefix predefined.
 * <p>
 * All logs sent under EngineLog are stored in a shared {@link RingBuffer} of {@link LogEntry} with default capacity at 1024 entries.
 *
 * @param source The source's prefix string.
 */
@API
public record EngineLog(String source) {
    private static final RingBuffer<LogEntry> history = new RingBuffer<>(1024);
    /**
     * Default source prefix.
     */
    public static final String defaultSource = "TCB/?";

    public EngineLog {
        if (source == null || source.isBlank()) source = defaultSource;
    }

    /**
     * Create a new {@link EngineLog} using the given source for prefix.
     *
     * @param source the class to use for prefix
     */
    public EngineLog(Class<?> source) {
        String simpleName = source != null ? source.getSimpleName() : "Unknown";
        this("TCB/" + simpleName);
    }

    /**
     * Log a message at the desired level.
     *
     * @param level the level to log at
     * @param message the message to log
     */
    public void log(Level level, String message) {
        log(level, source, message);
    }

    /**
     * Log a message at the {@link Level#Debug} level.
     *
     * @param message the message to log
     */
    public void debug(String message) {
        log(Level.Debug, message);
    }

    /**
     * Log a message at the {@link Level#Info} level.
     *
     * @param message the message to log
     */
    public void info(String message) {
        log(Level.Info, message);
    }

    /**
     * Log a message at the {@link Level#Warning} level.
     *
     * @param message the message to log
     */
    public void warning(String message) {
        log(Level.Warning, message);
    }

    /**
     * Log a message at the {@link Level#Error} level.
     *
     * @param message the message to log
     */
    public void error(String message) {
        log(Level.Error, message);
    }

    /**
     * The central logging method of {@link EngineLog}, all other logging methods routed to this.
     * <p>
     * If the {@code level} and {@code message} are valid, a new {@link LogEntry} is made and added to the history.
     * This will then invoke the callback to emit the log event with the new log entry.
     *
     * @param level the level of the log entry
     * @param source the source prefix for the log entry
     * @param message the message for the log entry
     */
    public static void log(Level level, String source, String message) {
        if (level == null || message == null || message.isBlank()) return;
        if (source == null || source.isBlank()) source = defaultSource;
        LogEntry newEntry = new LogEntry(LocalDateTime.now(), level, source, message);
        history.add(newEntry);
        EngineLogCallback.emit(newEntry);
    }

    /**
     * Log a message from a source at the {@link Level#Debug} level.
     *
     * @param source the source of the message
     * @param message the message to log
     */
    public static void debug(String source, String message) {
        log(Level.Debug, source, message);
    }

    /**
     * Log a message from a source at the {@link Level#Info} level.
     *
     * @param source the source of the message
     * @param message the message to log
     */
    public static void info(String source, String message) {
        log(Level.Info, source, message);
    }

    /**
     * Log a message from a source at the {@link Level#Warning} level.
     *
     * @param source the source of the message
     * @param message the message to log
     */
    public static void warning(String source, String message) {
        log(Level.Warning, source, message);
    }

    /**
     * Log a message from a source at the {@link Level#Error} level.
     *
     * @param source the source of the message
     * @param message the message to log
     */
    public static void error(String source, String message) {
        log(Level.Error, source, message);
    }

    /**
     * Get all the log entries in history.
     *
     * @return a list of log entries created from history
     */
    public static List<LogEntry> logs() {
        return history.toList();
    }

    /**
     * Get all the log entries of {@link Level#Debug} level in history.
     *
     * @return a list of debug log entries from history
     */
    public static List<LogEntry> debugLogs() {
        return logs().stream().filter(e -> e.level() == Level.Debug).toList();
    }

    /**
     * Get all the log entries of {@link Level#Info} level in history.
     *
     * @return a list of info log entries from history
     */
    public static List<LogEntry> infoLogs() {
        return logs().stream().filter(e -> e.level() == Level.Info).toList();
    }

    /**
     * Get all the log entries of {@link Level#Warning} level in history.
     *
     * @return a list of warning log entries from history
     */
    public static List<LogEntry> warningLogs() {
        return logs().stream().filter(e -> e.level() == Level.Warning).toList();
    }

    /**
     * Get all the log entries of {@link Level#Error} level in history.
     *
     * @return a list of error log entries from history
     */
    public static List<LogEntry> errorLogs() {
        return logs().stream().filter(e -> e.level() == Level.Error).toList();
    }

    /**
     * Clear all the log history.
     */
    public static void clear() {
        history.clear();
    }
}
