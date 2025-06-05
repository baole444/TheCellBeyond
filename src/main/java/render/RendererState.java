package render;

import static org.lwjgl.opengl.GL11.*;

public class RendererState {
    private static RendererState instance;

    public enum RenderPass {
        NORMAL,
        SELECTION
    }

    private RenderPass currentPass = RenderPass.NORMAL;

    private Shader currentShader = null;

    private RendererState() {}

    public static RendererState get() {
        if (instance == null) {
            instance = new RendererState();
        }
        return instance;
    }

    public void setRenderPass(RenderPass pass) {
        this.currentPass = pass;

        if (pass == RenderPass.SELECTION) {
            glDisable(GL_BLEND);
        } else {
            glEnable(GL_BLEND);

            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    public RenderPass getCurrentPass() {
        return currentPass;
    }

    public void setShader(Shader shader) {
        if (currentShader != shader) {
            if (currentShader != null) {
                currentShader.detach();
            }

            currentShader = shader;
            if (currentShader != null) {
                currentShader.use();
            }
        }
    }

    public Shader getCurrentShader() {
        return currentShader;
    }

    public void enableTextRendering() {
        if (currentPass != RenderPass.SELECTION) {
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    public void enableSpriteRendering() {
        if (currentPass != RenderPass.SELECTION) {
            glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    public static void cleanup() {
        if (instance != null && instance.currentShader != null) {
            instance.currentShader.detach();
            instance.currentShader = null;
        }
    }
}
