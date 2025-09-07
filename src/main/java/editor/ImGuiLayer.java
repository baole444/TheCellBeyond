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
import render.ObjectSelection;
import scene.Scene;
import org.joml.Math;
import utility.AssetReference;
import utility.PathResolver;
import utility.Settings;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

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
    private final Properties properties;
    private final SceneTree sceneTree;
    private ImGuiIO io;

    private static boolean resetLayout = false;

    public ImGuiLayer(long windowPtr, ObjectSelection objectSelection) {
        this.sceneEditorViewport = new SceneEditorViewport();
        this.windowPtr = windowPtr;
        this.properties = new Properties(objectSelection);
        this.sceneTree = new SceneTree();
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

            if (!io.getWantCaptureMouse() && mouseDown[1]) {
                ImGui.setWindowFocus(null);
            }

            if (!io.getWantCaptureMouse() || sceneEditorViewport.getWantCaptureMouse()) {
                MouseListener.mouseButtonCallback(w, button, action, mods);
            }
        });

        glfwSetScrollCallback(windowPtr, (w, x, y) -> {
            if (!io.getWantCaptureMouse() && (Math.abs(x) > 0 || Math.abs(y) > 0)) {
                ImGui.setWindowFocus(null);
            }

            if (!io.getWantCaptureMouse() || sceneEditorViewport.getWantCaptureMouse()) {
                MouseListener.mouseScrollCallback(w, x, y);
            } else {
                MouseListener.clear();
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
    }

    public void guiFont(ImGuiIO io) {
        final ImFontAtlas fontAtlas = io.getFonts();

        // Font config must be destroyed after call
        final ImFontConfig fontConfig = new ImFontConfig();

        // glyphs range

        //Merge font
        //fontConfig.setMergeMode(true); //For multiple font, turn this back on
        fontConfig.setPixelSnapH(true);

        // Get font data
        AssetReference assetReference = new AssetReference(Settings.PATH.CONSOLA);
        PathResolver resolver;

        if (!PathResolver.isInitialized()) {
            PathResolver.initialize(null);
        }
        resolver = PathResolver.get();

        try (InputStream stream = resolver.getAssetStream(assetReference.getResolvedPath())) {
            byte[] fontData = stream.readAllBytes();
            fontAtlas.addFontFromMemoryTTF(fontData, 14, fontConfig);
        } catch (IOException e) {
            System.err.println("ImGui failed to read font from '" + assetReference.getCanonicalPath() + "'");
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
        currentScene.imgui();
        sceneEditorViewport.imgui();
        properties.imgui();
        sceneTree.imgui();
        BottomPanel.imgui();

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0,0, Window.getWidth(), Window.getHeight());
        glClearColor(0, 0,0,1);
        glClear(GL_COLOR_BUFFER_BIT);

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

        // Make viewport the main windows
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

    public Properties loadProperties() {
        return this.properties;
    }

    public SceneEditorViewport getGameViewPort() {
        return this.sceneEditorViewport;
    }

    public static void resetLayout() {
        resetLayout = true;
    }
}
