package editor;

import TheCellBeyond.KeyListener;
import imgui.ImVec2;
import imgui.flag.*;
import imgui.type.ImBoolean;
import physic2d.Physic2D;
import physic2d.PhysicLayer;
import project.Project;
import render.texture.Sprite;
import utility.*;

import imgui.ImGui;
import imgui.type.ImString;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.HashMap;

public class ImEditorGui {
    private record ShortenLabelKey(String text, float width) {}

    private static final HashMap<ShortenLabelKey, String> shortenLabels = new HashMap<>();
    private static final float defaultWidth = 80.0f;

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
        String id = createID(label, caller);
        Vector2f out = new Vector2f(source);

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
                out.x = resetX;
                changed.set(true);
            }
        });
        ImGui.pushItemWidth(dragRemains);
        ImGui.sameLine();
        float[] valX = {out.x};
        if (ImGui.dragFloat("##drag_X_", valX, dragSpeed, minVal, maxVal)) {
            out.x = valX[0];
            changed.set(true);
        }
        ImGui.popItemWidth();
        ImGui.popID();
        ImGui.sameLine();

        ImGui.pushID("y_" + id);
        EditorColors.GreenButton.create(() -> {
            if (ImGui.button("Y##Reset_Y_" + id, resetWidth, 0.0f)) {
                out.y = resetY;
                changed.set(true);
            }
        });
        ImGui.pushItemWidth(dragRemains);
        ImGui.sameLine();
        float[] valY = {out.y};
        if (ImGui.dragFloat("##drag_Y_" + id, valY, dragSpeed, minVal, maxVal)) {
            out.y = valY[0];
            changed.set(true);
        }
        ImGui.popItemWidth();
        ImGui.popID();

        if (changed.get()) source.set(out.x, out.y);

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
            if (!IMEInput.isEmpty()) {
                out.set(out.get() + IMEInput);
            }
        }

        ImGui.pushItemWidth(ImGui.getContentRegionAvailX());
        boolean changed = ImGui.inputText("##" + label + id, out, flags);
        ImGui.popItemWidth();

        if (useLabel) ImGui.endTable();
        ImGui.popID();

        return changed ? out.get() : txt;
    }

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

    public static boolean iconButton(String id, EditorIcons.EditorIconSprite sprite, String toolTip, float width, float height) {
        if (width <= 0.0f || height <= 0.0f) {
            width = height = ImGui.getFontSize();
        }

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

    public static boolean selectableIcon(String id, EditorIcons.EditorIconSprite sprite, String toolTip, boolean selected, float width, float height) {
        if (id == null || id.isBlank()) return false;

        if (width <= 0.0f || height <= 0.0f) {
            width = height = ImGui.getFontSize();
        }

        Sprite icon = sprite.getIcon();
        if (icon == null) {
            return ImGui.selectable(id, selected, width, height);
        }

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

    public static void selectableIcon(String id, EditorIcons.EditorIconSprite sprite, String toolTip, ImBoolean selected, float width, float height) {
        if (id == null || sprite == null || selected == null) return;
        boolean before = selected.get();
        if (selectableIcon(id, sprite, toolTip, selected.get(), width, height)) selected.set(!before);
    }

    public static int physicLayerSelectable(String label, int mask, Object caller) {
        String id = createID(label, caller);
        ImGui.pushID(id);
        ImGui.text(label);
        ImGui.spacing();
        float cellWidth = ImGui.calcTextSizeX("99") + ImGui.getStyle().getFramePaddingX() * 2.0f;
        float availWidth = ImGui.getContentRegionAvailX();
        float spacing = ImGui.getStyle().getItemSpacingX();
        int maxColumn = Math.min(8, Math.max(1, (int) ((availWidth + spacing) / (cellWidth + spacing))));
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
        return Math.min(widthPadding, Math.max(0.0f, maxWidth));
    }
}
