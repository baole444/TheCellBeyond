package TheCellBeyond;

import TheCellBeyond.internal.LogicServer;
import TheCellBeyond.internal.RenderingServer;
import editor.EditorLayer;
import editor.dialogs.ExitConfirmDialog;
import editor.preference.UserPreference;
import eventviewer.EngineEventCallback;
import eventviewer.EngineEventListener;
import eventviewer.event.EditorEvent;
import eventviewer.event.Event;
import eventviewer.event.ProjectEvent;
import imgui.ImGui;
import org.joml.Vector2i;
import org.joml.Vector4f;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.Callback;
import org.lwjgl.system.Platform;
import physic2d.Physic2D;
import project.ClearColor;
import project.Project;
import project.RenderingSetting;
import project.VsyncMode;
import render.*;
import render.text.FontManager;
import scene.SceneManager;
import scripting.ScriptLoader;
import utility.AssetManager;
import utility.Settings;
import utility.log.EngineLog;
import utility.log.Stream2Log;

import java.awt.*;
import java.util.Objects;
import java.util.concurrent.locks.LockSupport;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.nfd.NativeFileDialog.NFD_Init;
import static org.lwjgl.util.nfd.NativeFileDialog.NFD_Quit;

/**
 * The main window of TheCellBeyond Engine, responsible for initializing GLFW window and IO callbacks.
 */
public final class Window implements EngineEventListener {
    private static final EngineLog Logger = new EngineLog(Window.class);
    /**
     * Engine main loop's average frame rate,
     * calculated using {@code accumulated frame/accumulated delta} at 1 delta interval.
     * <p>
     * This is not the FPS cap, adjusting this value only affect the displayed metric on screen.
     */
    public static float FPS = 0.0f;
    /**
     * Should the texture clear colour be overridden. Upon enabled (true), the {@link #r}{@link #g}{@link #b}{@link #a}
     * values will take effect as the clear colour until this is disabled (false) again.
     */
    public static boolean overrideClearColor = false;
    /**
     * The red component of the overriding texture clear colour, will take effect if {@link #overrideClearColor} is true.
     * <p>
     * This value is normalized internally to range of [0,1].
     */
    public float r;
    /**
     * The green component of the overriding texture clear colour, will take effect if {@link #overrideClearColor} is true.
     * <p>
     * This value is normalized internally to range of [0,1].
     */
    public float g;
    /**
     * The blue component of the overriding texture clear colour, will take effect if {@link #overrideClearColor} is true.
     * <p>
     * This value is normalized internally to range of [0,1].
     */
    public float b;
    /**
     * The alpha component of the overriding texture clear colour, will take effect if {@link #overrideClearColor} is true.
     * <p>
     * This value is normalized internally to range of (0,1).
     */
    public float a;
    private int width;
    private int height;
    private final String title;
    private long windowPtr;
    private static Window window = null;
    private EditorLayer editorLayer;
    private FrameBuffer frameBuffer;
    private ObjectSelection objectSelection;
    private final IconLoader iconFile = IconLoader.loadIcon(Settings.TexturePath.TCBIcon);
    private boolean shouldClose;
    private boolean forceClose = false;
    private long soundContext;
    private long audioDevice;
    private boolean projectLoaded = false;
    private static boolean noAudioSupport = true;
    private long targetFrameTime = 1_000_000_000L / 60;
    private final Vector4f clearColor = new Vector4f();

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
        Logger.info("Starting LWJGL " + Version.getVersion());
        initWindow();
        RenderingServer.init();
        Renderer.init();
        String renderer = glGetString(GL_RENDERER);
        String version = glGetString(GL_VERSION);
        Logger.info("Active GPU: " + renderer + " Driver version: " + version);
        EngineEventCallback.emit(Main.openProjectPath(), new EditorEvent(EditorEvent.Type.LoadProjectFromDisk));
        projectLoaded = Project.loaded();
        String projectDetail = " - [" + Project.preference().name() + "] [" + Project.projectRoot() + "]";
        glfwSetWindowTitle(windowPtr, this.title + projectDetail);
        loop();
        endScreen();
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

    public static EditorLayer getImGuiLayer() {
        return get().editorLayer;
    }

    public static ObjectSelection getObjectSelection() {
        return get().objectSelection;
    }

    public void forceClose() {
        forceClose = true;
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

    /**
     * Check if audio is supported.
     * @return false if not.
     */
    public static boolean noAudioSupport() {
        return noAudioSupport;
    }

    private void initWindow() {
        GLFWErrorCallback.createPrint(Stream2Log.err).set();
        if (!glfwInit()) throw new RuntimeException("Failed to initialize GLFW");
        String glslVer = "#version 330 core";
        Vector2i windowSize = screenSize();
        width = windowSize.x;
        height = windowSize.y;
        applyWindowHints();
        windowPtr = glfwCreateWindow(width, height, title, NULL, NULL);
        Logger.info("Creating new window, dimension: " + width + " x " + height);
        if (windowPtr == NULL) throw new RuntimeException("Failed to create editor window");
        setupWindowCallback();
        glfwMakeContextCurrent(windowPtr);
        applyVsync(VsyncMode.Enabled);
        glfwShowWindow(windowPtr);
        setupAudioDevice();
        GL.createCapabilities();
        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        NFD_Init();
        frameBuffer = new FrameBuffer(width, height);
        objectSelection = new ObjectSelection(width, height);
        glViewport(0, 0, width, height);
        editorLayer = new EditorLayer(windowPtr);
        editorLayer.initImGui(glslVer);
        glfwMaximizeWindow(windowPtr);
        loadIcon();
    }

    private void loadIcon() {
        if (iconFile == null) return;
        if (Platform.get() == Platform.LINUX || Platform.get() == Platform.FREEBSD) return;
        GLFWImage icon = GLFWImage.malloc();
        GLFWImage.Buffer bufferIcon = GLFWImage.malloc(1);
        icon.set(iconFile.width(), iconFile.height(), iconFile.icon());
        bufferIcon.put(0, icon);
        glfwSetWindowIcon(windowPtr, bufferIcon);
    }

    private void setupAudioDevice() {
        String defaultAudioDevice = alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER);
        if (defaultAudioDevice == null) {
            Logger.warning("No audio device found, audio will be disabled");
            return;
        }
        audioDevice = alcOpenDevice(defaultAudioDevice);
        if (audioDevice == 0) {
            Logger.warning("Failed to open audio device, audio will be disabled");
            return;
        }
        int[] ATTB = {0};
        soundContext = alcCreateContext(audioDevice, ATTB);
        if (soundContext == 0) {
            Logger.warning("Failed to create audio context, audio will be disabled");
            return;
        }
        alcMakeContextCurrent(soundContext);
        ALCCapabilities alcCapabilities = ALC.createCapabilities(audioDevice);
        ALCapabilities alCapabilities = AL.createCapabilities(alcCapabilities);
        if (!alCapabilities.OpenAL10) {
            Logger.warning("OpenAL10 is not supported on this device");
            return;
        }
        noAudioSupport = false;
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
        freeOld(glfwSetWindowSizeCallback(windowPtr, (_, w, h) -> {
            if (w <= 0 || h <= 0) return;
            width = w;
            height = h;
            frameBuffer.resize(width, height);
            objectSelection.resize(width, height);
            if (LogicServer.currentScene() != null && LogicServer.currentScene().viewport() != null && !LogicServer.runtimeMode()) {
                LogicServer.currentScene().viewport().updateAspectRatio(width, height);
            }
            glViewport(0, 0, width, height);
        }));
        freeOld(glfwSetCursorPosCallback(windowPtr, MouseListener::mousePosCallback));
        freeOld(glfwSetMouseButtonCallback(windowPtr, MouseListener::mouseButtonCallback));
        freeOld(glfwSetScrollCallback(windowPtr, MouseListener::mouseScrollCallback));
        freeOld(glfwSetKeyCallback(windowPtr, KeyListener::keyCallback));
        freeOld(glfwSetCharCallback(windowPtr, KeyListener::charCallback));
        glfwSetInputMode(windowPtr, GLFW_IME, GLFW_TRUE);
        freeOld(glfwSetWindowCloseCallback(windowPtr, _ -> {
            if (forceClose) {
                glfwSetWindowShouldClose(windowPtr, true);
                return;
            }
            if (projectLoaded && UserPreference.preferences().autoSaveOnExit()) SceneManager.saveCurrentScene();
            glfwSetWindowShouldClose(windowPtr, false);
            shouldClose = ExitConfirmDialog.exitDialog();
            if (shouldClose) glfwSetWindowShouldClose(windowPtr, true);
        }));

    }

    private static void freeOld(Callback old) {
        if (old != null) old.free();
    }

    private void endScreen() {
        FontManager.get().dispose();
        AssetManager.clearCache();
        RendererState.cleanup();
        EngineEventCallback.dispose();
        editorLayer.getImGuiGl3().shutdown();
        editorLayer.getImGuiGlfw().shutdown();
        ImGui.destroyContext();
        if (soundContext != 0) alcDestroyContext(soundContext);
        if (audioDevice != 0) alcCloseDevice(audioDevice);
        frameBuffer.dispose();
        glfwFreeCallbacks(window.windowPtr);
        glfwDestroyWindow(window.windowPtr);
        glfwTerminate();
        NFD_Quit();
        Objects.requireNonNull(glfwSetErrorCallback(null)).free();
    }

    private void loop() {
        float beginTime = (float) glfwGetTime();
        float endTime;
        float dt = -1.0f;
        long nextFrameDeadline = System.nanoTime();
        float accumulatedDT = 0.0f;
        int accumulatedFrame = 0;
        Shader defaultShader = AssetManager.getShader(AssetManager.loadShader(Settings.ShaderPath.DefaultTextureShader));
        Shader objectSelectShader = AssetManager.getShader(AssetManager.loadShader(Settings.ShaderPath.ObjectSelectionShader));
        Shader debugLineShader = AssetManager.getShader(AssetManager.loadShader(Settings.ShaderPath.DebugLine2Shader));
        DebugDraw.init(debugLineShader);
        RendererState rendererState = RendererState.get();
        while (!glfwWindowShouldClose(windowPtr)) {
            glfwPollEvents();
            ScriptLoader.processPending();
            LogicServer.updatePhysic(dt);
            Physic2D physic2D = LogicServer.currentScenePhysic2D();
            if (physic2D != null && LogicServer.runtimeMode()) RenderingServer.interpolationFactor = physic2D.interpolateAlpha();
            else RenderingServer.interpolationFactor = 1.0f;
            if (dt >= 0.0f) {
                accumulatedDT += dt;
                accumulatedFrame++;
                if (updateMetric(accumulatedFrame, accumulatedDT)) {
                    accumulatedDT = 0.0f;
                    accumulatedFrame = 0;
                }
                DebugDraw.startFrame();
                LogicServer.update(dt);
                RenderingServer.update();
                objectSelectionPass(rendererState, objectSelectShader);
                normalPass(rendererState, defaultShader);
                RenderingServer.postFrameClear();
                editorLayer.update(dt, LogicServer.currentScene());
            }
            MouseListener.endFrame();
            KeyListener.endFrame();
            glfwSwapBuffers(windowPtr);
            if (targetFrameTime > 0) {
                nextFrameDeadline += targetFrameTime;
                if (nextFrameDeadline < System.nanoTime() - targetFrameTime) nextFrameDeadline = System.nanoTime();
                long sleepUntil = nextFrameDeadline - 1_000_000L;
                if (System.nanoTime() < sleepUntil) LockSupport.parkNanos(sleepUntil - System.nanoTime());
                while (System.nanoTime() < nextFrameDeadline) Thread.onSpinWait();
            }
            endTime = (float) glfwGetTime();
            dt = endTime - beginTime;
            beginTime = endTime;
            if (forceClose) glfwSetWindowShouldClose(windowPtr, true);
        }
    }

    /**
     * Calculate metric per 1.0 delta.
     * When this method return true, it means the metrics are calculated and accumulation should be reset.
     * @param accumulatedFrame the frame count since last metric calculation
     * @param accumulatedDT the delta since last metric calculation
     * @return true if accumulation should be reset
     */
    private static boolean updateMetric(int accumulatedFrame, float accumulatedDT) {
        if (accumulatedDT < 1.0f) return false;
        FPS = accumulatedFrame / accumulatedDT;
        return true;
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

    private void normalPass(RendererState rendererState, Shader defaultShader) {
        rendererState.setRenderPass(RendererState.RenderPass.NORMAL);
        rendererState.setShader(defaultShader);
        frameBuffer.use();
        if (overrideClearColor) {
            clearColor.set(r, g, b, a).normalize();
            glClearColor(clearColor.x, clearColor.y, clearColor.z, clearColor.w);
        }
        else if (projectLoaded) {
            ClearColor clearColor = Project.preference().clearColor();
            glClearColor(clearColor.r(), clearColor.g(), clearColor.b(), clearColor.a());
        }
        glClear(GL_COLOR_BUFFER_BIT);
        Renderer.get().render();
        DebugDraw.draw();
        frameBuffer.detach();
    }

    private void handleEditorEvent(EditorEvent event) {
        if (event.type != EditorEvent.Type.ProjectLoaded) return;
        projectLoaded = Project.loaded();
        if (!projectLoaded) return;
        ClearColor clearColor = Project.preference().clearColor();
        r = clearColor.r();
        g = clearColor.g();
        b = clearColor.b();
        a = clearColor.a();
        RenderingSetting setting = Project.preference().renderingSetting();
        applyVsync(setting.vsyncMode());
        targetFrameTime = setting.targetFrameRate() > 0 ? 1_000_000_000L / setting.targetFrameRate() : 0;
        String projectDetail = " - [" + Project.preference().name() + "] [" + Project.projectRoot() + "]";
        glfwSetWindowTitle(windowPtr, title + projectDetail);
    }

    private void handleProjectEvent(ProjectEvent event) {
        if (event.type == ProjectEvent.Type.TargetFrameRateChanged) {
            RenderingSetting setting = Project.preference().renderingSetting();
            targetFrameTime = setting.targetFrameRate() > 0 ? 1_000_000_000L / setting.targetFrameRate() : 0;
            return;
        }
        if (event.type == ProjectEvent.Type.VsyncModeChanged) {
            RenderingSetting setting = Project.preference().renderingSetting();
            applyVsync(setting.vsyncMode());
        }
    }

    private void applyVsync(VsyncMode mode) {
        switch (mode) {
            case Disabled -> glfwSwapInterval(0);
            case Enabled -> glfwSwapInterval(1);
            case Adaptive -> {
                Platform platform = Platform.get();
                if (platform == Platform.MACOSX) {
                    Logger.warning("Adaptive vsync is not available on macOS, falling back to vsync Enabled...");
                    glfwSwapInterval(1);
                    break;
                }
                String ext = platform == Platform.WINDOWS ? "WGL_EXT_swap_control_tear" : "GLX_EXT_swap_control_tear";
                if (glfwExtensionSupported(ext)) {
                    glfwSwapInterval(-1);
                    break;
                }
                Logger.warning("Adaptive vsync is not supported on this driver, falling back to vsync Enabled...");
                glfwSwapInterval(1);
            }
        }
    }

    @Override
    public void onEventEmit(Object object, Event event) {
        if (event instanceof EditorEvent editorEvent) {
            handleEditorEvent(editorEvent);
            return;
        }
        if (event instanceof ProjectEvent projectEvent) handleProjectEvent(projectEvent);
    }
}
