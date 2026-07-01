package scripting.builder.jdk.download;

import utility.log.EngineLog;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Extract a downloaded JDK archive into a managed base directory and return the extracted JDK home.
 * <p>
 * Zip archives are unpacked with zip on Windows, and tar for Linux/macOS with executable bits preserved.
 * </p>
 * Writes and deleted are confined to the base directory. Entries or root segments that would escape {@code base} are rejected.
 */
final class ArchiveExtractor {
    private static final EngineLog Logger = new EngineLog(ArchiveExtractor.class);
    private static final long TarTimeoutSeconds = 200L;

    private ArchiveExtractor() {}

    /**
     * Extract an archive into {@code installDir} and return the extracted JDK home.
     * @param archive the downloaded archive file
     * @param installDir the managed directory to extract into
     * @param archiveType the archive format, {@code zip} or {@code tar.gz}
     * @return the extracted JDK home directory
     * @throws DownloadException when the archive cannot be read, empty or the entry escapes the base directory
     */
    static Path extract(Path archive, Path installDir, String archiveType) throws DownloadException {
        if (archiveType.equals("zip")) return unzip(archive, installDir);
        return untar(archive, installDir);
    }

    private static Path unzip(Path archive, Path installDir) throws DownloadException {
        Path base = installDir.toAbsolutePath().normalize();
        String root = null;
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                root = writeZipEntry(zip, entry, base, root);
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw new DownloadException("Extraction failed: " + e.getMessage());
        }
        if (root == null) throw new DownloadException("Downloaded archive was empty");
        return base.resolve(root);
    }

    private static String writeZipEntry(ZipInputStream zip, ZipEntry entry, Path base, String root) throws IOException, DownloadException {
        Path target = base.resolve(entry.getName()).normalize();
        if (!target.startsWith(base)) {
            Logger.warning("Skipping unsafe archive entry: " + entry.getName());
            return root;
        }
        if (root == null) prepareRoot(base, base.resolve(topSegment(entry.getName())).normalize());
        if (entry.isDirectory()) Files.createDirectories(target);
        else {
            Files.createDirectories(target.getParent());
            Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return root == null ? topSegment(entry.getName()) : root;
    }

    private static Path untar(Path archive, Path installDir) throws DownloadException {
        Path base = installDir.toAbsolutePath().normalize();
        String root = tarRoot(archive);
        if (root == null) throw new DownloadException("Could not read downloaded archive");
        Path home = base.resolve(root).normalize();
        prepareRoot(base, home);
        runTar(new ProcessBuilder("tar", "-xzf", archive.toString(), "-C", base.toString()));
        return home;
    }

    private static String tarRoot(Path archive) throws DownloadException {
        Process process = startTar(new ProcessBuilder("tar", "-tzf", archive.toString()));
        try (InputStream stream = process.getInputStream()) {
            String line = new String(stream.readAllBytes(), StandardCharsets.UTF_8).strip();
            if (!process.waitFor(TarTimeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new DownloadException("Listing archive timed out");
            }
            int newLine = line.indexOf('\n');
            String first = newLine < 0 ? line : line.substring(0, newLine);
            return first.isBlank() ? null : topSegment(first.strip());
        } catch (IOException e) {
            throw new DownloadException("Could not read archive: " + e.getMessage());
        } catch (InterruptedException _) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new DownloadException("Listing archive interrupted");
        }
    }

    private static void runTar(ProcessBuilder builder) throws DownloadException {
        Process process = startTar(builder);
        try {
            if (!process.waitFor(TarTimeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new DownloadException("Extraction time out");
            }
            if (process.exitValue() != 0) throw new DownloadException("tar exited with code " + process.exitValue());
        } catch (InterruptedException _) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new DownloadException("Extraction interrupted");
        }
    }

    private static Process startTar(ProcessBuilder builder) throws DownloadException {
        try {
            return builder.redirectErrorStream(true).start();
        } catch (IOException e) {
            throw new DownloadException("Could not run tar: " + e.getMessage());
        }
    }

    /**
     * Clear the old extracted JDK root before extracting a new one.
     * <p>
     * A containment check against {@code base} is used to guard the recursive delete.
     * A normalized target escaping the managed installation directory (or equal to it) is cancelled.
     * @param base the managed install directory, should be absolute and normalized.
     * @param root the candidate root to clear
     * @throws DownloadException When attempting to escape management is rejected or failed to clean the old extracted file.
     */
    private static void prepareRoot(Path base, Path root) throws DownloadException {
        Path target = root.normalize();
        if (!target.startsWith(base) || target.equals(base)) throw new DownloadException("It is not allowed to clear archive root outside of the install directory: " + root.getFileName());
        if (!Files.exists(target)) return;
        try (Stream<Path> walk = Files.walk(target)) {
            walk.sorted(Comparator.reverseOrder()).forEach(ArchiveExtractor::deleteSilently);
        } catch (IOException e) {
            throw new DownloadException(String.format("Could not clear existing JDK at %s: %s", target.getFileName(), e.getMessage()));
        }
    }

    private static void deleteSilently(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            Logger.debug(String.format("Could not delete %s: %s", path, e.getMessage()));
        }
    }

    private static String topSegment(String name) {
        int slash = name.indexOf('/');
        return slash < 0 ? name : name.substring(0, slash);
    }
}
