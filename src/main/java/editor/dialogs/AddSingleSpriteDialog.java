package editor.dialogs;

import editor.EditorColors;
import eventviewer.EngineEventCallback;
import eventviewer.event.EditorEvent;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.*;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import imgui.type.ImString;
import org.joml.Vector2f;
import org.joml.Vector2i;
import project.Project;
import project.ProjectAsset;
import render.Texture;
import render.texture.ImageHeader;
import utility.TextureScale;
import utility.UnifiedPaths;
import utility.log.EngineLog;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

/**
 * Editor dialogue for adding texture unit to user project.
 */
public final class AddSingleSpriteDialog {
    private static final EngineLog Logger = new EngineLog(AddSingleSpriteDialog.class);
    private static final String PopupID = "Add new SingleSprite";
    private static final String FileSelectionID = "File_Selection";
    private static final String PreviewImageID = "SingleSprite_Preview";
    private static final String MetaID = "Meta_Editor";
    private static final ImVec2 DialogSize = new ImVec2(640.0f, 640.0f);
    private static final ImString selectedFilePath = new ImString(256);
    private static final ImString spriteName = new ImString(64);
    private static final ImString category = new ImString(64);
    private static final ImInt width = new ImInt();
    private static final ImInt ogW = new ImInt();
    private static final ImInt height = new ImInt();
    private static final ImInt ogH = new ImInt();
    private static final ImBoolean customSize = new ImBoolean(false);
    private static final float ButtonReserverY = ImGui.getFrameHeightWithSpacing();
    private static final float ButtonWidth = 120.0f;
    private static final float ButtonHeight = 30.0f;
    private static Texture previewTexture = null;
    private static final float imagePreviewYPercentage = 0.6f;
    private static float previewScale = 1.0f;
    private static final List<String> PictureFormats = List.of("png", "jpg", "jpeg", "bmp", "gif");
    private static boolean showDialog = false;

    private AddSingleSpriteDialog() {}

    /**
     * Toggle the show flag for this dialogue.
     */
    public static void show() {
        showDialog = true;
    }

    /**
     * Render this dialogue on screen.
     */
    public static void imgui() {
        if (!showDialog) return;
        ImGui.openPopup(PopupID);
        ImVec2 centre = ImGui.getMainViewport().getCenter();
        float pivotXY = 0.5f;
        ImGui.setNextWindowPos(centre.x, centre.y, ImGuiCond.Appearing, pivotXY, pivotXY);
        ImGui.setNextWindowSize(DialogSize);
        if (ImGui.beginPopupModal(PopupID, ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoScrollbar)) {
            renderFileSelection();
            renderPreviewSection();
            ImGui.separator();
            ImGui.inputText("Sprite name", spriteName);
            ImGui.inputText("Sprite category", category);
            ImGui.setCursorPosY(ImGui.getWindowHeight() - ButtonReserverY - ButtonHeight / 2.0f);
            float startX = ImGui.getCursorStartPosX();
            float buttonPivotX = ButtonWidth * 0.5f;
            float availX = ImGui.getContentRegionAvailX();
            float addX = startX + availX * 0.25f - buttonPivotX;
            float cancelX = startX + availX * 0.75f - buttonPivotX;
            boolean canAdd = selectedFilePath.isNotEmpty() && spriteName.isNotEmpty() && category.isNotEmpty();
            ImGui.setCursorPosX(addX);
            if (!canAdd) ImGui.beginDisabled();
            if (ImGui.button("Add", ButtonWidth, ButtonHeight)) addSingleSprite();
            if (!canAdd) ImGui.endDisabled();
            ImGui.sameLine();
            ImGui.setCursorPosX(cancelX);
            if (ImGui.button("Cancel", ButtonWidth, ButtonHeight)) {
                showDialog = false;
                ImGui.closeCurrentPopup();
            }
            ImGui.endPopup();
        }
        if (!ImGui.isPopupOpen(PopupID)) {
            showDialog = false;
            resetDialogData();
        }
    }

    private static void renderFileSelection() {
        ImGui.text("Click \"Browse Files\" to select an image");
        if (!ImGui.beginChild(FileSelectionID, new ImVec2(0.0f, 0.0f), ImGuiChildFlags.AutoResizeY)) {
            ImGui.endChild();
            return;
        }
        int browseButtonW = 120;
        ImGui.pushItemWidth(ImGui.getContentRegionAvailX() - browseButtonW - ImGui.getStyle().getItemSpacingX());
        ImGui.inputText("##filepath", selectedFilePath, ImGuiInputTextFlags.ReadOnly);
        ImGui.popItemWidth();
        ImGui.sameLine();
        if (ImGui.button("Browse Files", browseButtonW, 0)) {
            OpenFileDialog fileDialog = OpenFileDialog.get("Image", PictureFormats);
            String path = fileDialog.openDialog();
            if (path != null) {
                selectedFilePath.set(path);
                loadPreviewTexture(path);
            }
        }
        ImGui.endChild();
    }

    private static void renderPreviewSection() {
        float sectionY = DialogSize.y * imagePreviewYPercentage;
        if (!ImGui.beginChild(PreviewImageID, new ImVec2(0.0f, sectionY), ImGuiChildFlags.Borders)) {
            ImGui.endChild();
            return;
        }
        renderPreviewImage();
        ImGui.endChild();
        if (!ImGui.beginChild(MetaID, new ImVec2(0.0f, 0.0f), ImGuiChildFlags.Borders | ImGuiChildFlags.AutoResizeY)) {
            ImGui.endChild();
            return;
        }
        if (ImGui.checkbox("Adjust import size", customSize)) {
            if (!customSize.get()) resetImportSizeToDefault();
        }
        boolean adjustSize = customSize.get();
        if (!adjustSize) ImGui.beginDisabled();
        ImGui.sameLine();
        EditorColors.RedButton.create(() -> {
            if (ImGui.button("Reset")) resetImportSizeToDefault();
        });
        if (ImGui.beginTable("##import_size_table", 2, ImGuiTableFlags.BordersInnerV | ImGuiTableFlags.SizingStretchProp, ImGui.getContentRegionAvailX())) {
            ImGui.tableSetupColumn("##import_width", ImGuiTableColumnFlags.WidthFixed, ImGui.getContentRegionAvailX() / 2.0f);
            ImGui.tableSetupColumn("##import_height", ImGuiTableColumnFlags.WidthStretch);
            ImGui.tableNextColumn();
            ImGui.inputInt("Width", width);
            ImGui.tableNextColumn();
            ImGui.inputInt("Height", height);
            ImGui.endTable();
        }
        if (!adjustSize) ImGui.endDisabled();
        ImGui.endChild();
    }

    private static void renderPreviewImage() {
        float[] scale = {previewScale};
        if (ImGui.sliderFloat("Scale", scale, 0.1f, 2.0f, "%.2f")) previewScale = scale[0];
        ImGui.separator();
        if (previewTexture == null) {
            ImGui.textDisabled("Choose an image using the \"Browse Files\" button");
            return;
        }
        if (!previewTexture.readied()) {
            ImGui.textDisabled("Loading preview image...");
            return;
        }
        int w = width.get();
        int h = height.get();
        float scaledW = w * previewScale;
        float scaledH = h * previewScale;
        Vector2f scaledSize = new Vector2f(scaledW, scaledH);
        ImVec2 avail = ImGui.getContentRegionAvail();
        if (scaledW > avail.x || scaledH > avail.y) scaledSize = TextureScale.calculateFitDimension(scaledW, scaledH, avail.x, avail.y);
        float portX = (avail.x / 2.0f) - (scaledSize.x / 2.0f);
        float portY = (avail.y / 2.0f) - (scaledSize.y / 2.0f);
        ImVec2 centeredPos = new ImVec2(portX + ImGui.getCursorPosX(), portY + ImGui.getCursorPosY());
        ImGui.setCursorPos(centeredPos);
        ImGui.image(previewTexture.nativeID(), scaledSize.x, scaledSize.y, 0.0f, 1.0f, 1.0f, 0.0f);
    }

    private static void loadPreviewTexture(String filePath) {
        Vector2i imageSize = ImageHeader.sizeOfFile(filePath);
        if (imageSize != null) {
            width.set(imageSize.x);
            ogW.set(width);
            height.set(imageSize.y);
            ogH.set(height);
        }
        try {
            clearPreviewTexture();
            previewTexture = new Texture();
            previewTexture.init(filePath);
        } catch (Exception e) {
            Logger.error(String.format("Cannot load preview texture: %s", e.getMessage()));
            previewTexture = null;
        }
    }

    private static void addSingleSprite() {
        if (!Project.loaded()) return;
        try {
            Path projectRoot = Paths.get(Project.projectRoot());
            Path source = Paths.get(selectedFilePath.get());
            Path target;
            String relativePath;
            if (source.startsWith(projectRoot)) {
                relativePath = UnifiedPaths.resolveToRelative(projectRoot.toString(), source.toString());
            } else {
                Path dir = Paths.get(Project.projectRoot(), "assets");
                if (!Files.exists(dir)) Files.createDirectories(dir);
                String filename = source.getFileName().toString();
                target = dir.resolve(filename);
                target = createFile(dir, target, filename);
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                relativePath = "assets/" + target.getFileName().toString();
            }
            String name = spriteName.get().isBlank() ? defaultName(relativePath) : spriteName.get();
            String cat = category.get().isBlank() ? ProjectAsset.DefaultCategory : category.get();
            ProjectAsset assetMap = new ProjectAsset(cat, name, relativePath, width.get(), height.get());
            boolean success = Project.addAsset(cat, name, assetMap);
            if (success) {
                Logger.debug(String.format("New asset '%s/%s' added to project", cat, name));
                EngineEventCallback.emit(null, new EditorEvent(EditorEvent.Type.ReloadSceneResource));
            }
            showDialog = false;
            ImGui.closeCurrentPopup();
        } catch (IOException e) {
            Logger.error(String.format("Failed to copy asset file: %s", e.getMessage()));
        }
    }

    private static String defaultName(String relativePath) {
        String fileName = relativePath.substring(relativePath.lastIndexOf('/') + 1);
        int extension = fileName.lastIndexOf('.');
        return extension > 0 ? fileName.substring(0, extension) : fileName;
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
        while(true) {
            try {
                counter++;
                target = original.resolve("copy_" + counter + "_" + filename);
                Files.createFile(target);
                return target;
            } catch (FileAlreadyExistsException ignore) {}
        }
    }

    private static void resetDialogData() {
        customSize.set(false);
        selectedFilePath.clear();
        spriteName.clear();
        category.clear();
        width.set(0);
        ogW.set(0);
        height.set(0);
        ogH.set(0);
        clearPreviewTexture();
    }

    private static void clearPreviewTexture() {
        if (previewTexture == null) return;
        String path = previewTexture.canonicalPath();
        if (path != null && !UnifiedPaths.isPathInsideProject(path)) previewTexture.dispose();
        previewTexture = null;
    }

    private static void resetImportSizeToDefault() {
        width.set(ogW);
        height.set(ogH);
    }
}
