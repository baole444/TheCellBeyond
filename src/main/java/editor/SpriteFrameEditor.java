package editor;

import components.AnimatedSpriteRenderer;
import imgui.ImGui;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;

public class SpriteFrameEditor {
    public static AnimatedSpriteRenderer selectedAnimatedSpriteRenderer;
    private static final float animationListXPercentage = 0.25f;
    private static final float CONTROL_RESERVE = ImGui.getFrameHeightWithSpacing();
    private static final float SEPARATOR_RESERVE = ImGui.getStyle().getItemSpacingY();
    private static final float padding = 4.0f;

    static void imgui() {
        ImGui.beginChild("##Sprite frame controls", 0, CONTROL_RESERVE + padding, false);
        ImGui.text("Controls go here");
        ImGui.endChild();

        if (!ImGui.beginTable("##SFE_Table_Id", 2, ImGuiTableFlags.NoBordersInBody | ImGuiTableFlags.SizingStretchProp, ImGui.getContentRegionAvail())) return;

        float remainWidth = Math.max(120.0f, ImGui.getContentRegionAvailX() * animationListXPercentage);
        ImGui.tableSetupColumn("##AnimationList_Column", ImGuiTableColumnFlags.WidthFixed, remainWidth);
        ImGui.tableSetupColumn("##AnimationFrames_Column", ImGuiTableColumnFlags.WidthStretch);

        ImGui.tableNextColumn();
        ImGui.beginChild("##AnimationList", ImGui.getContentRegionAvail(), true);
        ImGui.textWrapped("This hold list of animation");
        ImGui.endChild();
        ImGui.tableNextColumn();
        ImGui.textWrapped("Sprite frames go here");

        ImGui.endTable();
    }
}
