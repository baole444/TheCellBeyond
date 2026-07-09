package scripting.builder.jdk;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * JDKManager cache the detected JDK installations in common path, user added, and at {@code JAVA_HOME}.
 * <p>
 * Callers are responsible for initial {@link #scan(Collection)}, supplied with persisted user paths, along with owning the user's JDK selection.
 * The manager only detects, caches and returns installations.
 * </p>
 * Detections, with fallback process invoking {@code java -version}, are ran on a dedicated single thread executor.
 * Getting detected installations are thread safe (volatile), use the following methods:
 * <ul>
 *     <li>{@link #javaHome()} - Get installation at {@code JAVA_HOME} env.</li>
 *     <li>{@link #userAdded()} - Get installation(s) added by user.</li>
 *     <li>{@link #detected()} - Get installation(s) detected on common paths. </li>
 * </ul>
 */
public final class JDKManager {
    private static final ExecutorService Executor = Executors.newSingleThreadExecutor(JDKManager::worker);
    private static volatile JDKInstallation javaHome;
    private static volatile List<JDKInstallation> detected = List.of();
    private static volatile List<JDKInstallation> userAdded = List.of();
    private static volatile CompletableFuture<Void> rescanInFlight;

    private JDKManager() {}

    /**
     * Perform initial scan for JDK installations, resolve JAVA_HOME, detect common locations,
     * and inspect the given paths added by the user.
     * <p>
     * The JAVA_HOME entry is resolved immediately without invoking process or fallback,
     * while other entries ran via the executor.
     * @param userPaths the persisted JDK home paths added by the user
     * @return a future completing when the scan finished
     */
    public static CompletableFuture<Void> scan(Collection<Path> userPaths) {
        javaHome = resolveJavaHome(false);
        List<Path> snapshot = userPaths == null ? List.of() : List.copyOf(userPaths);
        return CompletableFuture.runAsync(() -> rebuild(snapshot), Executor);
    }

    /**
     * Resolve JAVA_HOME, detect common locations and inspect the current paths added by the user again.
     * <p>
     * When there are an ongoing rescan request, that future is return to prevent spamming scan request.
     * @return a future completing when the rescan finished
     */
    public static synchronized CompletableFuture<Void> rescan() {
        if (rescanInFlight != null && !rescanInFlight.isDone()) return rescanInFlight;
        rescanInFlight = CompletableFuture.runAsync(() -> rebuild(userPaths()), Executor);
        return rescanInFlight;
    }

    /**
     * Inspect the given directory as a JDK home.
     * If it is valid, the installation will be added under the added by user cache.
     * @param directory the directory to check
     * @return a future of the inspection result
     */
    public static CompletableFuture<JDKInstallation> add(Path directory) {
        return CompletableFuture.supplyAsync(() -> inspectAndCache(directory), Executor);
    }

    /**
     * Remove a JDK added by the user from cache by the given home path.
     * @param home the home path to remove
     * @return a future completing when the removal has been applied
     */
    public static CompletableFuture<Void> remove(Path home) {
        if (home == null) return CompletableFuture.completedFuture(null);
        Path target = home.toAbsolutePath().normalize();
        return CompletableFuture.runAsync(() -> filterUserAdded(userAdded, target), Executor);
    }

    /**
     * Get the resolved JAVA_HOME JDK installation.
     * @return the JDK at JAVA_HOME, or null when the env variable is not set
     */
    public static JDKInstallation javaHome() {
        return javaHome;
    }

    /**
     * Get the detected JDK installations from common locations.
     * @return the list of detected installation
     */
    public static List<JDKInstallation> detected() {
        return detected;
    }

    /**
     * Get the JDK installations added by the user.
     * @return the list of installation added by the user
     */
    public static List<JDKInstallation> userAdded() {
        return userAdded;
    }

    private static void filterUserAdded(List<JDKInstallation> source, Path target) {
        userAdded = source.stream().filter(jdk -> !sameHome(jdk, target)).toList();
    }

    private static void rebuild(Collection<Path> userPaths) {
        javaHome = resolveJavaHome(true);
        detected = JDKDetector.detectCommonInstalled();
        userAdded = inspectAll(userPaths);
    }

    private static JDKInstallation inspectAndCache(Path directory) {
        JDKInstallation jdk = JDKDetector.inspect(directory, JDKInstallation.Source.UserAdded);
        if (!jdk.valid()) return jdk;
        Path home = jdk.home().toAbsolutePath().normalize();
        for (JDKInstallation existing : userAdded) {
            if (sameHome(existing, home)) return jdk;
        }
        List<JDKInstallation> updated = new ArrayList<>(userAdded);
        updated.add(jdk);
        userAdded = List.copyOf(updated);
        return jdk;
    }

    private static List<JDKInstallation> inspectAll(Collection<Path> paths) {
        List<JDKInstallation> list = new ArrayList<>();
        Set<Path> seen = new HashSet<>();
        for (Path path : paths) {
            JDKInstallation jdk = JDKDetector.inspect(path, JDKInstallation.Source.UserAdded);
            Path home = jdk.home() == null ? null : jdk.home().toAbsolutePath().normalize();
            if (home != null && !seen.add(home)) continue;
            list.add(jdk);
        }
        return list;
    }

    private static List<Path> userPaths() {
        List<Path> paths = new ArrayList<>();
        userAdded.stream().filter(jdk -> jdk.home() != null).forEach(jdk -> paths.add(jdk.home()));
        return paths;
    }

    private static JDKInstallation resolveJavaHome(boolean useFallback) {
        String env = System.getenv("JAVA_HOME");
        if (env == null || env.isBlank()) return null;
        return JDKDetector.inspect(Path.of(env), JDKInstallation.Source.JavaHome, useFallback);
    }

    private static boolean sameHome(JDKInstallation jdk, Path normalizedHome) {
        Path home = jdk.home();
        return home != null && home.toAbsolutePath().normalize().equals(normalizedHome);
    }

    private static Thread worker(Runnable task) {
        Thread thread = new Thread(task, "tcb-jdk-scan");
        thread.setDaemon(true);
        return thread;
    }
}
