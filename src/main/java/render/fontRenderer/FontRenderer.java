package render.fontRenderer;

import TCB_Field.Window;
import org.joml.Matrix4f;
import render.Shader;

public class FontRenderer {
    private static FontBatch fontBatch;

    public static void initFontRenderer() {
        if (fontBatch != null) {
            fontBatch.dispose();
        }

        Shader fontShader = new Shader("assets/shaders/defaultFont.glsl");
        fontShader.compile();

        fontBatch = new FontBatch("assets/fonts/Consola.ttf", 16, fontShader);

        Matrix4f projection = new Matrix4f().ortho(0, Window.loadWidth(), 0, Window.loadHeight(), -1, 1);

        fontBatch.setProjection(projection);
    }

    public static FontBatch getFontBatch() {
        if (fontBatch == null) {
            initFontRenderer();
        }

        return fontBatch;
    }

    public static void dispose() {
        if (fontBatch != null) {
            fontBatch.dispose();
            fontBatch = null;
        }
    }
}
