package scripting.builder;

import eventviewer.EngineEventCallback;
import eventviewer.event.EditorEvent;
import project.Project;
import utility.log.EngineLog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runner for the Editor to invoke the script project's Gradle wrapper and build the scripts jar.
 * <p>
 * Builds run on a single background thread to not block the editor's rendering. Only 1 build runs at a time per runner.
 * Build's output is redirected to {@link EngineLog} for previewing in the console.
 */
public final class GradleRunner {
    private static final EngineLog Logger = new EngineLog(GradleRunner.class);
    private static final EngineLog GradleOutput = new EngineLog("Gradle");
    private static final String ScriptsSrcDir = "scripts-src";
    private static final String WrapperUnix = "gradlew";
    private static final String WrapperWindows = "gradlew.bat";
    private static final String CleanTask = "clean";
    private static final String JarTask = "jar";
    private static final String PropertiesTask = "properties";
    private static final String QuietFlag = "-q";
    private static final String BuildDirProperty = "buildDir:";
    private static final String DefaultBuildDir = "build";
    private static final String JavaHomeEnv = "JAVA_HOME";
    private static final String GradleJavaHomeProperty = "-Dorg.gradle.java.home=";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor(GradleRunner::buildThread);
    private static final AtomicBoolean buildInProgress = new AtomicBoolean(false);

    private GradleRunner() {}

    /**
     * Check if there is an ongoing build running or queued on this runner.
     * @return true when a build is occupying the runner
     */
    public static boolean buildInProgress() {
        return buildInProgress.get();
    }

    /**
     * Run {@code gradlew clean jar} in the project's {@code scripts-src} directory, or {@code gradlew jar} when {@code cleanBuildScripts} is false.
     * The process output is redirected to {@link EngineLog}.
     * <p>
     * When there is a build in progress, no second build is launched, a completed future with {@link BuildResult#inProgress()} is returned instead.
     * </p>
     * When overriding JVM is enabled, the runner will not enforce the JVM home, and user can use whatever they choose to supply.
     * @param jdkHome the JDK home to use
     * @param cleanBuildScripts the flag to run clean task before build and jar
     * @param overrideJVM  the flag to allow user to supply their own JVM
     * @return a future completing with the build outcome
     */
    public static CompletableFuture<BuildResult> buildScripts(Path jdkHome, boolean cleanBuildScripts, boolean overrideJVM) {
        if (!overrideJVM && jdkHome == null) {
            Logger.error("No valid JDK provided for Gradle JVM to build scripts!");
            EngineEventCallback.emit(new EditorEvent(EditorEvent.Type.MissingRunnerJDK));
            return CompletableFuture.completedFuture(BuildResult.noJDK());
        }
        if (!buildInProgress.compareAndSet(false, true)) return CompletableFuture.completedFuture(BuildResult.inProgress());
        return CompletableFuture.supplyAsync(() -> runBuild(jdkHome, cleanBuildScripts, overrideJVM), executor).whenComplete((_, _) -> buildInProgress.set(false));
    }

    /**
     * Resolve the project's actual Gradle build output directory, honouring user's customized {@code layout.buildDirectory}.
     * The system fallback to Gradle's default if the custom layout can't be read.
     * <p>
     * This is intended for callers that exclude the build output from a source scan.
     * @param jdkHome the JDK home to use
     * @param overrideJVM  the flag to allow user to supply their own JVM
     * @return a future completing with the build directory path, or null when no project is opened
     */
    public static CompletableFuture<Path> resolveBuildDir(Path jdkHome, boolean overrideJVM) {
        return CompletableFuture.supplyAsync(() -> runResolveBuildDir(jdkHome, overrideJVM), executor);
    }

    private static BuildResult runBuild(Path jdkHome, boolean cleanBuildScripts, boolean overrideJVM) {
        Path scriptRoot = scriptsRoot();
        if (scriptRoot == null) {
            Logger.error("No project is opened, cannot build scripts");
            return BuildResult.launchFailed();
        }
        Path wrapper = wrapper(scriptRoot);
        if (!Files.isRegularFile(wrapper)) {
            Logger.error(String.format("Gradle wrapper not found at '%s'. Please generate script project first!", wrapper));
            return BuildResult.launchFailed();
        }
        ensureExecutable(wrapper);
        List<String> command = buildCommand(wrapper, jdkHome, cleanBuildScripts, overrideJVM);
        Logger.info("Building scripts: " + String.join(" ", command));
        long start = System.nanoTime();
        try {
            Process process = gradleProcess(command, scriptRoot, jdkHome, overrideJVM).start();
            tee(process);
            int exitCode = process.waitFor();
            long durationMs = (System.nanoTime() - start) / 1_000_000L;
            return finish(exitCode, durationMs);
        } catch (IOException e) {
            Logger.error("Failed to launch Gradle build: " + e.getMessage());
            return BuildResult.launchFailed();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Logger.error("Gradle build interrupted");
            return BuildResult.launchFailed();
        }
    }

    private static Path runResolveBuildDir(Path jdkHome, boolean overrideJVM) {
        Path scriptsRoot = scriptsRoot();
        if (scriptsRoot == null) return null;
        Path fallback = scriptsRoot.resolve(DefaultBuildDir);
        Path wrapper = wrapper(scriptsRoot);
        if (!Files.isRegularFile(wrapper) || (!overrideJVM && jdkHome == null)) return fallback;
        ensureExecutable(wrapper);
        try {
            List<String> command = overrideJVM ? List.of(wrapper.toString(), PropertiesTask, QuietFlag) : List.of(wrapper.toString(), gradleJavaHomeArg(jdkHome), PropertiesTask, QuietFlag);
            Process process = gradleProcess(command, scriptsRoot, jdkHome, overrideJVM).start();
            Path resolved = parseBuildDir(process, fallback);
            process.waitFor();
            return resolved;
        } catch (IOException e) {
            Logger.warning("Could not resolve Gradle build directory: " + e.getMessage());
            return fallback;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return fallback;
        }
    }

    private static BuildResult finish(int exitCode, long durationMs) {
        String elapsed = formatDuration(durationMs);
        if (exitCode == 0) {
            Logger.info("Script build succeeded in " + elapsed);
            return new BuildResult(true, exitCode, durationMs);
        }
        Logger.error(String.format("Script build failed (exit code %d) in %s", exitCode, elapsed));
        return new BuildResult(false, exitCode, durationMs);
    }

    private static Path parseBuildDir(Process process, Path fallback) throws IOException {
        Path result = fallback;
        try (BufferedReader reader = reader(process)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith(BuildDirProperty)) continue;
                String value = line.substring(BuildDirProperty.length()).trim();
                if (!value.isEmpty()) result = Path.of(value);
            }
        }
        return result;
    }

    private static void tee(Process process) throws IOException {
        try (BufferedReader reader = reader(process)) {
            String line;
            while ((line = reader.readLine()) != null) GradleOutput.debug(line);
        }
    }

    private static BufferedReader reader(Process process) {
        return new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
    }

    private static ProcessBuilder gradleProcess(List<String> command, Path workingDir, Path jdkHome, boolean overrideJVM) {
        ProcessBuilder process = new ProcessBuilder(command).directory(workingDir.toFile()).redirectErrorStream(true);
        if (!overrideJVM && jdkHome != null) process.environment().put(JavaHomeEnv, jdkHome.toAbsolutePath().toString());
        return process;
    }

    private static List<String> buildCommand(Path wrapper, Path jdkHome, boolean cleanBuildScripts, boolean overrideJVM) {
        List<String> command = new ArrayList<>();
        command.add(wrapper.toString());
        if (!overrideJVM) command.add(gradleJavaHomeArg(jdkHome));
        if (cleanBuildScripts) command.add(CleanTask);
        command.add(JarTask);
        return command;
    }

    private static String gradleJavaHomeArg(Path jdkHome) {
        return GradleJavaHomeProperty + jdkHome.toAbsolutePath();
    }

    private static Path scriptsRoot() {
        String root = Project.projectRoot();
        if (root == null || root.isBlank()) return null;
        return Path.of(root, ScriptsSrcDir);
    }

    private static Path wrapper(Path scriptRoot) {
        return scriptRoot.resolve(onWindows() ? WrapperWindows : WrapperUnix);
    }

    private static void ensureExecutable(Path wrapper) {
        if (onWindows() || Files.isExecutable(wrapper)) return;
        if (!wrapper.toFile().setExecutable(true)) Logger.warning(String.format("Could not make `%s` executable.%nThe build may fail with a permission error.", wrapper));
    }

    private static boolean onWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static Thread buildThread(Runnable task) {
        Thread thread = new Thread(task, "tcb-gradle-runner");
        thread.setDaemon(true);
        return thread;
    }

    private static String formatDuration(long durationMs) {
        if (durationMs < 1000L) return durationMs + "ms";
        long hours = durationMs / 3_600_000L;
        long minutes = (durationMs / 60_000L) % 60L;
        long seconds = (durationMs / 1000L) % 60L;
        StringBuilder elapsed = new StringBuilder();
        if (hours > 0) elapsed.append(hours).append("h ");
        if (minutes > 0) elapsed.append(minutes).append("m ");
        if (seconds > 0) elapsed.append(seconds).append("s");
        return elapsed.toString().trim();
    }
}
