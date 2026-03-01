package editor.preference;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * EditorPreferences contain user's choice on some functionality and behaviour of the Editor.
 * @param autoSaveOnExit should editing scene be saved automatically on exit
 * @param autoSaveOnChangeScene should editing scene be saved automatically on switching to a different scene
 * @param showGridLine should the gridline be shown in the scene's viewport
 */
public record EditorPreferences(boolean autoSaveOnExit, boolean autoSaveOnChangeScene, boolean showGridLine) {
    /**
     * Create default behaviour.
     */
    @JsonIgnore
    public EditorPreferences() {
        this(false, false, false);
    }
}
