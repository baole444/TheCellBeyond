package TheCellBeyond;

import org.joml.Vector2f;
import org.joml.Vector4f;
import render.RenderNode;
import render.Renderable;
import render.commands.RenderCommand;

public abstract class RenderableObject extends GameObject implements Renderable {
    protected transient boolean renderDirty = true;
    public boolean visible = true;
    public boolean behind = false;
    public boolean clip = false;
    public  int lightMask = 1;
    public final Vector4f selfModulate = new Vector4f(1.0f);

    public boolean repeatSource = false;
    public int repeatTime = 1;
    public final Vector2f repeatSize = new Vector2f();

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
