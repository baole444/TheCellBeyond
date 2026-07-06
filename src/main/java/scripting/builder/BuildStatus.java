package scripting.builder;

import java.util.List;

/**
 * Status of script building process.
 * @param phase the current phase in the process
 * @param scriptCount the amount of script transpiled
 * @param buildMS the duration of the process, only have meaning on succeeded
 * @param exitCode the process exit code
 * @param errors the list of errors, only have meaning on build failed status
 */
public record BuildStatus(BuildPhase phase, int scriptCount, long buildMS, int exitCode, List<String> errors) {
    static BuildStatus idle() {
        return new BuildStatus(BuildPhase.Idle, 0, 0L, 0, List.of());
    }

    static BuildStatus transpiling() {
        return new BuildStatus(BuildPhase.Transpiling, 0, 0L, 0, List.of());
    }

    static BuildStatus buildFail() {
        return new BuildStatus(BuildPhase.BuildFailed, 0, 0L, BuildResult.LaunchFailedExitCode, List.of());
    }

    static BuildStatus transpileFail(List<String> errors) {
        return new BuildStatus(BuildPhase.TranspilerFailed, 0, 0L, 0, List.copyOf(errors));
    }

    static BuildStatus building(int transpiledCount) {
        return new BuildStatus(BuildPhase.Building, transpiledCount, 0L, 0, List.of());
    }

    static BuildStatus succeeded(int scriptCount, long duration) {
        return new BuildStatus(BuildPhase.Succeeded, scriptCount, duration, 0, List.of());
    }
}
