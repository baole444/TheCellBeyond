package utility.log;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record LogEntry(LocalDateTime timeStamp, EngineLog.Level level, String source, String message) {
    public String formatedTimeStamp() {
        return timeStamp.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }
}
