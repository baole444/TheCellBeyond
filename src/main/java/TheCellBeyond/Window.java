package TheCellBeyond;

import editor.ImGuiLayer;
import editor.Properties;
import editor.SceneTree;
import editor.StartupWindow;
import editor.preference.RecentProject;
import editor.preference.UserPreference;
import project.Project;
import project.ProjectPreference;
import eventviewer.EngineEventCallback;
import eventviewer.EngineEventListener;
import eventviewer.event.Event;
import imgui.ImGui;
import org.joml.Vector2i;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWWindowCloseCallback;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;
import org.lwjgl.opengl.GL;
import physic2d.Physic2D;
import render.*;
import render.text.FontManager;
import scene.SceneEditor;
import scene.Scene;
import scene.SceneInit;
import utility.AssetsPool;
import editor.dialog.ExitConfirmDialog;
import utility.Settings;
import utility.log.EngineLog;

import java.awt.*;
import java.util.List;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class Window implements EngineEventListener {
    private static final EngineLog LOGGER = new EngineLog(Window.class);
    private int width;
    private int height;
    private final String title;

    private long windowPtr;
    public float r, g, b, a;
    private static Window window = null;
    private static Scene currentScene;
    private static String currentSceneName;
    private boolean runtimeMode = false;

    private ImGuiLayer imGuiLayer;
    private FrameBuffer frameBuffer;
    private ObjectSelection objectSelection;

    private final IconLoader iconFile = IconLoader.loadIcon(Settings.TexturePath.TCBIcon);

    private boolean shouldClose;
    private boolean forceClose = false;

    private long soundContext;
    private long audioDevice;

    private boolean projectLoaded = false;

    public Window() {
        this.width = 640;
        this.height = 480;
        this.title = "The Cell Beyond";
        EngineEventCallback.register(this);

        r = 0.027f;
        g = 0.122f;
        b = 0.067f;
        a = 1.0f;
    }

    public static void changeScene(SceneInit sceneInit) {
        if (currentScene != null) currentScene.destroy();

        Properties.clearSelection();
        SceneTree.clearSelection();

        currentScene = new Scene(sceneInit);
        currentScene.loadLevel();
        currentScene.init();
        currentScene.start();
    }

    public static Window get() {
        // make new windows at begin
        if (Window.window == null) {
            Window.window = new Window();
        }
        return Window.window;
    }

    public void run() {
        LOGGER.info("Starting LWJGL " + Version.getVersion());

        initWindow();

        if (!projectLoaded) {
            StartupWindow.show(windowPtr, imGuiLayer, width, height);

            projectLoaded = (Project.currentProject() != null && Project.projectRoot() != null);

            if (glfwWindowShouldClose(windowPtr)) {
                endScr();
                return;
            }
        }

        if (projectLoaded) {
            String projectDetail = " - [" + Project.preference().name() + "] [" + Project.projectRoot() + "]";

            glfwSetWindowTitle(windowPtr, this.title + projectDetail);
            loop();
        }

        String renderer = glGetString(GL_RENDERER);
        String version = glGetString(GL_VERSION);

        LOGGER.info("Active GPU: " + renderer + " Driver version: " + version);

        endScr();
        glfwSetErrorCallback(null).free();
    }

    private void initWindow() {
        //error return
        GLFWErrorCallback.createPrint(System.err).set();

        //start GLFW
        if (!glfwInit()) {
            LOGGER.error("Unable to start GLFW.");
            System.exit(-1);
        }

        String glslVer = "#version 330 core";
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);

        // Update width, height to current screen resolution
        // Make it smaller a bit
        width = getScrSize().x;
        height = getScrSize().y;

        //config GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_MAXIMIZED, GLFW_FALSE);


        // Spawn window
        windowPtr = glfwCreateWindow(width, height, title, NULL, NULL);
        LOGGER.info("Creating new window, dimension: " + width + " x " + height);
        if (windowPtr == NULL) {
            LOGGER.error("Failed to spawn window.");
            System.exit(-1);
        }
        glfwSetWindowSizeCallback(windowPtr, (window, w, h) -> {
            if (w <= 0 || h <= 0) return;

            width = w;
            height = h;

            frameBuffer.resize(width, height);
            objectSelection.resize(width, height);
            if (currentScene != null && currentScene.viewport() != null && !runtimeMode) {
                currentScene.viewport().updateAspectRatio(width, height);
            }
            glViewport(0, 0, width, height);
        });

        glfwSetCursorPosCallback(windowPtr, MouseListener::mousePosCallback);
        glfwSetMouseButtonCallback(windowPtr, MouseListener::mouseButtonCallback);
        glfwSetScrollCallback(windowPtr, MouseListener::mouseScrollCallback);
        glfwSetKeyCallback(windowPtr, KeyListener::keyCallback);
        glfwSetCharCallback(windowPtr, KeyListener::charCallback);

        glfwSetInputMode(windowPtr, GLFW_IME, GLFW_TRUE);

        //exit callback setting
        glfwSetWindowCloseCallback(windowPtr, new GLFWWindowCloseCallback() {
            @Override
            public void invoke(long l) {
                if (forceClose) {
                    glfwSetWindowShouldClose(windowPtr, true);
                    return;
                }

                if (UserPreference.editorPreferences().autoSaveOnExit()) currentScene.saveLevel();

                glfwSetWindowShouldClose(windowPtr, false);
                shouldClose = ExitConfirmDialog.exitDialog();
                if (shouldClose) {
                    glfwSetWindowShouldClose(windowPtr, true);
                }
            }
        });

        glfwMakeContextCurrent(windowPtr);
        glfwSwapInterval(1);
        glfwShowWindow(windowPtr);
        String defaultAudioDevice = alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER);
        audioDevice = alcOpenDevice(defaultAudioDevice);

        int[] attbs = {0};
        soundContext = alcCreateContext(audioDevice, attbs);
        alcMakeContextCurrent(soundContext);


        ALCCapabilities alcCapabilities = ALC.createCapabilities(audioDevice);
        ALCapabilities alCapabilities = AL.createCapabilities(alcCapabilities);

        if (!alCapabilities.OpenAL10) {
            LOGGER.warning("OpenAL10 is not supported on this device");
            System.exit(-2);
        }

        GL.createCapabilities();
        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

        frameBuffer = new FrameBuffer(width, height);
        objectSelection = new ObjectSelection(width, height);

        glViewport(0, 0, width, height);

        imGuiLayer = new ImGuiLayer(windowPtr);
        imGuiLayer.initImGui(glslVer);

        glfwMaximizeWindow(windowPtr);

        if (iconFile != null) {
            GLFWImage icon = GLFWImage.malloc();
            GLFWImage.Buffer bufferIcon = GLFWImage.malloc(1);
            icon.set(iconFile.width(), iconFile.height(), iconFile.icon());
            bufferIcon.put(0, icon);
            glfwSetWindowIcon(windowPtr, bufferIcon);
        }

        projectLoaded = (Project.currentProject() != null && Project.projectRoot() != null);

        if (projectLoaded) Window.changeScene(new SceneEditor());

    }

    /**
     * Return current active display size that the windows is on.
    */
    public Vector2i getScrSize() {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int w = device.getDisplayMode().getWidth();
        int h = device.getDisplayMode().getHeight();

        return new Vector2i(w, h);
    }

    private void endScr(){
        FontManager.get().dispose();
        AssetsPool.clearCache();
        RendererState.cleanup();

        imGuiLayer.getImGuiGl3().shutdown();
        imGuiLayer.getImGuiGlfw().shutdown();
        ImGui.destroyContext();

        alcDestroyContext(soundContext);
        alcCloseDevice(audioDevice);

        frameBuffer.dispose();
        glfwFreeCallbacks(window.windowPtr);
        glfwDestroyWindow(window.windowPtr);

        glfwTerminate();
    }

    public void loop () {
        float beginTime = (float)glfwGetTime();
        float endTime;
        float dt = -1.0f;

        Shader defaultShader = AssetsPool.loadShader(Settings.ShaderPath.DefaultTextureShader);
        Shader objectSelectShader = AssetsPool.loadShader(Settings.ShaderPath.ObjectSelectionShader);
        Shader debugLineShader = AssetsPool.loadShader(Settings.ShaderPath.DebugLine2Shader);
        DebugDraw.init(debugLineShader);
        RendererState rendererState = RendererState.get();

        while (!glfwWindowShouldClose(windowPtr)) {
            glfwPollEvents(); //poll events

            if (dt >= 0.0f) {
                rendererState.setRenderPass(RendererState.RenderPass.SELECTION);
                rendererState.setShader(objectSelectShader);
                objectSelection.useWrite();
                glViewport(0, 0, frameBuffer.getWidth(), frameBuffer.getHeight());
                glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
                currentScene.render();
                objectSelection.detachWrite();

                rendererState.setRenderPass(RendererState.RenderPass.NORMAL);
                rendererState.setShader(defaultShader);

                DebugDraw.startFrame();
                frameBuffer.use();
                glClearColor(r, g, b, a);
                glClear(GL_COLOR_BUFFER_BIT);

                if (runtimeMode) {
                    currentScene.update(dt);
                } else {
                    currentScene.editorUpdate(dt);
                }
                currentScene.render();
                DebugDraw.draw();

                frameBuffer.detach();
                imGuiLayer.update(dt, currentScene);
            }

            MouseListener.endFrame();
            KeyListener.endFrame();

            glfwSwapBuffers(windowPtr);

            endTime = (float)glfwGetTime();
            dt = endTime - beginTime;
            beginTime = endTime;

            if (forceClose) glfwSetWindowShouldClose(windowPtr, true);
        }
    }

    @Override
    public void onEventEmit(Object object, Event event) {
        switch (event.type) {
            case ENGINE_START -> {
                runtimeMode = true;
                currentScene.saveLevel();
                Window.changeScene(new SceneEditor());
                LOGGER.info("Test play started.");
            }
            case ENGINE_END -> {
                runtimeMode = false;
                Window.changeScene(new SceneEditor());
                LOGGER.info("Test play stopped.");
            }
            case LEVEL_LOAD -> {
                runtimeMode = false;

                Window.changeScene(new SceneEditor());
                LOGGER.debug("Loading current level...");
            }
            case LEVEL_SAVE -> {
                currentScene.saveLevel();
                LOGGER.debug("Saving current level...");
            }
            case PROJECT_LOAD -> {
                String projectPath = object.toString();
                LOGGER.info("Loading project file at " + projectPath);
                Project.loadFromYaml(projectPath);
                projectLoaded = (Project.currentProject() != null && Project.projectRoot() != null);

                if (projectLoaded) {
                    MouseListener.setStartupMode(false);
                    String projectDetail = " - [" + Project.preference().name() + "] [" + Project.projectRoot() + "]";
                    glfwSetWindowTitle(windowPtr, this.title + projectDetail);
                    List<String> availScenes = Project.getSceneNames();
                    if (availScenes.isEmpty()) {
                        setCurrentSceneName(null);
                        Window.changeScene(new SceneEditor());
                        return;
                    }

                    RecentProject project = UserPreference.recentProject(projectPath);
                    String lastOpenScene = project != null ? project.lastOpenScene() : null;
                    if (lastOpenScene == null || !availScenes.contains(lastOpenScene)) {
                        lastOpenScene = availScenes.getFirst();
                    }

                    setCurrentSceneName(lastOpenScene);
                    Window.changeScene(new SceneEditor());
                }
            }
            case SCENE_LOAD -> {
                this.runtimeMode = false;
                String sceneName = (String) object;
                setCurrentSceneName(sceneName);
                String path = Project.projectYMLPath();
                if (path != null) {
                    ProjectPreference preference = Project.preference();
                    RecentProject update = new RecentProject(preference.name(), path, sceneName);
                    UserPreference.updateRecentProject(update);
                }

                Window.changeScene(new SceneEditor());
                LOGGER.debug("Requested to load Scene: " + sceneName);
            }
        }
    }

    public long getWindowPtr() {
        return windowPtr;
    }

    public static Scene getScene() {
        return currentScene;
    }

    public static int getWidth() {
        return get().width;
    }

    public static int getHeight() {
        return get().height;
    }

    public static FrameBuffer getFrameBuffer() {
        return get().frameBuffer;
    }

    public static float getTargetAspectRatio() {
        return Project.getGameAspectRatio();
    }

    public static ImGuiLayer getImGuiLayer() {
        return get().imGuiLayer;
    }

    public static ObjectSelection getObjectSelection() {
        return get().objectSelection;
    }

    public static String getCurrentSceneName() {
        return currentSceneName;
    }

    public static void setCurrentSceneName(String currentSceneName) {
        Window.currentSceneName = currentSceneName;
    }

    public static Physic2D getPhysic2D() {
        return currentScene.getPhysic2D();
    }

    public boolean isRuntimeMode() {
        return runtimeMode;
    }

    public void forceClose() {
        forceClose = true;
    }
}
