package editor;

import TCB_Field.GameObject;
import TCB_Field.KeyListener;
import TCB_Field.Prefab;
import components.MouseCtrl;
import components.Sprite;
import components.SpriteSheet;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;
import imgui.type.ImString;
import org.joml.Math;
import org.joml.Vector2f;
import org.joml.Vector4f;
import utility.AssetsPool;

import static org.lwjgl.glfw.GLFW.*;

public class ImEditorGui {
    private static float defaultWidth = 150.0f;
    public static void drawVec2Ctrl(String label, Vector2f val) {
        drawVec2Ctrl(label, val, 0.0f, defaultWidth);
    }

    public static void drawVec2Ctrl(String label, Vector2f val, float resetVal) {
        drawVec2Ctrl(label, val, resetVal, defaultWidth);
    }

    public static void drawVec2Ctrl(String label, Vector2f val, float resetVal, float columnWidth) {
        ImGui.pushID(label);

        ImGui.columns(2);
        ImGui.setColumnWidth(0, columnWidth);
        ImGui.text(label);
        ImGui.nextColumn();

        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0, 0);
        float lineHeight = ImGui.getFontSize() + ImGui.getStyle().getFramePaddingY() * 2.0f;
        Vector2f labelSize = new Vector2f(lineHeight + 3.0f, lineHeight);
        float remainWidth = (ImGui.calcItemWidth() - labelSize.x * 2.0f) / 2.0f;

        //=================== x button ===================
        ImGui.pushItemWidth(remainWidth);
        ImGui.pushStyleColor(ImGuiCol.Button, 0.7f, 0.2f, 0.2f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.8f, 0.3f, 0.3f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.7f, 0.2f, 0.2f, 1.0f);
        if (ImGui.button("x", labelSize.x, labelSize.y)) {
            val.x = resetVal;
        }
        ImGui.popStyleColor(3);
        ImGui.sameLine();
        float[] valX = {val.x};
        ImGui.dragFloat("##x", valX, 0.01f);
        ImGui.popItemWidth();
        ImGui.sameLine();
        //================================================

        //=================== y button ===================
        ImGui.pushItemWidth(remainWidth);
        ImGui.pushStyleColor(ImGuiCol.Button, 0.2f, 0.7f, 0.2f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.3f, 0.8f, 0.3f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.2f, 0.7f, 0.2f, 1.0f);
        if (ImGui.button("y", labelSize.x, labelSize.y)) {
            val.y = resetVal;
        }
        ImGui.popStyleColor(3);
        ImGui.sameLine();
        float[] valY = {val.y};
        ImGui.dragFloat("##y", valY, 0.01f);
        ImGui.popItemWidth();
        ImGui.sameLine();
        //================================================


        ImGui.nextColumn();

        // Update value here
        val.x = valX[0];
        val.y = valY[0];


        // End and reset
        ImGui.popStyleVar();
        ImGui.columns(1);
        ImGui.popID();
    }

    public static void spriteKeyTransform(String label, Vector2f val, float step) {
        ImGui.pushID(label);
        ImGui.newLine();
        ImGui.columns(2);
        ImGui.setColumnWidth(0, defaultWidth - 40f);
        ImGui.nextColumn();

        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0, 0);
        float lineHeight = ImGui.getFontSize() + ImGui.getStyle().getFramePaddingY() * 2.0f;
        Vector2f labelSize = new Vector2f(lineHeight + 2.0f, lineHeight);
        float remainWidth = (ImGui.calcItemWidth() - labelSize.x * 2.0f) / 2.0f;

        //=================== up ===================
        ImGui.nextColumn();
        ImGui.nextColumn();
        ImGui.pushItemWidth(remainWidth);
        ImGui.pushStyleColor(ImGuiCol.Button, 0.2f, 0.7f, 0.2f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.3f, 0.8f, 0.3f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.2f, 0.7f, 0.2f, 1.0f);
        ImGui.invisibleButton("empty", 90.0f, labelSize.y);
        ImGui.sameLine();

        if (ImGui.button("  Up  ", 80.0f, labelSize.y) || KeyListener.isKeyTapped(GLFW_KEY_UP)) {
            val.y += step;
        }

        ImGui.sameLine();
        ImGui.invisibleButton("empty", 90.0f, labelSize.y);
        ImGui.popStyleColor(3);
        ImGui.popItemWidth();
        ImGui.newLine();
        //================================================

        //=================== Left central Right ===================
        ImGui.nextColumn();
        ImGui.text(label);
        ImGui.nextColumn();
        ImGui.pushItemWidth(remainWidth);
        ImGui.pushStyleColor(ImGuiCol.Button, 0.7f, 0.2f, 0.2f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.8f, 0.3f, 0.3f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.7f, 0.2f, 0.2f, 1.0f);

        if (ImGui.button(" Left ", 80.0f, labelSize.y) || KeyListener.isKeyTapped(GLFW_KEY_LEFT)) {
            val.x -= step;
        }
        ImGui.popStyleColor(3);

        ImGui.sameLine();
        ImGui.invisibleButton("empty", 10.0f, labelSize.y);
        ImGui.sameLine();

        ImGui.pushStyleColor(ImGuiCol.Button, 0.6f, 0.25f, 0.0f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.75f, 0.31f, 0.0f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.6f, 0.25f, 0.0f, 1.0f);

        if (ImGui.button("Nearest", 80.0f, labelSize.y) || KeyListener.isKeyTapped(GLFW_KEY_N)) {
            float epsilon = 0.0001f;
            while (Math.abs(val.x % 0.32f) > epsilon) {
                float offsetX = val.x % 0.32f;
                if (offsetX != 0 && Math.abs(offsetX) >= 0.16f) {
                    val.x += offsetX;
                } else if (offsetX != 0 && Math.abs(offsetX) < 0.16f) {
                    val.x -= offsetX;
                } else {
                    break;
                }
            }

            while (Math.abs(val.y % 0.32f) > epsilon) {
                float offsetY = val.y % 0.32f;
                if (offsetY != 0 && Math.abs(offsetY) >= 0.16f) {
                    val.y += offsetY;
                } else if (offsetY != 0 && Math.abs(offsetY) < 0.16f) {
                    val.y -= offsetY;
                } else {
                    break;
                }
            }

        }
        ImGui.popStyleColor(3);

        ImGui.sameLine();
        ImGui.invisibleButton("empty", 10.0f, labelSize.y);
        ImGui.sameLine();

        ImGui.pushStyleColor(ImGuiCol.Button, 0.7f, 0.2f, 0.2f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.8f, 0.3f, 0.3f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.7f, 0.2f, 0.2f, 1.0f);

        if (ImGui.button(" Right ", 80.0f, labelSize.y) || KeyListener.isKeyTapped(GLFW_KEY_RIGHT)) {
            val.x += step;
        }

        ImGui.popStyleColor(3);
        ImGui.popItemWidth();
        ImGui.newLine();
        //================================================

        //=================== down ===================
        ImGui.nextColumn();
        ImGui.nextColumn();
        ImGui.pushItemWidth(remainWidth);
        ImGui.pushStyleColor(ImGuiCol.Button, 0.2f, 0.7f, 0.2f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.3f, 0.8f, 0.3f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.2f, 0.7f, 0.2f, 1.0f);
        ImGui.invisibleButton("empty", 90.0f, labelSize.y);
        ImGui.sameLine();
        if (ImGui.button(" Down ", 80.0f, labelSize.y) || KeyListener.isKeyTapped(GLFW_KEY_DOWN)) {
            val.y -= step;
        }

        ImGui.sameLine();
        ImGui.invisibleButton("empty", 90.0f, labelSize.y);
        ImGui.popStyleColor(3);
        ImGui.popItemWidth();


        ImGui.nextColumn();



        // End and reset
        ImGui.popStyleVar();
        ImGui.columns(1);
        ImGui.popID();
        ImGui.newLine();
    }

    public static float dragFloatCtrl(String label, float val) {
        ImGui.pushID(label);

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
        float[] valA = {val};
        float reset = 0.0f;
        if (ImGui.button("Reset", labelSize.x, labelSize.y)) {
            valA[0] = 0.0f;
        }
        ImGui.popStyleColor(3);
        ImGui.sameLine();
        ImGui.dragFloat("##dragFloat", valA, 0.01f);
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

    public static int dragIntCtrl(String label, int val) {
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

    public static boolean colorCtrl(String label, Vector4f val) {
        boolean result = false;
        ImGui.pushID(label);

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

    public static void drawSpriteList (SpriteSheet spriteSps, GameObject levelEditorObject, ImVec2 winPos, ImVec2 winSize) {
        ImVec2 objectSpace = new ImVec2();
        ImGui.getStyle().getItemSpacing(objectSpace);

        float windowX2 = winPos.x + winSize.x;
        for (int i = 0; i < spriteSps.size(); i++) {
            Sprite sprites = spriteSps.spriteIndex(i);
            float spriteWidth = sprites.loadWidth() * 3;
            float spriteHeight = sprites.loadHeight() * 3;
            int id = sprites.loadTexId();

            Vector2f[] texCoord = sprites.loadTexCrd();

            ImGui.pushID(i);

            if (ImGui.imageButton(id, spriteWidth, spriteHeight,
                    texCoord[2].x, texCoord[0].y,
                    texCoord[0].x, texCoord[2].y
            )
            ) {
                GameObject obj = Prefab.genSpsObj(sprites, 0.32f, 0.32f);

                // Bind to mouse cursor
                levelEditorObject.getComponent(MouseCtrl.class).pickObj(obj);
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

    public static void drawSpriteList (String spritePath, GameObject levelEditorObject, ImVec2 winPos, ImVec2 winSize, int widthMod) {
        ImVec2 objectSpace = new ImVec2();
        ImGui.getStyle().getItemSpacing(objectSpace);

        SpriteSheet spriteSps = AssetsPool.loadSpSheet(spritePath);


        float windowX2 = winPos.x + (winSize.x / widthMod);
        for (int i = 0; i < spriteSps.size(); i++) {
            Sprite sprites = spriteSps.spriteIndex(i);

            /*
                Ratio in this context is how much smaller (for ratio >= 1) the image is compare to allowed space for object button
                If width < height => height should be scale to match allowed space. (height is currently closer to target)
                    width will use same ratio as height to maintain image aspect ratio.

                Ratio in this context is how much bigger (for ratio < 1) the image is compare to allowed space for object button
                If width < height => width should be scale to match allowed space (width is currently closer to target)
                    height will use same ratio as height to maintain image aspect ratio.

                Keep scaling to minimum for compact design and fast calculation
            */
            float ratioX = 48.0f / sprites.loadWidth();
            float ratioY = 48.0f /  sprites.loadWidth();
            if (ratioX >= 1.0f && ratioY >= 1.0f) {
                if (ratioX < ratioY) {
                    ratioX = ratioY;
                } else if (ratioX >= ratioY) {
                    ratioY = ratioX;
                }
            } else if (ratioX < 1.0f && ratioY < 1.0f) {
                if (ratioX >= ratioY) {
                    ratioX = ratioY;
                } else if (ratioX < ratioY) {
                    ratioY = ratioX;
                }
            }


            float spriteWidth = sprites.loadWidth() * ratioX;
            float spriteHeight = sprites.loadHeight() * ratioY;
            int id = sprites.loadTexId();

            Vector2f[] texCoord = sprites.loadTexCrd();

            ImGui.pushID(i);

            if (ImGui.imageButton(id, spriteWidth, spriteHeight,
                    texCoord[2].x, texCoord[0].y,
                    texCoord[0].x, texCoord[2].y
            )
            ) {
                float rX = 32.0f / sprites.loadWidth();
                float rY = 32.0f /  sprites.loadWidth();
                if (rX >= 1.0f && rY >= 1.0f) {
                    if (rX < rY) {
                        rX = rY;
                    } else if (rX >= rY) {
                        rY = rX;
                    }
                } else if (rX < 1.0f && rY < 1.0f) {
                    if (rX >= ratioY) {
                        rX = rY;
                    } else if (rX < rY) {
                        rY = rX;
                    }
                }
                GameObject obj = Prefab.genSpsObj(sprites, (sprites.loadWidth() / 100.0f * rX) , (sprites.loadHeight() / 100.0f * rY));

                // Bind to mouse cursor
                levelEditorObject.getComponent(MouseCtrl.class).pickObj(obj);
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

    public static String nameCtrl(String label, String val) {
        ImGui.pushID(label);

        ImGui.columns(2);
        ImGui.setColumnWidth(0, defaultWidth);
        ImGui.text(label);
        ImGui.nextColumn();

        ImString string = new ImString(val, 256);
        if (ImGui.inputText("##" + label, string)) {
            ImGui.columns(1);
            ImGui.popID();

            return string.get();
        }

        // End and reset
        ImGui.columns(1);
        ImGui.popID();

        return val;
    }
}
