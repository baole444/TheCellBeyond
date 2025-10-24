package utility.log;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

    public record Entry(LocalDateTime timeStamp, Level level, String message, String source) {
        public String formatedTimeStamp() {
            return timeStamp.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        }
    }

    private static final List<Entry> history = new ArrayList<>();
    private static final Level defaultLevel = Level.Debug;
    private static final int historyLimit = 1024;


}
