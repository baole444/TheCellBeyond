package editor.template;

import TheCellBeyond.GameObject2D;
import editor.EditorWidget;
import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.type.ImBoolean;
import physic2d.RayCast2D;

import java.util.UUID;

/**
 * Template for {@link RayCast2D}'s editor UI.
 */
final class RayCast2DTemplate implements ObjectTemplate<RayCast2D> {
    private static final RayCast2DTemplate instance = new RayCast2DTemplate();
    private RayCast2DTemplate() {}
    /**
     * Execute the rendering code for the Editor UI, related to this object.
     * This method is passive, and mst be call to render the UI.
     *
     * @param object the context object
     */
    @Override
    public void editorUI(RayCast2D object) {
        ImGui.spacing();
        UUID uuid = object.getUUID();
        boolean openRay = ImGui.collapsingHeader("RayCast2D##RayCast2D_Properties_Header_" + uuid, ImGuiTreeNodeFlags.DefaultOpen);
        if (!openRay) return;
        EditorWidget.dragVec2Ctrl("Target position", object.targetPosition, 0.0f, 0.15f, 0.01f, object);
        ImGui.spacing();
        ImBoolean tmp = new ImBoolean(object.collideWithAreas);
        if (ImGui.checkbox("Collide with Areas##RayCast2D_Collide_With_Areas_Checkbox_" + uuid, tmp)) object.collideWithAreas = tmp.get();
        tmp.set(object.collideWithBodies);
        if (ImGui.checkbox("Collide with Bodies##RayCast2D_Collide_With_Bodies_Checkbox_" + uuid, tmp)) object.collideWithBodies = tmp.get();
        ImGui.spacing();
        ImGui.indent();
        EditorWidget.physicLayerSelectable("Collision Mask", object.collisionMask, object);
        ImGui.unindent();
        ImGui.spacing();
        tmp.set(object.excludeParent);
        if (ImGui.checkbox("Exclude parent##RayCast2D_Exclude_Parent_Checkbox_" + uuid, tmp)) object.excludeParent = tmp.get();
        tmp.set(object.hitFromInside);
        if (ImGui.checkbox("Hit from inside##RayCast2D_Hit_From_Inside_Checkbox_" + uuid, tmp)) object.hitFromInside = tmp.get();
        ImGui.spacing();
        tmp.set(object.enabled);
        if (ImGui.checkbox("Enabled##RayCast2D_Enabled_Checkbox_" + uuid, tmp)) object.enabled = tmp.get();
    }

    /**
     * Render the content of {@link #editorUI(RayCast2D)} and call {@link GameObject2DTemplate#render(GameObject2D)}.
     * @param rayCast2D the context object
     */
    static void render(RayCast2D rayCast2D) {
        if (rayCast2D == null) return;
        instance.editorUI(rayCast2D);
        GameObject2DTemplate.render(rayCast2D);
    }
}
