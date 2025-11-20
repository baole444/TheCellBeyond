package project;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record ProjectPreference(
        String name,
        int gameWindowWidth, int gameWindowHeight,
        boolean allowResize, boolean maintainAspectRatio,
        float textureGlobalScale
) {
    @JsonIgnore
    public ProjectPreference() {
        this("Untitled Project", 640, 480, false, true, 1.0f);
    }

    @JsonIgnore
    public ProjectPreference(String name) {
        this(name, 640, 480, false, true, 1.0f);
    }
}