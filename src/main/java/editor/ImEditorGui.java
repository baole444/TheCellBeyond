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

    public static boolean dragVec2Ctrl(String label, Vector2f source, float resetVal, Object caller) {
        return dragVec2Ctrl(label, source, resetVal, resetVal, 0.1f, caller);
    }

    public static boolean dragVec2Ctrl(String label, Vector2f source, float resetVal, float dragSpeed, Object caller) {
        return dragVec2Ctrl(label, source, resetVal, resetVal, dragSpeed, caller);
    }

    public static boolean dragVec2Ctrl(String label, Vector2f source, float resetX, float resetY, float dragSpeed, Object caller) {
        String id = createID(label, caller);
        Vector2f out = new Vector2f(source);

        ImGui.pushID(id);
        if (!ImGui.beginTable("##Table_" + id, 2, ImGuiTableFlags.BordersInnerV | ImGuiTableFlags.SizingStretchProp, ImGui.getContentRegionAvailX())) {
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

        float resetWidth = ImGui.calcTextSizeX(" X ");
        float dragRemains = (ImGui.getContentRegionAvailX() - resetWidth * 2.0f) / 2.0f;
        boolean changed = false;
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0, 0);

        ImGui.pushID("x");
        ImGui.pushStyleColor(ImGuiCol.Button, 0.7f, 0.2f, 0.2f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.8f, 0.3f, 0.3f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.7f, 0.2f, 0.2f, 1.0f);
        if (ImGui.button("X##Reset_X_" + id, resetWidth, 0.0f)) {
            out.x = resetX;
            changed = true;
        }
        ImGui.popStyleColor(3);
        ImGui.pushItemWidth(dragRemains);
        ImGui.sameLine();
        float[] valX = {out.x};
        if (ImGui.dragFloat("##dragX", valX, dragSpeed)) {
            out.x = valX[0];
            changed = true;
        }
        ImGui.popItemWidth();
        ImGui.popID();
        ImGui.sameLine();

        ImGui.pushID("y");
        ImGui.pushStyleColor(ImGuiCol.Button, 0.2f, 0.7f, 0.2f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.3f, 0.8f, 0.3f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.2f, 0.7f, 0.2f, 1.0f);
        if (ImGui.button("Y##Reset_Y_" + id, resetWidth, 0.0f)) {
            out.y = resetY;
            changed = true;
        }
        ImGui.popStyleColor(3);
        ImGui.pushItemWidth(dragRemains);
        ImGui.sameLine();
        float[] valY = {out.y};
        if (ImGui.dragFloat("##dragY", valY, dragSpeed)) {
            out.y = valY[0];
            changed = true;
        }
        ImGui.popItemWidth();
        ImGui.popID();

        if (changed) source.set(out.x, out.y);

        ImGui.popStyleVar();
        ImGui.endTable();
        ImGui.popID();

        return changed;
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

        ImGui.pushID(id);
        if (!ImGui.beginTable("##Table_" + id, 2, ImGuiTableFlags.BordersInnerV | ImGuiTableFlags.SizingStretchProp, ImGui.getContentRegionAvailX())) {
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

        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0, 0);

        ImGui.pushStyleColor(ImGuiCol.Button, 0.7f, 0.2f, 0.2f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.8f, 0.3f, 0.3f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.7f, 0.2f, 0.2f, 1.0f);
        if (ImGui.button("Reset##Reset_" + id)) val = resetVal;
        ImGui.popStyleColor(3);
        ImGui.sameLine();

        ImGui.pushItemWidth(ImGui.getContentRegionAvailX());
        boolean changed = ImGui.dragFloat("##dragFloat", valA, dragSpeed, minVal, maxVal);
        ImGui.popItemWidth();

        ImGui.popStyleVar();
        ImGui.endTable();
        ImGui.popID();

        return changed ? valA[0] : val;
    }

    public static int dragIntCtrl(String label, int val, Object caller) {
        String id = createID(label, caller);
        int[] valA = {val};

        ImGui.pushID(id);
        if (!ImGui.beginTable("##Table_" + id, 2, ImGuiTableFlags.BordersInnerV | ImGuiTableFlags.SizingStretchProp, ImGui.getContentRegionAvailX())) {
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

        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0, 0);

        ImGui.pushStyleColor(ImGuiCol.Button, 0.7f, 0.2f, 0.2f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.8f, 0.3f, 0.3f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.7f, 0.2f, 0.2f, 1.0f);
        if (ImGui.button("Reset##Reset_" + id)) val = 0;
        ImGui.popStyleColor(3);
        ImGui.sameLine();

        ImGui.pushItemWidth(ImGui.getContentRegionAvailX());
        boolean changed = ImGui.dragInt("##dragInt" + id, valA, 1);
        ImGui.popItemWidth();

        ImGui.popStyleVar();
        ImGui.endTable();
        ImGui.popID();

        return changed ? valA[0] : val;
    }

    public static boolean colorCtrl(String label, Vector4f val, Object caller) {
        String id = createID(label, caller);
        float[] color = {val.x, val.y, val.z, val.w};

        ImGui.pushID(id);
        if (!ImGui.beginTable("##Table_" + id, 2, ImGuiTableFlags.BordersInnerV | ImGuiTableFlags.SizingStretchProp, ImGui.getContentRegionAvailX())) {
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

        ImGui.pushItemWidth(ImGui.getContentRegionAvailX());
        boolean changed = ImGui.colorEdit4("##color" + id, color);
        ImGui.popItemWidth();

        if (changed) val.set(color[0], color[1], color[2], color[3]);

        ImGui.endTable();
        ImGui.popID();

        return changed;
    }

    public static String inputText(String label, String txt, Object caller) {
        String id = createID(label, caller);
        ImString out = new ImString(txt, 256);

        ImGui.pushID(id);
        if (!ImGui.beginTable("##Table_" + id, 2, ImGuiTableFlags.BordersInnerV | ImGuiTableFlags.SizingStretchProp, ImGui.getContentRegionAvailX())) {
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

        ImGui.pushItemWidth(ImGui.getContentRegionAvailX());
        boolean changed = ImGui.inputText("##" + label + id, out);
        ImGui.popItemWidth();

        ImGui.endTable();
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

        ImGui.pushID(id);
        if (!ImGui.beginTable("##Table_" + id, 2, ImGuiTableFlags.BordersInnerV | ImGuiTableFlags.SizingStretchProp, ImGui.getContentRegionAvailX())) {
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

        if (KeyListener.hasTextInput()) {
            String IMEInput = KeyListener.getTextInput();
            if (!IMEInput.isEmpty()) {
                out.set(out.get() + IMEInput);
            }
        }

        ImGui.pushItemWidth(ImGui.getContentRegionAvailX());
        boolean changed = ImGui.inputText("##" + label + id, out, flags);
        ImGui.popItemWidth();

        ImGui.endTable();
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
        boolean interact = ImGui.selectable("##" + id + "_Selectable_Icon", selected, width + framePadding.x, height + framePadding.y);
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
        if (width <= 0.0f || height <= 0.0f) {
            width = height = ImGui.getFontSize();
        }

        Sprite icon = sprite.getIcon();
        if (icon == null) {
            ImGui.selectable(id, selected, width, height);
            return;
        }
        ImGui.beginGroup();
        ImVec2 framePadding = ImGui.getStyle().getFramePadding();
        ImVec2 cursorPos = ImGui.getCursorPos();
        ImGui.setCursorPos(cursorPos.x + framePadding.x, cursorPos.y + framePadding.y);
        ImGui.selectable("##" + id + "_Selectable_Icon", selected, width + framePadding.x, height + framePadding.y);
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

        String dots = "...";
        float dWidth = ImGui.calcTextSizeX(dots);

        if (dWidth >= availableWidth) {
            shortenLabels.put(cachedKey, dots);
            return dots;
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

        result = bestFit == 0 ? dots : label.substring(0, bestFit) + dots;
        shortenLabels.put(cachedKey, result);
        return result;
    }

    private static String createID(String label, Object caller) {
        if (caller == null) return label;

        if (caller instanceof String s) return label + "__" + s;

        return label + "__" + System.identityHashCode(caller);
    }
}
