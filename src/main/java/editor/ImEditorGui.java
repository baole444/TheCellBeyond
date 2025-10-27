package editor;

import TheCellBeyond.KeyListener;
import imgui.ImVec2;
import imgui.flag.*;
import imgui.type.ImBoolean;
import render.texture.Sprite;
import utility.*;

import imgui.ImGui;
import imgui.type.ImString;
import org.joml.Vector2f;
import org.joml.Vector4f;

public class ImEditorGui {
    private static final float defaultWidth = 70.0f;

    private static final Vector2f tmpPixelVector = new Vector2f();

    public static boolean dragVec2PixelToWorld(String label, Vector2f source, float resetVal, Object caller) {
        String id = createID(label, caller);
        ImGui.pushID(id);

        if (!ImGui.beginTable("##Table_" + id, 2, ImGuiTableFlags.BordersInnerV | ImGuiTableFlags.SizingStretchProp, ImGui.getContentRegionAvailX())) {
            return false;
        }
        ImGui.tableSetupColumn("##label_" + label + id, ImGuiTableColumnFlags.WidthFixed, defaultWidth);
        ImGui.tableSetupColumn("##content_" + label + id, ImGuiTableColumnFlags.WidthStretch);
        ImGui.tableNextColumn();
        float labelSpace = ImGui.getContentRegionAvailX();
        ImGui.text(label);
        if (ImGui.isItemHovered() && labelSpace <= ImGui.calcTextSizeX(label)) {
            ImGui.beginTooltip();
            ImGui.text(label);
            ImGui.endTooltip();
        }
        ImGui.tableNextColumn();

        float resetWidth = ImGui.calcTextSizeX(" X ");
        float dragRemains = (ImGui.getContentRegionAvailX() - resetWidth) / 2.0f;
        boolean changed = false;
        WorldUnit.worldToPixel(source, tmpPixelVector);
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0, 0);

        ImGui.pushID("x");
        ImGui.pushStyleColor(ImGuiCol.Button, 0.7f, 0.2f, 0.2f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.8f, 0.3f, 0.3f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.7f, 0.2f, 0.2f, 1.0f);
        if (ImGui.button("X##Reset_X_" + id, resetWidth, 0.0f)) {
            tmpPixelVector.x = resetVal;
            changed = true;
        }
        ImGui.popStyleColor(3);
        ImGui.pushItemWidth(dragRemains);
        ImGui.sameLine();
        float[] valX = {tmpPixelVector.x};
        if (ImGui.dragFloat("##dragX", valX, 1.0f)) {
            tmpPixelVector.x = valX[0];
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
            tmpPixelVector.y = resetVal;
            changed = true;
        }
        ImGui.popStyleColor(3);
        ImGui.pushItemWidth(dragRemains);
        ImGui.sameLine();
        float[] valY = {tmpPixelVector.y};
        if (ImGui.dragFloat("##dragY", valY, 0.1f)) {
            tmpPixelVector.y = valY[0];
            changed = true;
        }
        ImGui.popItemWidth();
        ImGui.popID();

        if (changed) WorldUnit.pixelToWorld(tmpPixelVector, source);

        ImGui.popStyleVar();
        ImGui.endTable();
        ImGui.popID();

        return changed;
    }

    public static boolean dragVec2Ctrl(String label, Vector2f source, float resetVal, Object caller) {
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
        ImGui.text(label);
        if (ImGui.isItemHovered() && labelSpace <= ImGui.calcTextSizeX(label)) {
            ImGui.beginTooltip();
            ImGui.text(label);
            ImGui.endTooltip();
        }
        ImGui.tableNextColumn();

        float resetWidth = ImGui.calcTextSizeX(" X ");
        float dragRemains = (ImGui.getContentRegionAvailX() - resetWidth) / 2.0f;
        boolean changed = false;
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0, 0);

        ImGui.pushID("x");
        ImGui.pushStyleColor(ImGuiCol.Button, 0.7f, 0.2f, 0.2f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.8f, 0.3f, 0.3f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.7f, 0.2f, 0.2f, 1.0f);
        if (ImGui.button("X##Reset_X_" + id, resetWidth, 0.0f)) {
            out.x = resetVal;
            changed = true;
        }
        ImGui.popStyleColor(3);
        ImGui.pushItemWidth(dragRemains);
        ImGui.sameLine();
        float[] valX = {out.x};
        if (ImGui.dragFloat("##dragX", valX, 1.0f)) {
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
            out.y = resetVal;
            changed = true;
        }
        ImGui.popStyleColor(3);
        ImGui.pushItemWidth(dragRemains);
        ImGui.sameLine();
        float[] valY = {out.y};
        if (ImGui.dragFloat("##dragY", valY, 0.1f)) {
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
        ImGui.text(label);
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
        if (ImGui.button("Reset##Reset_" + id)) val = 0.0f;
        ImGui.popStyleColor(3);
        ImGui.sameLine();

        ImGui.pushItemWidth(ImGui.getContentRegionAvailX());
        boolean changed = ImGui.dragFloat("##dragFloat", valA, 1.0f);
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
        ImGui.text(label);
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
        ImGui.text(label);
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
        ImGui.text(label);
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
        ImGui.text(label);
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

    private static String createID(String label, Object caller) {
        if (caller == null) return label;

        if (caller instanceof String s) return label + "__" + s;

        return label + "__" + System.identityHashCode(caller);
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

        ImVec2 cursorPos = ImGui.getCursorPos();
        boolean interact = ImGui.selectable("##" + id + "_Selectable_Icon", selected, width, height);
        if (ImGui.isItemHovered() && toolTip != null) {
            ImGui.beginTooltip();
            ImGui.text(toolTip);
            ImGui.endTooltip();
        }
        ImGui.setCursorPos(cursorPos);

        int textureID = icon.getTextureID();
        Vector2f[] textureCoordinates = icon.getTextureCoordinates();
        ImGui.image(textureID, width, height,
                textureCoordinates[2].x, textureCoordinates[0].y,
                textureCoordinates[0].x, textureCoordinates[2].y
        );

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
        ImGui.setCursorPosY(ImGui.getCursorPosY() + ImGui.getStyle().getItemSpacingY());
    }
}
