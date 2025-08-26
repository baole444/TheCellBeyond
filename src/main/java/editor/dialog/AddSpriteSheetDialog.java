package editor.dialog;

import editor.project.Project;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImInt;
import imgui.type.ImString;
import org.joml.Vector2f;
import org.joml.Vector2i;
import render.Texture;
import utility.IdPool;
import utility.TextureScale;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

public class AddSpriteSheetDialog {
    private static final IdPool ID_POOL = new IdPool(0, false);
    private static final String POPUP_ID = "Add new SpriteSheet";
    private static final String FILE_SELECTION_ID = "File_Selection";
    private static final String PREVIEW_SHEET_ID = "SpriteSheet_Preview";
    private static final ImVec2 DIALOG_SIZE = new ImVec2(900.0f, 700.0f);
    private static boolean showDialog = false;
    //private static boolean editMode = false;
    private static final boolean enableBorder = true;

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

    private static final float fileYPercentage = 0.1f;
    private static final float previewYPercentage = 0.65f;
    private static final float propertiesYPercentage = 0.2f;

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
            ImGui.separator();

            renderPreviewSection();
            ImGui.separator();

            ImGui.inputText("Sheet name", sheetName);
            ImGui.inputText("Sheet category", category);
            ImGui.separator();

            int buttonW = 120;
            boolean canAdd = !selectedFilePath.isEmpty() && sheetName.isEmpty() &&
                    spriteSize.x > 0 && spriteSize.y > 0 && numberOfSprite > 0;

            if (canAdd) {
                if (ImGui.button("Add Sheet", buttonW, 0)) addSpriteSheet();
            } else {
                ImGui.beginDisabled();
                ImGui.button("Add Sheet", buttonW, 0);
                ImGui.endDisabled();
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
        ImGui.beginChild(FILE_SELECTION_ID, ImGuiWindowFlags.None, sectionY, !enableBorder);

        int browseButtonW = 120;
        ImGui.pushItemWidth(ImGui.getContentRegionAvailX() - browseButtonW - ImGui.getStyle().getItemSpacingX());
        ImGui.inputText("##filepath", selectedFilePath, ImGuiInputTextFlags.ReadOnly);
        ImGui.popItemWidth();

        ImGui.sameLine();
        if (ImGui.button("Browse Files", browseButtonW, 0)) {
            OpenFileDialog fileDialog = OpenFileDialog.get(PICTURE_FORMATS);
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
        ImGui.beginChild(PREVIEW_SHEET_ID, ImGuiWindowFlags.None, sectionY, enableBorder);

        ImGui.columns(2, "Preview_Editor_Columns", enableBorder);

        int previewX = (int) (DIALOG_SIZE.x * 0.7f);
        ImGui.setColumnWidth(0, previewX);

        renderPreviewImage();

        ImGui.nextColumn();

        renderSpritePropertiesEditor();

        ImGui.columns(1);
        ImGui.endChild();
    }

    private static void renderPreviewImage() {
        float[] scale = {previewScale};
        if (ImGui.sliderFloat("Scale", scale, 0.1f, 2.0f, "%.2f")) previewScale = scale[0];

        ImGui.separator();

        if (previewTexture != null && previewTexture.isReady()) {
            int imageW = previewTexture.getWidth();
            int imageH = previewTexture.getHeight();

            int maxW = (int) (ImGui.getColumnWidth() - 20);
            int maxH = (int) (DIALOG_SIZE.y * previewYPercentage) - 100;

            Vector2f scaledSize = TextureScale.calculateFitDimension(imageW * previewScale, imageH * previewScale, maxW, maxH);

            ImVec2 cursorPos = ImGui.getCursorScreenPos();

            ImGui.image(previewTexture.getID(), scaledSize.x, scaledSize.y);

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

        int columns = (int) Math.ceil((originalWidth - x) / (spriteSize.x + spriteSpacing.x));
        int rows = (int) Math.ceil((originalHeight - y) / (spriteSize.y + spriteSpacing.y));

        int gridColor = ImGui.getColorU32(1.0f, 0.0f, 0.0f, 0.8f);

        ImVec2 startPos = new ImVec2();
        ImVec2 endPos = new ImVec2();
        for (int i = 0; i <= columns; i++) {
            float xPos = imagePos.x + startX + (i * (spriteSize.x + spriteSpacing.x) * scaleX);

            if (xPos <= imagePos.x + imageSize.x) {
                startPos.set(xPos, imagePos.y);
                endPos.set(xPos, imagePos.y + imageSize.y);
                drawList.addLine(startPos, endPos, gridColor, 1);
            }
        }

        startPos.set(0, 0);
        endPos.set(0, 0);
        for (int i = 0; i <= rows; i++) {
            float yPos = imagePos.y + startY + (i * (spriteSize.y + spriteSpacing.y) * scaleY);

            if (yPos <= imagePos.y + imageSize.y) {
                startPos.set(imagePos.x, yPos);
                endPos.set(imagePos.x + imageSize.x, yPos);
                drawList.addLine(startPos, endPos, gridColor, 1);
            }
        }
    }

    private static void renderSpritePropertiesEditor() {
        inputInt("No. of sprites", numberOfSprite, 1);

        ImGui.text("Sprite size:");
        inputInt("Width", spriteSize.x, 1);
        inputInt("Height", spriteSize.y, 1);
        ImGui.separator();

        ImGui.text("Sprite spacing:");
        inputInt("Horizontal", spriteSpacing.x, 0);
        inputInt("Vertical", spriteSpacing.y, 0);
        ImGui.separator();

        ImGui.text("Start position:");
        inputInt("X Offset", spriteStartPosition.x, 0);
        inputInt("Y Offset", spriteStartPosition.y, 0);
        ImGui.separator();
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

    private static void inputInt(String label, int target, int minValue) {
        String id = label + "_" + ID_POOL.newId();
        ImGui.pushID(id);
        final boolean modified;
        final ImInt destination = new ImInt(target);

        modified = ImGui.inputInt(label, destination);

        if (modified) spriteSize.x = Math.max(destination.get(), minValue);

        ImGui.popID();
    }

    private static void addSpriteSheet() {
        if (Project.projectRoot() == null) return;

        try {
            Path dir = Paths.get(Project.projectRoot(), "sheets");
            if (!Files.exists(dir)) Files.createDirectories(dir);

            Path source = Paths.get(selectedFilePath.get());
            String filename = source.getFileName().toString();

            Path target = dir.resolve(filename);

            createFile(dir, target, filename);

            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

            // TODO: Update the project file with the new entry
            System.out.println("New sheet added");

            showDialog = false;
            ImGui.closeCurrentPopup();
        } catch (IOException e) {
            System.err.println("Failed to copy sheet file: " + e.getMessage());
        }
    }

    private static void createFile(Path original, Path target, String filename) throws IOException {
        try {
            Files.createFile(target);
            return;
        } catch (FileAlreadyExistsException ignore) {}

        target = original.resolve("copy_" + filename);
        try {
            Files.createFile(target);
            return;
        } catch (FileAlreadyExistsException ignore) {}

        int counter = 0;
        while (true) {
            try {
                counter++;
                target = original.resolve("copy_" + counter + "_" + filename);
                Files.createFile(target);
                return;
            } catch (FileAlreadyExistsException ignore) {}
        }
    }
}
