package editor.template;

import TheCellBeyond.GameObject;
import components.RemoteTransform2D;
import editor.EditorColors;
import editor.EditorIcons;
import editor.EditorWidget;
import editor.payload.GameObjectDragDropPayload;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiChildFlags;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;
import imgui.type.ImBoolean;

/**
 * Template for {@link RemoteTransform2D}'s editor UI
 */
final class RemoteTransform2DTemplate implements ComponentTemplate<RemoteTransform2D> {
    private static final RemoteTransform2DTemplate instance = new RemoteTransform2DTemplate();
    private RemoteTransform2DTemplate() {}

    /**
     * Execute the rendering code for the Editor UI, related to this component.
     * This method is passive, and must be call to render the UI.
     *
     * @param component the context component
     */
    @Override
    public void editorUI(RemoteTransform2D component) {
        if (component == null) return;
        boolean accept = false;
        if (ImGui.beginChild("##RemoteTransform2D_DropTarget_Region_" + component.getUUID(), new ImVec2(0.0f, 0.0f), ImGuiChildFlags.AutoResizeY)) {
            accept = inputPath(component);
        }
        ImGui.endChild();
        if (!accept) return;
        acceptDragDrop(component);
        ImGui.spacing();
        ImBoolean useGlobal = new ImBoolean(component.useGlobalTransform);
        if (ImGui.checkbox("Use Global Transform##RemoteTransform2D_UseGlobalTransform_CheckBox_" + component.getUUID(), useGlobal)) component.useGlobalTransform = useGlobal.get();
        useGlobalTooTip(component);
        ImGui.spacing();
        ImBoolean updatePos = new ImBoolean(component.updatePosition);
        if (ImGui.checkbox("Update Position##RemoteTransform2D_UpdatePosition_Checkbox_" + component.getUUID(), updatePos)) component.updatePosition = updatePos.get();
        broadcastTooltips("updatePosition = %s", component.updatePosition, "If true, position will be broadcast to the targeted object.");
        ImGui.spacing();
        ImBoolean updateRot = new ImBoolean(component.updateRotation);
        if (ImGui.checkbox("Update Rotation##RemoteTransform2D_UpdateRotation_Checkbox_" + component.getUUID(), updateRot)) component.updateRotation = updateRot.get();
        broadcastTooltips("updateRotation = %s", component.updateRotation, "If true, rotation will be broadcast to the targeted object.");
        ImGui.spacing();
        ImBoolean updateSl = new ImBoolean(component.updateScale);
        if (ImGui.checkbox("Update Scale##RemoteTransform2D_UpdateScale_Checkbox_" + component.getUUID(), updateSl)) component.updateScale = updateSl.get();
        broadcastTooltips("updateScale = %s", component.updateScale, "If true, scale will be broadcast to the targeted object.");
    }

    private static void useGlobalTooTip(RemoteTransform2D component) {
        if (!ImGui.isItemHovered()) return;
        ImGui.beginTooltip();
        ImGui.text(String.format("useGlobalTransform = %s", component.useGlobalTransform));
        ImGui.separator();
        ImGui.spacing();
        ImGui.text("If true, the broadcasted transform will be applied to the targeted object's global transform.");
        ImGui.spacing();
        ImGui.pushStyleColor(ImGuiCol.Text, EditorColors.YellowHighLight);
        ImGui.text("Note: RemoteTransform2D always use its global transform as the broadcast source,");
        ImGui.text("      only the target are affected by this property");
        ImGui.popStyleColor(1);
        ImGui.endTooltip();
    }

    private static void broadcastTooltips(String format, boolean sendBroadcast, String text) {
        if (!ImGui.isItemHovered()) return;
        ImGui.beginTooltip();
        ImGui.text(String.format(format, sendBroadcast));
        ImGui.separator();
        ImGui.spacing();
        ImGui.text(text);
        ImGui.endTooltip();
    }

    private static boolean inputPath(RemoteTransform2D component) {
        if (!ImGui.beginTable("##RemoteTransform2D_TargetPath_Layout_" + component.getUUID(), 2, ImGuiTableFlags.BordersInnerV)) return false;
        ImGui.tableSetupColumn("##RemoteTransform2D_TargetPath_Input_Column_" + component.getUUID(), ImGuiTableColumnFlags.WidthStretch);
        ImGui.tableSetupColumn("##RemoteTransform2D_TargetPath_Clear_Input_Column_" + component.getUUID(), ImGuiTableColumnFlags.WidthFixed);
        ImGui.tableNextColumn();
        component.targetPath(EditorWidget.inputText("Target Path", component.targetPath(), component));
        pathToolTip(component);
        ImGui.tableNextColumn();
        if (EditorWidget.iconButton("##RemoteTransform2D_TargetPath_Input_Clear_Button_" + component.getUUID(), EditorIcons.Icons.Reset, "Click to reset the target path to default")) component.targetPath("");
        ImGui.endTable();
        return true;
    }

    private static void pathToolTip(RemoteTransform2D component) {
        if (!ImGui.isItemHovered()) return;
        ImGui.beginTooltip();
        ImGui.text(String.format("Path: %s", component.targetPath()));
        ImGui.separator();
        ImGui.spacing();
        if (component.validTarget()) {
            ImGui.spacing();
            ImGui.text("Target info:");
            ImGui.indent();
            ImGui.text(String.format("Name: %s", component.target().name()));
            ImGui.text("Type: " + component.target().getClass().getSimpleName());
            ImGui.text(String.format("UUID: %s", component.target().getUUID()));
            ImGui.unindent();
            ImGui.spacing();
        }
        ImGui.pushStyleColor(ImGuiCol.Text, EditorColors.InstructionHighLight);
        ImGui.text("Enter the hierarchy path to the target object, ");
        ImGui.text("or drag and drop an object from the Scene Tree here.");
        ImGui.popStyleColor(1);
        ImGui.endTooltip();
    }

    private static void acceptDragDrop(RemoteTransform2D component) {
        if (!ImGui.beginDragDropTarget()) return;
        Object payload = ImGui.acceptDragDropPayload(GameObjectDragDropPayload.getPayloadType());
        if (payload != null) {
            GameObject dropObj = GameObjectDragDropPayload.getPayload();
            if (dropObj != null) {
                component.targetPath(dropObj.asPath().originPath);
                GameObjectDragDropPayload.clearPayload();
            }
        }
        ImGui.endDragDropTarget();
    }

    /**
     * Render the content of {@link #editorUI(RemoteTransform2D)}.
     * @param remoteTransform2D the context component
     */
    static void render(RemoteTransform2D remoteTransform2D) {
        if (remoteTransform2D == null) return;
        instance.editorUI(remoteTransform2D);
    }
}
