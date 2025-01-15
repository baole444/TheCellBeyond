package TCB_Field;

import editor.Project;
import editor.Properties;
import eventviewer.EventSystem;
import eventviewer.IEvent;
import eventviewer.event.Event;
import imgui.ImGui;
import org.joml.Vector2i;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWWindowCloseCallback;
import org.lwjgl.opengl.GL;
import render.*;
import render.Renderer;
import scene.LevelEditorSceneInit;
import scene.Scene;
import scene.SceneInit;
import utility.AssetsPool;
import utility.ExitConfirmDialog;

import java.awt.*;
import java.util.List;
import java.util.Map;

import static editor.Project.CurrentProject;
import static editor.Project.ProjectRoot;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window implements IEvent {
    private int width;
    private int height;
    private String title;
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

    private ExitConfirmDialog exitConfirmDialog;
    private boolean shouldClose;

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
        return get().currentScene;
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

        String renderer = glGetString(GL_RENDERER);
        String version = glGetString(GL_VERSION);

        System.out.println("Active GPU: " + renderer + " Driver version: " + version);

        loop();

        //free memories

        endScr();
        //end GLFW and error callback

        glfwSetErrorCallback(null).free();
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


        Window.changeScene(new LevelEditorSceneInit());
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

    public void endScr(){
        imGuiLayer.getImGuiGl3().shutdown();
        imGuiLayer.getImGuiGlfw().shutdown();
        ImGui.destroyContext();
        glfwFreeCallbacks(window.glfwWindow);
        glfwDestroyWindow(window.glfwWindow);
        glfwTerminate();
    }

    public void loop () {

        float beginTime = (float)glfwGetTime();
        float endTime;
        float dt = -1.0f;

        Shader defaultShader = AssetsPool.loadShader("assets/shaders/default.glsl");
        Shader objectSelectShader = AssetsPool.loadShader("assets/shaders/objSelection.glsl");

        while (!glfwWindowShouldClose(glfwWindow)) {
            glfwPollEvents(); //poll events
            // Pass 1: object selection layer (invisible)
            glDisable(GL_BLEND);
            objectSelection.useWrite();

            glViewport(0, 0, 1920, 1080);
            glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            Renderer.setShader(objectSelectShader);
            currentScene.render();

            objectSelection.detachWrite();
            glEnable(GL_BLEND);

            // Pass 2: Visualized scene

            DebugDraw.startFrame();

            this.frameBuffer.use();

            glClearColor(r, g, b, a);
            glClear(GL_COLOR_BUFFER_BIT);

            if (dt >= 0) {
                Renderer.setShader(defaultShader);
                if (runtimeMode) {
                    currentScene.update(dt); // Using main update when not in editor
                } else {
                    currentScene.editorUpdate(dt); // Using editor update under edit mode
                }
                DebugDraw.draw();
                currentScene.render();

            }
            this.frameBuffer.detach();

            this.imGuiLayer.update(dt, currentScene);

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

    @Override
    public void whenNotice(Object object, Event event) {
        switch (event.type) {
            case EngineStart:
                this.runtimeMode = true;
                currentScene.saveLevel();
                Window.changeScene(new LevelEditorSceneInit()); // Reset view to runtime mode.
                System.out.println("Engine starting.");
                break;
            case EngineEnd:
                this.runtimeMode = false;
                Window.changeScene(new LevelEditorSceneInit()); // Reset to Editor runtime.
                System.out.println("Engine stopping.");
                break;
            case LevelLoad:
                Window.changeScene(new LevelEditorSceneInit());
                System.out.println("Loading current level...");
                break;
            case LevelSave:
                currentScene.saveLevel();
                System.out.println("Saving current level...");
                break;
            case LoadProject:
                System.out.println(object);

                Project.loadFromYaml(object.toString());

                String projectDetail = " - [" + CurrentProject.getProject().getName() + "] [" + ProjectRoot + "]";

                glfwSetWindowTitle(glfwWindow, this.title + projectDetail);

                /*
                Project project = Project.loadFromYaml(object.toString());
                System.out.println(project.toString());

                Map<String, Project.projectSceneMap> sceneMap = project.getScenes();
                Map<String, Project.projectAssetMap> assetMap = project.getAssets();
                Map<String, Project.projectSheetMap> sheetMap = project.getSheets();
                for (Map.Entry<String, Project.projectSceneMap> entry: sceneMap.entrySet()) {
                    String sceneName = entry.getKey();
                    Project.projectSceneMap projectSceneMap = entry.getValue();

                    List<String> assetList = projectSceneMap.getAsset();
                    List<String> sheetList = projectSceneMap.getSheet();

                    System.out.println("Scene name: " + sceneName);
                    System.out.println("Path: " + projectSceneMap.getPath());

                    System.out.println("Assets:");
                    for (String asset : assetList) {
                        Project.projectAssetMap aM = assetMap.get(asset);
                        System.out.println("  Name: " + asset);
                        System.out.println("  Path: " + aM.getPath());
                    }

                    System.out.println("Sheets:");
                    for (String sheet : sheetList) {
                        Project.projectSheetMap sM = sheetMap.get(sheet);
                        System.out.println("  Name: " + sheet);
                        System.out.println("    Category: "+ sM.getCategory());
                        System.out.println("    Path: " + sM.getPath());
                        System.out.println("    Sprite Count: " + sM.getCount());
                        System.out.println("    sizeX: " + sM.getSizeX() + " pixel(s)");
                        System.out.println("    sizeY: " + sM.getSizeY() + " pixel(s)");
                        System.out.println("    Padding: " + sM.getPadding() + " pixel(s)");
                    }
                    System.out.println("\n");
                }
                */
                break;
            case LoadScene:
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
