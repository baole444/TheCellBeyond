package scripting.builder;

import eventviewer.EngineEventCallback;
import eventviewer.event.EditorEvent;
import project.Project;
import scripting.transpiler.TranspilerProperties;
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
 * Runner the script project's Gradle wrapper and build the scripts jar.
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
    private static final String JavaHomeEnv = "JAVA_HOME";
    private static final String GradleJavaHomeProperty = "-Dorg.gradle.java.home=";
    private static final String InitScriptFlag = "--init-script";
    private static final String InitScriptFileName = "tcb-script-sourceset.init.gradle";
    /**
     * Init script injected when Editor control the build JVM, applying the transpiler's generate
     * source directory onto {@code main} java source set.
     */
    private static final String InitScriptContent = String.format("""
            allprojects { proj ->
                proj.pluginManager.withPlugin('java') {
                    proj.sourceSets.main.java.srcDir(proj.file('%s'))
                }
            }
            """, TranspilerProperties.TranspilerOutputDir);
    private static final ExecutorService Executor = Executors.newSingleThreadExecutor(GradleRunner::buildThread);
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
        return CompletableFuture.supplyAsync(() -> runBuild(jdkHome, cleanBuildScripts, overrideJVM), Executor).whenComplete((_, _) -> buildInProgress.set(false));
    }

    private static BuildResult runBuild(Path jdkHome, boolean cleanBuildScripts, boolean overrideJVM) {
        Path scriptRoot = scriptsRoot();
        if (scriptRoot == null) {
            Logger.error("No project is opened, cannot build scripts");
            return BuildResult.launchFailed();
        }
        Path wrapper = scriptRoot.resolve(onWindows() ? WrapperWindows : WrapperUnix);
        if (!Files.isRegularFile(wrapper)) {
            Logger.error(String.format("Gradle wrapper not found at '%s'. Please generate script project first!", wrapper));
            return BuildResult.launchFailed();
        }
        ensureExecutable(wrapper);
        Path initScript = overrideJVM ? null : ensureInitScript();
        List<String> command = buildCommand(wrapper, jdkHome, initScript, cleanBuildScripts, overrideJVM);
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

    private static BuildResult finish(int exitCode, long durationMs) {
        String elapsed = formatDuration(durationMs);
        if (exitCode == 0) {
            Logger.info("Script build succeeded in " + elapsed);
            return new BuildResult(true, exitCode, durationMs);
        }
        Logger.error(String.format("Script build failed (exit code %d) in %s", exitCode, elapsed));
        return new BuildResult(false, exitCode, durationMs);
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

    private static List<String> buildCommand(Path wrapper, Path jdkHome, Path initScript, boolean cleanBuildScripts, boolean overrideJVM) {
        List<String> command = new ArrayList<>();
        command.add(wrapper.toString());
        if (!overrideJVM) command.add(GradleJavaHomeProperty + jdkHome.toAbsolutePath());
        if (initScript != null) {
            command.add(InitScriptFlag);
            command.add(initScript.toString());
        }
        if (cleanBuildScripts) command.add(CleanTask);
        command.add(JarTask);
        return command;
    }

    private static Path scriptsRoot() {
        String root = Project.projectRoot();
        if (root == null || root.isBlank()) return null;
        return Path.of(root, ScriptsSrcDir);
    }

    /**
     * Write the source set init script to a temp file. If this operation failed,
     * the build will proceed and relying on the declared source set.
     * @return the init script path, or null on failure
     */
    private static Path ensureInitScript() {
        Path initScript = Path.of(System.getProperty("java.io.tmpdir"), InitScriptFileName);
        try {
            Files.writeString(initScript, InitScriptContent, StandardCharsets.UTF_8);
            return initScript;
        } catch (IOException e) {
            Logger.warning("Failed to write the script source set init script: " + e.getMessage());
            return null;
        }
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
