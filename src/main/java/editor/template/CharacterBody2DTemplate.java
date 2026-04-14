package editor.template;

import editor.EditorWidget;
import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import org.joml.Vector2f;
import physic2d.CharacterBody2D;
import physic2d.PhysicBody2D;
import physic2d.enums.MotionMode;

import java.util.UUID;

/**
 * Template for {@link CharacterBody2D}'s editor UI.
 */
final class CharacterBody2DTemplate implements IObjectTemplate<CharacterBody2D> {
    private static final CharacterBody2DTemplate instance = new CharacterBody2DTemplate();
    private CharacterBody2DTemplate() {}
    /**
     * Execute the rendering code for the Editor UI, related to this object.
     * This method is passive, and mst be call to render the UI.
     *
     * @param object the context object
     */
    @Override
    public void editorUI(CharacterBody2D object) {
        ImGui.spacing();
        UUID uuid = object.getUUID();
        boolean openChar = ImGui.collapsingHeader("CharacterBody2D##CharacterBody2D_Properties_Header_" + uuid, ImGuiTreeNodeFlags.DefaultOpen);
        if (!openChar) {
            return;
        }
        ImGui.indent();
        ImGui.text("Motion Mode:");
        MotionMode currentMode = object.motionMode;
        if (ImGui.beginCombo("##Select_Character_Motion_Mode_Combo_" + uuid, currentMode.name())) {
            for (MotionMode mode : MotionMode.values()) {
                String display = mode.name();
                String label = display + "##Select_" + display + "_Selectable_" + uuid;
                if (ImGui.selectable(label, mode == currentMode)) object.motionMode = mode;
            }
            ImGui.endCombo();
        }
        ImGui.spacing();
        Vector2f upDir = object.upDirection();
        if (EditorWidget.dragVec2Ctrl("Up Direction", upDir, 0.0f, 1.0f, 0.1f, object)) object.upDirection(upDir);
        if (object.motionMode == MotionMode.Grounded) {
            ImGui.spacing();
            float slopeAngle = EditorWidget.dragFloatCtrl("Max Slope Angle", object.maxFloorAngle, 45.0f, 1.0f, object, 0.0f, 90.0f);
            if (Float.compare(slopeAngle, object.maxFloorAngle) != 0) object.maxFloorAngle = slopeAngle;
            float snapDistance = EditorWidget.dragFloatCtrl("Floor Snapping Distance", object.floorSnapDistance, 0.1f, 0.1f, object);
            if (Float.compare(snapDistance, object.floorSnapDistance) != 0) object.floorSnapDistance = snapDistance;
        }
        ImGui.spacing();
        float margin = EditorWidget.dragFloatCtrl("Safe Margin", object.safeMargin, 0.01f, object, 0.001f);
        if (Float.compare(margin, object.safeMargin) != 0) object.safeMargin = margin;
        ImGui.unindent();
        object.recoverFromPenetration = EditorWidget.checkboxCtrl("Recover From Penetration", object.recoverFromPenetration, object);
    }

    /**
     * Render the content of {@link #editorUI(CharacterBody2D)} and call {@link PhysicBody2DTemplate#render(PhysicBody2D)}.
     * @param characterBody2D the context object
     */
    static void render(CharacterBody2D characterBody2D) {
        if (characterBody2D == null) return;
        instance.editorUI(characterBody2D);
        PhysicBody2DTemplate.render(characterBody2D);
    }
}
