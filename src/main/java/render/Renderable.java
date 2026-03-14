package render;

import org.joml.Vector4f;
import render.commands.RenderCommand;

public interface Renderable {
    RenderCommand buildRenderCommand();
    boolean renderDirty();
    void renderDirty(boolean dirty);
    int renderZIndex();
}
