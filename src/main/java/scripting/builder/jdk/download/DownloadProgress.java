package scripting.builder.jdk.download;

import java.nio.file.Path;

/**
 * A immutable snapshot of an ongoing JDK download process, which can be pull from {@link JDKDownloader}.
 * @param phase the current stage of the download
 * @param byteDone bytes transferred so far, only meaningful while in {@code Downloading} phase.
 * @param byteTotal total bytes to transfer, or {@code 0} when unknown
 * @param message a short status message or error line
 * @param home the resolved JDK home once finished, or null on failure
 */
public record DownloadProgress(Phase phase, long byteDone, long byteTotal, String message, Path home) {
    public enum Phase {
        Idle,
        Querying,
        Downloading,
        Verifying,
        Extracting,
        Done,
        Failed
    }

    static DownloadProgress idle() {
        return new DownloadProgress(Phase.Idle, 0, 0, "", null);
    }

    static DownloadProgress querying() {
        return new DownloadProgress(Phase.Querying, 0, 0, "Fetching latest info...", null);
    }

    static DownloadProgress downloading(long done, long total) {
        return new DownloadProgress(Phase.Downloading, done, total, "Downloading...", null);
    }

    static DownloadProgress verifying() {
        return new DownloadProgress(Phase.Verifying, 0, 0, "Verifying checksum...", null);
    }

    static DownloadProgress extracting() {
        return new DownloadProgress(Phase.Extracting, 0, 0, "Extracting...", null);
    }

    static DownloadProgress done(Path home) {
        return new DownloadProgress(Phase.Done, 0, 0, "Done", home);
    }

    static DownloadProgress failed(String message) {
        return new DownloadProgress(Phase.Failed, 0, 0, message, null);
    }

    /**
     * The download progress in fraction, in range of {@code [0, 1]}, 0 for when total is unknown.
     * @return the completed fraction
     */
    public float fraction() {
        if (byteTotal <= 0) return 0.0f;
        return Math.min(1.0f, (float) byteDone / byteTotal);
    }
}
