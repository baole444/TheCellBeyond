package utility.log;

import utility.RingBuffer;

import java.time.LocalDateTime;
import java.util.List;

public class EngineLog {
    public enum Level {
        Debug(0, "DEBUG"),
        Info(1, "INFO"),
        Warning(2, "WARN"),
        Error(3, "ERROR");

        public final int priority;
        public final String prefix;

        Level(int priority, String prefix) {
            this.priority = priority;
            this.prefix = prefix;
        }
    }

    private static final RingBuffer<LogEntry> history = new RingBuffer<>(1024);
    public static final String defaultSource = "TCB/Main";
    public final String source;

    public EngineLog(Class<?> source) {
        String simpleName = source.getSimpleName();
        this.source = "TCB/" + simpleName;
    }

    public void log(Level level, String message) {
        log(level, source, message);
    }

    public void debug(String message) {
        log(Level.Debug, message);
    }

    public void info(String message) {
        log(Level.Info, message);
    }

    public void warning(String message) {
        log(Level.Warning, message);
    }

    public void error(String message) {
        log(Level.Error, message);
    }

    public static void log(Level level, String source, String message) {
        if (level == null || message == null || message.isBlank()) return;
        if (source == null || source.isBlank()) source = defaultSource;

        LogEntry newEntry = new LogEntry(LocalDateTime.now(), level, source, message);

        history.add(newEntry);
        EngineLogCallback.emit(newEntry);
    }

    public static void debug(String source, String message) {
        log(Level.Debug, source, message);
    }

    public static void info(String source, String message) {
        log(Level.Info, source, message);
    }

    public static void warning(String source, String message) {
        log(Level.Warning, source, message);
    }

    public static void error(String source, String message) {
        log(Level.Error, source, message);
    }

    public static List<LogEntry> logs() {
        return history.toList();
    }

    public static List<LogEntry> debugLogs() {
        return logs().stream().filter(e -> e.level() == Level.Debug).toList();
    }

    public static List<LogEntry> infoLogs() {
        return logs().stream().filter(e -> e.level() == Level.Info).toList();
    }

    public static List<LogEntry> warningLogs() {
        return logs().stream().filter(e -> e.level() == Level.Warning).toList();
    }

    public static List<LogEntry> errorLogs() {
        return logs().stream().filter(e -> e.level() == Level.Error).toList();
    }

    public static void clear() {
        history.clear();
    }
}
