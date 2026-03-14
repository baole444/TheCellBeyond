package TheCellBeyond;

import org.joml.Vector4f;
import render.RenderNode;
import render.Renderable;
import render.commands.RenderCommand;

public abstract class RenderableObject extends GameObject implements Renderable {
    protected boolean renderDirty = false;
    public boolean visible = true;
    public final Vector4f selfModulate = new Vector4f(1.0f);

    protected RenderableObject(String name) {
        super(name);
    }

    @Override
    public RenderCommand buildRenderCommand() {
        return null;
    }

    @Override
    public boolean renderDirty() {
        return renderDirty;
    }

    @Override
    public void renderDirty(boolean dirty) {
        renderDirty = dirty;
    }
}
