package editor.preference;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record EditorPreferences(boolean autoSaveOnExit, boolean autoSaveOnChangeScene) {
    @JsonIgnore
    public EditorPreferences() {
        this(false, false);
    }
}
