package TheCellBeyond;

import TheCellBeyond.internal.LogicServer;
import editor.ImGuiLayer;
import editor.StartupWindow;
import editor.preference.UserPreference;
import eventviewer.event.Event;
import org.joml.Vector4f;
import project.ClearColor;
import project.Project;
import eventviewer.EngineEventCallback;
import eventviewer.EngineEventListener;
import eventviewer.event.EditorEvent;
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
import render.*;
import render.text.FontManager;
import scene.SceneManager;
import utility.AssetsPool;
import editor.dialog.ExitConfirmDialog;
import utility.Settings;
import utility.log.EngineLog;

import java.awt.*;
import java.util.Objects;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * The main window of TCB, responsible for initializing GLFW window and IO callbacks.
 */
public final class Window implements EngineEventListener {
    private static final EngineLog LOGGER = new EngineLog(Window.class);
    private int width;
    private int height;
    private final String title;

    private long windowPtr;
    public float r, g, b, a;
    public boolean overrideClearColor = false;
    private static Window window = null;

    private ImGuiLayer imGuiLayer;
    private FrameBuffer frameBuffer;
    private ObjectSelection objectSelection;

    private final IconLoader iconFile = IconLoader.loadIcon(Settings.TexturePath.TCBIcon);
    private boolean shouldClose;
    private boolean forceClose = false;
    private long soundContext;
    private long audioDevice;
    private boolean projectLoaded = false;

    /**
     * Create a new window instance and register it with the Engine Event Callback.
     */
    public Window() {
        this.width = 640;
        this.height = 480;
        this.title = "TheCellBeyond";
        r = 0.027f;
        g = 0.122f;
        b = 0.067f;
        a = 1.0f;
        register();
    }

    /**
     * Get the current window instance.
     * @return {@link Window} instance
     */
    public static Window get() {
        if (Window.window == null) Window.window = new Window();
        return Window.window;
    }

    /**
     * Start the window life cycle.
     */
    public void run() {
        LOGGER.info("Starting LWJGL " + Version.getVersion());
        initWindow();
        Renderer.init();
        if (!projectLoaded) {
            StartupWindow.show(windowPtr, imGuiLayer, width, height);
            projectLoaded = (Project.currentProject() != null && Project.projectRoot() != null);

            if (glfwWindowShouldClose(windowPtr)) {
                endScreen();
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

        endScreen();
        Objects.requireNonNull(glfwSetErrorCallback(null)).free();
    }

    private void initWindow() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) {
            LOGGER.error("Failed to initialize window: GLFW init failed");
            System.exit(-1);
        }
        String glslVer = "#version 330 core";
        Vector2i windowSize = screenSize();
        width = windowSize.x;
        height = windowSize.y;
        applyWindowHints();
        windowPtr = glfwCreateWindow(width, height, title, NULL, NULL);
        LOGGER.info("Creating new window, dimension: " + width + " x " + height);
        if (windowPtr == NULL) {
            LOGGER.error("Failed to create GLFW window: null pointer");
            System.exit(-1);
        }
        setupWindowCallback();
        glfwMakeContextCurrent(windowPtr);
        glfwSwapInterval(1);
        glfwShowWindow(windowPtr);
        setupAudioDevice();

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
    }

    private void setupAudioDevice() {
        String defaultAudioDevice = alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER);
        audioDevice = alcOpenDevice(Objects.requireNonNull(defaultAudioDevice));
        int[] ATTB = {0};
        soundContext = alcCreateContext(audioDevice, ATTB);
        alcMakeContextCurrent(soundContext);
        ALCCapabilities alcCapabilities = ALC.createCapabilities(audioDevice);
        ALCapabilities alCapabilities = AL.createCapabilities(alcCapabilities);
        if (!alCapabilities.OpenAL10) {
            LOGGER.warning("OpenAL10 is not supported on this device");
            System.exit(-2);
        }
    }

    private static void applyWindowHints() {
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_MAXIMIZED, GLFW_FALSE);
    }

    private void setupWindowCallback() {
        glfwSetWindowSizeCallback(windowPtr, (window, w, h) -> {
            if (w <= 0 || h <= 0) return;
            width = w;
            height = h;
            frameBuffer.resize(width, height);
            objectSelection.resize(width, height);
            if (LogicServer.currentScene() != null && LogicServer.currentScene().viewport() != null && !LogicServer.runtimeMode()) {
                LogicServer.currentScene().viewport().updateAspectRatio(width, height);
            }
            glViewport(0, 0, width, height);
        });

        glfwSetCursorPosCallback(windowPtr, MouseListener::mousePosCallback);
        glfwSetMouseButtonCallback(windowPtr, MouseListener::mouseButtonCallback);
        glfwSetScrollCallback(windowPtr, MouseListener::mouseScrollCallback);
        glfwSetKeyCallback(windowPtr, KeyListener::keyCallback);
        glfwSetCharCallback(windowPtr, KeyListener::charCallback);
        glfwSetInputMode(windowPtr, GLFW_IME, GLFW_TRUE);
        glfwSetWindowCloseCallback(windowPtr, new GLFWWindowCloseCallback() {
            @Override
            public void invoke(long l) {
                if (forceClose) {
                    glfwSetWindowShouldClose(windowPtr, true);
                    return;
                }
                if (projectLoaded && UserPreference.editorPreferences().autoSaveOnExit()) SceneManager.saveCurrentScene();
                glfwSetWindowShouldClose(windowPtr, false);
                shouldClose = ExitConfirmDialog.exitDialog();
                if (shouldClose) {
                    glfwSetWindowShouldClose(windowPtr, true);
                }
            }
        });
    }

    /**
     * Return current active display size that the windows is on.
    */
    public static Vector2i screenSize() {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int w = device.getDisplayMode().getWidth();
        int h = device.getDisplayMode().getHeight();

        return new Vector2i(w, h);
    }

    private void endScreen() {
        FontManager.get().dispose();
        AssetsPool.clearCache();
        RendererState.cleanup();
        EngineEventCallback.dispose();

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

    /**
     * Engine main loop.
     */
    public void loop () {
        float beginTime = (float) glfwGetTime();
        float endTime;
        float dt = -1.0f;

        Shader defaultShader = AssetsPool.loadShader(Settings.ShaderPath.DefaultTextureShader);
        Shader objectSelectShader = AssetsPool.loadShader(Settings.ShaderPath.ObjectSelectionShader);
        Shader debugLineShader = AssetsPool.loadShader(Settings.ShaderPath.DebugLine2Shader);
        DebugDraw.init(debugLineShader);
        RendererState rendererState = RendererState.get();

        while (!glfwWindowShouldClose(windowPtr)) {
            glfwPollEvents();

            LogicServer.updatePhysic(dt);
            if (dt >= 0.0f) {
                DebugDraw.startFrame();
                LogicServer.update(dt);
                objectSelectionPass(rendererState, objectSelectShader);
                normalPass(rendererState, defaultShader, dt);
                imGuiLayer.update(dt, LogicServer.currentScene());
            }

            MouseListener.endFrame();
            KeyListener.endFrame();
            glfwSwapBuffers(windowPtr);
            endTime = (float) glfwGetTime();
            dt = endTime - beginTime;
            beginTime = endTime;
            if (forceClose) glfwSetWindowShouldClose(windowPtr, true);
        }
    }

    private void objectSelectionPass(RendererState rendererState, Shader objectSelectShader) {
        rendererState.setRenderPass(RendererState.RenderPass.SELECTION);
        rendererState.setShader(objectSelectShader);
        objectSelection.useWrite();
        glViewport(0, 0, frameBuffer.getWidth(), frameBuffer.getHeight());
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        Renderer.get().render();
        objectSelection.detachWrite();
    }

    private void normalPass(RendererState rendererState, Shader defaultShader, float dt) {
        rendererState.setRenderPass(RendererState.RenderPass.NORMAL);
        rendererState.setShader(defaultShader);
        frameBuffer.use();
        if (overrideClearColor) {
            glClearColor(r, g, b, a);
        } else {
            Vector4f clearColor = Project.preference().clearColor().toVector();
            glClearColor(clearColor.x, clearColor.y, clearColor.z, clearColor.w);
        }

        glClear(GL_COLOR_BUFFER_BIT);
        Renderer.get().render();
        DebugDraw.draw();
        frameBuffer.detach();
    }

    @Override
    public void onEventEmit(Object object, Event event) {
        if (!(event instanceof EditorEvent editorEvent)) return;
        if (editorEvent.type != EditorEvent.Type.ProjectLoaded) return;
        projectLoaded = Project.currentProject() != null && Project.projectRoot() != null;
        if (!projectLoaded) return;

        ClearColor clearColor = Project.preference().clearColor();
        r = clearColor.r();
        g = clearColor.g();
        b = clearColor.b();
        a = clearColor.a();

        String projectDetail = " - [" + Project.preference().name() + "] [" + Project.projectRoot() + "]";
        glfwSetWindowTitle(windowPtr, title + projectDetail);
    }

    /**
     * Get this window pointer.
     * @return the pointer address
     */
    public long getWindowPtr() {
        return windowPtr;
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

    public void forceClose() {
        forceClose = true;
    }
}
