package TheCellBeyond;

import editor.EditorWidget;
import imgui.ImGui;
import imgui.type.ImBoolean;
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

    /**
     * Control how many times the texture repeats. Each texture copy spreads evenly from the original
     * by {@link #repeatSize}. This can be used to fill up spaces when the camera is zoomed out.
     */
    public int repeatTime = 1;

    /**
     * The offset for texture, in world units. The textures of this object's components and children are repeated,
     * and offset by this value.
     * <p>
     * When scrolling, the position of objects and components loops, create the illusion of an infinite scrolling background.
     * This will only work properly if the size are larger than the screen size.
     * </p>
     * If an axis is set to {@code 0.0}, the textures will not be repeated on that axis.
     */
    public final Vector2f repeatSize = new Vector2f();

    protected RenderableObject(String name) {
        super(name);
    }

    @Override
    public void  additionalImGuiLogic() {
        ImGui.spacing();
        boolean openRender = ImGui.collapsingHeader("RenderableObject##RenderableObject_Render_Properties_Header_" + getUUID());
        if (!openRender) return;
        ImBoolean tmp = new ImBoolean(visible);
        if (ImGui.checkbox("Visible##RenderableObject_Visible_Checkbox_" + getUUID(), tmp)) visible = tmp.get();
        EditorWidget.colorCtrl("Self Modulate", selfModulate, this);
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
