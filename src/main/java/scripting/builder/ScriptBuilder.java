package scripting.builder;

import project.Project;
import scripting.ScriptLoader;
import scripting.transpiler.codegen.TranspileResult;
import scripting.transpiler.codegen.Transpiler;
import utility.log.EngineLog;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Script builder handle the translation and building of translated script files into JAR for the engine to load.
 * <p>
 * The build process run async on a single thread executor. On completing a build, invoke {@link ScriptLoader} to reload
 * the script jars for the engine if requested.
 */
public final class ScriptBuilder {
    private static final EngineLog Logger = new EngineLog(ScriptBuilder.class);
    private static final String ScriptsSrcDir = "scripts-src";
    private static final ExecutorService Executor = Executors.newSingleThreadExecutor(ScriptBuilder::builder);
    private static final AtomicBoolean inProgress = new AtomicBoolean(false);
    private static volatile BuildStatus status = BuildStatus.idle();
    private static volatile int pendingScriptCount = 0;
    private static volatile long pendingBuildDurationMs = 0L;

    private ScriptBuilder() {}

    /**
     * Check if there is an ongoing build process.
     * @return true if there is
     */
    public static boolean inProgress() {
        return inProgress.get();
    }

    /**
     * Get the latest build status snapshot. This action is thread safe.
     * @return the current status
     */
    public static BuildStatus status() {
        return status;
    }

    /**
     * Start the transpile and build process. If there is an ongoing proceed, this do nothing.
     * <p>
     * The transpile and build process run in the background async. Upon completed, if {@code reloadOnFinish} is true,
     * This will invoke {@link ScriptLoader} to reload the script jars.
     * @param jdkHome the JDK home to use for the Gradle runner
     * @param cleanBuildScripts run the Gradle clean task before jar
     * @param overrideJVM use the caller's own JVM configuration instead
     * @param reloadOnFinish reload the script jar into the engine on success.
     */
    public static void build(Path jdkHome, boolean cleanBuildScripts, boolean overrideJVM, boolean reloadOnFinish) {
        if (!inProgress.compareAndSet(false, true)) return;
        Logger.info("Project script building started...");
        status = BuildStatus.transpiling();
        CompletableFuture.supplyAsync(() -> run(jdkHome, cleanBuildScripts, overrideJVM), Executor).whenComplete((succeeded, _) -> {
            if (!succeeded) {
                inProgress.set(false);
                return;
            }
            if (!reloadOnFinish) {
                status = BuildStatus.succeeded(pendingScriptCount, pendingBuildDurationMs);
                inProgress.set(false);
                return;
            }
            ScriptLoader.reload().whenComplete((_, _) -> {
                status = BuildStatus.succeeded(pendingScriptCount, pendingBuildDurationMs);
                inProgress.set(false);
            });
        });
    }

    private static boolean run(Path jdkHome, boolean cleanBuildScripts, boolean overrideJVM) {
        try {
            String root = Project.projectRoot();
            if (root == null || root.isBlank()) {
                fail(BuildStatus.buildFail(), "Failed to build scripts: no project opened");
                return false;
            }
            Path scriptRoot = Path.of(root, ScriptsSrcDir);
            Path buildDir = GradleRunner.resolveBuildDir(jdkHome, overrideJVM).join();
            TranspileResult transpiled = Transpiler.transpileProject(scriptRoot, buildDir);
            if (!transpiled.success()) {
                status = BuildStatus.transpileFail(transpiled.errors());
                inProgress.set(false);
                return false;
            }
            int scriptCount = transpiled.transpileCount();
            Logger.info(String.format("Translated %d TCBScript (.tcbs) files", scriptCount));
            status = BuildStatus.building(scriptCount);
            BuildResult result = GradleRunner.buildScripts(jdkHome, cleanBuildScripts, overrideJVM).join();
            if (!result.success()) {
                status = new BuildStatus(BuildPhase.BuildFailed, scriptCount, result.durationMs(), result.exitCode(), List.of());
                inProgress.set(false);
                return false;
            }
            pendingScriptCount = scriptCount;
            pendingBuildDurationMs = result.durationMs();
            return true;
        } catch (RuntimeException e) {
            fail(BuildStatus.buildFail(), "Failed to build scripts: " + e.getMessage());
            return false;
        }
    }

    private static void fail(BuildStatus failure, String message) {
        Logger.error(message);
        status = failure;
        inProgress.set(false);
    }

    private static Thread builder(Runnable task) {
        Thread thread = new Thread(task, "tcb-script-builder");
        thread.setDaemon(true);
        return thread;
    }
}
