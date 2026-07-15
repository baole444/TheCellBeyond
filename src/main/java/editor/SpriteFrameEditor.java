package editor;

import components.AnimatedSpriteRenderer;
import components.Animation;
import components.Frame;
import editor.payload.SpriteDragDropPayload;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;
import imgui.type.ImBoolean;
import imgui.type.ImFloat;
import imgui.type.ImString;
import org.joml.Vector2f;
import render.texture.Sprite;
import utility.TextureScale;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class SpriteFrameEditor {
    private static final String ControlSection = "##Sprite frame controls";
    private static final float PreviewFrameSize = 96.0f;
    private static final float InputWidth = ImGui.calcTextSizeX("999.999");
    private static AnimatedSpriteRenderer editingAnimatedSprite;
    private static String selectedName;
    private static int selectedFrame;

    private static String editingName;
    private static final ImString EditingNameBuffer = new ImString(512);

    private static final float animationListXPercentage = 0.25f;
    private static final float animationListWidth = 120.0f;
    private static final float ControlReserve = ImGui.getFrameHeightWithSpacing();
    private static final float padding = 4.0f;

    static void edit(AnimatedSpriteRenderer animatedSprite) {
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
        if (editingAnimatedSprite.isDestroyed()) {
            clearDialogData();
            return;
        }

        String current = editingAnimatedSprite.currentAnimationName();
        if (!Objects.equals(selectedName, current)) {
            selectedName = current;
            selectedFrame = 0;
        }

        if (!ImGui.beginChild(ControlSection, 0, ControlReserve + padding, false)) {
            ImGui.endChild();
            return;
        }
        renderAnimationControl();
        ImGui.endChild();
        if (!ImGui.beginTable("##SFE_Table_Id", 2, ImGuiTableFlags.NoBordersInBody | ImGuiTableFlags.SizingStretchProp, ImGui.getContentRegionAvail())) return;
        float remainWidth = Math.max(animationListWidth, ImGui.getContentRegionAvailX() * animationListXPercentage);
        ImGui.tableSetupColumn("##SFE_AnimationList_Column", ImGuiTableColumnFlags.WidthFixed, remainWidth);
        ImGui.tableSetupColumn("##SFE_AnimationFrames_Column", ImGuiTableColumnFlags.WidthStretch);
        ImGui.tableNextColumn();
        if (ImGui.beginChild("##SFE_AnimationList", ImGui.getContentRegionAvail(), true)) {
            renderAnimationList();
            ImGui.endChild();
        }
        ImGui.tableNextColumn();
        if (ImGui.beginChild("##SFE_AnimationFrames", ImGui.getContentRegionAvail(), true)) {
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
        }
        ImGui.endChild();
        spriteDragDropPayload();
        ImGui.endTable();
    }

    private static void renderAnimationControl() {
        if (selectedName == null) {
            if (EditorWidget.iconButton("Add##Add_New_Animation_SFC", EditorIcons.Icons.New, "Create new animation")) selectedName = editingAnimatedSprite.newAnimation();
            return;
        }
        if (!ImGui.beginTable("##Animation_Frame_Control_SFC", 5, ImGuiTableFlags.BordersInnerV | ImGuiTableFlags.SizingFixedFit)) {
            ImGui.textDisabled("Control function failed to initiate");
            return;
        }
        ImGui.tableSetupColumn("##NCD_Animation_Column_SFC");
        ImGui.tableSetupColumn("##Default_Animation_Column_SFC");
        ImGui.tableSetupColumn("##Playback_Animation_Column_SFC");
        ImGui.tableSetupColumn("##Frame_Animation_Column_SFC");
        ImGui.tableSetupColumn("##FrameTime_Animation_Column_SFC");
        ImGui.tableNextColumn();
        if (EditorWidget.iconButton("Add##Add_New_Animation_SFC", EditorIcons.Icons.New, "Create new animation")) editingAnimatedSprite.newAnimation();
        ImGui.sameLine();
        if (EditorWidget.iconButton("Duplicate##Duplicate_Animation_SFC", EditorIcons.Icons.Copy, "Duplicate selected animation")) editingAnimatedSprite.duplicateAnimation(selectedName);
        ImGui.sameLine();
        if (EditorWidget.iconButton("Delete##Delete_Animation_SFC", EditorIcons.Icons.Delete, "Delete selected animation")) {
            editingAnimatedSprite.removeAnimation(selectedName);
            if (Objects.equals(editingName, selectedName)) {
                editingName = null;
                EditingNameBuffer.clear();
            }
            selectedName = null;
            ImGui.endTable();
            return;
        }
        ImGui.tableNextColumn();
        boolean currentlyDefault = Objects.equals(editingAnimatedSprite.defaultAnimation(), selectedName);
        ImBoolean setAsDefault = new ImBoolean(currentlyDefault);
        if (ImGui.checkbox("Default", setAsDefault)) {
            boolean setVal = setAsDefault.get();
            if (setVal) editingAnimatedSprite.setDefaultAnimation(selectedName);
            else editingAnimatedSprite.setDefaultAnimation(null);
        }
        ImGui.tableNextColumn();
        ImBoolean enableLoop = new ImBoolean(editingAnimatedSprite.isAnimationLoop(selectedName));
        if (ImGui.checkbox("Loop", enableLoop)) editingAnimatedSprite.setAnimationLoop(enableLoop.get(), selectedName);
        ImGui.sameLine();
        ImFloat fps = new ImFloat(editingAnimatedSprite.getAnimationFPS(selectedName));
        ImGui.pushItemWidth(InputWidth);
        if (ImGui.inputFloat("FPS", fps, 0.0f, 0.0f, "%.2f")) editingAnimatedSprite.setFPS(fps.get(), selectedName);
        ImGui.popItemWidth();
        ImGui.sameLine();
        if (EditorWidget.iconButton("Step Back##Step_Back_Frame_SFC", EditorIcons.SpriteFrameIcons.PreviousFrame, "Step animation back 1 frame")) {
            editingAnimatedSprite.pause();
            Animation current = editingAnimatedSprite.currentAnimation();
            if (current != null) {
                current.setCurrentFrameIndex(selectedFrame - 1);
                selectedFrame = current.currentFrameIndex();
            }
        }
        boolean play = editingAnimatedSprite.isPlaying();
        boolean backward = editingAnimatedSprite.isBackward();
        ImGui.sameLine();
        if (EditorWidget.iconButton("Play Backward##Play_Animation_Backward_SFC", EditorIcons.SpriteFrameIcons.PlayBackward, "Play/Resume the animation backward")) {
            if (play && !backward) editingAnimatedSprite.playBackward(selectedName);
            else if (!play) editingAnimatedSprite.resume();
        }
        ImGui.sameLine();
        if (play) if (EditorWidget.iconButton("Pause##Pause_Animation_SFC", EditorIcons.SpriteFrameIcons.Pause, "Pause the animation")) editingAnimatedSprite.pause();
        else if (EditorWidget.iconButton("Stop##Stop_Animation_SFC", EditorIcons.SpriteFrameIcons.Stop, "Stop the animation")) editingAnimatedSprite.stop();
        ImGui.sameLine();
        if (EditorWidget.iconButton("Play#Play_Animation_SFC", EditorIcons.SpriteFrameIcons.Play, "Play/Resume the animation")) {
            if (play && backward) editingAnimatedSprite.play(selectedName);
            else if (!play) editingAnimatedSprite.resume();
        }
        ImGui.sameLine();
        if (EditorWidget.iconButton("Step Forward##Step_Forward_Frame_SFC", EditorIcons.SpriteFrameIcons.NextFrame, "Step animation forward 1 frame")) {
            editingAnimatedSprite.pause();
            Animation animation = editingAnimatedSprite.currentAnimation();
            if (animation != null) {
                animation.setCurrentFrameIndex(selectedFrame + 1);
                selectedFrame = animation.currentFrameIndex();
            }
        }
        ImGui.tableNextColumn();
        if (EditorWidget.iconButton("Move Frame Left##Move_Frame_Left_SFC", EditorIcons.SpriteFrameIcons.MoveFrameLeft, "Move selected frame to the left of the current index")) {
            editingAnimatedSprite.stop();
            boolean moved = editingAnimatedSprite.moveFrameLeft(selectedName, selectedFrame);
            if (moved) selectedFrame--;
        }
        ImGui.sameLine();
        if (EditorWidget.iconButton("Move Frame Right##Move_Frame_Right_SFC", EditorIcons.SpriteFrameIcons.MoveFrameRight, "Move selected frame to the right of the current index")) {
            editingAnimatedSprite.stop();
            boolean moved = editingAnimatedSprite.moveFrameRight(selectedName, selectedFrame);
            if (moved) selectedFrame++;
        }
        ImGui.sameLine();
        if (EditorWidget.iconButton("Delete Frame##Delete_Frame_SFC", EditorIcons.Icons.Delete, "Delete the selected frame from the animation")) {
            editingAnimatedSprite.stop();
            editingAnimatedSprite.removeFrame(selectedName, selectedFrame);
            selectedFrame = 0;
        }
        ImGui.tableNextColumn();
        Animation animation = editingAnimatedSprite.currentAnimation();
        ImFloat speedMultiplier = new ImFloat(animation.speedMultiplier());
        ImGui.pushItemWidth(InputWidth);
        if (ImGui.inputFloat("Speed", speedMultiplier, 0.0f, 0.0f, "%.2f")) animation.setSpeed(speedMultiplier.get());
        ImGui.popItemWidth();
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
                if (!isSelected) {
                    selectedName = entry.getKey();
                    selectedFrame = 0;
                    editingAnimatedSprite.setCurrentAnimation(entry.getKey());
                }
            }
            if (renderAnimationNameEdit(entry.getKey(), cursorPos, isSelected)) continue;
            if (ImGui.isItemHovered() && ImGui.isMouseDoubleClicked(ImGuiMouseButton.Left)) {
                editingName = entry.getKey();
                EditingNameBuffer.set(editingName);
                ImGui.spacing();
                continue;
            }
            ImGui.setCursorPos(cursorPos.x, cursorPos.y + (height - ImGui.getTextLineHeight()) / 2.0f);
            if (Objects.equals(editingAnimatedSprite.defaultAnimation(), entry.getKey())) {
                ImGui.text("(Default)");
                ImGui.sameLine();
            }
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
        float textLineHeight = ImGui.getTextLineHeightWithSpacing();
        float consumedWidth = 0.0f;
        for (int i = 0; i < frames.size(); i++) {
            Sprite sprite = frames.get(i).sprite;
            if (sprite == null) continue;
            int textureID = sprite.getTextureID();
            Vector2f scaledSpriteSize = TextureScale.calculateFitDimension(sprite.getWidth(), sprite.getHeight(), PreviewFrameSize, PreviewFrameSize - textLineHeight);
            Vector2f[] textureCoordinates = sprite.getTextureCoordinates();
            String compositeId = selectedName + "_frame_" + i;
            ImGui.pushID(compositeId);
            ImVec2 cursorPos = ImGui.getCursorPos();
            boolean selected = selectedFrame == i;
            if (ImGui.selectable("##" + compositeId + "_selection", selected, scaledSpriteSize.x, scaledSpriteSize.y + textLineHeight)) {
                selectedFrame = i;
                if  (!editingAnimatedSprite.isPlaying()) editingAnimatedSprite.currentAnimation().setCurrentFrameIndex(i);
            }
            ImGui.setCursorPos(cursorPos);
            ImGui.beginGroup();
            ImGui.image(textureID, scaledSpriteSize.x, scaledSpriteSize.y,
                    textureCoordinates[2].x, textureCoordinates[0].y,
                    textureCoordinates[0].x, textureCoordinates[2].y
            );
            float buttonWidth = ImGui.getItemRectSizeX();
            consumedWidth += (buttonWidth + spacing);
            float cursorX = ImGui.getCursorPosX();
            String frameIndex = Integer.toString(i);
            float indexWidth = ImGui.calcTextSizeX(frameIndex);
            float offset = Math.max((scaledSpriteSize.x - indexWidth) * 0.5f, 0.0f);
            ImGui.setCursorPosX(cursorX + offset);
            ImGui.text(frameIndex);
            ImGui.endGroup();
            ImGui.popID();
            if (consumedWidth + buttonWidth + spacing <= availWidth) {
                ImGui.sameLine();
                continue;
            }
            consumedWidth = 0.0f;
            ImGui.spacing();
        }
    }

    private static boolean renderAnimationNameEdit(String name, ImVec2 cursorPos, boolean isSelected) {
        if (!Objects.equals(editingName, name)) return false;
        ImGui.setCursorPos(cursorPos);
        ImGui.setKeyboardFocusHere();
        ImGui.inputText("##SFC_" + "Edit_" + name, EditingNameBuffer);
        if (ImGui.isItemFocused() &&  ImGui.isKeyPressed(ImGuiKey.Escape)) {
            EditingNameBuffer.clear();
            editingName = null;
            ImGui.spacing();
            return true;
        }
        if ((ImGui.isItemFocused() && ImGui.isKeyPressed(ImGuiKey.Enter)) || !isSelected) {
            String newName = EditingNameBuffer.get().trim();
            boolean success = editingAnimatedSprite.renameAnimation(name, newName);
            if (success) selectedName = newName;
            EditingNameBuffer.clear();
            editingName = null;
            ImGui.spacing();
            return true;
        }
        ImGui.spacing();
        return true;
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
        selectedFrame = 0;
        editingName = null;
        EditingNameBuffer.clear();
        editingAnimatedSprite = null;
        selectedName = null;
    }
}
