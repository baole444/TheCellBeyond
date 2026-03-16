package components;

import render.Renderable;
import render.commands.RenderCommand;

public abstract class RenderableComponent extends Component implements Renderable {
    protected transient boolean renderDirty = true;

    protected RenderableComponent(String name) {
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
