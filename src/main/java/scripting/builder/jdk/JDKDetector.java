package scripting.builder.jdk;

import utility.log.EngineLog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * JDKDetector is a collection of static method used to detect valid JDk installation in common path or a given home directory.
 */
public final class JDKDetector {
    private static final EngineLog Logger = new EngineLog(JDKDetector.class);
    private static final List<Path> CommonRoots = commonRoots();
    public static final int RequiredVersion = 25;
    private static final Pattern ReleaseEntry = Pattern.compile("(?m)^([A-Z_]+)=\"?([^\"\\r\\n]*)\"?[ \\t]*$");
    private static final Pattern VersionOutput = Pattern.compile("version \"([^\"]+)\"");
    private static final String ReleaseFile = "release";
    private static final String JavaVersionKey = "JAVA_VERSION";
    private static final String ImplementorKey = "IMPLEMENTOR";
    private static final String VersionFlag = "-version";
    private static final long ReadTimeoutSeconds = 5L;

    private JDKDetector() {}

    /**
     * Look for JDK installation in common installation path.
     * @return the valid JDKs found, duplicates resolved by home
     */
    static List<JDKInstallation> detectCommonInstalled() {
        List<JDKInstallation> result = new ArrayList<>();
        Set<Path> seen = new HashSet<>();
        for (Path root : CommonRoots) {
            for (Path candidate : childDirs(root)) {
                JDKInstallation jdk = inspect(candidate, JDKInstallation.Source.Detected);
                if (jdk.valid() && seen.add(jdk.home().toAbsolutePath().normalize())) result.add(jdk);
            }
        }
        return result;
    }

    /**
     * Inspect a single directory as a JDK home. If the release file is missing or can't be parsed,
     * process will be used to invoke {@code java -version} to check the version.
     * <p>
     * This is a blocking process and should not be call on main thread.
     * @param home the directory to inspect
     * @param source the source to record on the result
     * @return a valid installation record, or invalid if no usable JDK was found
     */
    static JDKInstallation inspect(Path home, JDKInstallation.Source source) {
        return inspect(home, source, true);
    }

    /**
     * Inspect a single directory as a JDK home.
     * <p>
     * This is a blocking process and should not be call on main thread.
     * @param home the directory to inspect
     * @param source the source to record on the result
     * @param useFallback true to use process and invoke {@code java -version} to check the version when the release file is missing or can't be parsed.
     * @return a valid installation record, or invalid if no usable JDK was found
     */
    static JDKInstallation inspect(Path home, JDKInstallation.Source source, boolean useFallback) {
        if (home == null) return invalid(null, source);
        Path resolved = resolveHome(home);
        if (!hasJavac(resolved)) return invalid(home, source);
        Map<String, String> release = readRelease(resolved);
        String version = release.get(JavaVersionKey);
        int featureVersion = featureVersion(version);
        if (featureVersion < 0 && useFallback) featureVersion = readVersion(resolved);
        if (featureVersion < RequiredVersion) return invalid(home, source);
        return new JDKInstallation(resolved, clean(release.get(ImplementorKey)), version.trim(), featureVersion, source, true);
    }

    /**
     * Fallback use when the JDK is missing {@code release} file, or it can't be parsed.
     * @param home the directory of the JDK installation
     * @return the feature version number, -1 if failed to read or timeout
     */
    static int readVersion(Path home) {
        Path java = javaBinary(home);
        if (java == null) return -1;
        Process process = null;
        try {
            process = new ProcessBuilder(java.toString(), VersionFlag).redirectErrorStream(true).start();
            if (!process.waitFor(ReadTimeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return -1;
            }
            return parseReadVersion(readProcess(process));
        } catch (IOException e) {
            Logger.debug(String.format("Failed to read Java version at %s: %s", java, e.getMessage()));
            return -1;
        } catch (InterruptedException _) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    static int parseReadVersion(String output) {
        Matcher matcher = VersionOutput.matcher(output);
        return matcher.find() ? featureVersion(matcher.group(1)) : -1;
    }

    private static JDKInstallation invalid(Path home, JDKInstallation.Source source) {
        return new JDKInstallation(home, null, null, -1, source, false);
    }

    private static Path resolveHome(Path home) {
        Path bundle = home.resolve("Contents").resolve("Home");
        return Files.isDirectory(bundle) ? bundle : home;
    }

    private static boolean hasJavac(Path home) {
        Path bin = home.resolve("bin");
        return Files.isRegularFile(bin.resolve("javac")) || Files.isRegularFile(bin.resolve("javac.exe"));
    }

    private static Map<String, String> readRelease(Path home) {
        Path release = home.resolve(ReleaseFile);
        if (!Files.isRegularFile(release)) return Map.of();
        try {
            Map<String, String> entries = new HashMap<>();
            Matcher matcher = ReleaseEntry.matcher(Files.readString(release, StandardCharsets.UTF_8));
            while (matcher.find()) entries.put(matcher.group(1), matcher.group(2));
            return entries;
        } catch (IOException e) {
            Logger.debug(String.format("Failed to read release file at %s: %s", release, e.getMessage()));
            return Map.of();
        }
    }

    private static String readProcess(Process process) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) output.append(line).append('\n');
            return output.toString();
        } catch (IOException e) {
            return "";
        }
    }

    private static Path javaBinary(Path home) {
        Path bin = home.resolve("bin");
        Path unix = bin.resolve("java");
        if (Files.isRegularFile(unix)) return unix;
        Path windows = bin.resolve("java.exe");
        return Files.isRegularFile(windows) ? windows : null;
    }

    private static int featureVersion(String javaVersion) {
        if (javaVersion == null || javaVersion.isBlank()) return -1;
        String[] parts = javaVersion.trim().split("\\.");
        try {
            int first = Integer.parseInt(parts[0]);
            if (first == 1 && parts.length > 1) return Integer.parseInt(parts[1]);
            return first;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String clean(String value) {
        if (value == null) return null;
        return value.isBlank() ? null : value.trim();
    }

    private static List<Path> commonRoots() {
        List<Path> roots = new ArrayList<>();
        String os = System.getProperty("os.name", "").toLowerCase();
        String userHome = System.getProperty("User.home", "");
        if (!userHome.isBlank()) roots.add(Path.of(userHome, ".sdkman", "candidates", "java"));
        if (os.contains("win")) {
            roots.add(Path.of("C:", "Program Files", "Microsoft"));
            roots.add(Path.of("C:", "Program Files", "Eclipse Adoptium"));
            roots.add(Path.of("C:", "Program Files", "Java"));
            return roots;
        }
        if (os.contains("mac")) {
            if (!userHome.isBlank()) roots.add(Path.of(userHome, "Library", "Java", "JavaVirtualMachines"));
            roots.add(Path.of("/Library/Java/JavaVirtualMachines"));
            return roots;
        }
        roots.add(Path.of("/usr/java"));
        roots.add(Path.of("/usr/lib/jvm"));
        return roots;
    }

    private static List<Path> childDirs(Path root) {
        if (!Files.isDirectory(root)) return List.of();
        try (Stream<Path> stream = Files.list(root)) {
            return stream.filter(Files::isDirectory).toList();
        } catch (IOException _) {
            return List.of();
        }
    }
}
