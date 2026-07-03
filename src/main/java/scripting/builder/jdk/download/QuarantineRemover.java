package scripting.builder.jdk.download;

import utility.log.EngineLog;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Clear the {@code com.apple.quarantine} attribute from the downloaded JDK tree so Gatekeeper don't
 * block the extracted {@code java} / {@code javac} binaries when they are used for the first time by the runner.
 * <p>
 * This only apply for macOS, the callers are responsible for check the host OS before calling this.
 */
final class QuarantineRemover {
    private static final EngineLog Logger = new EngineLog(QuarantineRemover.class);
    private static final String QuarantineAttribute = "com.apple.quarantine";
    private static final long TimeoutSeconds = 60L;

    private QuarantineRemover() {}

    /**
     * Recursively remove the quarantine ATTB from the targeted path.
     * @param target the extracted JDK home
     */
    static void strip(Path target) {
        if (target == null) return;
        try {
            Process process = new ProcessBuilder("xattr", "-dr", QuarantineAttribute, target.toString()).redirectErrorStream(true).start();
            if (!process.waitFor(TimeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                Logger.warning("Timed out clearing quarantine attribute from " + target);
            }
        } catch (IOException e) {
            Logger.warning(String.format("Failed to clear quarantine attribute from %s: %s", target, e.getMessage()));
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }
}
