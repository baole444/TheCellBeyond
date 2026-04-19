package editor.widgets;

import editor.EditorColors;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;

/**
 * Collapsing header with separated callback for expand and interaction.
 */
public final class CollapsibleHeader {
    private CollapsibleHeader() {}

    /**
     * Create the collapsible header with callbacks. The label interact callback is prioritized over expansion callback in execution order.
     * @param id unique id of the header
     * @param label the display label
     * @param onExpand callback when header is expanded
     * @param onLabelInteract callback when label of the header is clicked
     */
    public static void create(String id, String label, Runnable onExpand, Runnable onLabelInteract) {
        create(id, label, "", onExpand, onLabelInteract, CollapsibleHeaderFlag.None);

    }

    /**
     * Create the collapsible header with callbacks. The label interact callback is prioritized over expansion callback in execution order.
     * @param id unique id of the header
     * @param label the display label
     * @param onExpand callback when header is expanded
     * @param onLabelInteract callback when label of the header is clicked
     * @param headerFlag flag for the header widget
     */
    public static void create(String id, String label, Runnable onExpand, Runnable onLabelInteract, CollapsibleHeaderFlag headerFlag) {
        create(id, label, "", onExpand, onLabelInteract, headerFlag);
    }

    /**
     * Create the collapsible header with callbacks. The label interact callback is prioritized over expansion callback in execution order.
     * @param id unique id of the header
     * @param label the display label
     * @param labelTooltip tooltip when hovering over the label
     * @param onExpand callback when header is expanded
     * @param onLabelInteract callback when label of the header is clicked
     * @param headerFlag flag for the header widget
     */
    public static void create(String id, String label, String labelTooltip, Runnable onExpand, Runnable onLabelInteract, CollapsibleHeaderFlag headerFlag) {
        boolean expanded = createInternalLayout(id, label, labelTooltip, onLabelInteract, headerFlag);
        if (expanded && onExpand != null) onExpand.run();
    }

    /**
     * Create the collapsible header with callbacks. The label interact callback is prioritized over expansion callback in execution order.
     * @param id unique id of the header
     * @param label the display label
     * @param onLabelInteract callback when label of the header is clicked
     * @return true if the header is expanded
     */
    public static boolean create(String id, String label, Runnable onLabelInteract) {
        return create(id, label, "", onLabelInteract, CollapsibleHeaderFlag.None);
    }

    /**
     * Create the collapsible header with callbacks. The label interact callback is prioritized over expansion callback in execution order.
     * @param id unique id of the header
     * @param label the display label
     * @param labelTooltip tooltip when hovering over the label
     * @param onLabelInteract callback when label of the header is clicked
     * @param headerFlag flag for the header widget
     * @return true if the header is expanded
     */
    public static boolean create(String id, String label, String labelTooltip, Runnable onLabelInteract, CollapsibleHeaderFlag headerFlag) {
        return createInternalLayout(id, label, labelTooltip, onLabelInteract, headerFlag);
    }

    private static boolean createInternalLayout(String id, String label, String labelTooltip, Runnable onLabelInteract, CollapsibleHeaderFlag headerFlag) {
        if (!ImGui.beginTable("##CollapsibleHeader_Layout_Table_" + id, 2, ImGuiTableFlags.BordersInnerV | ImGuiTableFlags.SizingFixedFit)) return false;
        ImGui.tableSetupColumn("##CollapsibleHeader_DropDown_Column", ImGuiTableColumnFlags.WidthFixed);
        ImGui.tableSetupColumn("##CollapsibleHeader_Label_Column", ImGuiTableColumnFlags.WidthStretch);
        ImGui.tableNextColumn();
        boolean expanded = createDropDown(id, headerFlag);
        ImGui.tableNextColumn();
        createLabel(id, label, labelTooltip, onLabelInteract);
        ImGui.endTable();
        return expanded;
    }

    private static boolean createDropDown(String id, CollapsibleHeaderFlag headerFlag) {
        ImGui.pushStyleColor(ImGuiCol.Header, 0.0f, 0.0f, 0.0f, 0.0f);
        boolean open = ImGui.collapsingHeader("##CollapsibleHeader_DropDown_Header_" + id, headerFlag.imGuiTreeNodeFlag);
        if (ImGui.isItemHovered()) {
            ImGui.beginTooltip();
            ImGui.pushStyleColor(ImGuiCol.Text, EditorColors.InstructionHighLight);
            ImGui.text("Click to expand or collapse the header.");
            ImGui.popStyleColor();
            ImGui.endTooltip();
        }
        ImGui.popStyleColor();
        return open;
    }

    private static void createLabel(String id, String label, String labelTooltip, Runnable onLabelInteract) {
        ImGui.pushStyleVar(ImGuiStyleVar.ButtonTextAlign, new ImVec2(0.0f, 0.5f));
        boolean interact = ImGui.button(label + (id.startsWith("##") ? id : "##" + id) + "_InteractButton", ImGui.getContentRegionAvailX(), 0.0f);
        ImGui.popStyleVar(1);
        if (labelTooltip != null && !labelTooltip.isBlank() && ImGui.isItemHovered()) {
            ImGui.beginTooltip();
            ImGui.text(labelTooltip);
            ImGui.endTooltip();
        }
        if (interact && onLabelInteract != null) onLabelInteract.run();
    }
}
