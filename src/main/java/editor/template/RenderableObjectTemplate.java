package editor.template;

import TheCellBeyond.RenderableObject;
import editor.EditorWidget;
import imgui.ImGui;
import imgui.type.ImBoolean;

/**
 * Template for {@link RenderableObject}'s editor UI.
 */
public class RenderableObjectTemplate implements ObjectTemplate<RenderableObject> {
    private static final RenderableObjectTemplate instance = new RenderableObjectTemplate();
    private RenderableObjectTemplate() {}
    /**
     * Execute the rendering code for the Editor UI, related to this object.
     * This method is passive, and mst be call to render the UI.
     *
     * @param object the context object
     */
    @Override
    public void editorUI(RenderableObject object) {
        ImGui.spacing();
        boolean openRender = ImGui.collapsingHeader("RenderableObject##RenderableObject_Render_Properties_Header_" + object.getUUID());
        if (!openRender) return;
        ImBoolean tmp = new ImBoolean(object.visible);
        if (ImGui.checkbox("Visible##RenderableObject_Visible_Checkbox_" + object.getUUID(), tmp)) object.visible = tmp.get();
        EditorWidget.colorCtrl("Self Modulate", object.selfModulate, object);
    }

    /**
     * Render the content of ({@link #editorUI(RenderableObject)}.
     * @param renderableObject the context object
     */
    static void render(RenderableObject renderableObject) {
        if (renderableObject == null) return;
        instance.editorUI(renderableObject);
    }
}
