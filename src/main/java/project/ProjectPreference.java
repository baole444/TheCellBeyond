package project;

import com.fasterxml.jackson.annotation.JsonIgnore;
import physic2d.Physic2D;
import scripting.API;

@API
public record ProjectPreference(
        String name,
        int gameWindowWidth, int gameWindowHeight,
        boolean allowResize, boolean maintainAspectRatio,
        WindowResizeMode resizeMode,
        float textureGlobalScale,
        ClearColor clearColor,
        int physicFrameRate,
        RenderingSetting renderingSetting
) {
    public ProjectPreference {
        if (name == null || name.isBlank()) name = "Untitled Project";
        gameWindowWidth = Math.max(1, gameWindowWidth);
        gameWindowHeight = Math.max(1, gameWindowHeight);
        if (resizeMode == null) resizeMode = WindowResizeMode.Scale;
        textureGlobalScale = Math.max(0.01f, textureGlobalScale);
        if (clearColor == null) clearColor = new ClearColor();
        if (physicFrameRate <= 0) physicFrameRate = 60;
        physicFrameRate = Math.clamp(physicFrameRate, Physic2D.MinPhysicFrameRate, Physic2D.MaxPhysicFrameRate);
        if (renderingSetting == null) renderingSetting = new RenderingSetting();
    }

    @JsonIgnore
    public ProjectPreference() {
        this("Untitled Project",
                640, 480,
                false, true,
                WindowResizeMode.Scale,
                1.0f,
                new ClearColor(),
                60,
                new RenderingSetting()
        );
    }

    @JsonIgnore
    public ProjectPreference(String name) {
        this(name,
                640, 480,
                false, true,
                WindowResizeMode.Scale,
                1.0f,
                new ClearColor(),
                60,
                new RenderingSetting()
        );
    }
}