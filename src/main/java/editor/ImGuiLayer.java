package editor;

import TheCellBeyond.MouseListener;
import TheCellBeyond.Window;
import editor.preference.UserPreference;
import imgui.*;
import imgui.callback.ImStrConsumer;
import imgui.callback.ImStrSupplier;
import imgui.flag.*;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.type.ImBoolean;
import scene.Scene;
import org.joml.Math;
import utility.AssetReference;
import utility.FontPT;
import utility.UnifiedPaths;
import utility.Settings;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;

public class ImGuiLayer {
    private static final String DOCK_ID = "###EDITOR_DOCK";
    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
    private final long windowPtr;
    private final SceneEditorViewport sceneEditorViewport;
    private ImGuiIO io;
    private static boolean resetLayout = false;
    private static boolean exitFrameEarly = false;
    private static final AtomicBoolean wantedCaptureMouse = new AtomicBoolean(false);
    private static final AtomicBoolean wantedCaptureKey = new AtomicBoolean(false);
    private static final AtomicBoolean prioritizeEngineInputCallback = new AtomicBoolean(false);

    public ImGuiLayer(long windowPtr) {
        this.sceneEditorViewport = new SceneEditorViewport();
        this.windowPtr = windowPtr;
    }

    public void initImGui(String glslVer) {
        ImGui.createContext();
        this.io = ImGui.getIO();
        guiFont(io);
        io.setBackendFlags(ImGuiBackendFlags.HasMouseCursors);

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
            boolean SEVWantMouse = sceneEditorViewport.getWantCaptureMouse();
            if (!wantCaptureMouse || SEVWantMouse || prioritizeEngineInputCallback.get()) {
                MouseListener.mouseButtonCallback(w, button, action, mods);
            }
        });

        glfwSetScrollCallback(windowPtr, (w, x, y) -> {
            boolean wantCaptureMouse = io.getWantCaptureMouse();
            if (!wantCaptureMouse && (Math.abs(x) > 0 || Math.abs(y) > 0)) {
                ImGui.setWindowFocus(null);
            }
            boolean SEVWantMouse = sceneEditorViewport.getWantCaptureMouse();
            if (!wantCaptureMouse || SEVWantMouse || prioritizeEngineInputCallback.get()) {
                MouseListener.mouseScrollCallback(w, x, y);
            }
        });

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

    public void guiFont(ImGuiIO io) {
        final ImFontAtlas fontAtlas = io.getFonts();
        final ImFontConfig fontConfig = new ImFontConfig();
        fontConfig.setPixelSnapH(true);
        ImFontGlyphRangesBuilder glyphRangesBuilder = new ImFontGlyphRangesBuilder();
        glyphRangesBuilder.addRanges(fontAtlas.getGlyphRangesDefault());
        glyphRangesBuilder.addRanges(fontAtlas.getGlyphRangesVietnamese());

        fontConfig.setGlyphRanges(glyphRangesBuilder.buildRanges());

        // Get font data
        AssetReference assetReference = new AssetReference(Settings.FontPath.NotoSansMono);
        UnifiedPaths resolver;

        if (!UnifiedPaths.isInitialized()) {
            UnifiedPaths.initialize(null);
        }
        resolver = UnifiedPaths.get();

        try (InputStream stream = resolver.getAssetStream(assetReference.resolvedPath())) {
            byte[] fontData = stream.readAllBytes();
            fontAtlas.addFontFromMemoryTTF(fontData, FontPT.pointToPixel(12), fontConfig);
        } catch (IOException e) {
            System.err.println("ImGui failed to read font from '" + assetReference.canonicalPath() + "'");
            fontAtlas.addFontDefault();
        }

        fontAtlas.build();
        fontConfig.destroy();
    }

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

        if (currentScene != null) currentScene.imgui();
        sceneEditorViewport.imgui();
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
        ImGui.begin(DOCK_ID, new ImBoolean(true), winFlag);
        ImGui.popStyleVar(2);
        int id = ImGui.getID(DOCK_ID);
        ImGui.dockSpace(id);

        if (!DefaultEditorLayout.dockingValid(id) || resetLayout) {
            DefaultEditorLayout.resetLayout(id);
            resetLayout = false;
            exitFrameEarly = true;
        }

        MenuBar.imgui();
        ImGui.end();
    }

    public ImGuiImplGlfw getImGuiGlfw() {
        return imGuiGlfw;
    }

    public ImGuiImplGl3 getImGuiGl3() {
        return imGuiGl3;
    }

    public SceneEditorViewport getSceneEditorViewPort() {
        return this.sceneEditorViewport;
    }

    public static void resetLayout() {
        resetLayout = true;
    }

    public static boolean editorWantCaptureMouse() {
        return wantedCaptureMouse.get();
    }

    public static boolean editorWantCaptureKeyboard() {
        return wantedCaptureMouse.get();
    }

    public static void prioritizeEngineInputCallback(boolean prioritize) {
        prioritizeEngineInputCallback.set(prioritize);
    }
}
