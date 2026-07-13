package editor.log;

import utility.log.EngineLog;
import utility.log.EngineLogCallback;
import utility.log.EngineLogListener;
import utility.log.LogEntry;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * LogFileWriter stream the entries of {@link EngineLog} entries to a session bound log file on disk.
 * <p>
 * Streaming is done on a dedicated background thread, so write operations don't block the main thread.
 *
 */
public final class LogFileWriter implements EngineLogListener {
    private static final DateTimeFormatter FileNameFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS");
    private static final long PollTimeoutMs = 200L;
    private static LogFileWriter instance;
    private final BlockingQueue<LogEntry> queue = new LinkedBlockingQueue<>();
    private final Path logDir;
    private Thread worker;
    private BufferedWriter writer;
    private volatile boolean running = false;
    private boolean failed = false;

    private LogFileWriter(Path logDir) {
        this.logDir = logDir;
    }

    /**
     * Start streaming logs into a new session file under the given directory.
     * @param logDir the directory to write into
     */
    public static void start(Path logDir) {
        if (instance != null || logDir == null) return;
        instance = new LogFileWriter(logDir);
        instance.begin();
    }

    /**
     * Stop the stream, then flush and close the session file.
     */
    public static void stop() {
        if (instance == null) return;
        instance.end();
        instance = null;
    }

    private void begin() {
        running = true;
        List<LogEntry> backlog = EngineLog.logs();
        backlog.forEach(queue::offer);
        EngineLogCallback.register(this);
        worker = new Thread(this::run, "tcb-log-writer");
        worker.setDaemon(true);
        worker.start();
    }

    private void run() {
        LogArchiver.archive(logDir);
        while (running || !queue.isEmpty()) {
            LogEntry entry;
            try {
                entry = queue.poll(PollTimeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            if (entry == null) continue;
            write(entry);
            if (queue.isEmpty()) flush();
        }
        flush();
    }

    private void end() {
        EngineLogCallback.unregister(this);
        running = false;
        if (worker != null) {
            try {
                worker.join();
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            }
        }
        LogEntry entry;
        while ((entry = queue.poll()) != null) write(entry);
        flush();
        if (writer == null) return;
        try {
            writer.close();
        } catch (IOException e) {
            System.err.println("Failed to close log file: " + e.getMessage());
        }
        writer = null;
    }

    private void write(LogEntry entry) {
        if (failed || entry == null) return;
        if (writer == null) {
            try {
                Files.createDirectories(logDir);
                Path file = logDir.resolve(entry.timeStamp().format(FileNameFormat) + ".log");
                writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                failed = true;
                System.err.println("Failed to open log file: " + e.getMessage());
                return;
            }
        }
        try {
            writer.write(format(entry));
            writer.newLine();
        } catch (IOException e) {
            failed = true;
            System.err.println("Failed to write log entry to file: " + e.getMessage());
        }
    }

    private void flush() {
        if (writer == null) return;
        try {
            writer.flush();
        } catch (IOException e) {
            failed = true;
            System.err.println("Failed to flush log file: " + e.getMessage());
        }
    }

    private static String format(LogEntry entry) {
        return String.format("[%s][%s][%s]: %s", entry.formatedTimeStamp(), entry.source(), entry.level().prefix, entry.message());
    }

    @Override
    public void onNewLog(LogEntry entry) {
        if (entry == null || !running) return;
        queue.offer(entry);
    }
}
