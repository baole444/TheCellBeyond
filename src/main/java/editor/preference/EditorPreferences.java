package editor.preference;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Persisted user's choices on some functionality and behaviour of the Editor.
 * @param autoSaveOnExit  control auto save editing scene on exit
 * @param autoSaveOnChangeScene control auto save editing scene when switching to another scene
 * @param showGridLine control drawing and snapping to gridline on scene's editor viewport
 * @param cleanBuildScripts control invoking clean task before jar in Gradle runner for user's scripts
 * @param overrideGradleJVM control user's ability to override important settings for Gradle runner and build script.
 */
public record EditorPreferences(
        boolean autoSaveOnExit, boolean autoSaveOnChangeScene,
        boolean showGridLine,
        boolean cleanBuildScripts, boolean overrideGradleJVM,
        boolean reloadOnFinishBuildScripts
) {
    /**
     * The current preference schema version. Files without a version are treated as version 0.
     */
    static final int SaveVersion = 1;
    /**
     * The YAML key that store the preference schema version number.
     */
    static final String VersionKey = "version";
    private static final boolean DefaultAutoSaveOnExit = false;
    private static final boolean DefaultAutoSaveOnChangeScene = false;
    private static final boolean DefaultShowGridLine = false;
    private static final boolean DefaultCleanBuildScripts = true;
    private static final boolean DefaultOverrideGradleJVM = false;
    private static final boolean DefaultReloadOnFinishBuildScripts = true;

    /**
     * Create default editor behaviour.
     */
    @JsonIgnore
    public EditorPreferences() {
        this(DefaultAutoSaveOnExit, DefaultAutoSaveOnChangeScene, DefaultShowGridLine, DefaultCleanBuildScripts, DefaultOverrideGradleJVM, DefaultReloadOnFinishBuildScripts);
    }

    @JsonIgnore
    public EditorPreferences autoSaveOnExit(boolean autoSaveOnExit) {
        return new EditorPreferences(autoSaveOnExit, autoSaveOnChangeScene, showGridLine, cleanBuildScripts, overrideGradleJVM, reloadOnFinishBuildScripts);
    }

    @JsonIgnore
    public EditorPreferences autoSaveOnChangeScene(boolean autoSaveOnChangeScene) {
        return new EditorPreferences(autoSaveOnExit, autoSaveOnChangeScene, showGridLine, cleanBuildScripts, overrideGradleJVM, reloadOnFinishBuildScripts);
    }

    @JsonIgnore
    public EditorPreferences showGridLine(boolean showGridLine) {
        return new EditorPreferences(autoSaveOnExit, autoSaveOnChangeScene, showGridLine, cleanBuildScripts, overrideGradleJVM, reloadOnFinishBuildScripts);
    }

    @JsonIgnore
    public EditorPreferences cleanBuildScripts(boolean cleanBuildScripts) {
        return new EditorPreferences(autoSaveOnExit, autoSaveOnChangeScene, showGridLine, cleanBuildScripts, overrideGradleJVM, reloadOnFinishBuildScripts);
    }

    @JsonIgnore
    public EditorPreferences overrideGradleJVM(boolean overrideGradleJVM) {
        return new EditorPreferences(autoSaveOnExit, autoSaveOnChangeScene, showGridLine, cleanBuildScripts, overrideGradleJVM, reloadOnFinishBuildScripts);
    }

    @JsonIgnore
    public EditorPreferences reloadOnFinishBuildScripts(boolean reloadOnFinishBuildScripts) {
        return new EditorPreferences(autoSaveOnExit, autoSaveOnChangeScene, showGridLine, cleanBuildScripts, overrideGradleJVM, reloadOnFinishBuildScripts);
    }
}
