package editor.dialog;

import editor.EditorIcons;
import editor.EditorWidget;
import eventviewer.EngineEventCallback;
import eventviewer.event.EditorEvent;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.*;
import imgui.type.ImFloat;
import imgui.type.ImInt;
import imgui.type.ImString;
import org.joml.Vector2f;
import org.joml.Vector2i;
import project.Project;
import project.ProjectSheetMap;
import render.Texture;
import utility.IdPool;
import utility.UnifiedPaths;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

/**
 * Editor dialogue for importing sprite sheet to user project.
 */
public class AddSpriteSheetDialog {
    private static final IdPool IDPool = new IdPool(0, false);
    private static final String PopupID = "Add new SpriteSheet";
    private static final String PreviewSheetID = "SpriteSheet_Preview";
    private static final ImVec2 DialogSize = new ImVec2(900.0f, 720.0f);
    private static final int GridColor = ImGui.getColorU32(1.0f, 0.0f, 0.0f, 0.8f);
    private static boolean showDialog = false;
    private static final ImString selectedFilePath = new ImString(256);
    private static Texture previewTexture = null;
    private static final ImString sheetName = new ImString(64);
    private static final ImString category = new ImString(64);
    private static int numberOfSprite = 1;
    private static final Vector2i spriteSize = new Vector2i(16);
    private static final Vector2i spriteSpacing = new Vector2i();
    private static final Vector2i spriteStartPosition = new Vector2i();
    private static float previewScale = 1.0f;
    private static final List<String> PictureFormats = List.of("png", "jpg", "jpeg", "bmp", "gif");
    private static final float ButtonReserver = ImGui.getFrameHeightWithSpacing();
    private static final float SeparatorReserve = ImGui.getStyle().getItemSpacingY();
    private static final float Padding = 4.0f;
    private static final ImVec2 propertiesSizeCache = new ImVec2();

    private AddSpriteSheetDialog() {}

    /**
     * Toggle the show flag for this dialogue.
     * @param editingSpriteSheet is the dialogue open for editing detail on existing sprite sheet or not
     */
    public static void show(boolean editingSpriteSheet) {
        //editMode = editingSpriteSheet;
        showDialog = true;
    }

    private static void resetDialogData() {
        IDPool.reset();
        selectedFilePath.clear();
        previewScale = 1.0f;
        sheetName.clear();
        category.clear();
        numberOfSprite = 1;
        spriteSize.set(16);
        spriteSpacing.zero();
        spriteStartPosition.zero();
        clearPreviewTexture();
    }

    private static void clearPreviewTexture() {
        if (previewTexture == null) return;
        String path = previewTexture.getCanonicalPath();
        if (path != null && !UnifiedPaths.isPathInsideProject(path)) previewTexture.dispose();
        previewTexture = null;
    }

    /**
     * Render the dialogue on screen.
     */
    public static void imgui() {
        if (!showDialog) return;
        ImGui.openPopup(PopupID);
        ImVec2 centre = ImGui.getMainViewport().getCenter();
        float pivotXY = 0.5f;
        ImGui.setNextWindowPos(centre.x, centre.y, ImGuiCond.Appearing, pivotXY, pivotXY);
        ImGui.setNextWindowSize(DialogSize);
        if (ImGui.beginPopupModal(PopupID, ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoScrollbar)) {
            float buttonWidth = 120;
            float buttonHeight = 30;
            float regionHeight = ImGui.getContentRegionAvailY() - ButtonReserver - SeparatorReserve - buttonHeight - Padding;
            if (ImGui.beginChild("##ASSD_Dialog_Region", 0.0f, regionHeight, ImGuiChildFlags.Borders)) {
                renderDialogContent();
                ImGui.endChild();
            }
            renderDialogButtons(buttonWidth, buttonHeight);
            ImGui.endPopup();
            IDPool.reset();
        }
        if (!ImGui.isPopupOpen(PopupID)) {
            showDialog = false;
            resetDialogData();
        }
    }

    private static void renderDialogContent() {
        renderFileSelection();
        renderPreviewSection();
        renderSpritePropertiesEditor();
    }

    private static void renderDialogButtons(float buttonWidth, float buttonHeight) {
        ImGui.setCursorPosY(ImGui.getWindowHeight() - ButtonReserver - (buttonHeight / 2) -ImGui.getStyle().getWindowPaddingY());
        float buttonPivotX = buttonWidth * 0.5f;
        float availX = ImGui.getContentRegionAvailX();
        float addX = (availX * 0.25f) - buttonPivotX;
        float cancelX = (availX * 0.75f) - buttonPivotX;
        boolean canAdd = !selectedFilePath.isEmpty() && !sheetName.isEmpty() && spriteSize.x > 0 && spriteSize.y > 0 && numberOfSprite > 0;
        ImGui.setCursorPosX(addX);
        if (!canAdd) ImGui.beginDisabled();
        if (ImGui.button("Add Sheet##ASSD_Add_Sheet", buttonWidth, buttonHeight)) addSpriteSheet();
        if (!canAdd) ImGui.endDisabled();
        ImGui.sameLine();
        ImGui.setCursorPosX(cancelX);
        if (ImGui.button("Cancel##ASSD_Cancell", buttonWidth, buttonHeight)) {
            showDialog = false;
            ImGui.closeCurrentPopup();
        }
    }

    private static void renderFileSelection() {
        if (!ImGui.beginTable("##ASSD_File_Path_Selection_Layout", 3, ImGuiTableFlags.SizingFixedFit)) return;
        ImGui.tableSetupColumn("##ASSD_File_Path_Selection_label_Column", ImGuiTableColumnFlags.WidthFixed);
        ImGui.tableSetupColumn("##ASSD_File_Path_Selection_Input_Column", ImGuiTableColumnFlags.WidthStretch);
        ImGui.tableSetupColumn("##ASSD_File_Path_Selection_Search_Column", ImGuiTableColumnFlags.WidthFixed);
        ImGui.tableNextColumn();
        ImGui.setCursorPosY(ImGui.getCursorPosY() + (ImGui.getFrameHeightWithSpacing() - ImGui.getTextLineHeightWithSpacing()) / 2.0f);
        ImGui.text("Image file:");
        ImGui.tableNextColumn();
        int browseButtonW = 120;
        ImGui.setNextItemWidth(ImGui.getContentRegionAvailX());
        ImGui.inputTextWithHint("##ASSD_Filepath_Input", "Click \"Browse File\" to select a path...", selectedFilePath, ImGuiInputTextFlags.ReadOnly);
        ImGui.tableNextColumn();
        if (ImGui.button("Browse Files", browseButtonW, 0)) {
            OpenFileDialog fileDialog = OpenFileDialog.get("Sheet Image", PictureFormats);
            String path = fileDialog.openDialog();
            if (path != null) {
                selectedFilePath.set(path);
                if (sheetName.isEmpty()) sheetName.set("New Sheet");
                loadPreviewTexture(path);
            }
        }
        ImGui.endTable();
        ImGui.spacing();
    }

    private static void renderPreviewSection() {
        if (!ImGui.beginChild(PreviewSheetID, 0.0f, ImGui.getContentRegionAvailY() - propertiesSizeCache.y - Padding, ImGuiChildFlags.None)) {
            ImGui.endChild();
            return;
        }
        renderPreviewImage();
        ImGui.endChild();
    }

    private static void renderPreviewImage() {
        if (ImGui.beginTable("##ASSD_Sheet_Preview_Zoom_Control_Layout", 4, ImGuiTableFlags.SizingFixedFit)) {
            ImGui.tableSetupColumn("##ASSD_Sheet_Preview_Zoom_Control_Label_Column", ImGuiTableColumnFlags.WidthFixed);
            ImGui.tableSetupColumn("##ASSD_Sheet_Preview_Zoom_Control_Input_Column", ImGuiTableColumnFlags.WidthFixed);
            ImGui.tableSetupColumn("##ASSD_Sheet_Preview_Zoom_Control_Slider_Column", ImGuiTableColumnFlags.WidthStretch);
            ImGui.tableSetupColumn("##ASSD_Sheet_Preview_Zoom_Control_Reset_Column", ImGuiTableColumnFlags.WidthFixed);
            ImGui.tableNextColumn();
            ImGui.setCursorPosY(ImGui.getCursorPosY() + (ImGui.getFrameHeightWithSpacing() - ImGui.getTextLineHeightWithSpacing()) / 2.0f);
            ImGui.text("Zoom:");
            ImGui.tableNextColumn();
            ImFloat z = new ImFloat(previewScale);
            ImGui.setNextItemWidth(ImGui.calcTextSizeX("+AAA.AAA"));
            if (ImGui.inputFloat("##ASSD_Zoom_Level_Direct_Input", z, 0.0f, 0.0f)) previewScale = Math.clamp(z.get(), 0.1f, 4.0f);
            if (ImGui.isItemHovered()) {
                ImGui.beginTooltip();
                ImGui.text("Enter the zoom level (1.0 -> 4.0)");
                ImGui.endTooltip();
            }
            ImGui.tableNextColumn();
            float[] val = {previewScale};
            ImGui.setNextItemWidth(ImGui.getContentRegionAvailX());
            if (ImGui.sliderFloat("##ASSD_Zoom_level_Slider_Control", val, 0.1f, 4.0f)) previewScale = val[0];
            if (ImGui.isItemHovered()) {
                ImGui.beginTooltip();
                ImGui.text("Slide the bar to change zoom level");
                ImGui.endTooltip();
            }
            ImGui.tableNextColumn();
            if (EditorWidget.iconButton("##ASSD_Zoom_level_Control_Reset_Button", EditorIcons.Icons.Reset, "Reset zoom to 1.0")) previewScale = 1.0f;
            ImGui.endTable();
        }
        ImGui.separator();
        if (previewTexture == null) {
            ImGui.textDisabled("Choose a sprite sheet using the \"Browse Files\" button");
            return;
        }
        if (!previewTexture.isReady()) {
            ImGui.textDisabled("Loading preview image...");
            return;
        }
        if (!ImGui.beginChild("##ASSD_Sheet_Preview_Region", ImGui.getContentRegionAvail(), ImGuiChildFlags.None, ImGuiWindowFlags.HorizontalScrollbar)) {
            ImGui.endChild();
            return;
        }
        int imageW = previewTexture.getWidth();
        int imageH = previewTexture.getHeight();
        ImVec2 avail = ImGui.getContentRegionAvail();
        float scaledImageW = imageW * previewScale;
        float scaledImageH = imageH * previewScale;
        float portX = (avail.x / 2.0f) - (scaledImageW / 2.0f);
        float portY = (avail.y / 2.0f) - (scaledImageH / 2.0f);
        ImVec2 centeredPos = new ImVec2(portX + ImGui.getCursorPosX(), portY + ImGui.getCursorPosY());
        ImGui.setCursorPos(centeredPos);
        ImVec2 cursorPos = ImGui.getCursorScreenPos();
        ImGui.image(previewTexture.getID(), scaledImageW, scaledImageH, 0.0f, 1.0f, 1.0f, 0.0f);
        if (spriteSize.x > 0 && spriteSize.y > 0) drawSpriteDivider(cursorPos, new Vector2f(scaledImageW, scaledImageH), imageW, imageH);
        ImGui.endChild();
    }

    private static void drawSpriteDivider(ImVec2 imagePos, Vector2f imageSize, float originalWidth, float originalHeight) {
        ImDrawList drawList = ImGui.getWindowDrawList();
        float scaleX = imageSize.x / originalWidth;
        float scaleY = imageSize.y / originalHeight;
        int x = spriteStartPosition.x;
        int y = spriteStartPosition.y;
        float startX = x * scaleX;
        float startY = y * scaleY;
        int spritesPerRow = (int) Math.floor((originalWidth - x + spriteSpacing.x) / (float) (spriteSize.x + spriteSpacing.x));
        if (spritesPerRow <= 0) spritesPerRow = 1;
        int totalRows = (int) Math.ceil((double) numberOfSprite / spritesPerRow);
        int remainingSprites = numberOfSprite;
        ImVec2 startPos = new ImVec2();
        ImVec2 endPos = new ImVec2();
        for (int row = 0; row < totalRows; row++) {
            int spritesInThisRow = Math.min(remainingSprites, spritesPerRow);
            for (int col = 0; col <= spritesInThisRow; col++) {
                float xPos1 = imagePos.x + startX + (col * (spriteSize.x + spriteSpacing.x) * scaleX);
                if (xPos1 <= imagePos.x + imageSize.x) {
                    float yTop = imagePos.y + startY + (row * (spriteSize.y + spriteSpacing.y) * scaleY);
                    float yBottom = yTop + (spriteSize.y * scaleY);
                    startPos.set(xPos1, yTop);
                    endPos.set(xPos1, Math.min(yBottom, imagePos.y + imageSize.y));
                    drawList.addLine(startPos, endPos, GridColor, 1);
                }
                if (spriteSpacing.x > 0 && col < spritesInThisRow) {
                    float xPos2 = imagePos.x + startX + ((col * (spriteSize.x + spriteSpacing.x) + spriteSize.x) * scaleX);
                    if (xPos2 <= imagePos.x + imageSize.x) {
                        float yTop = imagePos.y + startY + (row * (spriteSize.y + spriteSpacing.y) * scaleY);
                        float yBottom = yTop + (spriteSize.y * scaleY);
                        startPos.set(xPos2, yTop);
                        endPos.set(xPos2, Math.min(yBottom, imagePos.y + imageSize.y));
                        drawList.addLine(startPos, endPos, GridColor, 1);
                    }
                }
            }
            float yTop = imagePos.y + startY + (row * (spriteSize.y + spriteSpacing.y) * scaleY);
            float yBottom = yTop + (spriteSize.y * scaleY);
            if (yTop <= imagePos.y + imageSize.y) {
                float xEnd = imagePos.x + startX + (spritesInThisRow * (spriteSize.x + spriteSpacing.x) * scaleX);
                startPos.set(imagePos.x + startX, yTop);
                endPos.set(Math.min(xEnd, imagePos.x + imageSize.x), yTop);
                drawList.addLine(startPos, endPos, GridColor, 1);
                if (yBottom <= imagePos.y + imageSize.y) {
                    startPos.set(imagePos.x + startX, yBottom);
                    endPos.set(Math.min(xEnd, imagePos.x + imageSize.x), yBottom);
                    drawList.addLine(startPos, endPos, GridColor, 1);
                }
                if (spriteSpacing.y > 0 && remainingSprites > spritesInThisRow) {
                    float ySpacingTop = imagePos.y + startY + ((row * (spriteSize.y + spriteSpacing.y) + spriteSize.y) * scaleY);
                    if (ySpacingTop <= imagePos.y + imageSize.y) {
                        startPos.set(imagePos.x + startX, ySpacingTop);
                        endPos.set(Math.min(xEnd, imagePos.x + imageSize.x), ySpacingTop);
                        drawList.addLine(startPos, endPos, GridColor, 1);
                    }
                }
            }
            remainingSprites -= spritesInThisRow;
            if (remainingSprites <= 0) break;
        }
    }

    private static void renderSpritePropertiesEditor() {
        ImGui.beginGroup();
        numberOfSprite = inputInt("Number of sprites", numberOfSprite, 1);
        ImGui.separator();
        if (!ImGui.beginTable("##SpriteSheetPropertiesTable", 3, ImGuiTableFlags.BordersInnerV | ImGuiTableFlags.SizingStretchProp, ImGui.getContentRegionAvailX())) return;
        float columnWidth = ImGui.getContentRegionAvailX() / 3.0f;
        ImGui.tableSetupColumn("##sprite_size_column", ImGuiTableColumnFlags.WidthFixed, columnWidth);
        ImGui.tableSetupColumn("##sprite_spacing_column", ImGuiTableColumnFlags.WidthFixed, columnWidth);
        ImGui.tableSetupColumn("##sprite_start_pos_column", ImGuiTableColumnFlags.WidthStretch);
        ImGui.tableNextColumn();
        ImGui.text("Sprite size:");
        spriteSize.x = inputInt("Width", spriteSize.x, 1);
        spriteSize.y = inputInt("Height", spriteSize.y, 1);
        ImGui.tableNextColumn();
        ImGui.text("Sprite spacing:");
        spriteSpacing.x = inputInt("X space", spriteSpacing.x, 0);
        spriteSpacing.y = inputInt("Y space", spriteSpacing.y, 0);
        ImGui.tableNextColumn();
        ImGui.text("Start position:");
        spriteStartPosition.x = inputInt("X offset", spriteStartPosition.x, 0);
        spriteStartPosition.y = inputInt("Y offset", spriteStartPosition.y, 0);
        ImGui.endTable();
        ImGui.separator();
        ImGui.inputText("Sheet name", sheetName);
        ImGui.inputText("Sheet category", category);
        ImGui.endGroup();
        ImGui.getItemRectSize(propertiesSizeCache);
    }

    private static void loadPreviewTexture(String filePath) {
        try {
            clearPreviewTexture();
            previewTexture = new Texture();
            previewTexture.init(filePath);
        } catch (Exception e) {
            System.err.println("Failed to load preview texture: " + e.getMessage());
            previewTexture = null;
        }
    }

    private static int inputInt(String label, int target, int minValue) {
        String id = label + "_" + "ASD" + IDPool.newId();
        ImGui.pushID(id);
        final boolean modified;
        final ImInt destination = new ImInt(target);
        modified = ImGui.inputInt(label, destination);
        if (modified) target = Math.max(destination.get(), minValue);
        ImGui.popID();
        return target;
    }

    private static void addSpriteSheet() {
        if (Project.projectRoot() == null) return;
        String cat = category.get().trim();
        String name = sheetName.get().trim();
        if (cat.isEmpty() || name.isEmpty()) return;
        try {
            Path projectRoot = Paths.get(Project.projectRoot());
            Path source = Paths.get(selectedFilePath.get());
            Path target;
            String relativePath;
            if (source.startsWith(projectRoot)) {
                relativePath = UnifiedPaths.resolveToRelative(projectRoot.toString(), source.toString());
            } else {
                Path dir = Paths.get(Project.projectRoot(), "sheets");
                if (!Files.exists(dir)) Files.createDirectories(dir);
                String filename = source.getFileName().toString();
                target = dir.resolve(filename);
                target = createFile(dir, target, filename);
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                relativePath = "sheets/" + target.getFileName().toString();
            }
            ProjectSheetMap sheetMap = new ProjectSheetMap(relativePath, numberOfSprite,
                    spriteSize.x, spriteSize.y, spriteSpacing.x, spriteSpacing.y,
                    spriteStartPosition.x, spriteStartPosition.y
            );
            boolean success = Project.addSheet(cat, name, sheetMap);
            if (success) {
                System.out.println("New sheet '" + sheetName.get() + "' added to project");
                EngineEventCallback.emit(null, new EditorEvent(EditorEvent.Type.ReloadSceneResource));
            }
            showDialog = false;
            ImGui.closeCurrentPopup();
        } catch (IOException e) {
            System.err.println("Failed to copy sheet file: " + e.getMessage());
        }
    }

    private static Path createFile(Path original, Path target, String filename) throws IOException {
        try {
            Files.createFile(target);
            return target;
        } catch (FileAlreadyExistsException ignore) {}
        target = original.resolve("copy_" + filename);
        try {
            Files.createFile(target);
            return target;
        } catch (FileAlreadyExistsException ignore) {}
        int counter = 0;
        while (true) {
            try {
                counter++;
                target = original.resolve("copy_" + counter + "_" + filename);
                Files.createFile(target);
                return target;
            } catch (FileAlreadyExistsException ignore) {}
        }
    }
}
