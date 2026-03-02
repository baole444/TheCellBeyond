package editor.template;

import TheCellBeyond.Transform2D;
import editor.EditorWidget;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiChildFlags;
import imgui.flag.ImGuiCol;
import imgui.type.ImBoolean;
import utility.WorldUnit;

import java.util.UUID;

/**
 * Template for {@link Transform2D}'s editor UI.
 */
class Transform2DTemplate implements ComponentTemplate<Transform2D> {
    private static final Transform2DTemplate instance = new Transform2DTemplate();
    private Transform2DTemplate() {}

    /**
     * Execute the rendering code for the Editor UI, related to this component.
     * This method is passive, and must be call to render the UI.
     *
     * @param component the context component
     */
    @Override
    public void editorUI(Transform2D component) {
        if (component == null) return;
        UUID uuid = component.getUUID();
        ImGui.pushStyleColor(ImGuiCol.Header, 0.0f, 0.0f, 0.0f, 0.0f);
        boolean open = ImGui.collapsingHeader("Transform2D##Transform2D_Header_" + uuid);
        ImGui.popStyleColor(1);
        if (!open) return;
        if (ImGui.beginChild("##Transform2D_Properties_Region_" + uuid, new ImVec2(0.0f, 0.0f), ImGuiChildFlags.AutoResizeY | ImGuiChildFlags.Border)) {
            EditorWidget.dragVec2Ctrl("Position", component.position, 0.0f, WorldUnit.WorldUnitsPerPixel, component);
            EditorWidget.dragVec2Ctrl("Scale", component.scale, 1.0f, component);
            component.rotation = EditorWidget.dragFloatCtrl("Rotation", component.rotation, component);
            component.zIndex = EditorWidget.dragIntCtrl("Z-Index", component.zIndex, component);
            ImBoolean rZIndex = new ImBoolean(component.relativeZIndex);
            if (ImGui.checkbox("Z-Index as Relative##Relative_Transform_ZIndex_" + uuid, rZIndex)) component.relativeZIndex = rZIndex.get();
            relativeZIndexTooltip(component);
        }
        ImGui.endChild();
    }

    private static void relativeZIndexTooltip(Transform2D component) {
        if (!ImGui.isItemHovered()) return;
        ImGui.beginTooltip();
        ImGui.text(String.format("relativeZIndex = %s", component.relativeZIndex));
        ImGui.spacing();
        ImGui.separator();
        ImGui.text("If true, the final z-Index of this transform is relative to the parent/owning object.");
        ImGui.text("For example, if this transform's z-Index is 2 and final z-Index of the parent/owning object is 3, ");
        ImGui.text("this transform's effective z-Index is 2 + 3 = 5");
        ImGui.endTooltip();
    }

    /**
     * Render the content of {@link #editorUI(Transform2D)}
     * @param transform2D the content component
     */
    static void render(Transform2D transform2D) {
        instance.editorUI(transform2D);
    }
}
