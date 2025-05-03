package TCB_Field;

import editor.OpenProjectDialog;
import editor.project.Project;
import editor.Properties;
import eventviewer.EventSystem;
import eventviewer.IEvent;
import eventviewer.event.Event;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiConfigFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
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
import physic_2d.FlatPhysic;
import render.*;
import render.Renderer;
import render.text.FontManager;
import scene.LevelEditorSceneInit;
import scene.Scene;
import scene.SceneInit;
import utility.AssetsPool;
import utility.ExitConfirmDialog;
import utility.Settings;

import java.awt.*;

import static editor.project.Project.CurrentProject;
import static editor.project.Project.ProjectRoot;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window implements IEvent {
    private int width;
    private int height;
    private final String title;
    private long glfwWindow; //windows pointer
    public float r, g, b, a;
    private static Window window = null; // start with no window
    private static Scene currentScene;
    private static String currentSceneName;
    private boolean runtimeMode = false; //Run without editor (release) or not

    private String glslVer = null;
    private ImGuiLayer imGuiLayer;
    private FrameBuffer frameBuffer;
    private ObjectSelection objectSelection;
    private Properties properties;

    private final IconLoader iconFile = IconLoader.loadIcon("assets/texture/TCB icon.png");

    private final ExitConfirmDialog exitConfirmDialog;
    private boolean shouldClose;

    private long soundContext;
    private long audioDevice;

    private boolean projectLoaded = false;

    public Window() {
        this.width = 640;
        this.height = 480;
        this.title = "The Cell Beyond";
        this.exitConfirmDialog = new ExitConfirmDialog();
        EventSystem.addViewer(this);

        r = 0.027f;
        g = 0.122f;
        b = 0.067f;
        a = 1;
    }

    public static void changeScene(SceneInit sceneInit) {
        if (currentScene != null) {
            // Destroy
            currentScene.destroy();
        }

        loadImGui().loadProperties().setActiveGameObj(null);

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

    public static Scene getScene() {
        return currentScene;
    }

    public static int loadWidth() {
        return get().width;
    }

    public static int loadHeight() {
        return get().height;
    }

    public void run() {
        System.out.println("Starting LWJGL " + Version.getVersion());

        initWindow();

        if (!projectLoaded) {
            showStartupScreen();

            projectLoaded = (CurrentProject != null && ProjectRoot != null);

            if (glfwWindowShouldClose(glfwWindow)) {
                endScr();
                return;
            }
        }

        if (projectLoaded) {
            String projectDetail = " - [" + CurrentProject.getProject().getName() + "] [" + ProjectRoot + "]";

            glfwSetWindowTitle(glfwWindow, this.title + projectDetail);
            loop();
        }

        String renderer = glGetString(GL_RENDERER);
        String version = glGetString(GL_VERSION);

        System.out.println("Active GPU: " + renderer + " Driver version: " + version);

        //free memories

        endScr();

        //end GLFW and error callback
        glfwSetErrorCallback(null).free();
    }

    private void showStartupScreen() {
        MouseListener.setStartupMode(true);

        float beginTime = (float)glfwGetTime();
        float endTime;
        float dt = -1.0f;

        while (!glfwWindowShouldClose(glfwWindow) && !projectLoaded) {
            glfwPollEvents();

            glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT);

            imGuiLayer.getImGuiGlfw().newFrame();
            imGuiLayer.getImGuiGl3().newFrame();
            ImGui.newFrame();

            ImGui.setNextWindowPos(width / 2.0f, height / 2.0f, ImGuiCond.Always, 0.5f, 0.5f);
            ImGui.setNextWindowSize(400, 200);
            ImGui.begin("Welcome to The Cell Beyond Editor", ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoCollapse);

            ImGui.text("Please select a project to open:");

            if (ImGui.button("Open Project", 150, 30)) {
                ImGuiLayer.set_openFileDialog(new ImBoolean(true));
            }

            OpenProjectDialog openProjectDialog = new OpenProjectDialog();

            openProjectDialog.imgui(ImGuiLayer.get_openFileDialog());

            projectLoaded = (CurrentProject != null && ProjectRoot != null);

            ImGui.end();

            ImGui.render();
            imGuiLayer.getImGuiGl3().renderDrawData(ImGui.getDrawData());

            if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
                final long backupWindowPtr = glfwGetCurrentContext();
                ImGui.updatePlatformWindows();
                ImGui.renderPlatformWindowsDefault();
                glfwMakeContextCurrent(backupWindowPtr);
            }

            glfwSwapBuffers(glfwWindow);

            endTime = (float) glfwGetTime();

            dt = endTime - beginTime;
            beginTime = endTime;
        }
    }

    private void initWindow() {
        //error return
        GLFWErrorCallback.createPrint(System.err).set();

        //start GLFW
        if (!glfwInit()) {
            System.out.println("Unable to start GLFW.");
            System.exit(-1);
        }

        glslVer = "#version 330 core";
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);

        // Update width, height to current screen resolution
        // Make it smaller a bit
        this.width = getScrSize().x;
        this.height = getScrSize().y;

        //config GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_MAXIMIZED, GLFW_FALSE);
        //glfwWindowHint(GLFW_DECORATED, 0);

        // Spawn window
        glfwWindow = glfwCreateWindow(this.width, this.height, this.title, NULL, NULL);
        if (glfwWindow == NULL) {
            System.out.println("Failed to spawn window.");
            System.exit(-1);
        }
        System.out.println("Generating Window, dimension: " + this.width + " x " + this.height);

        glfwSetCursorPosCallback(glfwWindow, MouseListener::mousePosCallback); // :: is java syntax lambda function
        glfwSetMouseButtonCallback(glfwWindow, MouseListener::mouseButtonCallback);
        glfwSetScrollCallback(glfwWindow, MouseListener::mouseScrollCallback);
        glfwSetKeyCallback(glfwWindow, KeyListener::keyCallback);
        glfwSetCharCallback(glfwWindow, KeyListener::charCallback);

        glfwSetInputMode(glfwWindow, GLFW_IME, GLFW_TRUE);

        //exit callback setting
        glfwSetWindowCloseCallback(glfwWindow, new GLFWWindowCloseCallback() {
            @Override
            public void invoke(long l) {
                glfwSetWindowShouldClose(glfwWindow, false);
                exitConfirmDialog.reloadDialog();

                shouldClose = exitConfirmDialog.exitDialog();
                if (shouldClose) {
                    glfwSetWindowShouldClose(glfwWindow, true);
                }
            }
        });

        // OpenGL context current
        glfwMakeContextCurrent(glfwWindow);

        //V-sync yes
        glfwSwapInterval(1);

        //Make window visible
        glfwShowWindow(glfwWindow);

        // Init sound
        String defaultAudioDevice = alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER);
        audioDevice = alcOpenDevice(defaultAudioDevice);

        int[] attbs = {0};
        soundContext = alcCreateContext(audioDevice, attbs);
        alcMakeContextCurrent(soundContext);


        ALCCapabilities alcCapabilities = ALC.createCapabilities(audioDevice);
        ALCapabilities alCapabilities = AL.createCapabilities(alcCapabilities);

        if (!alCapabilities.OpenAL10) {
            System.out.println("OpenAL10 not supported on this device");
            System.exit(-2);
        }

        GL.createCapabilities();
        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

        this.frameBuffer = new FrameBuffer(this.width, this.height);
        this.objectSelection = new ObjectSelection(this.width, this.height);

        glViewport(0, 0, this.width, this.height);

        this.imGuiLayer = new ImGuiLayer(glfwWindow, objectSelection);
        this.imGuiLayer.initImGui(glslVer);

        //Set Icon
        GLFWImage icon = GLFWImage.malloc();
        GLFWImage.Buffer bufferIcon = GLFWImage.malloc(1);
        icon.set(iconFile.loadIconW(), iconFile.loadIconH(), iconFile.getIcon());
        bufferIcon.put(0, icon);
        glfwSetWindowIcon(glfwWindow, bufferIcon);

        projectLoaded = (CurrentProject != null && ProjectRoot != null);

        if (projectLoaded) {
            Window.changeScene(new LevelEditorSceneInit());
        }
    }


    /**
     * Return current active display size that the windows is on.
     * <ul>
     *      <li>Format: <code>Vector(width, height);</code></li>
     *      <li>Type: <cite>integer</cite></li>
     *      <li>Use: joml <code>Vector2i</code> class</li>
     * </ul>
     * Call:<br>
     * <code>this.variable_one = getScrSize().x;<br>
     * this.variable_two = getScrSize().y;</code>
    */
    public Vector2i getScrSize() {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int w = device.getDisplayMode().getWidth();
        int h = device.getDisplayMode().getHeight();

        return new Vector2i(w, h);
    }

    private void endScr(){
        imGuiLayer.getImGuiGl3().shutdown();
        imGuiLayer.getImGuiGlfw().shutdown();
        ImGui.destroyContext();

        alcDestroyContext(soundContext);
        alcCloseDevice(audioDevice);

        frameBuffer.dispose();
        glfwFreeCallbacks(window.glfwWindow);
        glfwDestroyWindow(window.glfwWindow);

        glfwTerminate();
    }

    public void loop () {
        float beginTime = (float)glfwGetTime();
        float endTime;
        float dt = -1.0f;

        Shader defaultShader = AssetsPool.loadShader(Settings.PATH.DEFAULT_TEXTURE_SHADER);
        Shader objectSelectShader = AssetsPool.loadShader(Settings.PATH.OBJECT_SELECTION_SHADER);

        RendererState rendererState = RendererState.get();

        while (!glfwWindowShouldClose(glfwWindow)) {
            glfwPollEvents(); //poll events

            if (dt >= 0) {
                // Pass 1: object selection layer (invisible)

                rendererState.setRenderPass(RendererState.RenderPass.SELECTION);
                rendererState.setShader(objectSelectShader);

                objectSelection.useWrite();

                glViewport(0, 0, 1920, 1080);
                glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

                currentScene.render();

                objectSelection.detachWrite();

                // Pass 2: Visualized scene
                rendererState.setRenderPass(RendererState.RenderPass.NORMAL);
                rendererState.setShader(defaultShader);

                DebugDraw.startFrame();

                this.frameBuffer.use();

                glClearColor(r, g, b, a);
                glClear(GL_COLOR_BUFFER_BIT);

                if (runtimeMode) {
                    currentScene.update(dt); // Using the main update when not in the editor
                } else {
                    currentScene.editorUpdate(dt); // Using editor update under edit mode
                }
                currentScene.render();
                DebugDraw.draw();

                this.frameBuffer.detach();

                //this.frameBuffer.renderToScreen();

                this.imGuiLayer.update(dt, currentScene);
            }

            MouseListener.endFrame();
            KeyListener.endFrame();

            glfwSwapBuffers(glfwWindow);

            endTime = (float)glfwGetTime();
            dt = endTime - beginTime;
            beginTime = endTime;
        }
    }

    public static FrameBuffer loadFrameBuffer() {
        return get().frameBuffer;
    }

    public static float loadTargetAspectRatio() {
        return 4.0f / 3.0f;
    }

    public static ImGuiLayer loadImGui() {
        return get().imGuiLayer;
    }

    public static String getCurrentSceneName() {
        return currentSceneName;
    }

    public static void setCurrentSceneName(String currentSceneName) {
        Window.currentSceneName = currentSceneName;
    }

    public static FlatPhysic getFlatPhysic() {
        return currentScene.getFlatPhysic();
    }

    public boolean isRuntimeMode() {
        return runtimeMode;
    }

    @Override
    public void whenNotice(Object object, Event event) {
        switch (event.type) {
            case ENGINE_START:
                this.runtimeMode = true;
                currentScene.saveLevel();
                Window.changeScene(new LevelEditorSceneInit(currentSceneName)); // Reset view to runtime mode.
                System.out.println("Engine starting.");
                break;
            case ENGINE_END:
                this.runtimeMode = false;
                Window.changeScene(new LevelEditorSceneInit(currentSceneName)); // Reset to Editor runtime.
                System.out.println("Engine stopping.");
                break;
            case LEVEL_LOAD:
                if (this.runtimeMode) this.runtimeMode = false;

                Window.changeScene(new LevelEditorSceneInit(currentSceneName));
                System.out.println("Loading current level...");
                break;
            case LEVEL_SAVE:
                currentScene.saveLevel();
                System.out.println("Saving current level...");
                break;
            case PROJECT_LOAD:
                System.out.println("Loading project file at "+ object.toString());

                Project.loadFromYaml(object.toString());

                projectLoaded = (CurrentProject != null && ProjectRoot != null);

                if (projectLoaded) {
                    MouseListener.setStartupMode(false);

                    String projectDetail = " - [" + CurrentProject.getProject().getName() + "] [" + ProjectRoot + "]";

                    glfwSetWindowTitle(glfwWindow, this.title + projectDetail);

                    if (currentScene == null) {
                        Window.changeScene(new LevelEditorSceneInit());
                    }
                }
                break;
            case SCENE_LOAD:
                if (this.runtimeMode) {
                    this.runtimeMode = false;
                }
                String sceneName = (String) object;

                setCurrentSceneName(sceneName);

                Window.changeScene(new LevelEditorSceneInit(sceneName));

                System.out.println("Requested to load Scene: " + sceneName);

                break;
        }
    }
}
