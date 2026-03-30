package editor.template;

import TheCellBeyond.GameObject2D;
import editor.EditorWidget;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.type.ImBoolean;
import physic2d.CollisionObject2D;

import java.util.UUID;

/**
 * Template for {@link CollisionObject2D}'s editor UI.
 */
final class CollisionObject2DTemplate implements ObjectTemplate<CollisionObject2D> {
    private static final CollisionObject2DTemplate instance = new CollisionObject2DTemplate();
    private CollisionObject2DTemplate() {}
    /**
     * Execute the rendering code for the Editor UI, related to this object.
     * This method is passive, and mst be call to render the UI.
     *
     * @param object the context object
     */
    @Override
    public void editorUI(CollisionObject2D object) {
        ImGui.spacing();
        UUID uuid = object.getUUID();
        boolean openCollision = ImGui.collapsingHeader("CollisionObject2D##CollisionObject2D_Properties_Header_" + uuid);
        if (!openCollision) return;
        ImBoolean tmp = new ImBoolean(object.isSensor());
        if (ImGui.checkbox("Sensor Mode##CollisionObject2D_isSensor_Checkbox_" + uuid, tmp)) object.setSensor(tmp.get());
        tmp.set(object.isActive());
        if (ImGui.checkbox("Active##CollisionObject2D_isActive_Checkbox_" + uuid, tmp)) object.setActive(tmp.get());
        ImGui.indent();
        ImGui.pushStyleColor(ImGuiCol.Header, 0.0f, 0.0f, 0.0f, 0.0f);
        boolean open = ImGui.collapsingHeader("Physic Layers##CollisionObject2D_Physic_Layers_" + uuid);
        ImGui.popStyleColor(1);
        if (open) {
            ImGui.separator();
            int collisionLayer = EditorWidget.physicLayerSelectable("Collision Layer", object.getCollisionLayer(), object);
            ImGui.spacing();
            int collisionMask = EditorWidget.physicLayerSelectable("Collision Mask", object.getCollisionMask(), object);
            object.setCollisionLayer(collisionLayer);
            object.setCollisionMask(collisionMask);
            ImGui.separator();
            ImGui.spacing();
        }
        ImGui.unindent();

    }

    /**
     * Render the content of {@link #editorUI(CollisionObject2D)} and call {@link GameObject2DTemplate#render(GameObject2D)}.
     * @param collisionObject2D the content object
     */
    static void render(CollisionObject2D collisionObject2D) {
        if (collisionObject2D == null) return;
        instance.editorUI(collisionObject2D);
        GameObject2DTemplate.render(collisionObject2D);
    }
}
