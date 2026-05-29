package editor;

import TheCellBeyond.KeyListener;
import editor.widgets.CollapsibleHeader;
import editor.widgets.CollapsibleHeaderFlag;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.*;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import org.joml.Vector2f;
import org.joml.Vector4f;
import physic2d.Physic2D;
import physic2d.PhysicLayer;
import project.Project;
import render.texture.Sprite;
import utility.TextureScale;

import java.util.HashMap;

/**
 * EditorWidget contains a collection of static methods for editing various data type in the editor.
 * It also provides methods for creating icon button, selectable and collapsible headers.
 */
public final class EditorWidget {
    private record ShortenLabelKey(String text, float width) {}
    private static final HashMap<ShortenLabelKey, String> shortenLabels = new HashMap<>();
    private static final float defaultWidth = 80.0f;
    private static final ImBoolean checkboxTmp = new ImBoolean();
    private static final Vector2f dragVec2Tmp = new Vector2f();
    private EditorWidget() {}

    /**
     * Render a label text with centre aligned text.
     * @param label the display label
     */
    public static void textCenterAlign(String label) {
        if (label == null) return;
        float remainWidth = ImGui.getContentRegionAvailX();
        float textWidth = ImGui.calcTextSizeX(label);
        float offset = Math.max((remainWidth - textWidth) * 0.5f, 0.0f);
        ImGui.setCursorPosX(ImGui.getCursorPosX() + offset);
        ImGui.text(label);
    }

    public static boolean dragVec2Ctrl(String label, Vector2f source, float resetVal, Object caller) {
        return dragVec2Ctrl(label, source, resetVal, resetVal, 0.1f, caller, 0.0f, 0.0f);
    }

    public static boolean dragVec2Ctrl(String label, Vector2f source, float resetVal, float dragSpeed, Object caller) {
        return dragVec2Ctrl(label, source, resetVal, resetVal, dragSpeed, caller, 0.0f, 0.0f);
    }

    public static boolean dragVec2Ctrl(String label, Vector2f source, float resetX, float resetY, float dragSpeed, Object caller) {
        return dragVec2Ctrl(label, source, resetX, resetY, dragSpeed, caller, 0.0f, 0.0f);
    }

    public static boolean dragVec2Ctrl(String label, Vector2f source, float resetX, float resetY, float dragSpeed, Object caller, float minVal, float maxVal) {
        if (source == null || caller == null) return false;
        String id = createID(label, caller);
        dragVec2Tmp.set(source);
        boolean useLabel = label != null && !label.isBlank();
        ImGui.pushID(id);
        if (useLabel) {
            if (!ImGui.beginTable("##Table_" + id, 2, ImGuiTableFlags.BordersInnerV | ImGuiTableFlags.SizingStretchProp, ImGui.getContentRegionAvailX())) {
                ImGui.popID();
                return false;
            }
            ImGui.tableSetupColumn("##label_" + label + id, ImGuiTableColumnFlags.WidthFixed, defaultWidth);
            ImGui.tableSetupColumn("##content_" + label + id, ImGuiTableColumnFlags.WidthStretch);
            ImGui.tableNextColumn();
            float labelSpace = ImGui.getContentRegionAvailX();
            ImGui.text(shortenLabel(label, labelSpace));
            if (ImGui.isItemHovered() && labelSpace <= ImGui.calcTextSizeX(label)) {
                ImGui.beginTooltip();
                ImGui.text(label);
                ImGui.endTooltip();
            }
            ImGui.tableNextColumn();
        }
        float resetWidth = ImGui.calcTextSizeX(" X ");
        float dragRemains = (ImGui.getContentRegionAvailX() - resetWidth * 2.0f) / 2.0f;
        final ImBoolean changed = new ImBoolean(false);
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0, 0);
        ImGui.pushID("x_" + id);
        EditorColors.RedButton.create(() -> {
            if (ImGui.button("X##Reset_X_" + id, resetWidth, 0.0f)) {
                dragVec2Tmp.x = resetX;
                changed.set(true);
            }
        });
        ImGui.pushItemWidth(dragRemains);
        ImGui.sameLine();
        float[] valX = {dragVec2Tmp.x};
        if (ImGui.dragFloat("##drag_X_", valX, dragSpeed, minVal, maxVal)) {
            dragVec2Tmp.x = valX[0];
            changed.set(true);
        }
        ImGui.popItemWidth();
        ImGui.popID();
        ImGui.sameLine();
        ImGui.pushID("y_" + id);
        EditorColors.GreenButton.create(() -> {
            if (ImGui.button("Y##Reset_Y_" + id, resetWidth, 0.0f)) {
                dragVec2Tmp.y = resetY;
                changed.set(true);
            }
        });
        ImGui.pushItemWidth(dragRemains);
        ImGui.sameLine();
        float[] valY = {dragVec2Tmp.y};
        if (ImGui.dragFloat("##drag_Y_" + id, valY, dragSpeed, minVal, maxVal)) {
            dragVec2Tmp.y = valY[0];
            changed.set(true);
        }
        ImGui.popItemWidth();
        ImGui.popID();
        if (changed.get()) source.set(dragVec2Tmp);
        ImGui.popStyleVar();
        if (useLabel) ImGui.endTable();
        ImGui.popID();
        return changed.get();
    }

    public static float dragFloatCtrl(String label, float val, Object caller) {
        return dragFloatCtrl(label, val, 0.0f, 0.01f, caller, 0.0f, 0.0f);
    }

    public static float dragFloatCtrl(String label, float val, float resetVal, Object caller) {
        return dragFloatCtrl(label, val, resetVal, 0.01f, caller, 0.0f, 0.0f);
    }

    public static float dragFloatCtrl(String label, float val, Object caller, float dragSpeed) {
        return dragFloatCtrl(label, val, 0.0f, dragSpeed, caller, 0.0f, 0.0f);
    }

    public static float dragFloatCtrl(String label, float val, float resetVal, float dragSpeed, Object caller) {
        return dragFloatCtrl(label, val, resetVal, dragSpeed, caller, 0.0f, 0.0f);
    }

    public static float dragFloatCtrl(String label, float val, float resetVal, Object caller, float minVal) {
        return dragFloatCtrl(label, val, resetVal, 0.01f, caller, minVal, Float.MAX_VALUE);
    }

    public static float dragFloatCtrl(String label, float val, float resetVal, float dragSpeed, Object caller, float minVal) {
        return dragFloatCtrl(label, val, resetVal, dragSpeed, caller, minVal, Float.MAX_VALUE);
    }

    public static float dragFloatCtrl(String label, float val, float resetVal, float dragSpeed, Object caller, float minVal, float maxVal) {
        String id = createID(label, caller);
        float[] valA = {val};
        boolean useLabel = label != null && !label.isBlank();
        ImGui.pushID(id);
        if (useLabel) {
            if (!ImGui.beginTable("##Table_" + id, 2, ImGuiTableFlags.BordersInnerV | ImGuiTableFlags.SizingStretchProp, ImGui.getContentRegionAvailX())) {
                ImGui.popID();
                return val;
            }
            ImGui.tableSetupColumn("##label_" + label + id, ImGuiTableColumnFlags.WidthFixed, defaultWidth);
            ImGui.tableSetupColumn("##content_" + label + id, ImGuiTableColumnFlags.WidthStretch);
            ImGui.tableNextColumn();
            float labelSpace = ImGui.getContentRegionAvailX();
            ImGui.text(shortenLabel(label, labelSpace));
            if (ImGui.isItemHovered() && labelSpace <= ImGui.calcTextSizeX(label)) {
                ImGui.beginTooltip();
                ImGui.text(label);
                ImGui.endTooltip();
            }
            ImGui.tableNextColumn();
        }
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0, 0);
        ImBoolean reset = new ImBoolean(false);
        EditorColors.RedButton.create(() -> reset.set(ImGui.button("Reset##Reset_" + id)));
        if (reset.get()) val = resetVal;
        ImGui.sameLine();
        ImGui.pushItemWidth(ImGui.getContentRegionAvailX());
        boolean changed = ImGui.dragFloat("##dragFloat", valA, dragSpeed, minVal, maxVal);
        ImGui.popItemWidth();
        ImGui.popStyleVar();
        if (useLabel) ImGui.endTable();
        ImGui.popID();
        return changed ? valA[0] : val;
    }

    public static int dragIntCtrl(String label, int val, Object caller) {
        return dragIntCtrl(label, val, 0, caller , 0 ,0);
    }

    public static int dragIntCtrl(String label, int val, int resetVal, Object caller) {
        return dragIntCtrl(label, val, resetVal, caller , 0 ,0);
    }

    public static int dragIntCtrl(String label, int val, int resetVal, Object caller, int min) {
        return dragIntCtrl(label, val, resetVal, caller , min ,Integer.MAX_VALUE);
    }

    public static int dragIntCtrl(String label, int val, int resetVal, Object caller, int min, int max) {
        String id = createID(label, caller);
        int[] valA = {val};
        boolean useLabel = label != null && !label.isBlank();
        ImGui.pushID(id);
        if (useLabel) {
            if (!ImGui.beginTable("##Table_" + id, 2, ImGuiTableFlags.BordersInnerV | ImGuiTableFlags.SizingStretchProp, ImGui.getContentRegionAvailX())) {
                ImGui.popID();
                return val;
            }
            ImGui.tableSetupColumn("##label_" + label + id, ImGuiTableColumnFlags.WidthFixed, defaultWidth);
            ImGui.tableSetupColumn("##content_" + label + id, ImGuiTableColumnFlags.WidthStretch);
            ImGui.tableNextColumn();
            float labelSpace = ImGui.getContentRegionAvailX();
            ImGui.text(shortenLabel(label, labelSpace));
            if (ImGui.isItemHovered() && labelSpace <= ImGui.calcTextSizeX(label)) {
                ImGui.beginTooltip();
                ImGui.text(label);
                ImGui.endTooltip();
            }
            ImGui.tableNextColumn();
        }
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0, 0);
        ImBoolean reset = new ImBoolean(false);
        EditorColors.RedButton.create(() -> reset.set(ImGui.button("Reset##Reset_" + id)));
        if (reset.get()) val = resetVal;
        ImGui.sameLine();
        ImGui.pushItemWidth(ImGui.getContentRegionAvailX());
        boolean changed = ImGui.dragInt("##dragInt" + id, valA, 1, min, max);
        ImGui.popItemWidth();
        ImGui.popStyleVar();
        if (useLabel) ImGui.endTable();
        ImGui.popID();
        return changed ? valA[0] : val;
    }

    public static boolean colorCtrl(String label, Vector4f val, Object caller) {
        String id = createID(label, caller);
        float[] color = {val.x, val.y, val.z, val.w};
        boolean useLabel = label != null && !label.isBlank();
        ImGui.pushID(id);
        if (useLabel) {
            if (!ImGui.beginTable("##Table_" + id, 2, ImGuiTableFlags.BordersInnerV | ImGuiTableFlags.SizingStretchProp, ImGui.getContentRegionAvailX())) {
                ImGui.popID();
                return false;
            }
            ImGui.tableSetupColumn("##label_" + label + id, ImGuiTableColumnFlags.WidthFixed, defaultWidth);
            ImGui.tableSetupColumn("##content_" + label + id, ImGuiTableColumnFlags.WidthStretch);
            ImGui.tableNextColumn();
            float labelSpace = ImGui.getContentRegionAvailX();
            ImGui.text(shortenLabel(label, labelSpace));
            if (ImGui.isItemHovered() && labelSpace <= ImGui.calcTextSizeX(label)) {
                ImGui.beginTooltip();
                ImGui.text(label);
                ImGui.endTooltip();
            }
            ImGui.tableNextColumn();
        }
        ImGui.pushItemWidth(ImGui.getContentRegionAvailX());
        boolean changed = ImGui.colorEdit4("##color" + id, color);
        ImGui.popItemWidth();
        if (changed) val.set(color[0], color[1], color[2], color[3]);
        if (useLabel) ImGui.endTable();
        ImGui.popID();
        return changed;
    }

    public static String inputText(String label, String txt, Object caller) {
        String id = createID(label, caller);
        ImString out = new ImString(txt, 256);
        boolean useLabel = label != null && !label.isBlank();
        ImGui.pushID(id);
        if (useLabel) {
            if (!ImGui.beginTable("##Table_" + id, 2, ImGuiTableFlags.BordersInnerV | ImGuiTableFlags.SizingStretchProp, ImGui.getContentRegionAvailX())) {
                ImGui.popID();
                return txt;
            }
            ImGui.tableSetupColumn("##label_" + label + id, ImGuiTableColumnFlags.WidthFixed, defaultWidth);
            ImGui.tableSetupColumn("##content_" + label + id, ImGuiTableColumnFlags.WidthStretch);
            ImGui.tableNextColumn();
            float labelSpace = ImGui.getContentRegionAvailX();
            ImGui.text(shortenLabel(label, labelSpace));
            if (ImGui.isItemHovered() && labelSpace <= ImGui.calcTextSizeX(label)) {
                ImGui.beginTooltip();
                ImGui.text(label);
                ImGui.endTooltip();
            }
            ImGui.tableNextColumn();
        }

        ImGui.pushItemWidth(ImGui.getContentRegionAvailX());
        boolean changed = ImGui.inputText("##" + label + id, out);
        ImGui.popItemWidth();
        if (useLabel) ImGui.endTable();
        ImGui.popID();
        return changed ? out.get() : txt;
    }

    public static String inputTextWithIME(String label, String txt, int bufferSize, Object caller) {
        String id = createID(label, caller);
        ImString out = new ImString(txt, bufferSize);
        int flags = ImGuiInputTextFlags.CallbackResize
                | ImGuiInputTextFlags.CallbackHistory
                | ImGuiInputTextFlags.CallbackCompletion
                | ImGuiInputTextFlags.CallbackCharFilter;
        boolean useLabel = label != null && !label.isBlank();
        ImGui.pushID(id);
        if (useLabel) {
            if (!ImGui.beginTable("##Table_" + id, 2, ImGuiTableFlags.BordersInnerV | ImGuiTableFlags.SizingStretchProp, ImGui.getContentRegionAvailX())) {
                ImGui.popID();
                return txt;
            }
            ImGui.tableSetupColumn("##label_" + label + id, ImGuiTableColumnFlags.WidthFixed, defaultWidth);
            ImGui.tableSetupColumn("##content_" + label + id, ImGuiTableColumnFlags.WidthStretch);
            ImGui.tableNextColumn();
            float labelSpace = ImGui.getContentRegionAvailX();
            ImGui.text(shortenLabel(label, labelSpace));
            if (ImGui.isItemHovered() && labelSpace <= ImGui.calcTextSizeX(label)) {
                ImGui.beginTooltip();
                ImGui.text(label);
                ImGui.endTooltip();
            }
            ImGui.tableNextColumn();
        }
        if (KeyListener.hasTextInput()) {
            String IMEInput = KeyListener.getTextInput();
            if (!IMEInput.isEmpty()) out.set(out.get() + IMEInput);
        }
        ImGui.pushItemWidth(ImGui.getContentRegionAvailX());
        boolean changed = ImGui.inputText("##" + label + id, out, flags);
        ImGui.popItemWidth();
        if (useLabel) ImGui.endTable();
        ImGui.popID();
        return changed ? out.get() : txt;
    }

    /**
     * Render a combo box for selecting an enum constant.
     * @param label the label for the combo box
     * @param current the currently selected constant
     * @param enumType the class of the enum type
     * @param caller the object that call this method
     * @return the newly selected enum constant or the current one
     */
    public static Enum<?> enumComboCtrl(String label, Enum<?> current, Class<?> enumType, Object caller) {
        if (enumType == null || !enumType.isEnum()) return current;
        String id = createID(label, caller);
        boolean useLabel = label != null && !label.isBlank();
        Enum<?> result = current;
        ImGui.pushID(id);
        if (useLabel) {
            if (!ImGui.beginTable("##Table_" + id, 2, ImGuiTableFlags.BordersInnerV | ImGuiTableFlags.SizingStretchProp, ImGui.getContentRegionAvailX())) {
                ImGui.popID();
                return current;
            }
            ImGui.tableSetupColumn("##label_" + label + id, ImGuiTableColumnFlags.WidthFixed, defaultWidth);
            ImGui.tableSetupColumn("##content_" + label + id, ImGuiTableColumnFlags.WidthStretch);
            ImGui.tableNextColumn();
            float labelSpace = ImGui.getContentRegionAvailX();
            ImGui.text(shortenLabel(label, labelSpace));
            if (ImGui.isItemHovered() && labelSpace <= ImGui.calcTextSizeX(label)) {
                ImGui.beginTooltip();
                ImGui.text(label);
                ImGui.endTooltip();
            }
            ImGui.tableNextColumn();
        }
        ImGui.pushItemWidth(ImGui.getContentRegionAvailX());
        String preview = current == null ? "Select one..." : current.name();
        if (ImGui.beginCombo("##Combo" + id, preview)) {
            for (Object constant : enumType.getEnumConstants()) {
                Enum<?> entry = (Enum<?>) constant;
                if (ImGui.selectable(entry.name() + "##" + id + "_" + entry.name(), entry == current)) result = entry;
            }
            ImGui.endCombo();
        }
        ImGui.popItemWidth();
        if (useLabel) ImGui.endTable();
        ImGui.popID();
        return result;
    }

    /**
     * Render an icon button. The size of the widget is limited by the font size.
     * This method return whether the icon had been clicked or not.
     * @param id the id for the widget
     * @param sprite the sprite for the icon
     * @param toolTip the tooltips to display
     * @return true if clicked
     */
    public static boolean iconButton(String id, EditorIcons.EditorIconSprite sprite, String toolTip) {
        Sprite icon = sprite.getIcon();
        if (icon == null) return ImGui.button(id);
        float sizeLimit = ImGui.getFontSize();
        int textureID = icon.getTextureID();
        Vector2f scaledSize = TextureScale.calculateFitSquare(icon.getWidth(), icon.getHeight(), sizeLimit);
        Vector2f[] textureCoordinates = icon.getTextureCoordinates();
        ImGui.pushStyleColor(ImGuiCol.Button, 1.0f, 1.0f, 1.0f, 0.0f);
        boolean clicked = ImGui.imageButton(id, textureID, scaledSize.x, scaledSize.y,
                textureCoordinates[2].x, textureCoordinates[0].y,
                textureCoordinates[0].x, textureCoordinates[2].y
        );
        ImGui.popStyleColor(1);
        if (ImGui.isItemHovered() && toolTip != null) {
            ImGui.beginTooltip();
            ImGui.text(toolTip);
            ImGui.endTooltip();
        }
        return clicked;
    }

    /**
     * Render an icon button, this method return whether the icon had been clicked or not.
     * @param id the id for the widget
     * @param sprite the sprite for the icon
     * @param toolTip the tooltips to display
     * @param width width for the widget
     * @param height height for the widget
     * @return true if clicked
     */
    public static boolean iconButton(String id, EditorIcons.EditorIconSprite sprite, String toolTip, float width, float height) {
        if (width <= 0.0f || height <= 0.0f) width = height = ImGui.getFontSize();
        Sprite icon = sprite.getIcon();
        if (icon == null) return ImGui.button(id);
        int textureID = icon.getTextureID();
        Vector2f[] textureCoordinates = icon.getTextureCoordinates();
        ImGui.pushStyleColor(ImGuiCol.Button, 1.0f, 1.0f, 1.0f, 0.0f);
        boolean clicked = ImGui.imageButton(id, textureID, width, height,
                textureCoordinates[2].x, textureCoordinates[0].y,
                textureCoordinates[0].x, textureCoordinates[2].y
        );
        ImGui.popStyleColor(1);
        if (ImGui.isItemHovered() && toolTip != null) {
            ImGui.beginTooltip();
            ImGui.text(toolTip);
            ImGui.endTooltip();
        }
        return clicked;
    }

    public static boolean iconButton(String id, Sprite icon, String toolTip, float width, float height) {
        if (width <= 0.0f || height <= 0.0f) width = height = ImGui.getFontSize();
        if (icon == null) return ImGui.button(id);
        int textureID = icon.getTextureID();
        Vector2f[] textureCoordinates = icon.getTextureCoordinates();
        ImGui.pushStyleColor(ImGuiCol.Button, 1.0f, 1.0f, 1.0f, 0.0f);
        boolean clicked = ImGui.imageButton(id, textureID, width, height,
                textureCoordinates[2].x, textureCoordinates[0].y,
                textureCoordinates[0].x, textureCoordinates[2].y
        );
        ImGui.popStyleColor(1);
        if (ImGui.isItemHovered() && toolTip != null) {
            ImGui.beginTooltip();
            ImGui.text(toolTip);
            ImGui.endTooltip();
        }
        return clicked;
    }

    /**
     * Render a selectable icon, this method return whether the selectable had been interacted or not.
     * @param id the id for the widget
     * @param sprite the sprite for the icon
     * @param toolTip the tooltips to display
     * @param selected the selectable current state
     * @param width width for the widget
     * @param height heigh for the widget
     * @return true if interacted
     */
    public static boolean selectableIcon(String id, EditorIcons.EditorIconSprite sprite, String toolTip, boolean selected, float width, float height) {
        if (id == null || id.isBlank()) return false;
        if (width <= 0.0f || height <= 0.0f) width = height = ImGui.getFontSize();
        Sprite icon = sprite.getIcon();
        if (icon == null) return ImGui.selectable(id, selected, width, height);
        ImGui.beginGroup();
        ImVec2 framePadding = ImGui.getStyle().getFramePadding();
        ImVec2 cursorPos = ImGui.getCursorPos();
        ImGui.setCursorPos(cursorPos.x + framePadding.x, cursorPos.y + framePadding.y);
        boolean interact = ImGui.selectable("##" + id + "_Selectable_Icon", selected, width, height + framePadding.y);
        if (ImGui.isItemHovered() && toolTip != null) {
            ImGui.beginTooltip();
            ImGui.text(toolTip);
            ImGui.endTooltip();
        }
        ImGui.setCursorPos(cursorPos.x + framePadding.x, cursorPos.y + framePadding.y * 1.5f);
        int textureID = icon.getTextureID();
        Vector2f[] textureCoordinates = icon.getTextureCoordinates();
        ImGui.image(textureID, width, height,
                textureCoordinates[2].x, textureCoordinates[0].y,
                textureCoordinates[0].x, textureCoordinates[2].y
        );
        ImGui.endGroup();
        return interact;
    }

    /**
     * Render a selectable icon, this method will update the selection status of the selectable via the tracking boolean
     * @param id the id for the widget
     * @param sprite the sprite for the icon
     * @param toolTip the tooltips to display
     * @param selected the selection tracking boolean
     * @param width width for the widget
     * @param height heigh for the widget
     */
    public static void selectableIcon(String id, EditorIcons.EditorIconSprite sprite, String toolTip, ImBoolean selected, float width, float height) {
        if (id == null || sprite == null || selected == null) return;
        boolean before = selected.get();
        if (selectableIcon(id, sprite, toolTip, selected.get(), width, height)) selected.set(!before);
    }

    /**
     * Render a physic layer editing table, this method is not collision layer or collision mask specific.
     * @param label the display label
     * @param mask the mask value
     * @param caller the object that call this method
     * @return the updated mask value if changed
     */
    public static int physicLayerSelectable(String label, int mask, Object caller) {
        String id = createID(label, caller);
        ImGui.pushID(id);
        ImGui.text(label);
        ImGui.spacing();
        float cellWidth = ImGui.calcTextSizeX("99") + ImGui.getStyle().getFramePaddingX() * 2.0f;
        float availWidth = ImGui.getContentRegionAvailX();
        float spacing = ImGui.getStyle().getItemSpacingX();
        int maxColumn = Math.clamp((int) ((availWidth + spacing) / (cellWidth + spacing)), 1, 8);
        int layers = Physic2D.MaxLayer;
        int newMask = mask;
        float width = maxColumn * (cellWidth + spacing);
        if (!ImGui.beginTable("##Layers_Toggle_Table" + id, maxColumn, ImGuiTableFlags.Borders | ImGuiTableFlags.SizingFixedFit, new ImVec2(Math.min(width, availWidth), 0.0f))) {
            ImGui.popID();
            return mask;
        }
        for (int i = 0; i < maxColumn; i++) ImGui.tableSetupColumn("Layer_Toggle_Column_" + i + "_" + id, ImGuiTableColumnFlags.WidthFixed, cellWidth);
        for (int i = 0; i < layers; i++) {
            ImGui.tableNextColumn();
            boolean isSelected = PhysicLayer.isLayerInMask(newMask, i);
            ImGui.beginGroup();
            String layerIndex = String.valueOf(i);
            float remainWidth = Math.min(ImGui.getContentRegionAvailX(), cellWidth);
            float textWidth = ImGui.calcTextSizeX(layerIndex);
            float offset = Math.max((remainWidth - textWidth) * 0.5f, 0.0f);
            float cursorX = ImGui.getCursorPosX();
            if (ImGui.selectable("##Layer_Selectable_" + i + "_" + id, isSelected, remainWidth, ImGui.getTextLineHeightWithSpacing())) newMask = PhysicLayer.toggleLayerInMask(newMask, i);
            ImGui.sameLine();
            ImGui.setCursorPosX(cursorX + offset);
            ImGui.text(layerIndex);
            ImGui.endGroup();
            if (ImGui.isItemHovered()) {
                ImGui.beginTooltip();
                ImGui.text(Project.getPhysicLayerName(i));
                ImGui.endTooltip();
            }
        }
        ImGui.endTable();
        ImGui.popID();
        return newMask;
    }

    /**
     * Render checkbox with the given label and the initial value.
     * This method is responsible for handling the change of checkbox value after user interaction via its return value.
     * @param label the label for the checkbox
     * @param val the initial value
     * @param caller the object that call this method
     * @return the final value of the checkbox if changed, or the initial value
     */
    public static boolean checkboxCtrl(String label, boolean val, Object caller) {
        checkboxTmp.set(val);
        boolean changed = ImGui.checkbox(label + "##" + createID(label, caller), checkboxTmp);
        return changed ? checkboxTmp.get() : val;
    }

    private static String shortenLabel(String label, float availableWidth) {
        if (label == null || label.isBlank()) return label;
        ShortenLabelKey cachedKey = new ShortenLabelKey(label, availableWidth);
        String result = shortenLabels.get(cachedKey);
        if (result != null) return result;
        float fullWidth = ImGui.calcTextSizeX(label);
        if (fullWidth <= availableWidth) {
            shortenLabels.put(cachedKey, label);
            return label;
        }
        String space = " ";
        float dWidth = ImGui.calcTextSizeX(space);
        if (dWidth >= availableWidth) {
            shortenLabels.put(cachedKey, space);
            return space;
        }
        float targetW = availableWidth - dWidth;
        int l = 0;
        int r = label.length();
        int bestFit = 0;
        while (l <= r) {
            int mid = (l + r) / 2;
            String sub = label.substring(0, mid);
            float sWidth = ImGui.calcTextSizeX(sub);
            if (sWidth < targetW) {
                bestFit = mid;
                l = mid + 1;
            } else {
                r = mid - 1;
            }
        }
        result = bestFit == 0 ? space : label.substring(0, bestFit) + space;
        shortenLabels.put(cachedKey, result);
        return result;
    }

    private static String createID(String label, Object caller) {
        if (caller == null) return label;
        if (caller instanceof String s) return label + "__" + s;
        return label + "__" + System.identityHashCode(caller);
    }

    /**
     * Get the minimum width required by the given label and system default limit.
     * <p>
     * For labels with ID string behind it ({@code label##label_id}), the ID part is ignored.
     * @param label the label string that needed to be allocated
     * @return the width value to fit the label text
     */
    public static float allocateLabelWidth(String label) {
        return allocateLabelWidth(label, defaultWidth);
    }

    /**
     * Get the minimum width required by the given label and allocation limit.
     * <p>
     * For labels with ID string behind it ({@code label##label_id}), the ID part is ignored.
     * @param label the label string that needed to be allocated
     * @param maxWidth maximum width that can be allocated
     * @return the width value to fit the label text
     */
    public static float allocateLabelWidth(String label, float maxWidth) {
        if (label == null) return 1.0f;
        float widthPadding = ImGui.calcTextSizeX(label, true) + 1.0f;
        return Math.clamp(maxWidth, 0.0f, widthPadding);
    }

    /**
     * A header widget that can be expanded or collapsed by interacting with the drop-down arrow,
     * or execute logic in callback when interacting with the label.
     * <p>
     * This is a combination of collapsing header, and table to separate interaction.
     * It also enables dynamic naming without having the header collapsed when the visible label changed.
     *
     * @param id unique id of the header
     * @param label the display label
     * @param onExpand callback when header is expanded
     * @param onLabelInteract callback when label of the header is clicked
     */
    public static void collapsibleHeader(String id, String label, Runnable onExpand, Runnable onLabelInteract) {
        CollapsibleHeader.create(id, label, onExpand, onLabelInteract);
    }

    /**
     * A header widget that can be expanded or collapsed by interacting with the drop-down arrow,
     * or execute logic in callback when interacting with the label.
     * <p>
     * This is a combination of collapsing header, and table to separate interaction.
     * It also enables dynamic naming without having the header collapsed when the visible label changed.
     *
     * @param id unique id of the header
     * @param label the display label
     * @param labelTooltip tooltip when hovering over the label
     * @param onExpand callback when header is expanded
     * @param onLabelInteract callback when label of the header is clicked
     * @param headerFlag flag for the header widget
     */
    public static void collapsibleHeader(String id, String label, String labelTooltip, Runnable onExpand, Runnable onLabelInteract, CollapsibleHeaderFlag headerFlag) {
        CollapsibleHeader.create(id, label, labelTooltip, onExpand, onLabelInteract, headerFlag);
    }

    /**
     * A header widget that can be expanded or collapsed by interacting with the drop-down arrow,
     * or execute logic in callback when interacting with the label.
     * <p>
     * This is a combination of collapsing header, and table to separate interaction.
     * It also enables dynamic naming without having the header collapsed when the visible label changed.
     *
     * @param id unique id of the header
     * @param label the display label
     * @param onLabelInteract callback when label of the header is clicked
     * @return true if the header is expanded
     */
    public static boolean collapsibleHeader(String id, String label, Runnable onLabelInteract) {
        return CollapsibleHeader.create(id, label, onLabelInteract);
    }

    /**
     * A header widget that can be expanded or collapsed by interacting with the drop-down arrow,
     * or execute logic in callback when interacting with the label.
     * <p>
     * This is a combination of collapsing header, and table to separate interaction.
     * It also enables dynamic naming without having the header collapsed when the visible label changed.
     *
     * @param id unique id of the header
     * @param label the display label
     * @param labelTooltip tooltip when hovering over the label
     * @param onLabelInteract callback when label of the header is clicked
     * @param headerFlag flag for the header widget
     * @return true if the header is expanded
     */
    public static boolean collapsibleHeader(String id, String label, String labelTooltip, Runnable onLabelInteract, CollapsibleHeaderFlag headerFlag) {
        return CollapsibleHeader.create(id, label, labelTooltip, onLabelInteract, headerFlag);
    }
}
