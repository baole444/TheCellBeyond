package scripting.builder;

/**
 * Outcome of a {@link GradleRunner} build invocation.
 * @param success true if the build finished successfully with {@code exCode == 0}
 * @param exitCode the Gradle process exit code, or {@value InProgressExitCode}, {@value LaunchFailedExitCode} for in process reject and launch failed
 * @param durationMs the time took to finish the build, in milliseconds
 */
public record BuildResult(boolean success, int exitCode, long durationMs) {
    /**
     * Exit code for when a build is skipped due to an ongoing build request.
     */
    public static final int InProgressExitCode = -1;
    /**
     * Exit code for when the Gradle process could not be launched, such as missing wrapper.
     */
    public static final int LaunchFailedExitCode = -2;

    /**
     * Result for a build request rejected due to an ongoing build request.
     * @return a failed result with {@link #InProgressExitCode}
     */
    public static BuildResult inProgress() {
        return new BuildResult(false, InProgressExitCode, 0L);
    }

    /**
     * Result for a build that could not be launched.
     * @return a failed result with {@link #LaunchFailedExitCode}
     */
    public static BuildResult launchFailed() {
        return new BuildResult(false, LaunchFailedExitCode, 0L);
    }
}
