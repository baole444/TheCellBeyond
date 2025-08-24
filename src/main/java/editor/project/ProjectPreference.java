package editor.project;

import org.joml.Vector2f;

public class ProjectPreference {
    private static ProjectPreference instance;

    private int gameWindowWidth = 1280;
    private int gameWindowHeight = 960;

    private boolean allowResize = false;
    private boolean maintainAspectRatio = true;

    private ProjectPreference() {}

    public static ProjectPreference get() {
        if (instance == null) instance = new ProjectPreference();

        return instance;
    }

    public int getGameWindowWidth() {
        return gameWindowWidth;
    }

    public void setGameWindowWidth(int gameWindowWidth) {
        this.gameWindowWidth = gameWindowWidth;
    }

    public int getGameWindowHeight() {
        return gameWindowHeight;
    }

    public void setGameWindowHeight(int gameWindowHeight) {
        this.gameWindowHeight = gameWindowHeight;
    }

    public float getGameAspectRatio() {
        return (float) gameWindowWidth / gameWindowHeight;
    }

    public boolean isAllowResize() {
        return allowResize;
    }

    public void setAllowResize(boolean allowResize) {
        this.allowResize = allowResize;
    }

    public boolean isMaintainAspectRatio() {
        return maintainAspectRatio;
    }

    public void setMaintainAspectRatio(boolean maintainAspectRatio) {
        this.maintainAspectRatio = maintainAspectRatio;
    }
}