package editor;

import components.AnimatedSpriteRenderer;
import components.Animation;
import components.Frame;
import editor.payload.SpriteDragDropPayload;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;
import imgui.type.ImBoolean;
import imgui.type.ImFloat;
import imgui.type.ImString;
import org.joml.Vector2f;
import render.texture.Sprite;
import utility.TextureScale;

import java.util.*;

import static org.lwjgl.glfw.GLFW.*;

public class SpriteFrameEditor {
    private static final String CONTROL_SECTION = "##Sprite frame controls";
    private static final float PREVIEW_FRAME_SIZE = 80.0f;
    private static AnimatedSpriteRenderer editingAnimatedSprite;
    private static String selectedName;

    private static String editingName;
    private static final ImString editingNameBuffer = new ImString(512);

    private static final float animationListXPercentage = 0.25f;
    private static final float CONTROL_RESERVE = ImGui.getFrameHeightWithSpacing();
    private static final float padding = 4.0f;

    public static void edit(AnimatedSpriteRenderer animatedSprite) {
        if (animatedSprite != editingAnimatedSprite) {
            clearDialogData();
            editingAnimatedSprite = animatedSprite;
        }
    }

    static void imgui() {
        if (editingAnimatedSprite == null) {
            ImGui.textWrapped("Select an Animated Sprite Renderer component from Inspector panel to start editing its details");
            return;
        }
        if (editingAnimatedSprite.gameObject == null || editingAnimatedSprite.gameObject.isRemoved() || editingAnimatedSprite.getUUID() == null) {
            clearDialogData();
            return;
        }

        selectedName = editingAnimatedSprite.currentAnimationName();

        if (!ImGui.beginChild(CONTROL_SECTION, 0, CONTROL_RESERVE + padding, false)) return;

        if (ImGui.button("Add")) editingAnimatedSprite.newAnimation();
        ImGui.sameLine();
        if (selectedName != null) {
            if (ImGui.button("Duplicate")) {
                editingAnimatedSprite.duplicateAnimation(selectedName);
            }
            ImGui.sameLine();
            if (ImGui.button("Delete")) {
                editingAnimatedSprite.removeAnimation(selectedName);
                if (Objects.equals(editingName, selectedName)) {
                    editingName = null;
                    editingNameBuffer.clear();
                }
                selectedName = null;
            }

            ImGui.sameLine();
            ImBoolean enableLoop = new ImBoolean(editingAnimatedSprite.isAnimationLoop(selectedName));
            if (ImGui.checkbox("Loop", enableLoop)) editingAnimatedSprite.setAnimationLoop(enableLoop.get(), selectedName);

            ImGui.sameLine();
            ImFloat fps = new ImFloat(editingAnimatedSprite.getAnimationFPS(selectedName));
            if (ImGui.inputFloat("FPS", fps)) editingAnimatedSprite.setFPS(fps.get(), selectedName);

            ImGui.sameLine();
            if (ImGui.button("Play")) editingAnimatedSprite.play(selectedName);

            ImGui.sameLine();
            if (ImGui.button("Stop")) editingAnimatedSprite.stop();


        } else {
            ImGui.beginDisabled();
            ImGui.button("Duplicate");
            ImGui.sameLine();
            ImGui.button("Delete");
            ImGui.endDisabled();

        }

        ImGui.endChild();

        if (!ImGui.beginTable("##SFE_Table_Id", 2, ImGuiTableFlags.NoBordersInBody | ImGuiTableFlags.SizingStretchProp, ImGui.getContentRegionAvail())) return;

        float remainWidth = Math.max(120.0f, ImGui.getContentRegionAvailX() * animationListXPercentage);
        ImGui.tableSetupColumn("##AnimationList_Column", ImGuiTableColumnFlags.WidthFixed, remainWidth);
        ImGui.tableSetupColumn("##AnimationFrames_Column", ImGuiTableColumnFlags.WidthStretch);

        ImGui.tableNextColumn();
        if (ImGui.beginChild("##AnimationList", ImGui.getContentRegionAvail(), true)) {
            renderAnimationList();
            ImGui.endChild();
        }

        ImGui.tableNextColumn();
        if (ImGui.beginChild("##AnimationFrames", ImGui.getContentRegionAvail(), true)) {
            if (selectedName == null) {
                ImGui.beginDisabled();
                ImGui.textWrapped("Select an animation on the left panel or create a new animation to start editing its frames");
                ImGui.endDisabled();
            } else {
                ImGui.beginDisabled();
                ImGui.textWrapped("Drag and drop Sprites here to form a frame sequence");
                ImGui.endDisabled();
                renderFrameList();
            }

            ImGui.endChild();
        }
        spriteDragDropPayload();

        ImGui.endTable();
    }

    private static void renderAnimationList() {
        HashMap<String, Animation> animations = editingAnimatedSprite.animations();

        for (Map.Entry<String, Animation> entry : animations.entrySet()) {
            boolean isSelected = Objects.equals(selectedName, entry.getKey());
            ImVec2 cursorPos = ImGui.getCursorPos();
            float height = ImGui.getTextLineHeight() + ImGui.getStyle().getFramePaddingY() * 2;
            ImGui.setNextItemAllowOverlap();
            if (ImGui.selectable("##" + entry.getKey(), isSelected, 0.0f, height)) {
                selectedName = entry.getKey();
                editingAnimatedSprite.setCurrentAnimation(entry.getKey());
            }

            if (Objects.equals(editingName, entry.getKey())) {
                ImGui.setCursorPos(cursorPos);
                ImGui.setKeyboardFocusHere();
                ImGui.inputText("##" + "Edit_" + entry.getKey(), editingNameBuffer);
                if (ImGui.isItemFocused() && ImGui.isKeyPressed(GLFW_KEY_ESCAPE)) {
                    editingNameBuffer.clear();
                    editingName = null;
                    ImGui.spacing();
                    continue;
                }

                if ((ImGui.isItemFocused() && ImGui.isKeyPressed(GLFW_KEY_ENTER)) || !isSelected) {
                    editingAnimatedSprite.renameAnimation(entry.getKey(), editingNameBuffer.get());
                    selectedName = editingNameBuffer.get();
                    editingNameBuffer.clear();
                    editingName = null;
                    ImGui.spacing();
                    continue;
                }

                ImGui.spacing();
                continue;
            }

            if (ImGui.isItemHovered() && ImGui.isMouseDoubleClicked(GLFW_MOUSE_BUTTON_1)) {
                editingName = entry.getKey();
                editingNameBuffer.set(editingName);
                ImGui.spacing();
                continue;
            }

            ImGui.setCursorPos(cursorPos.x, cursorPos.y + (height - ImGui.getTextLineHeight()) / 2.0f);
            ImGui.text(entry.getKey());
            ImGui.spacing();
        }
    }

    private static void renderFrameList() {
        if (selectedName == null) return;
        Animation animation = editingAnimatedSprite.currentAnimation();
        if (animation == null) return;
        List<Frame> frames = animation.frames();
        if (frames.isEmpty()) return;

        float spacing = ImGui.getStyle().getItemSpacingX();
        float availWidth = ImGui.getContentRegionAvailX();
        float consumedWidth = 0.0f;
        for (int i = 0; i < frames.size(); i++) {
            Sprite sprite = frames.get(i).sprite;
            if (sprite == null) continue;
            int textureID = sprite.getTextureID();
            Vector2f scaledSpriteSize = TextureScale.calculateFitSquare(sprite.getWidth(), sprite.getHeight(), PREVIEW_FRAME_SIZE);
            Vector2f[] textureCoordinates = sprite.getTextureCoordinates();

            String compositeId = selectedName + "_frame_" + i;
            ImGui.pushID(compositeId);

            ImGui.imageButton(compositeId, textureID, scaledSpriteSize.x, scaledSpriteSize.y,
                    textureCoordinates[2].x, textureCoordinates[0].y,
                    textureCoordinates[0].x, textureCoordinates[2].y
            );
            float buttonWidth = ImGui.getItemRectSizeX() + spacing;
            consumedWidth += buttonWidth;

            ImGui.popID();
            if (consumedWidth + buttonWidth <= availWidth) {
                ImGui.sameLine();
                continue;
            }

            consumedWidth = 0.0f;
            ImGui.spacing();
        }
    }

    private static void spriteDragDropPayload() {
        if (ImGui.beginDragDropTarget()) {
            Animation currentAnimation = editingAnimatedSprite.currentAnimation();
            if (currentAnimation == null) {
                ImGui.endDragDropTarget();
                return;
            }

            if (ImGui.isWindowHovered()) {
                ImDrawList drawList = ImGui.getWindowDrawList();
                ImVec2 min = ImGui.getItemRectMin();
                ImVec2 max = ImGui.getItemRectMax();
                drawList.addRect(min, max, ImGui.colorConvertFloat4ToU32(0.2f, 0.7f, 0.2f, 0.8f), 0 , 0 , 2);
            }

            Object payLoad = ImGui.acceptDragDropPayload(SpriteDragDropPayload.getPayloadType());

            if (payLoad == null) {
                ImGui.endDragDropTarget();
                return;
            }

            Sprite dropSprite = SpriteDragDropPayload.getPayload();
            if (dropSprite != null) {
                currentAnimation.addFrame(dropSprite, 1.0f);
                float fps = editingAnimatedSprite.getAnimationFPS(selectedName);
                editingAnimatedSprite.setFPS(fps, selectedName);
            }

            ImGui.endDragDropTarget();
        }
    }

    static void clearDialogData() {
        editingName = null;
        editingNameBuffer.clear();
        editingAnimatedSprite = null;
        selectedName = null;
    }
}
