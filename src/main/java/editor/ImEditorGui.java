package editor;

import TheCellBeyond.GameObject;
import TheCellBeyond.KeyListener;
import utility.*;
import components.MouseCtrl;
import render.texture.Sprite;
import render.texture.SpriteSheet;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiStyleVar;
import imgui.type.ImString;
import org.joml.Math;
import org.joml.Vector2f;
import org.joml.Vector4f;
import utility.prefabrication.Prefab;

import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

public class ImEditorGui {
    private static final float defaultWidth = 180.0f;

    private static final Vector2f tmpPixelVector = new Vector2f();
    private static final Vector2f tmpWorldVector = new Vector2f();

    public static void drawVec2Ctrl(String label, Vector2f val, Object caller) {
        drawVec2Ctrl(label, val, 0.0f, defaultWidth, caller);
    }

    public static void drawVec2Ctrl(String label, Vector2f val, float resetVal, Object caller) {
        drawVec2Ctrl(label, val, resetVal, defaultWidth, caller);
    }

    public static void drawVec2Ctrl(String label, Vector2f source, float resetVal, float columnWidth, Object caller) {
        String id = createID(label, caller);
        ImGui.pushID(id);

        ImGui.columns(2);
        ImGui.setColumnWidth(0, columnWidth);
        ImGui.text(label);
        ImGui.nextColumn();

        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0, 0);
        float lineHeight = ImGui.getFontSize() + ImGui.getStyle().getFramePaddingY() * 2.0f;
        Vector2f labelSize = new Vector2f(lineHeight + 3.0f, lineHeight);
        float remainWidth = (ImGui.calcItemWidth() - labelSize.x * 2.0f) / 2.0f;

        WorldUnit.worldToPixel(source, tmpPixelVector);
        boolean updated = false;

        //=================== x button ===================
        ImGui.pushItemWidth(remainWidth);
        ImGui.pushStyleColor(ImGuiCol.Button, 0.7f, 0.2f, 0.2f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.8f, 0.3f, 0.3f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.7f, 0.2f, 0.2f, 1.0f);
        if (ImGui.button("x", labelSize.x, labelSize.y)) {
            tmpPixelVector.x = resetVal;
            updated = true;
        }
        ImGui.popStyleColor(3);
        ImGui.sameLine();
        float[] valX = {tmpPixelVector.x};
        if (ImGui.dragFloat("##x", valX, 0.1f)) {
            tmpPixelVector.x = valX[0];
            updated = true;
        }
        ImGui.popItemWidth();
        ImGui.sameLine();
        //================================================

        //=================== y button ===================
        ImGui.pushItemWidth(remainWidth);
        ImGui.pushStyleColor(ImGuiCol.Button, 0.2f, 0.7f, 0.2f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.3f, 0.8f, 0.3f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.2f, 0.7f, 0.2f, 1.0f);
        if (ImGui.button("y", labelSize.x, labelSize.y)) {
            tmpPixelVector.y = resetVal;
            updated = true;
        }
        ImGui.popStyleColor(3);
        ImGui.sameLine();
        float[] valY = {tmpPixelVector.y};
        if (ImGui.dragFloat("##y", valY, 0.1f)) {
            tmpPixelVector.y = valY[0];
            updated = true;
        }

        ImGui.popItemWidth();
        ImGui.sameLine();
        //================================================
        if (updated) WorldUnit.pixelToWorld(tmpPixelVector, source);

        ImGui.nextColumn();
        
        // End and reset
        ImGui.popStyleVar();
        ImGui.columns(1);
        ImGui.popID();
    }

    public static float dragFloatCtrl(String label, float val, Object caller) {
        String id = createID(label, caller);
        ImGui.pushID(id);

        ImGui.columns(2);
        ImGui.setColumnWidth(0, defaultWidth);
        ImGui.text(label);
        ImGui.nextColumn();

        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0, 0);
        float lineHeight = ImGui.getFontSize() + ImGui.getStyle().getFramePaddingY() * 2.0f;
        Vector2f labelSize = new Vector2f(lineHeight * 2.4f, lineHeight);
        float remainWidth = (ImGui.calcItemWidth() - labelSize.x);

        //=================== reset button ===================
        ImGui.pushItemWidth(remainWidth);
        ImGui.pushStyleColor(ImGuiCol.Button, 0.7f, 0.2f, 0.2f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.8f, 0.3f, 0.3f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.7f, 0.2f, 0.2f, 1.0f);
        if (ImGui.button("Reset", labelSize.x, labelSize.y)) {
            val = 0.0f;
        }
        ImGui.popStyleColor(3);
        ImGui.sameLine();
        float[] valA = {val};
        ImGui.dragFloat("##dragFloat", valA, 1.0f);
        ImGui.popItemWidth();
        ImGui.sameLine();
        //================================================

        ImGui.nextColumn();

        // End and reset
        ImGui.popStyleVar();
        ImGui.columns(1);
        ImGui.popID();

        return valA[0];
    }

    public static int dragIntCtrl(String label, int val, Object caller) {
        String id = createID(label, caller);
        ImGui.pushID(label);

        ImGui.columns(2);
        ImGui.setColumnWidth(0, defaultWidth);
        ImGui.text(label);
        ImGui.nextColumn();

        int[] valA = {val};
        ImGui.dragInt("##dragInt", valA, 1);

        // End and reset
        ImGui.columns(1);
        ImGui.popID();

        return valA[0];
    }

    public static boolean colorCtrl(String label, Vector4f val, Object caller) {
        String id = createID(label, caller);
        boolean result = false;
        ImGui.pushID(id);

        ImGui.columns(2);
        ImGui.setColumnWidth(0, defaultWidth);
        ImGui.text(label);
        ImGui.nextColumn();

        float[] color = {val.x, val.y, val.z, val.w};
        if(ImGui.colorEdit4("##color", color)) {
            val.set(color[0], color[1], color[2], color[3]);
            result = true;
        }

        // End and reset
        ImGui.columns(1);
        ImGui.popID();

        return result;
    }

    public static String inputText(String label, String txt, Object caller) {
        String id = createID(label, caller);
        ImGui.pushID(id);

        ImGui.columns(2);
        ImGui.setColumnWidth(0, defaultWidth);
        ImGui.text(label);
        ImGui.nextColumn();

        ImString outString = new ImString(txt, 256);
        if (ImGui.inputText("##" + label, outString)) {
            ImGui.columns(1);
            ImGui.popID();

            return outString.get();
        }

        // End and reset
        ImGui.columns(1);
        ImGui.popID();

        return txt;
    }

    /*
    public static void drawSpriteList (List<SpriteSheet> sheets, GameObject levelEditorObject, ImVec2 winPos, ImVec2 winSize) {
        ImVec2 objectSpace = new ImVec2();
        ImGui.getStyle().getItemSpacing(objectSpace);

        float windowX2 = winPos.x + winSize.x;

        for (SpriteSheet sprites : sheets) {
            for (int i = 0; i < sprites.size(); i++) {
                Sprite sps = sprites.spriteIndex(i);

                Vector2f scaledSprite = TextureScale.calculateFitDimension(sps.getWidth(), sps.getHeight());

                float spriteWidth = scaledSprite.x;
                float spriteHeight = scaledSprite.y;
                int id = sps.getTextureID();

                Vector2f[] texCoord = sps.getTextureCoordinates();

                ImGui.pushID(i);

                String strId = Integer.toString(id);
                ImGui.imageButton(strId, id, spriteWidth, spriteHeight,
                        texCoord[2].x, texCoord[0].y,
                        texCoord[0].x, texCoord[2].y
                );

                if (ImGui.isItemClicked()) {
                    Vector2f spriteWorldSize = WorldUnit.pixelToWorld(new Vector2f(sps.getWidth(), sps.getHeight()));

                    GameObject obj = Prefab.genSpsObj(sps, spriteWorldSize.x, spriteWorldSize.y);

                    // Bind to mouse cursor
                    levelEditorObject.getFirstComponent(MouseCtrl.class).pickObj(obj);
                }

                if (ImGui.isItemHovered()) {
                    ImGui.beginTooltip();

                    ImGui.text("Preview");
                    ImGui.image(id, spriteWidth * 2, spriteHeight * 2,
                            texCoord[2].x, texCoord[0].y,
                            texCoord[0].x, texCoord[2].y);
                    ImGui.text("Width: " + sps.getWidth());
                    ImGui.text("Height: " + sps.getHeight());

                    ImGui.endTooltip();
                }

                ImGui.popID();

                ImVec2 lastButtonPos = new ImVec2();
                ImGui.getItemRectMax(lastButtonPos);
                float lastButtonX2 = lastButtonPos.x;
                float nextButtonX2 = lastButtonX2 + objectSpace.x + spriteWidth;

                if (i + 1 < sprites.size() && nextButtonX2 < windowX2) {
                    ImGui.sameLine();
                }
            }
        }
    }

    public static void drawSpriteList (String spritePath, GameObject levelEditorObject, ImVec2 winPos, ImVec2 winSize, int widthMod) {
        ImVec2 objectSpace = new ImVec2();
        ImGui.getStyle().getItemSpacing(objectSpace);

        SpriteSheet spriteSps = AssetsPool.loadSpriteSheet(spritePath);


        float windowX2 = winPos.x + (winSize.x / widthMod);
        for (int i = 0; i < spriteSps.size(); i++) {
            Sprite sprites = spriteSps.spriteIndex(i);

            Vector2f scaledSprite = TextureScale.calculateFitDimension(sprites.getWidth(), sprites.getHeight());

            float spriteWidth = scaledSprite.x;
            float spriteHeight = scaledSprite.y;
            int id = sprites.getTextureID();

            Vector2f[] texCoord = sprites.getTextureCoordinates();

            ImGui.pushID(i);
            String strId = Integer.toString(id);
            ImGui.imageButton(strId, id, spriteWidth, spriteHeight,
                    texCoord[2].x, texCoord[0].y,
                    texCoord[0].x, texCoord[2].y
            );

            if (ImGui.isItemClicked()) {
                Vector2f spriteWorldSize = WorldUnit.pixelToWorld(new Vector2f(sprites.getWidth(), sprites.getHeight()));

                GameObject obj = Prefab.genSpsObj(sprites, spriteWorldSize.x, spriteWorldSize.y);

                // Bind to mouse cursor
                levelEditorObject.getFirstComponent(MouseCtrl.class).pickObj(obj);
            }

            if (ImGui.isItemHovered()) {
                ImGui.beginTooltip();

                ImGui.text("Preview");
                ImGui.image(id, spriteWidth * 2, spriteHeight * 2,
                        texCoord[2].x, texCoord[0].y,
                        texCoord[0].x, texCoord[2].y);
                ImGui.text("Width: " + sprites.getWidth());
                ImGui.text("Height: " + sprites.getHeight());

                ImGui.endTooltip();
            }

            ImGui.popID();

            ImVec2 lastButtonPos = new ImVec2();
            ImGui.getItemRectMax(lastButtonPos);
            float lastButtonX2 = lastButtonPos.x;
            float nextButtonX2 = lastButtonX2 + objectSpace.x + spriteWidth;

            if (i + 1 < spriteSps.size() && nextButtonX2 < windowX2) {
                ImGui.sameLine();
            }
        }
    }

    public static void drawSpriteList (String[] spritePath, GameObject levelEditorObject, ImVec2 winPos, ImVec2 winSize) {
        ImGui.newLine();
        ImGui.columns(spritePath.length);

        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0, 0);
        float remainWidth = (ImGui.calcItemWidth() / spritePath.length);

        for (int i = 0; i < spritePath.length; i++) {
            ImGui.setColumnWidth(i, (winSize.x) / spritePath.length);
            ImGui.pushItemWidth(remainWidth);
            drawSpriteList(spritePath[i], levelEditorObject, winPos, winSize, spritePath.length);
            ImGui.popItemWidth();

            ImGui.sameLine();
            ImGui.nextColumn();
        }

        ImGui.popStyleVar();
        ImGui.columns(1);
    }
     */

    public static String inputTextWithIME(String label, String txt, int bufferSize, Object caller) {
        String id = createID(label, caller);
        ImString out = new ImString(txt, bufferSize);

        int flags = ImGuiInputTextFlags.CallbackResize
                | ImGuiInputTextFlags.CallbackHistory
                | ImGuiInputTextFlags.CallbackCompletion
                | ImGuiInputTextFlags.CallbackCharFilter;

        ImGui.pushID(id);
        ImGui.columns(2);
        ImGui.setColumnWidth(0,defaultWidth);
        ImGui.text(label);
        ImGui.nextColumn();

        if (KeyListener.hasTextInput()) {
            String IMEInput = KeyListener.getTextInput();
            if (!IMEInput.isEmpty()) {
                out.set(out.get() + IMEInput);
            }
        }

        boolean changed = ImGui.inputText("##" + label, out, flags);

        ImGui.columns(1);
        ImGui.popID();

        return changed ? out.get() : txt;
    }

    private static String createID(String label, Object caller) {
        if (caller == null) return label;

        if (caller instanceof String s) return label + "__" + s;

        return label + "__" + System.identityHashCode(caller);
    }
}
