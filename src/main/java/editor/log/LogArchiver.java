package editor.log;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * LogArchiver compress loose session log files of past days in a zip archive for each day.
 * <p>
 * Archiving is done on startup, so logs of previous dats are completed and safe to compress.
 */
final class LogArchiver {
    private static final String DatePattern = "yyyy-MM-dd";
    private static final DateTimeFormatter DateFormat = DateTimeFormatter.ofPattern(DatePattern);
    private static final int DatePatternLength = DatePattern.length();
    private static final String LogExtension = ".log";

    private LogArchiver() {}

    /**
     * Group {@code .log} files older than today by their date and compress each day into a zip archive.
     * Files that are archived are removed from disk.
     * @param logDir the project's log directory to scan
     */
    static void archive(Path logDir) {
        if (logDir == null || !Files.isDirectory(logDir)) return;
        LocalDate today = LocalDate.now();
        Map<LocalDate, List<Path>> byDate = new HashMap<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(logDir, "*" + LogExtension)) {
            for (Path file : stream) {
                LocalDate date = parseDate(file);
                if (date == null || !date.isBefore(today)) continue;
                byDate.computeIfAbsent(date, _ -> new ArrayList<>()).add(file);
            }
        } catch (IOException e) {
            System.err.println("Failed to scan log directory for archiving: " + e.getMessage());
            return;
        }
        for (Map.Entry<LocalDate, List<Path>> entry : byDate.entrySet()) {
            List<Path> files = entry.getValue();
            if (files.isEmpty()) continue;
            LocalDate date = entry.getKey();
            Path archive = uniqueArchivePath(logDir, date);
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
                for (Path file : files) {
                    zip.putNextEntry(new ZipEntry(file.getFileName().toString()));
                    Files.copy(file, zip);
                    zip.closeEntry();
                }
            } catch (IOException e) {
                System.err.printf("Failed to archive logs for %s: %s", date, e.getMessage());
                continue;
            }
            for (Path file : files) {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException e) {
                    System.err.printf("Failed to delete archived log %s: %s", file.getFileName(), e.getMessage());
                }
            }
        }
    }

    private static LocalDate parseDate(Path file) {
        String name = file.getFileName().toString();
        if (name.length() < DatePatternLength) return null;
        try {
            return LocalDate.parse(name.substring(0, DatePatternLength), DateFormat);
        } catch (DateTimeParseException _) {
            return null;
        }
    }

    private static Path uniqueArchivePath(Path logDir, LocalDate date) {
        String base = date.format(DateFormat);
        Path candidate = logDir.resolve(base + ".zip");
        int counter = 1;
        while (Files.exists(candidate)) {
            candidate = logDir.resolve(base + "_" + counter + ".zip");
            counter++;
        }
        return candidate;
    }
}
