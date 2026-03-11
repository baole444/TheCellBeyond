package TheCellBeyond;

import render.RenderNode;
import render.Renderable;
import render.commands.RenderCommand;

public abstract class RenderableObject extends GameObject implements Renderable {
    private RenderNode renderNode = null;
    protected boolean renderDirty = false;

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

    @Override
    public RenderNode renderNode() {
        return renderNode;
    }
}
