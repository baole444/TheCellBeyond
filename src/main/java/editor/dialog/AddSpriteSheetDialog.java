package editor.dialog;

import editor.project.Project;
import editor.project.ProjectSheetMap;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.*;
import imgui.type.ImInt;
import imgui.type.ImString;
import org.joml.Vector2f;
import org.joml.Vector2i;
import render.Texture;
import utility.IdPool;
import utility.PathResolver;
import utility.TextureScale;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

public class AddSpriteSheetDialog {
    private static final IdPool ID_POOL = new IdPool(0, false);
    private static final String POPUP_ID = "Add new SpriteSheet";
    private static final String FILE_SELECTION_ID = "File_Selection";
    private static final String PREVIEW_SHEET_ID = "SpriteSheet_Preview";
    private static final String META_ID = "Meta_Editor";
    private static final ImVec2 DIALOG_SIZE = new ImVec2(900.0f, 720.0f);
    private static boolean showDialog = false;
    private static final boolean enableBorder = true;
    private static final int gridColor = ImGui.getColorU32(1.0f, 0.0f, 0.0f, 0.8f);

    // Standard path length limit is 256
    private static final ImString selectedFilePath = new ImString(256);
    private static Texture previewTexture = null;

    private static final ImString sheetName = new ImString(64);
    private static final ImString category = new ImString(64);
    private static int numberOfSprite = 1;
    private static final Vector2i spriteSize = new Vector2i(16);
    private static final Vector2i spriteSpacing = new Vector2i();
    private static final Vector2i spriteStartPosition = new Vector2i();

    private static float previewScale = 1.0f;

    private static final float fileYPercentage = 0.03f;
    private static final float previewYPercentage = 0.60f;
    private static final float metaYPercentage = 0.22f;

    private static final List<String> PICTURE_FORMATS = List.of(
            "png", "jpg", "jpeg", "bmp", "gif"
    );

    public static void show(boolean editingSpriteSheet) {
        //editMode = editingSpriteSheet;
        showDialog = true;
    }

    private static void resetDialogData() {
        ID_POOL.reset();
        selectedFilePath.clear();
        if (previewTexture != null) {
            previewTexture.dispose();
            previewTexture = null;
        }
        previewScale = 1.0f;
        sheetName.clear();
        category.clear();
        numberOfSprite = 1;
        spriteSize.set(16);
        spriteSpacing.zero();
        spriteStartPosition.zero();
    }

    public static void imgui() {
        if (!showDialog) return;

        ImGui.openPopup(POPUP_ID);

        ImVec2 centre = ImGui.getMainViewport().getCenter();
        float pivotXY = 0.5f;

        ImGui.setNextWindowPos(centre.x, centre.y, ImGuiCond.Appearing, pivotXY, pivotXY);
        ImGui.setNextWindowSize(DIALOG_SIZE, ImGuiCond.FirstUseEver);

        if (ImGui.beginPopupModal(POPUP_ID, ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoScrollbar)) {
            renderFileSelection();
            renderPreviewSection();
            renderSpritePropertiesEditor();

            float buttonReserverY = ImGui.getFrameHeightWithSpacing();
            ImGui.setCursorPosY(ImGui.getWindowHeight() - buttonReserverY - ImGui.getStyle().getWindowPaddingY());

            float buttonWidth = 120;
            float buttonPivotX = buttonWidth * 0.5f;
            float availX = ImGui.getContentRegionAvailX();
            float addX = (availX * 0.25f) - (buttonPivotX);
            float cancelX = (availX * 0.75f) - (buttonPivotX);
            boolean canAdd = !selectedFilePath.isEmpty() && !sheetName.isEmpty() &&
                    spriteSize.x > 0 && spriteSize.y > 0 && numberOfSprite > 0;
            ImGui.setCursorPosX(addX);
            if (canAdd) {
                if (ImGui.button("Add Sheet", buttonWidth, 0)) addSpriteSheet();
            } else {
                ImGui.beginDisabled();
                ImGui.button("Add Sheet", buttonWidth, 0);
                ImGui.endDisabled();
            }

            ImGui.sameLine();
            ImGui.setCursorPosX(cancelX);
            if (ImGui.button("Cancel", buttonWidth, 0)) {
                showDialog = false;
                ImGui.closeCurrentPopup();
            }

            ImGui.endPopup();
            ID_POOL.reset();
        }

        if (!ImGui.isPopupOpen(POPUP_ID)) {
            showDialog = false;
            resetDialogData();
        }
    }

    private static void renderFileSelection() {
        ImGui.text("Click \"Browse Files\" to select an image");

        int sectionY = (int) (DIALOG_SIZE.y * fileYPercentage);
        ImGui.beginChild(FILE_SELECTION_ID, 0, sectionY, !enableBorder);

        int browseButtonW = 120;
        ImGui.pushItemWidth(ImGui.getContentRegionAvailX() - browseButtonW - ImGui.getStyle().getItemSpacingX());
        ImGui.inputText("##filepath", selectedFilePath, ImGuiInputTextFlags.ReadOnly);
        ImGui.popItemWidth();

        ImGui.sameLine();
        if (ImGui.button("Browse Files", browseButtonW, 0)) {
            OpenFileDialog fileDialog = OpenFileDialog.get("Sheet Image", PICTURE_FORMATS);
            String path = fileDialog.openDialog();

            if (path != null) {
                selectedFilePath.set(path);
                if (sheetName.isEmpty()) sheetName.set("New Sheet");
                loadPreviewTexture(path);
            }
        }

        ImGui.endChild();
    }

    private static void renderPreviewSection() {
        int sectionY = (int) (DIALOG_SIZE.y * previewYPercentage);
        ImGui.beginChild(PREVIEW_SHEET_ID, 0, sectionY, enableBorder);
        renderPreviewImage();
        ImGui.endChild();
    }

    private static void renderPreviewImage() {
        float[] scale = {previewScale};
        if (ImGui.sliderFloat("Scale", scale, 0.1f, 2.0f, "%.2f")) previewScale = scale[0];
        ImGui.separator();

        if (previewTexture != null && previewTexture.isReady()) {
            int imageW = previewTexture.getWidth();
            int imageH = previewTexture.getHeight();
            ImVec2 avail = ImGui.getContentRegionAvail();

            float scaledImageW = imageW * previewScale;
            float scaledImageH = imageH * previewScale;
            Vector2f scaledSize = new Vector2f(scaledImageW, scaledImageH);

            if (scaledImageW > avail.x || scaledImageH > avail.y) {
                scaledSize = TextureScale.calculateFitDimension(scaledImageW, scaledImageH, avail.x, avail.y);
            }

            float portX = (avail.x / 2.0f) - (scaledSize.x / 2.0f);
            float portY = (avail.y / 2.0f) - (scaledSize.y / 2.0f);

            ImVec2 centeredPos = new ImVec2(portX + ImGui.getCursorPosX(), portY + ImGui.getCursorPosY());

            ImGui.setCursorPos(centeredPos);
            ImVec2 cursorPos = ImGui.getCursorScreenPos();

            ImGui.image(previewTexture.getID(), scaledSize.x, scaledSize.y, 0.0f, 1.0f, 1.0f, 0.0f);
            if (spriteSize.x > 0 && spriteSize.y > 0) drawSpriteDivider(cursorPos, scaledSize, imageW, imageH);
        } else {
            ImGui.textDisabled("Choose a sprite sheet using the \"Browse Files\" button");
        }
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
                    drawList.addLine(startPos, endPos, gridColor, 1);
                }

                if (spriteSpacing.x > 0 && col < spritesInThisRow) {
                    float xPos2 = imagePos.x + startX + ((col * (spriteSize.x + spriteSpacing.x) + spriteSize.x) * scaleX);
                    if (xPos2 <= imagePos.x + imageSize.x) {
                        float yTop = imagePos.y + startY + (row * (spriteSize.y + spriteSpacing.y) * scaleY);
                        float yBottom = yTop + (spriteSize.y * scaleY);

                        startPos.set(xPos2, yTop);
                        endPos.set(xPos2, Math.min(yBottom, imagePos.y + imageSize.y));
                        drawList.addLine(startPos, endPos, gridColor, 1);
                    }
                }
            }

            float yTop = imagePos.y + startY + (row * (spriteSize.y + spriteSpacing.y) * scaleY);
            float yBottom = yTop + (spriteSize.y * scaleY);
            if (yTop <= imagePos.y + imageSize.y) {
                float xEnd = imagePos.x + startX + (spritesInThisRow * (spriteSize.x + spriteSpacing.x) * scaleX);

                startPos.set(imagePos.x + startX, yTop);
                endPos.set(Math.min(xEnd, imagePos.x + imageSize.x), yTop);
                drawList.addLine(startPos, endPos, gridColor, 1);

                if (yBottom <= imagePos.y + imageSize.y) {
                    startPos.set(imagePos.x + startX, yBottom);
                    endPos.set(Math.min(xEnd, imagePos.x + imageSize.x), yBottom);
                    drawList.addLine(startPos, endPos, gridColor, 1);
                }

                if (spriteSpacing.y > 0 && remainingSprites > spritesInThisRow) {
                    float ySpacingTop = imagePos.y + startY + ((row * (spriteSize.y + spriteSpacing.y) + spriteSize.y) * scaleY);
                    if (ySpacingTop <= imagePos.y + imageSize.y) {
                        startPos.set(imagePos.x + startX, ySpacingTop);
                        endPos.set(Math.min(xEnd, imagePos.x + imageSize.x), ySpacingTop);
                        drawList.addLine(startPos, endPos, gridColor, 1);
                    }
                }
            }

            remainingSprites -= spritesInThisRow;
            if (remainingSprites <= 0) break;
        }
    }

    private static void renderSpritePropertiesEditor() {
        int ySection =(int) (DIALOG_SIZE.y * metaYPercentage);
        if (!ImGui.beginChild(META_ID, 0, ySection, enableBorder)) return;

        numberOfSprite = inputInt("Number of sprites", numberOfSprite, 1);
        ImGui.separator();

        if (!ImGui.beginTable("##SpriteSheetPropertiesTable", 3, ImGuiTableFlags.BordersInnerV | ImGuiTableFlags.SizingStretchProp, ImGui.getContentRegionAvailX())) {
            ImGui.endChild();
            return;
        }

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
        ImGui.endChild();
    }

    private static void loadPreviewTexture(String filePath) {
        try {
            if (previewTexture != null) previewTexture.dispose();

            previewTexture = new Texture();
            previewTexture.init(filePath);
        } catch (Exception e) {
            System.err.println("Failed to load preview texture: " + e.getMessage());
            previewTexture = null;
        }
    }

    private static int inputInt(String label, int target, int minValue) {
        String id = label + "_" + ID_POOL.newId();
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
                relativePath = PathResolver.resolveToRelative(projectRoot.toString(), source.toString());
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
                Project.loadProjectData();
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
