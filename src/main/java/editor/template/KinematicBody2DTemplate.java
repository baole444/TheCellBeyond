package editor.template;

import editor.EditorWidget;
import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.type.ImBoolean;
import org.joml.Vector2f;
import physic2d.KinematicBody2D;
import physic2d.PhysicBody2D;

import java.util.UUID;

/**
 * Template for {@link KinematicBody2D}'s editor UI.
 */
final class KinematicBody2DTemplate implements IObjectTemplate<KinematicBody2D> {
    private static final KinematicBody2DTemplate instance = new KinematicBody2DTemplate();
    private KinematicBody2DTemplate() {}
    /**
     * Execute the rendering code for the Editor UI, related to this object.
     * This method is passive, and mst be call to render the UI.
     *
     * @param object the context object
     */
    @Override
    public void editorUI(KinematicBody2D object) {
        ImGui.spacing();
        UUID uuid = object.getUUID();
        boolean openKinematic = ImGui.collapsingHeader("KinematicBody2D##KinematicBody2D_Properties_Header_" + uuid, ImGuiTreeNodeFlags.DefaultOpen);
        if (!openKinematic) return;
        ImGui.indent();
        Vector2f vec = object.velocity();
        if (EditorWidget.dragVec2Ctrl("Velocity", vec, 0.0f, object)) object.velocity(vec);
        float angle = EditorWidget.dragFloatCtrl("Angular velocity", object.angularVelocity(), 0.0f, 1.0f, object);
        if (Float.compare(angle, object.angularVelocity()) != 0) object.angularVelocity(angle);
        ImGui.spacing();
        ImBoolean tmp = new ImBoolean(object.fixedRotation());
        if (ImGui.checkbox("Fixed rotation##KinematicBody2D_fixedRotation_CheckBox_" + uuid, tmp)) object.fixedRotation(tmp.get());
        tmp.set(object.bullet());
        if (ImGui.checkbox("Bullet##KinematicBody2D_bullet_CheckBox_" + uuid, tmp)) object.bullet(tmp.get());
        ImGui.unindent();
    }

    /**
     * Render the content of {@link #editorUI(KinematicBody2D)} and call {@link PhysicBody2DTemplate#render(PhysicBody2D)}.
     * @param kinematicBody2D the context object
     */
    static void render(KinematicBody2D kinematicBody2D) {
        if (kinematicBody2D == null) return;
        instance.editorUI(kinematicBody2D);
        PhysicBody2DTemplate.render(kinematicBody2D);
    }
}
