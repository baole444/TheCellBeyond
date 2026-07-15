package editor;

import TheCellBeyond.GameObject;
import TheCellBeyond.TileMap;
import components.AnimatedSpriteRenderer;
import components.AnimationPlayer;
import components.Component;
import components.Controller2D;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiChildFlags;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;
import imgui.flag.ImGuiWindowFlags;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The auxiliary inspector panel beneath the scene editor viewport.
 */
public final class BottomPanel {
    /**
     * The ID string of the bottom panel for layout control.
     */
    public static final String WindowID = "##Editor_Bottom_Panel";
    private static final String TableID = "##Bottom Panel Tab Buttons";
    private static final float TabButtonReserve = ImGui.getFrameHeightWithSpacing();
    private static final float SeparatorReserve = ImGui.getStyle().getItemSpacingY();
    private static final float Padding = 4.0f;
    private static float tabWidth;
    private static boolean widthCalculated = false;
    private static TabName selectedTab = TabName.Output;
    private static TabName workingTab = null;
    private static GameObject currentObject = null;

    private enum TabName {
        Output("Output"),
        ControllerBinding("Controller Binding"),
        TileSet("Tile Set"),
        TileMap("Tile Map"),
        SpriteFrame("Sprite Frame"),
        AnimationPlayer("Animation Player");

        final String name;

        TabName(String name) {
            this.name = name;
        }
    }

    private BottomPanel() {}

    /**
     * Clear and reset state of the auxiliary panels.
     */
    public static void clear() {
        selectedTab = TabName.Output;
        workingTab = null;
        currentObject = null;
        SpriteFrameEditor.clearDialogData();
        TileSetEditor.clearDialogData();
        TileMapEditor.clearDialogData();
    }

    /**
     * Pass a component into the auxiliary inspector if supported.
     * @param component the component to open with
     */
    public static void interacted(Component component) {
        if (component == null) return;
        switch (component) {
            case Controller2D controller2D -> {
                ControllerBindingEditor.edit(controller2D);
                workingTab = selectedTab = TabName.ControllerBinding;
            }
            case AnimatedSpriteRenderer spriteFrame -> {
                SpriteFrameEditor.edit(spriteFrame);
                workingTab = selectedTab = TabName.SpriteFrame;
            }
            case AnimationPlayer _ -> workingTab = selectedTab = TabName.AnimationPlayer;
            default -> clear();
        }
    }

    /**
     * Pass a game object into the auxiliary inspector if supported.
     * @param go the game object to open with
     */
    public static void interacted(GameObject go) {
        if (go == null) {
            clear();
            return;
        }
        if (go == currentObject) return;
        clear();
        currentObject = go;
        if (go instanceof TileMap tileMap) {
            TileSetEditor.edit(tileMap);
            TileMapEditor.edit(tileMap);
            workingTab = selectedTab = TabName.TileSet;
        }
    }

    /**
     * Render the panel on screen.
     */
    static void imgui() {
        if (currentObject != null && currentObject.isDestroyed()) clear();
        ImGui.setNextWindowSizeConstraints(
                new ImVec2(0, TabButtonReserve + SeparatorReserve + Padding),
                new ImVec2(Float.MAX_VALUE, Float.MAX_VALUE)
        );
        if (!ImGui.begin(WindowID)) {
            ImGui.end();
            return;
        }
        float contentReserve = ImGui.getContentRegionAvailY() - TabButtonReserve - SeparatorReserve;
        if (contentReserve <= TabButtonReserve) {
            ImGui.end();
            return;
        }
        if (!ImGui.beginChild("##Multipurpose_tab_region", 0.0f, contentReserve, false)) {
            ImGui.endChild();
            ImGui.end();
            return;
        }
        renderTabContent();
        ImGui.endChild();
        ImGui.separator();
        renderTabButtons();
        ImGui.end();
    }

    private static void renderTabContent() {
        switch (selectedTab) {
            case ControllerBinding -> ControllerBindingEditor.imgui();
            case TileSet -> TileSetEditor.imgui();
            case TileMap -> TileMapEditor.imgui();
            case SpriteFrame -> SpriteFrameEditor.imgui();
            case AnimationPlayer -> AnimationPlayerEditor.imgui();
            case null, default -> ConsoleOutput.imgui();
        }
    }

    private static void renderTabButtons() {
        if (!ImGui.beginChild("##Editor_Bottom_Panel_Tabs", 0.0f, 0.0f, ImGuiChildFlags.None, ImGuiWindowFlags.NoScrollbar)) {
            ImGui.endChild();
            return;
        }
        if (!widthCalculated) {
            tabWidth = getMaxTabNameWidth();
            widthCalculated = true;
        }
        List<TabName> workingTabs = getWorkingTabs();
        ImVec2 remainTableSize = ImGui.getContentRegionAvail();
        if (!ImGui.beginTable(TableID, workingTabs.size(), ImGuiTableFlags.BordersInnerV | ImGuiTableFlags.SizingStretchProp, remainTableSize)) {
            ImGui.endChild();
            return;
        }
        for (TabName tab : workingTabs) {
            String id = "##Bottom panel " + tab.name + " column";
            ImGui.tableSetupColumn(id, ImGuiTableColumnFlags.WidthFixed, tabWidth + Padding);
        }
        ImVec2 availSpace;
        ImVec2 cursorPos;
        for (TabName tab : workingTabs) {
            ImGui.tableNextColumn();
            String id = "##Bottom Panel " + tab.name + " tab selectable";
            boolean selected = selectedTab == tab;
            availSpace = ImGui.getContentRegionAvail();
            cursorPos = ImGui.getCursorPos();
            if (ImGui.selectable(id, selected, availSpace)) selectedTab = tab;
            float remainWidth = availSpace.x;
            float textWidth = ImGui.calcTextSizeX(tab.name);
            float offset = Math.max((remainWidth - textWidth) * 0.5f, 0.0f);
            ImGui.setCursorPos(cursorPos.x + offset, cursorPos.y);
            ImGui.text(tab.name);
        }
        ImGui.endTable();
        ImGui.endChild();
    }

    private static List<TabName> getWorkingTabs() {
        List<TabName> tabs = new ArrayList<>();
        tabs.add(TabName.Output);
        if (workingTab != null) {
            tabs.add(workingTab);
            if (workingTab == TabName.TileSet) tabs.add(TabName.TileMap);
        }
        return tabs;
    }

    private static float getMaxTabNameWidth() {
        return Arrays.stream(TabName.values())
                .map(tabName -> ImGui.calcTextSizeX(tabName.name))
                .max(Float::compare)
                .orElse(0.0f);
    }
}
