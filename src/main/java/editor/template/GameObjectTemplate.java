package editor.template;

import TheCellBeyond.GameObject;
import components.Component;
import components.NotSerializeComponent;
import editor.BottomPanel;
import editor.EditorIcons;
import editor.EditorWidget;
import editor.widgets.CollapsibleHeaderFlag;
import imgui.ImGui;
import imgui.flag.ImGuiChildFlags;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;
import imgui.flag.ImGuiTreeNodeFlags;

import java.util.UUID;

/**
 * Template for {@link GameObject}'s editor UI.
 */
final class GameObjectTemplate implements IObjectTemplate<GameObject> {
    private static final GameObjectTemplate instance = new GameObjectTemplate();
    private GameObjectTemplate() {}
    /**
     * Execute the rendering code for the Editor UI, related to this object.
     * This method is passive, and mst be call to render the UI.
     *
     * @param object the context object
     */
    @Override
    public void editorUI(GameObject object) {
        String newName = EditorWidget.inputText("Name", object.name(), object);
        if (!newName.equals(object.name())) object.name(newName);
        ScriptExportCache.render(object);
        GameObjectTemplateHierarchy.render(object);
        ImGui.spacing();
        boolean openComponent = ImGui.collapsingHeader("Components##GameObject_Components_Header_" + object.getUUID(), ImGuiTreeNodeFlags.DefaultOpen);
        if (!openComponent) {
            ImGui.spacing();
            return;
        }
        ImGui.indent();
        for (Component c: object.getComponents()) {
            if (c instanceof NotSerializeComponent) continue;
            UUID uuid = c.getUUID();
            if (!ImGui.beginTable("##GameObject_Component_Table_Header_Layout_" + uuid, 2, ImGuiTableFlags.SizingFixedFit)) continue;
            ImGui.tableSetupColumn("##GameObject_Component_Header_Column_" + uuid, ImGuiTableColumnFlags.WidthStretch);
            ImGui.tableSetupColumn("##GameObject_Component_Delete_Column_" + uuid, ImGuiTableColumnFlags.WidthFixed);
            ImGui.tableNextColumn();
            boolean open = EditorWidget.collapsibleHeader(uuid.toString(), c.name(), "Click to open auxiliary editor panel if supported",
                    () -> BottomPanel.interacted(c), CollapsibleHeaderFlag.DefaultOpen
            );
            ImGui.tableNextColumn();
            boolean clicked = EditorWidget.iconButton("Delete##GameObject_Delete_Component_Button_" + uuid, EditorIcons.Icons.Delete, "Remove this component");
            ImGui.endTable();
            if (clicked) {
                object.removeComponent(c);
                continue;
            }
            if (!open) continue;
            if (ImGui.beginChild("##GameObject_Component_Properties_Region_" + uuid, 0.0f, 0.0f, ImGuiChildFlags.AutoResizeY | ImGuiChildFlags.Borders)) EditorTemplate.render(c);
            ImGui.endChild();
            ImGui.spacing();
        }
        ImGui.unindent();
        ImGui.spacing();
    }

    /**
     * Render the content of {@link #editorUI(GameObject)}.
     * @param gameObject the context object
     */
    static void render(GameObject gameObject) {
        if (gameObject == null) return;
        instance.editorUI(gameObject);
    }
}
