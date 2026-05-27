package editor;

import TheCellBeyond.MouseListener;
import TheCellBeyond.Window;
import editor.preference.UserPreference;
import imgui.*;
import imgui.callback.ImStrConsumer;
import imgui.callback.ImStrSupplier;
import imgui.flag.ImGuiBackendFlags;
import imgui.flag.ImGuiConfigFlags;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.type.ImBoolean;
import org.joml.Math;
import scene.Scene;
import utility.AssetReference;
import utility.FontPT;
import utility.Settings;
import utility.UnifiedPaths;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;

/**
 * The Editor UI layer.
 */
public final class ImGuiLayer {
    private static final String DockID = "###EDITOR_DOCK";
    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
    private final long windowPtr;
    private ImGuiIO io;
    private static boolean resetLayout = false;
    private static boolean exitFrameEarly = false;
    private static final AtomicBoolean wantedCaptureMouse = new AtomicBoolean(false);
    private static final AtomicBoolean wantedCaptureKey = new AtomicBoolean(false);
    private static final AtomicBoolean prioritizeEngineInputCallback = new AtomicBoolean(false);

    /**
     * Create a new {@link ImGuiLayer} with the given window pointer.
     * @param windowPtr the window pointer to put the layer in
     */
    public ImGuiLayer(long windowPtr) {
        this.windowPtr = windowPtr;
        EditorEventHandler.init();
    }

    /**
     * Init DearImGui backend.
     * @param glslVer the GLSL version to use
     */
    public void initImGui(String glslVer) {
        ImGui.createContext();
        this.io = ImGui.getIO();
        guiFont(io);
        io.setBackendFlags(ImGuiBackendFlags.HasMouseCursors);
        registerCallbacks();
        io.setSetClipboardTextFn(new ImStrConsumer() {
           @Override
           public void accept(final String s) {
               glfwSetClipboardString(windowPtr, s);
           }
        });
        io.setGetClipboardTextFn(new ImStrSupplier() {
           @Override
           public String get() {
               final String clipboardString = glfwGetClipboardString(windowPtr);
               return Objects.requireNonNullElse(clipboardString, "");
           }
        });
        io.setIniFilename(UserPreference.getEditorLayoutFilepath());
        io.addConfigFlags(ImGuiConfigFlags.ViewportsEnable);
        io.setConfigFlags(ImGuiConfigFlags.DockingEnable);
        imGuiGlfw.init(windowPtr, true);
        imGuiGl3.init(glslVer);
        EditorIcons.init();
    }

    private void registerCallbacks() {
        glfwSetMouseButtonCallback(windowPtr, (w, button, action, mods) -> {
            final boolean[] mouseDown = new boolean[5];
            mouseDown[0] = button == GLFW_MOUSE_BUTTON_1 && action != GLFW_RELEASE;
            mouseDown[1] = button == GLFW_MOUSE_BUTTON_2 && action != GLFW_RELEASE;
            mouseDown[2] = button == GLFW_MOUSE_BUTTON_3 && action != GLFW_RELEASE;
            mouseDown[3] = button == GLFW_MOUSE_BUTTON_4 && action != GLFW_RELEASE;
            mouseDown[4] = button == GLFW_MOUSE_BUTTON_5 && action != GLFW_RELEASE;
            io.setMouseDown(mouseDown);
            boolean wantCaptureMouse = io.getWantCaptureMouse();
            if (!wantCaptureMouse && mouseDown[1]) {
                ImGui.setWindowFocus(null);
            }
            boolean SEVWantMouse = SceneEditorViewport.getWantCaptureMouse();
            if (action == GLFW_RELEASE || !wantCaptureMouse || SEVWantMouse || prioritizeEngineInputCallback.get()) {
                MouseListener.mouseButtonCallback(w, button, action, mods);
            }
        });
        glfwSetScrollCallback(windowPtr, (w, x, y) -> {
            boolean wantCaptureMouse = io.getWantCaptureMouse();
            if (!wantCaptureMouse && (Math.abs(x) > 0 || Math.abs(y) > 0)) {
                ImGui.setWindowFocus(null);
            }
            boolean SEVWantMouse = SceneEditorViewport.getWantCaptureMouse();
            if (!wantCaptureMouse || SEVWantMouse || prioritizeEngineInputCallback.get()) {
                MouseListener.mouseScrollCallback(w, x, y);
            }
        });
    }

    private void guiFont(ImGuiIO io) {
        final ImFontAtlas fontAtlas = io.getFonts();
        final ImFontConfig fontConfig = new ImFontConfig();
        fontConfig.setPixelSnapH(true);
        ImFontGlyphRangesBuilder glyphRangesBuilder = new ImFontGlyphRangesBuilder();
        glyphRangesBuilder.addRanges(fontAtlas.getGlyphRangesDefault());
        glyphRangesBuilder.addRanges(fontAtlas.getGlyphRangesVietnamese());
        fontConfig.setGlyphRanges(glyphRangesBuilder.buildRanges());
        AssetReference assetReference = new AssetReference(Settings.FontPath.NotoSansMono);
        try (InputStream stream = UnifiedPaths.getAssetStream(assetReference.resolvedPath())) {
            byte[] fontData = stream.readAllBytes();
            fontAtlas.addFontFromMemoryTTF(fontData, FontPT.pointToPixel(12), fontConfig);
        } catch (IOException e) {
            System.err.println("ImGui failed to read font from '" + assetReference.canonicalPath() + "'");
            fontAtlas.addFontDefault();
        }
        fontAtlas.build();
        fontConfig.destroy();
    }

    /**
     * The render pipeline of the Editor UI.
     * @param dt delta time
     * @param currentScene the current scene.
     */
    public void update(float dt, Scene currentScene) {
        if (dt < 0.0f) return;
        imGuiGlfw.newFrame();
        imGuiGl3.newFrame();
        ImGui.newFrame();
        renderDocking();
        if (exitFrameEarly) {
            exitFrameEarly = false;
            ImGui.endFrame();
            return;
        }
        ResourcePanel.imgui();
        SceneEditorViewport.imgui();
        Properties.imgui();
        SceneTree.imgui();
        BottomPanel.imgui();
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0,0, Window.getWidth(), Window.getHeight());
        glClearColor(0, 0,0,1);
        glClear(GL_COLOR_BUFFER_BIT);
        wantedCaptureMouse.set(io.getWantCaptureMouse());
        wantedCaptureKey.set(io.getWantCaptureKeyboard());
        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());
        if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            final long backupWindowPtr = glfwGetCurrentContext();
            ImGui.updatePlatformWindows();
            ImGui.renderPlatformWindowsDefault();
            glfwMakeContextCurrent(backupWindowPtr);
        }
    }

    private void renderDocking() {
        int winFlag = ImGuiWindowFlags.MenuBar | ImGuiWindowFlags.NoDocking | ImGuiWindowFlags.NoTitleBar
                | ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove |
                ImGuiWindowFlags.NoBringToFrontOnFocus | ImGuiWindowFlags.NoNavFocus;

        ImGuiViewport mainViewport = ImGui.getMainViewport();
        ImGui.setNextWindowPos(mainViewport.getWorkPosX(), mainViewport.getWorkPosY());
        ImGui.setNextWindowSize(mainViewport.getWorkSizeX(), mainViewport.getWorkSizeY());
        ImGui.setNextWindowViewport(mainViewport.getID());

        ImGui.setNextWindowPos(0.0f, 0.0f);
        ImGui.setNextWindowSize(Window.getWidth(), Window.getHeight());

        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0.0f);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0.0f);
        ImGui.begin(DockID, new ImBoolean(true), winFlag);
        ImGui.popStyleVar(2);
        int id = ImGui.getID(DockID);
        ImGui.dockSpace(id);

        if (!DefaultEditorLayout.dockingValid(id) || resetLayout) {
            DefaultEditorLayout.resetLayout(id);
            resetLayout = false;
            exitFrameEarly = true;
        }

        MenuBar.imgui();
        ImGui.end();
    }

    /**
     * Get the GLFW implement of DearImgui.
     * @return the GLFW instance used by DearImGui backend
     */
    public ImGuiImplGlfw getImGuiGlfw() {
        return imGuiGlfw;
    }

    /**
     * Get the openGL implement of DearImGui.
     * @return the openGL implement of DearImGui backend
     */
    public ImGuiImplGl3 getImGuiGl3() {
        return imGuiGl3;
    }

    /**
     * Request the Editor UI to reset its layout.
     */
    public static void resetLayout() {
        resetLayout = true;
    }

    /**
     * Check if the Editor want mouse capture or not.
     * @return true if wanted mouse capture
     */
    public static boolean editorWantCaptureMouse() {
        return wantedCaptureMouse.get();
    }

    /**
     * Check if the Editor want keyboard capture or not.
     * @return true if wanted keyboard capture
     */
    public static boolean editorWantCaptureKeyboard() {
        return wantedCaptureKey.get();
    }

    /**
     * Should the editor prioritize the input to be consumed by the Editor's widget or not.
     * @param prioritize the priority state
     */
    public static void prioritizeEngineInputCallback(boolean prioritize) {
        prioritizeEngineInputCallback.set(prioritize);
    }
}
