package editor.dialogs;

import eventviewer.EngineEventCallback;
import eventviewer.event.EditorEvent;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.*;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import imgui.type.ImString;
import org.joml.Vector2f;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import project.Project;
import project.ProjectAssetMap;
import render.Texture;
import utility.TextureScale;
import utility.UnifiedPaths;

import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

/**
 * Editor dialogue for adding texture unit to user project.
 */
public class AddTextureUnitDialog {
    private static final String PopupID = "Add new TextureUnit";
    private static final String FileSelectionID = "File_Selection";
    private static final String PreviewImageID = "TextureUnit_Preview";
    private static final String MetaID = "Meta_Editor";
    private static final ImVec2 DialogSize = new ImVec2(640f, 640f);
    private static boolean showDialog = false;
    private static final ImString selectedFilePath = new ImString(256);
    private static final ImInt width = new ImInt();
    private static final ImInt ogW = new ImInt();
    private static final ImInt height = new ImInt();
    private static final ImInt ogH = new ImInt();
    private static final ImBoolean customSize = new ImBoolean(false);
    private static Texture previewTexture = null;
    private static final float imagePreviewYPercentage = 0.64f;
    private static float previewScale = 1.0f;
    private static final List<String> PictureFormats = List.of("png", "jpg", "jpeg", "bmp", "gif");

    /**
     * Create the dialogue module.
     */
    private AddTextureUnitDialog() {}

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
            float buttonReserverY = ImGui.getFrameHeightWithSpacing();
            ImGui.setCursorPosY(ImGui.getWindowHeight() - buttonReserverY - ImGui.getStyle().getWindowPaddingY());
            float startX = ImGui.getCursorStartPosX();
            float buttonWidth = 120.0f;
            float buttonPivotX = buttonWidth * 0.5f;
            float availX = ImGui.getContentRegionAvailX();
            float addX = startX + availX * 0.25f - buttonPivotX;
            float cancelX = startX + availX * 0.75f - buttonPivotX;
            boolean canAdd = !selectedFilePath.isEmpty();
            ImGui.setCursorPosX(addX);
            if (!canAdd) ImGui.beginDisabled();
            if (ImGui.button("Add Unit", buttonWidth, 0.0f)) addTextureUnit();
            if (!canAdd) ImGui.endDisabled();
            ImGui.sameLine();
            ImGui.setCursorPosX(cancelX);
            if (ImGui.button("Cancel", buttonWidth, 0.0f)) {
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
            OpenFileDialog fileDialog = OpenFileDialog.get("Sheet Image", PictureFormats);
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
        ImGui.pushStyleColor(ImGuiCol.Button, 0.7f, 0.2f, 0.2f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.8f, 0.3f, 0.3f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.7f, 0.2f, 0.2f, 1.0f);
        if (ImGui.button("Reset")) resetImportSizeToDefault();
        ImGui.popStyleColor(3);
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
        if (!previewTexture.isReady()) {
            ImGui.textDisabled("Loading preview image...");
            return;
        }
        int w = width.get();
        int h = height.get();
        float scaledW = w * previewScale;
        float scaledH = h * previewScale;
        Vector2f scaledSize = new Vector2f(scaledW, scaledH);
        ImVec2 avail = ImGui.getContentRegionAvail();
        if (scaledW > avail.x || scaledH > avail.y) {
            scaledSize = TextureScale.calculateFitDimension(scaledW, scaledH, avail.x, avail.y);
        }
        float portX = (avail.x / 2.0f) - (scaledSize.x / 2.0f);
        float portY = (avail.y / 2.0f) - (scaledSize.y / 2.0f);
        ImVec2 centeredPos = new ImVec2(portX + ImGui.getCursorPosX(), portY + ImGui.getCursorPosY());
        ImGui.setCursorPos(centeredPos);
        ImGui.image(previewTexture.getID(), scaledSize.x, scaledSize.y, 0.0f, 1.0f, 1.0f, 0.0f);
    }

    private static void loadPreviewTexture(String filePath) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer c = stack.mallocInt(1);
            if (STBImage.stbi_info(filePath, w, h, c)) {
                width.set(w.get());
                ogW.set(width);
                height.set(h.get());
                ogH.set(height);
            }
        }
        try {
            clearPreviewTexture();
            previewTexture = new Texture();
            previewTexture.init(filePath);
        } catch (Exception e) {
            System.err.println("Failed to load preview texture: " + e.getMessage());
            previewTexture = null;
        }
    }

    private static void addTextureUnit() {
        if (Project.projectRoot() == null) return;
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
            ProjectAssetMap assetMap = new ProjectAssetMap(relativePath, width.get(), height.get());
            boolean success = Project.addAsset(UUID.randomUUID(), assetMap);
            if (success) {
                System.out.println("New asset added to project");
                EngineEventCallback.emit(null, new EditorEvent(EditorEvent.Type.ReloadSceneResource));
            }
            showDialog = false;
            ImGui.closeCurrentPopup();
        } catch (IOException e) {
            System.err.println("Failed to copy asset file: " + e.getMessage());
        }
    }

    private static Path createFile(Path original, Path target, String filename) throws IOException{
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
        width.set(0);
        ogW.set(0);
        height.set(0);
        ogH.set(0);
        clearPreviewTexture();
    }

    private static void clearPreviewTexture() {
        if (previewTexture == null) return;
        String path = previewTexture.getCanonicalPath();
        if (path != null && !UnifiedPaths.isPathInsideProject(path)) previewTexture.dispose();
        previewTexture = null;
    }

    private static void resetImportSizeToDefault() {
        width.set(ogW);
        height.set(ogH);
    }
}
