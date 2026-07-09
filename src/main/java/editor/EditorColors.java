package editor;

import imgui.ImGui;
import imgui.flag.ImGuiCol;

/**
 * Common colours used by editor UI.
 */
public final class EditorColors {
    public static final int DebugLogColor = ImGui.colorConvertFloat4ToU32(0.36f, 0.36f, 0.36f, 1.0f);
    public static final int InfoLogColor = ImGui.colorConvertFloat4ToU32(0.86f, 0.86f, 0.86f, 1.0f);
    public static final int WarningLogColor = ImGui.colorConvertFloat4ToU32(0.86f, 0.86f, 0.28f, 1.0f);
    public static final int ErrorLogColor = ImGui.colorConvertFloat4ToU32(0.86f, 0.28f, 0.28f, 1.0f);

    private EditorColors() {}
    /**
     * Common red button style.
     */
    public static final class RedButton {
        private RedButton() {}
        /**
         * Base colour of the button's background.
         */
        public static final int Base = ImGui.colorConvertFloat4ToU32(0.7f, 0.2f, 0.2f, 1.0f);
        /**
         * Colour of the button's background when it is hovered.
         */
        public static final int Hovered = ImGui.colorConvertFloat4ToU32(0.8f, 0.3f, 0.3f, 1.0f);
        /**
         * Colour of the button's background when it is activated.
         */
        public static final int Active = ImGui.colorConvertFloat4ToU32(0.7f, 0.1f, 0.1f, 1.0f);

        /**
         * Apply red button style to upcoming buttons, must call {@link #popStyle()} to end style.
         */
        public static void pushStyle() {
            ImGui.pushStyleColor(ImGuiCol.Button, Base);
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, Hovered);
            ImGui.pushStyleColor(ImGuiCol.ButtonActive, Active);
        }

        /**
         * Stop applying the red button style, if {@link #pushStyle()} was never call,
         * this could cause incorrect style removal.
         */
        public static void popStyle() {
            ImGui.popStyleColor(3);
        }

        /**
         * Execute logic in between push and pop style.
         * @param logic the code to run in between
         */
        public static void create(Runnable logic) {
            pushStyle();
            if (logic != null) logic.run();
            popStyle();
        }
    }

    /**
     * Common green button style.
     */
    public static final class GreenButton {
        private GreenButton() {}
        /**
         * Base colour of the button's background.
         */
        public static final int Base = ImGui.colorConvertFloat4ToU32(0.2f, 0.7f, 0.2f, 1.0f);
        /**
         * Colour of the button's background when it is hovered.
         */
        public static final int Hovered = ImGui.colorConvertFloat4ToU32(0.3f, 0.8f, 0.3f, 1.0f);
        /**
         * Colour of the button's background when it is activated.
         */
        public static final int Active = ImGui.colorConvertFloat4ToU32(0.1f, 0.7f, 0.1f, 1.0f);

        /**
         * Apply green button style to upcoming buttons, must call {@link #popStyle()} to end style.
         */
        public static void pushStyle() {
            ImGui.pushStyleColor(ImGuiCol.Button, Base);
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, Hovered);
            ImGui.pushStyleColor(ImGuiCol.ButtonActive, Active);
        }

        /**
         * Stop applying the green button style, if {@link #pushStyle()} was never call,
         * this could cause incorrect style removal.
         */
        public static void popStyle() {
            ImGui.popStyleColor(3);
        }

        /**
         * Execute logic in between push and pop style.
         * @param logic the code to run in between
         */
        public static void create(Runnable logic) {
            pushStyle();
            if (logic != null) logic.run();
            popStyle();
        }
    }

    /**
     * Transparent colour with all 4 components set to 0.0f.
     */
    public static final int TransparentColor = ImGui.colorConvertFloat4ToU32(0.0f, 0.0f, 0.0f, 0.0f);
    /**
     * Default colour for window, menu bar and popup background.
     */
    public static final int PanelBgColor = ImGui.colorConvertFloat4ToU32(0.1f, 0.1f, 0.1f, 1.0f);
    /**
     * Yellow highlight colour.
     */
    public static final int YellowHighLight = ImGui.colorConvertFloat4ToU32(0.9f, 0.9f, 0.3f, 1.0f);
    /**
     * Light pale green colour for instruction text.
     */
    public static final int InstructionHighLight = ImGui.colorConvertFloat4ToU32(0.3f, 0.9f, 0.6f, 1.0f);
}
