package editor.template;

import editor.EditorWidget;
import imgui.ImGui;
import imgui.type.ImBoolean;
import physic2d.CollisionObject2D;
import physic2d.PhysicBody2D;

import java.util.UUID;

/**
 * Template for {@link PhysicBody2D}'s editor UI.
 */
final class PhysicBody2DTemplate implements ObjectTemplate<PhysicBody2D> {
    private static final PhysicBody2DTemplate instance = new PhysicBody2DTemplate();
    private PhysicBody2DTemplate() {}
    /**
     * Execute the rendering code for the Editor UI, related to this object.
     * This method is passive, and mst be call to render the UI.
     *
     * @param object the context object
     */
    @Override
    public void editorUI(PhysicBody2D object) {
        ImGui.spacing();
        UUID uuid = object.getUUID();
        boolean openPhysic = ImGui.collapsingHeader("PhysicBody2D##PhysicBody2D_Properties_Header_" + uuid);
        if (!openPhysic) return;
        float friction = EditorWidget.dragFloatCtrl("Friction", object.friction(), object);
        if (friction != object.friction()) object.friction(friction);
        ImBoolean sensor = new ImBoolean(object.isSensor());
        if (ImGui.checkbox("Sensor Mode##PhysicBody2D_isSensor_" + uuid, sensor)) object.setSensor(sensor.get());
    }

    /**
     * Render the content of {@link #editorUI(PhysicBody2D)} and call {@link CollisionObject2DTemplate#render(CollisionObject2D)}.
     * @param physicBody2D the context object
     */
    static void render(PhysicBody2D physicBody2D) {
        if (physicBody2D == null) return;
        instance.editorUI(physicBody2D);
        CollisionObject2DTemplate.render(physicBody2D);
    }
}
