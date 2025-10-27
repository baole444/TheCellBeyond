package editor;

import components.AnimatedSpriteRenderer;
import components.AnimationPlayer;
import components.Component;
import components.TileMap;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiChildFlags;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;
import imgui.flag.ImGuiWindowFlags;

import java.util.Arrays;

public class BottomPanel {
    public static final String WINDOW_ID = "###Editor_Bottom_Panel";
    private static final String TABLE_ID = "Tab Buttons";
    private static final float TAB_BUTTON_RESERVE = ImGui.getFrameHeightWithSpacing();
    private static final float SEPARATOR_RESERVE = ImGui.getStyle().getItemSpacingY();
    private static final float padding = 4.0f;
    private static float tabWidth;
    private static boolean widthCalculated = false;
    private static TabName selectedTab = TabName.Output;

    private enum TabName {
        Output("Output"),
        TileSet("Tile Set"),
        SpriteFrame("Sprite Frame"),
        AnimationPlayer("Animation Player");

        final String name;

        TabName(String name) {
            this.name = name;
        }

        static int size() {
            return values().length;
        }
    }

    public static void interacted(Component component) {
        if (component == null) return;
        switch (component) {
            case AnimatedSpriteRenderer spriteFrame -> {
                SpriteFrameEditor.edit(spriteFrame);
                selectedTab = TabName.SpriteFrame;
            }
            case TileMap tileSet -> {
                TileSetEditor.edit(tileSet);
                selectedTab = TabName.TileSet;
            }
            case AnimationPlayer animationPlayer -> {
                selectedTab = TabName.AnimationPlayer;
            }

            default -> {}
        }
    }

    public static void imgui() {
        ImGui.setNextWindowSizeConstraints(
                new ImVec2(0, TAB_BUTTON_RESERVE + SEPARATOR_RESERVE + padding),
                new ImVec2(Float.MAX_VALUE, Float.MAX_VALUE)
        );

        if (!ImGui.begin(WINDOW_ID)) {
            ImGui.end();
            return;
        }

        float contentReserve = ImGui.getContentRegionAvailY() - TAB_BUTTON_RESERVE - SEPARATOR_RESERVE;
        if (contentReserve > TAB_BUTTON_RESERVE) {
            if (!ImGui.beginChild("##Multipurpose_tab_region", 0.0f, contentReserve, false)) {
                ImGui.end();
                return;
            }
            renderTabContent();
            ImGui.endChild();
            ImGui.separator();
        }

        renderTabButtons();

        ImGui.end();
    }

    private static void renderTabContent() {
        switch (selectedTab) {
            case TileSet -> TileSetEditor.imgui();
            case SpriteFrame -> SpriteFrameEditor.imgui();
            case AnimationPlayer -> AnimationPlayerEditor.imgui();
            case null, default -> ConsoleOutput.imgui();
        }
    }

    private static void renderTabButtons() {
        if (!ImGui.beginChild("##Editor_Bottom_Panel_Tabs", 0.0f, 0.0f, ImGuiChildFlags.None, ImGuiWindowFlags.NoScrollbar)) {
            return;
        }
        if (!widthCalculated) {
            tabWidth = getMaxTabNameWidth();
            widthCalculated = true;
        }

        ImVec2 remainTableSize = ImGui.getContentRegionAvail();
        if (!ImGui.beginTable(TABLE_ID, TabName.size(), ImGuiTableFlags.BordersInnerV | ImGuiTableFlags.SizingStretchProp, remainTableSize)) {
            ImGui.endChild();
            return;
        }

        for (TabName tab : TabName.values()) {
            String id = "##" + tab.name + " column";
            ImGui.tableSetupColumn(id, ImGuiTableColumnFlags.WidthFixed, tabWidth + padding);
        }

        ImVec2 availSpace;
        ImVec2 cursorPos;
        for (TabName tab : TabName.values()) {
            ImGui.tableNextColumn();
            String id = "##" + tab.name + " tab";
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

    private static float getMaxTabNameWidth() {
        return Arrays.stream(TabName.values())
                .map(tabName -> ImGui.calcTextSizeX(tabName.name))
                .max(Float::compare)
                .orElse(0.0f);
    }
}
