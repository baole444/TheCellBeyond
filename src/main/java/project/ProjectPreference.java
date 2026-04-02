package project;

import com.fasterxml.jackson.annotation.JsonIgnore;
import physic2d.Physic2D;

public record ProjectPreference(
        String name,
        int gameWindowWidth, int gameWindowHeight,
        boolean allowResize, boolean maintainAspectRatio,
        float textureGlobalScale,
        ClearColor clearColor,
        int physicFrameRate
) {
    public ProjectPreference {
        if (name == null || name.isBlank()) name = "Untitled Project";
        gameWindowWidth = Math.max(1, gameWindowWidth);
        gameWindowHeight = Math.max(1, gameWindowHeight);
        textureGlobalScale = Math.max(0.01f, textureGlobalScale);
        if (clearColor == null) clearColor = new ClearColor();
        if (physicFrameRate <= 0) physicFrameRate = 60;
        physicFrameRate = Math.clamp(physicFrameRate, Physic2D.MinPhysicFrameRate, Physic2D.MaxPhysicFrameRate);
    }

    @JsonIgnore
    public ProjectPreference() {
        this("Untitled Project", 640, 480, false, true, 1.0f, new ClearColor(), 60);
    }

    @JsonIgnore
    public ProjectPreference(String name) {
        this(name, 640, 480, false, true, 1.0f, new ClearColor(), 60);
    }
}