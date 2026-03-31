package editor.template;

import components.Component2D;
import editor.EditorWidget;
import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import physic2d.collider.CircleCollider2D;
import physic2d.collider.CollisionShape2D;

/**
 * Template for {@link CircleCollider2D}'s editor UI.
 */
final class CircleCollider2DTemplate implements IComponentTemplate<CircleCollider2D> {
    private static final CircleCollider2DTemplate instance = new CircleCollider2DTemplate();
    private CircleCollider2DTemplate() {}
    /**
     * Execute the rendering code for the Editor UI, related to this component.
     * This method is passive, and must be call to render the UI.
     *
     * @param component the context component
     */
    @Override
    public void editorUI(CircleCollider2D component) {
        ImGui.spacing();
        boolean openCircle = ImGui.collapsingHeader("CircleCollider2D##CircleCollider2D_Properties_Header_" + component.getUUID(), ImGuiTreeNodeFlags.DefaultOpen);
        if (!openCircle) return;
        float r = EditorWidget.dragFloatCtrl("Radius", component.radius(), 0.16f, component, CollisionShape2D.MinimumShapeDimension);
        if (Float.compare(r, component.radius()) != 0) component.radius(r);
    }

    /**
     * Render the content of {@link #editorUI(CircleCollider2D)} and call {@link Component2DTemplate#render(Component2D)}.
     * @param circleCollider2D the context component
     */
    static void render(CircleCollider2D circleCollider2D) {
        if (circleCollider2D == null) return;
        instance.editorUI(circleCollider2D);
        Component2DTemplate.render(circleCollider2D);
    }
}
