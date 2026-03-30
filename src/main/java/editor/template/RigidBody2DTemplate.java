package editor.template;

import editor.EditorWidget;
import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.type.ImBoolean;
import org.joml.Vector2f;
import physic2d.PhysicBody2D;
import physic2d.RigidBody2D;

import java.util.UUID;

/**
 * Template for {@link RigidBody2D}'s editor UI.
 */
final class RigidBody2DTemplate implements ObjectTemplate<RigidBody2D> {
    private static final RigidBody2DTemplate instance = new RigidBody2DTemplate();
    private RigidBody2DTemplate() {}
    /**
     * Execute the rendering code for the Editor UI, related to this object.
     * This method is passive, and mst be call to render the UI.
     *
     * @param object the context object
     */
    @Override
    public void editorUI(RigidBody2D object) {
        ImGui.spacing();
        UUID uuid = object.getUUID();
        boolean openRigid = ImGui.collapsingHeader("RigidBody2D##RigidBody2D_Properties_Header", ImGuiTreeNodeFlags.DefaultOpen);
        if (!openRigid) return;
        ImGui.indent();
        Vector2f vTmp = object.initialVelocity();
        boolean vChanged = EditorWidget.dragVec2Ctrl("Velocity", vTmp, 0.0f, object);
        float angularV = EditorWidget.dragFloatCtrl("Angular Velocity", object.angularVelocity(), 0.0f, 1.0f,object);
        float ms = EditorWidget.dragFloatCtrl("Mass", object.mass(), 0.0f, object, 0.0f);
        float rollResist = EditorWidget.dragFloatCtrl("Roll Resistance", object.rollResistance(), 0.8f, object, 0.0f);
        float translateResist = EditorWidget.dragFloatCtrl("Translate Resistance", object.translateResistance(), 0.8f, object, 0.0f);
        float gravScale = EditorWidget.dragFloatCtrl("Gravity Scale", object.gravityScale(), 1.0f, object);
        ImBoolean fixedRot = new ImBoolean(object.fixedRotation());
        if (ImGui.checkbox("Fixed Rotation##RigidBody2D_fixedRotation_" + uuid, fixedRot)) object.fixedRotation(fixedRot.get());
        ImBoolean b = new ImBoolean(object.bullet());
        if (ImGui.checkbox("Bullet##RigidBody2D_bullet_" + uuid, b)) object.bullet(b.get());
        if (vChanged) object.initialVelocity(vTmp);
        if (Float.compare(angularV, object.angularVelocity()) != 0) object.angularVelocity(angularV);
        if (Float.compare(ms, object.mass()) != 0) object.mass(ms);
        if (Float.compare(rollResist, object.rollResistance()) != 0) object.rollResistance(rollResist);
        if (Float.compare(translateResist, object.translateResistance()) != 0) object.translateResistance(translateResist);
        if (Float.compare(gravScale, object.gravityScale()) != 0) object.gravityScale(gravScale);
        ImGui.unindent();
    }

    /**
     * Render the content of {@link #editorUI(RigidBody2D)} and call {@link PhysicBody2DTemplate#render(PhysicBody2D)}.
     * @param rigidBody2D the context object
     */
    static void render(RigidBody2D rigidBody2D) {
        if (rigidBody2D == null) return;
        instance.editorUI(rigidBody2D);
        PhysicBody2DTemplate.render(rigidBody2D);
    }
}
