package utility;

public class Settings {
    public static float GRID_WIDTH = 0.32f;
    public static float GRID_HEIGHT = 0.32f;

    public static int BOX_W = (int) (GRID_WIDTH * 200);
    public static int BOX_H = (int) (GRID_HEIGHT * 200);

    private static final String pathJoint = "/";

    public static class ShaderPath {
        public static final String ShaderCLassPath = "engine://assets/shaders";
        public static final String DefaultTextureShader = ShaderCLassPath + pathJoint + "defaultTexture.glsl";
        public static final String DefaultFontShader = ShaderCLassPath + pathJoint + "defaultFont.glsl";
        public static final String ObjectSelectionShader = ShaderCLassPath + pathJoint + "objSelection.glsl";
        public static final String DebugLine2Shader = ShaderCLassPath + pathJoint + "DBLine2.glsl";
    }

    public static class FontPath {
        public static final String FontClassPath = "engine://assets/fonts";
        public static final String Caudex = FontClassPath + pathJoint + "Caudex.ttf";
        public static final String NotoSansRegular = FontClassPath + pathJoint + "NotoSans-Regular.ttf";
        public static final String NotoSansMono = FontClassPath + pathJoint + "NotoSansMono_Regular.ttf";
        public static final String NotoSansJapan = FontClassPath + pathJoint + "NotoSansJP.ttf";
    }

    public static class TexturePath {
        public static final String TextureClassPath = "engine://assets/textures";
        public static final String EditorControls = TextureClassPath + pathJoint + "EditorControls.png";
        public static final String Gizmo = TextureClassPath + pathJoint + "Gizmo.png";
        public static final String ObjectIndicator = TextureClassPath + pathJoint + "indicator.png";
        public static final String TCBIcon = TextureClassPath + pathJoint + "TCB icon.png";
    }
}
