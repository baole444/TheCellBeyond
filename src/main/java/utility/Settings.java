package utility;

public class Settings {
    public static float GRID_WIDTH = 0.32f;
    public static float GRID_HEIGHT = 0.32f;

    public static int BOX_W = (int) (GRID_WIDTH * 200);
    public static int BOX_H = (int) (GRID_HEIGHT * 200);

    public static class PATH {
        public static final String CONSOLA = "engine://assets/fonts/Consola.ttf";

        public static final String DEFAULT_TEXTURE_SHADER = "engine://assets/shaders/defaultTexture.glsl";
        public static final String DEFAULT_FONT_SHADER = "engine://assets/shaders/defaultFont.glsl";
        public static final String OBJECT_SELECTION_SHADER = "engine://assets/shaders/objSelection.glsl";
        public static final String DEBUG_LINE2_SHADER = "engine://assets/shaders/DBLine2.glsl";
    }

    public static final String _projectVersion = "0.0.1";
}
