package editor.widgets;

import imgui.flag.ImGuiTreeNodeFlags;

/**
 * Flag for {@link editor.EditorWidget#collapsibleHeader}.
 */
public enum CollapsibleHeaderFlag {
    /**
     * No flag.
     */
    None(ImGuiTreeNodeFlags.None),

    /**
     * Default header to be open.
     */
    DefaultOpen(ImGuiTreeNodeFlags.DefaultOpen);

    final int imGuiTreeNodeFlag;

    CollapsibleHeaderFlag(int imGuiTreeNodeFlag) {
        this.imGuiTreeNodeFlag = imGuiTreeNodeFlag;
    }
}
