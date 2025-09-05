package editor.project;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record ProjectPreference(
        String name,
        int gameWindowWidth, int gameWindowHeight,
        boolean allowResize, boolean maintainAspectRatio
) {
    @JsonIgnore
    public ProjectPreference() {
        this("Untitled Project", 640, 480, false, true);
    }

    @JsonIgnore
    public ProjectPreference(String name) {
        this(name, 640, 480, false, true);
    }
}