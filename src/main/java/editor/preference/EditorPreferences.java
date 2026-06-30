package editor.preference;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Persisted user's choices on some functionality and behaviour of the Editor.
 * @param autoSaveOnExit  control auto save editing scene on exit
 * @param autoSaveOnChangeScene control auto save editing scene when switching to another scene
 * @param showGridLine control drawing and snapping to gridline on scene's editor viewport
 * @param cleanBuildScripts control invoking clean task before jar in Gradle runner for user's scripts
 */
public record EditorPreferences(boolean autoSaveOnExit, boolean autoSaveOnChangeScene, boolean showGridLine, boolean cleanBuildScripts) {
    /**
     * The current preference schema version. Files without a version a treated as version 0.
     */
    static final int SaveVersion = 1;
    /**
     * The YAML key that store the preference schema version number.
     */
    static final String VersionKey = "version";

    /**
     * Create default behaviour.
     */
    @JsonIgnore
    public EditorPreferences() {
        this(false, false, false, true);
    }

    @JsonIgnore
    public EditorPreferences autoSaveOnExit(boolean autoSaveOnExit) {
        return new EditorPreferences(autoSaveOnExit, autoSaveOnChangeScene, showGridLine, cleanBuildScripts);
    }

    @JsonIgnore
    public EditorPreferences autoSaveOnChangeScene(boolean autoSaveOnChangeScene) {
        return new EditorPreferences(autoSaveOnExit, autoSaveOnChangeScene, showGridLine, cleanBuildScripts);
    }

    @JsonIgnore
    public EditorPreferences showGridLine(boolean showGridLine) {
        return new EditorPreferences(autoSaveOnExit, autoSaveOnChangeScene, showGridLine, cleanBuildScripts);
    }

    @JsonIgnore
    public EditorPreferences cleanBuildScripts(boolean cleanBuildScripts) {
        return new EditorPreferences(autoSaveOnExit, autoSaveOnChangeScene, showGridLine, cleanBuildScripts);
    }
}
