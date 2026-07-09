package scripting.builder.jdk.download;

import utility.log.EngineLog;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handle download, checksum verification, and extraction of JDK via foojay API for the requested provider/version and the host platform.
 * <p>
 * The download process run on a dedicated executor with progress obtainable via {@link #progress()}.
 * Requesting download while there is an ongoing request will return the ongoing's outcome instead.
 */
public final class JDKDownloader {
    private static final EngineLog Logger = new EngineLog(JDKDownloader.class);
    private static final ExecutorService Executor = Executors.newSingleThreadExecutor(JDKDownloader::worker);
    private static volatile DownloadProgress progress = DownloadProgress.idle();
    private static volatile CompletableFuture<Path> ongoingDownload;
    private static final int BufferSize = 64 * 1024;
    private static final String ChecksumAlgorithm = "SHA-256";
    private static final String ChecksumType = "sha256";

    /**
     * Get the latest download progress snapshot. This action is thead safe.
     * @return the current progress
     */
    public static DownloadProgress progress() {
        return progress;
    }

    /**
     * Download and extract the requested JDK for the host into {@code installDir} async. When there is an ongoing download,
     * it is returned instead of starting a new download request.
     * @param installDir the managed directory to extract into, will be created if absent
     * @param version the Java feature version to provision
     * @param provider the distribution to provision from
     * @return a future of the extracted JDK home, or completing with null if there is any failure
     */
    public static synchronized CompletableFuture<Path> download(Path installDir, int version, JDKProvider provider) {
        if (installDir == null || provider == null) return CompletableFuture.completedFuture(null);
        if (ongoingDownload != null && !ongoingDownload.isDone()) return ongoingDownload;
        ongoingDownload = CompletableFuture.supplyAsync(() -> run(installDir, version, provider), Executor);
        return ongoingDownload;
    }

    private static Path run(Path installDir, int version, JDKProvider provider) {
        try {
            return proceedDownload(installDir, version, provider);
        } catch (DownloadException e) {
            Logger.warning("JDK download failed: " + e.getMessage());
            progress = DownloadProgress.failed(e.getMessage());
            return null;
        }
    }

    private static Path proceedDownload(Path installDir, int version, JDKProvider provider) throws DownloadException {
        String os = hostOS();
        String arch = hostArch();
        String archiveType = archiveTypeFor(os);
        progress = DownloadProgress.querying();
        DiscoPackage pkg = pick(DiscoClient.query(provider.distribution, os, arch, archiveType, version), version);
        if (pkg == null) throw new DownloadException(String.format("No %s %d package for %s/%s", provider.distribution, version, os, arch));
        PackageInfo info = DiscoClient.resolve(pkg.id());
        if (info == null || info.directDownloadUri() == null) throw new DownloadException("Failed to resolve download URL for " + pkg.filename());
        createDir(installDir);
        Path archive = installDir.resolve(pkg.filename());
        downloadTo(info.directDownloadUri(), archive, pkg.size());
        verifyChecksum(archive, info);
        progress = DownloadProgress.extracting();
        Path home = ArchiveExtractor.extract(archive, installDir, archiveType);
        deleteSilently(archive);
        if (os.equals("macos")) QuarantineRemover.strip(home);
        progress = DownloadProgress.done(home);
        return home;
    }

    private static DiscoPackage pick(List<DiscoPackage> packages, int version) {
        return packages.stream()
                .filter(pkg -> pkg.id() != null && pkg.majorVersion() >= version)
                .max(Comparator.comparingInt(DiscoPackage::majorVersion))
                .orElse(null);
    }

    private static void downloadTo(String url, Path target, long fallbackTotal) throws DownloadException {
        try {
            HttpResponse<InputStream> response = DiscoClient.openStream(url);
            if (response.statusCode() / 100 != 2) throw new DownloadException(String.format("Download failed (HTTP %d)", response.statusCode()));
            long total = response.headers().firstValueAsLong("content-length").orElse(fallbackTotal);
            progress = DownloadProgress.downloading(0, total);
            stream(response.body(), target, total);
        } catch (IOException e) {
            throw new DownloadException("Download failed: " + e.getMessage());
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            throw new DownloadException("Download interrupted");
        }
    }

    private static void stream(InputStream body, Path target, long total) throws IOException {
        try (InputStream in = body; OutputStream out = Files.newOutputStream(target)) {
            byte[] buffer = new byte[BufferSize];
            long done = 0;
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                done += read;
                progress = DownloadProgress.downloading(done, total);
            }
        }
    }

    private static void verifyChecksum(Path archive, PackageInfo info) throws DownloadException {
        String expected = info.checksum();
        if (expected == null || expected.isBlank()) return;
        if (info.checksumType() != null && !ChecksumType.equalsIgnoreCase(info.checksumType())) {
            Logger.warning(String.format("Checksum of %s type is not supported, skipping verification", info.checksumType()));
            return;
        }
        progress = DownloadProgress.verifying();
        String actual = sha256(archive);
        if (actual.equalsIgnoreCase(expected)) return;
        deleteSilently(archive);
        throw new DownloadException("Checksum mismatch for " + archive.getFileName());
    }

    private static String sha256(Path file) throws DownloadException {
        try (InputStream stream = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance(ChecksumAlgorithm);
            byte[] buffer = new byte[BufferSize];
            int read;
            while ((read = stream.read(buffer)) != -1) digest.update(buffer, 0, read);
            return toHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new DownloadException("Checksum failed: " + e.getMessage());
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        return hex.toString();
    }

    private static void createDir(Path dir) throws DownloadException {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new DownloadException("Failed to create install directory: " + e.getMessage());
        }
    }

    private static void deleteSilently(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            Logger.debug(String.format("Failed to delete %s: %s", path, e.getMessage()));
        }
    }

    private static String hostOS() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) return "windows";
        if (os.contains("mac")) return "macos";
        return "linux";
    }

    private static String hostArch() {
        String arch = System.getProperty("os.arch", "").toLowerCase();
        if (arch.equals("aarch64") || arch.equals("arm64")) return "aarch64";
        return "x64";
    }

    private static String archiveTypeFor(String os) {
        return os.equals("windows") ? "zip" : "tar.gz";
    }

    private static Thread worker(Runnable task) {
        Thread thread = new Thread(task, "tcb-jdk-downloader");
        thread.setDaemon(true);
        return thread;
    }
}
