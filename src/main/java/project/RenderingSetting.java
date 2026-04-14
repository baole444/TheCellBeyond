package project;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record RenderingSetting(VsyncMode vsyncMode, int targetFrameRate) {
    public static final int MinFrameRate = 0;
    public static final int MaxFrameRate = 480;

    public RenderingSetting {
        if (vsyncMode == null) vsyncMode = VsyncMode.Enabled;
        targetFrameRate = Math.clamp(targetFrameRate, MinFrameRate, MaxFrameRate);
    }

    @JsonIgnore
    public RenderingSetting() {
        this(VsyncMode.Enabled, 60);
    }
}
