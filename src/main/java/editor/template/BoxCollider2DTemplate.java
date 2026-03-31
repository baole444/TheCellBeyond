package editor.template;

import components.Component2D;
import editor.EditorWidget;
import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import org.joml.Vector2f;
import physic2d.collider.BoxCollider2D;

/**
 * Template for {@link BoxCollider2D}'s editor UI.
 */
final class BoxCollider2DTemplate implements IComponentTemplate<BoxCollider2D> {
    private static final BoxCollider2DTemplate instance = new BoxCollider2DTemplate();
    private BoxCollider2DTemplate() {}
    /**
     * Execute the rendering code for the Editor UI, related to this component.
     * This method is passive, and must be call to render the UI.
     *
     * @param component the context component
     */
    @Override
    public void editorUI(BoxCollider2D component) {
        ImGui.spacing();
        boolean openBox = ImGui.collapsingHeader("BoxCollier2D##BoxCollider2D_Properties_Header_" + component.getUUID(), ImGuiTreeNodeFlags.DefaultOpen);
        if (!openBox) return;
        Vector2f half = new Vector2f(component.halfSize());
        if (EditorWidget.dragVec2Ctrl("Half Size", half, 0.16f, component)) component.halfSize(half);
    }

    /**
     * Render the content of {@link #editorUI(BoxCollider2D)} and call {@link Component2DTemplate#render(Component2D)}.
     * @param boxCollider2D the context component
     */
    static void render(BoxCollider2D boxCollider2D) {
        if (boxCollider2D == null) return;
        instance.editorUI(boxCollider2D);
        Component2DTemplate.render(boxCollider2D);
    }
}
