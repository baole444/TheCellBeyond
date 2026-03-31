package editor.template;

import components.Component2D;
import editor.EditorWidget;
import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import physic2d.collider.CapsuleCollider2D;
import physic2d.collider.CollisionShape2D;

/**
 * Template for {@link CapsuleCollider2D}'s editor UI.
 */
final class CapsuleCollider2DTemplate implements IComponentTemplate<CapsuleCollider2D> {
    private static final CapsuleCollider2DTemplate instance = new CapsuleCollider2DTemplate();
    private CapsuleCollider2DTemplate() {}
    /**
     * Execute the rendering code for the Editor UI, related to this component.
     * This method is passive, and must be call to render the UI.
     *
     * @param component the context component
     */
    @Override
    public void editorUI(CapsuleCollider2D component) {
        ImGui.spacing();
        boolean openCapsule = ImGui.collapsingHeader("CapsuleCollier2D##CapsuleCollider2D_Properties_Header_" + component.getUUID(), ImGuiTreeNodeFlags.DefaultOpen);
        if (!openCapsule) return;
        float w = EditorWidget.dragFloatCtrl("Width", component.width(), 0.32f, component, CollisionShape2D.MinimumShapeDimension);
        float h = EditorWidget.dragFloatCtrl("Height", component.height(), 0.64f, component, CollisionShape2D.MinimumShapeDimension);
        if (Float.compare(w, component.width()) != 0) component.width(w);
        if (Float.compare(h, component.height()) != 0) component.height(h);
    }

    /**
     * Render the content of {@link #editorUI(CapsuleCollider2D)} and call {@link Component2DTemplate#render(Component2D)}.
     * @param capsuleCollider2D the context component
     */
    static void render(CapsuleCollider2D capsuleCollider2D) {
        if (capsuleCollider2D == null) return;
        instance.editorUI(capsuleCollider2D);
        Component2DTemplate.render(capsuleCollider2D);
    }
}
