package editor.project;

public record ProjectPreference(
        String name,
        int gameWindowWidth, int gameWindowHeight,
        boolean allowResize, boolean maintainAspectRatio
) {
    public ProjectPreference() {
        this("Untitled Project", 640, 480, false, true);
    }

    public ProjectPreference(String name) {
        this(name, 640, 480, false, true);
    }

    public float getGameAspectRatio() {
        return (float) gameWindowWidth / gameWindowHeight;
    }


}