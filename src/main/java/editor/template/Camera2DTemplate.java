package editor.template;

import TheCellBeyond.Camera2D;
import TheCellBeyond.GameObject2D;
import editor.EditorWidget;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.type.ImBoolean;
import org.joml.Vector2f;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Template for {@link Camera2D}'s editor UI.
 */
final class Camera2DTemplate implements ObjectTemplate<Camera2D> {
    private static final Camera2DTemplate instance = new Camera2DTemplate();
    private Camera2DTemplate() {}

    /**
     * Execute the rendering code for the Editor UI, related to this object.
     * This method is passive, and mst be call to render the UI.
     *
     * @param object the context object
     */
    @Override
    public void editorUI(Camera2D object) {
        ImGui.spacing();
        UUID uuid = object.getUUID();
        boolean openCamera = ImGui.collapsingHeader("Camera2D##Camera2D_Properties_Header_" + uuid, ImGuiTreeNodeFlags.DefaultOpen);
        if (!openCamera) return;
        ImBoolean enableState = new ImBoolean(object.enabled());
        if (ImGui.checkbox("Enabled##Camera2D_Enabled_CheckBox_" + uuid, enableState)) object.enabled(enableState.get());
        renderAnchorModeCombo(object, uuid);
        renderUpdateProcessCombo(object, uuid);
        ImGui.spacing();
        Vector2f zoomLevel = new Vector2f(object.zoom);
        if (EditorWidget.dragVec2Ctrl("Zoom", zoomLevel, 1.0f, 0.01f, object)) object.zoom.set(zoomLevel);
        renderDragProperties(object, uuid);
        renderLimitProperties(object, uuid);
        ImBoolean positionSmoothingState = new ImBoolean(object.enablePositionSmoothing);
        if (ImGui.checkbox("Enable Position Smoothing##Camera2D_EnablePositionSmoothing_CheckBox_" + uuid, positionSmoothingState)) object.enablePositionSmoothing = positionSmoothingState.get();
        object.positionSmoothingSpeed = Math.max(0.1f, EditorWidget.dragFloatCtrl("Position Smoothing Speed", object.positionSmoothingSpeed, 4.0f, 0.1f, object, 0.1f));
        ImBoolean ignoreRotationState = new ImBoolean(object.ignoreRotation);
        if (ImGui.checkbox("Ignore Rotation##Camera2D_IgnoreRotation_CheckBox_" + uuid, ignoreRotationState)) object.ignoreRotation = ignoreRotationState.get();
        ImBoolean rotationSmoothingState = new ImBoolean(object.enableRotationSmoothing);
        if (ImGui.checkbox("Enable Rotation Smoothing##Camera2D_EnableRotationSmoothing_CheckBox_" + uuid, rotationSmoothingState)) object.enableRotationSmoothing = rotationSmoothingState.get();
        object.rotationSmoothingSpeed = Math.max(0.1f, EditorWidget.dragFloatCtrl("Rotation Smoothing Speed", object.rotationSmoothingSpeed, 4.0f, object, 0.1f));
    }

    private static void renderUpdateProcessCombo(Camera2D object, UUID uuid) {
        ImGui.spacing();
        ImGui.text("Camera Update Process:");
        String hint = object.updateProcess == null ? "Select an update process..." : object.updateProcess.toString();
        if (!ImGui.beginCombo("##Camera2D_Select_Camera_Update_Process_" + uuid, hint)) return;
        List<Camera2D.UpdateProcess> updateProcesses = Arrays.stream(Camera2D.UpdateProcess.values()).toList();
        for (Camera2D.UpdateProcess process : updateProcesses) {
            String name = process.toString();
            String label = name + "##Select_" + name + "_Camera2D_UpdateProcess_Selectable_" + uuid;
            if (ImGui.selectable(label, Objects.equals(process, object.updateProcess))) object.updateProcess = process;
        }
        ImGui.endCombo();
    }

    private static void renderAnchorModeCombo(Camera2D object, UUID uuid) {
        ImGui.spacing();
        ImGui.text("Camera Anchor Mode:");
        String hint = object.anchorMode == null ? "Select an anchor mode..." : object.anchorMode.toString();
        if (!ImGui.beginCombo("##Camera2D_Select_Camera_Anchor_Mode_" + uuid, hint)) return;
        List<Camera2D.AnchorMode> anchorModes = Arrays.stream(Camera2D.AnchorMode.values()).toList();
        for (Camera2D.AnchorMode mode : anchorModes) {
            String name = object.anchorMode.toString();
            String label = name + "##Select_" + name + "_Camera2D__AnchorMode_Selectable_" + uuid;
            if (ImGui.selectable(label, Objects.equals(mode, object.anchorMode))) object.anchorMode = mode;
        }
        ImGui.endCombo();
    }

    private static void renderDragProperties(Camera2D object, UUID uuid) {
        String compositeDragID = "Camera Drag##Camera2D_Drag_Properties_Header_" + uuid;
        ImGui.pushStyleColor(ImGuiCol.Header, 0.0f, 0.0f, 0.0f, 0.0f);
        boolean openDrag = ImGui.collapsingHeader(compositeDragID, ImGuiTreeNodeFlags.DefaultOpen);
        ImGui.popStyleColor(1);
        if (!openDrag) return;
        ImBoolean dragHState = new ImBoolean(object.enableHorizontalDrag);
        ImBoolean dragVState = new ImBoolean(object.enableVerticalDrag);
        if (ImGui.checkbox("Enable Horizontal Drag##Camera2D_EnableHorizontalDrag_CheckBox_" + uuid, dragHState)) object.enableHorizontalDrag = dragHState.get();
        object.leftDragMargin = EditorWidget.dragFloatCtrl("Left Drag Margin", object.leftDragMargin, 0.2f, 0.01f, object);
        object.rightDragMargin = EditorWidget.dragFloatCtrl("Right Drag Margin", object.rightDragMargin, 0.2f, 0.01f, object);
        if (ImGui.checkbox("Enable Vertical Drag##Camera2D_Enable_Vertical_Drag_CheckBox_" + uuid, dragVState)) object.enableVerticalDrag = dragVState.get();
        object.topDragMargin = EditorWidget.dragFloatCtrl("Top Drag Margin", object.topDragMargin, 0.2f, 0.01f, object);
        object.bottomDragMargin = EditorWidget.dragFloatCtrl("Bottom Drag Margin", object.bottomDragMargin, 0.2f, 0.01f, object);
        object.horizontalDragOffset = Math.clamp(EditorWidget.dragFloatCtrl("Horizontal Drag Offset", object.horizontalDragOffset, 0.0f, 0.01f, object, -1.0f, 1.0f), -1.0f, 1.0f);
        object.verticalDragOffset = Math.clamp(EditorWidget.dragFloatCtrl("Vertical Drag Offset", object.verticalDragOffset, 0.0f, 0.01f, object, -1.0f, 1.0f), -1.0f, 1.0f);
        ImGui.spacing();
    }

    private static void renderLimitProperties(Camera2D object, UUID uuid) {
        String compositeLimitID = "Camera Limit##Camera2D_Limit_Properties_Header_" + uuid;
        ImGui.pushStyleColor(ImGuiCol.Header, 0.0f, 0.0f, 0.0f, 0.0f);
        boolean openLimit = ImGui.collapsingHeader(compositeLimitID, ImGuiTreeNodeFlags.DefaultOpen);
        ImGui.popStyleColor(1);
        if (!openLimit) return;
        ImBoolean limitState = new ImBoolean(object.enableLimit);
        ImBoolean limitSmoothingState = new ImBoolean(object.enableLimitSmoothing);
        if (ImGui.checkbox("Enable Limit##Camera2D_EnableLimit_CheckBox_" + uuid, limitState)) object.enableLimit = limitState.get();
        object.leftLimit = EditorWidget.dragFloatCtrl("Left Limit", object.leftLimit, -1024.0f, 1.0f, object);
        object.rightLimit = EditorWidget.dragFloatCtrl("Right Limit", object.rightLimit, 1024.0f, 1.0f, object);
        object.topLimit = EditorWidget.dragFloatCtrl("Top Limit", object.topLimit, 1024.0f, 1.0f, object);
        object.bottomLimit = EditorWidget.dragFloatCtrl("Bottom Limit", object.bottomLimit, -1024.0f, 1.0f, object);
        if (ImGui.checkbox("Enable Limit Smoothing##Camera2D_EnableLimitSmoothing_CheckBox_" + uuid, limitSmoothingState)) object.enableLimitSmoothing = limitSmoothingState.get();
        ImGui.spacing();
    }

    /**
     * Render the content of {@link #editorUI(Camera2D)} and call {@link GameObject2DTemplate#render(GameObject2D)}.
     * @param camera2D the context object
     */
    static void render(Camera2D camera2D) {
        if (camera2D == null) return;
        instance.editorUI(camera2D);
        GameObject2DTemplate.render(camera2D);
    }
}
