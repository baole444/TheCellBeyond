package TheCellBeyond.internal;

import TheCellBeyond.GameObject;
import TheCellBeyond.MouseListener;
import TheCellBeyond.Viewport;
import editor.Properties;
import editor.SceneTree;
import editor.preference.RecentProject;
import editor.preference.UserPreference;
import eventviewer.EngineEventCallback;
import eventviewer.EngineEventListener;
import eventviewer.event.EditorEvent;
import eventviewer.event.RuntimeEvent;
import eventviewer.event.Event;
import eventviewer.event.SceneEvent;
import physic2d.Physic2D;
import project.Project;
import project.ProjectPreference;
import scene.Scene;
import scene.SceneEditor;
import scene.SceneLoader;
import scene.SceneManager;
import utility.log.EngineLog;

import java.util.List;

public class LogicServer implements EngineEventListener {
    private static final LogicServer instance = new LogicServer();
    private static final EngineLog Logger = new EngineLog(LogicServer.class);
    private static Scene currentScene;
    private static boolean runtimeMode = false;
    private static boolean runtimeCrashed = false;

    private LogicServer() {
        register();
    }

    /**
     * Create and load an unsaved scene with the given root object.
     * The scene will not be registered with the project until saved.
     * <p>
     * This requires the engine to be out of play test mode or {@link #runtimeMode()} false to work properly.
     * @param root the root object for the new scene
     */
    public static void loadUnsavedScene(GameObject root) {
        if (root == null || runtimeMode) return;
        if (currentScene != null) currentScene.destroy();
        Properties.clearSelection();
        SceneTree.clearSelection();
        currentScene = new Scene(new SceneEditor());
        SceneManager.initUnsavedScene(currentScene, root);
        currentScene.init();
        currentScene.editorStart();
    }

    /**
     * Get the current scene.
     * @return the current scene or null if there is no scene
     */
    public static Scene currentScene() {
        return currentScene;
    }

    /**
     * Get the name of the current scene.
     * @return name string of the scene or null if there is no scene
     */
    public static String currentSceneName() {
        return currentScene == null ? null : currentScene.name();
    }

    /**
     * Get the viewport of the current scene.
     * @return the viewport or null if there is no scene
     */
    public static Viewport currentSceneViewport() {
        return currentScene == null ? null : currentScene.viewport();
    }

    /**
     * Get the physic 2D world of the current scene.
     * @return the physic world or null if there is no scene
     */
    public static Physic2D currentScenePhysic2D() {
        return currentScene == null ? null : currentScene.getPhysic2D();
    }

    /**
     * Check if the engine is in runtime or not.
     * @return true if runtime started
     */
    public static boolean runtimeMode() {
        return runtimeMode;
    }

    /**
     * Step the scene's physic world by fixed delta time.
     * <p>
     * The delta provide to this method is use to step physic world step.
     * @param dt variable delta time
     */
    public static void updatePhysic(float dt) {
        if (!runtimeMode || runtimeCrashed || currentScene == null) return;
        try {
            currentScene.updatePhysic(dt);
        } catch (Exception e) {
            runtimeCrashed = true;
            Logger.warning("Test play stopped due to exceptio.n");
            logCrash(e);
            EngineEventCallback.emit(null, new RuntimeEvent(RuntimeEvent.Type.RuntimeCrashed));
        }
    }

    /**
     * Step the scene's spatial world by the given delta time.
     * @param dt variable delta time
     */
    public static void update(float dt)  {
        if (runtimeCrashed || currentScene == null) return;
        if (runtimeMode) {
            try {
                currentScene.update(dt);
            } catch (Exception e) {
                runtimeCrashed = true;
                Logger.warning("Test play stopped due to exception.");
                logCrash(e);
                EngineEventCallback.emit(null, new RuntimeEvent(RuntimeEvent.Type.RuntimeCrashed));
            }
            return;
        }
        currentScene.editorUpdate(dt);
    }

    private static void logCrash(Exception e) {
        Logger.error("Runtime exception occurred:");
        Logger.error(String.format("Message: %s", e.getMessage()));

        int stackCount = 0;
        for (StackTraceElement element : e.getStackTrace()) {
            String className = element.getClassName();
            if (className.startsWith("java.") || className.startsWith("sun.") || className.startsWith("javax.") || className.startsWith("jdk.")) continue;
            Logger.error(String.format("\tat %s", element));
            stackCount++;
            if (className.equals(LogicServer.class.getCanonicalName()) && element.getMethodName().equals("loop")) break;
            if (stackCount >= 15) {
                Logger.error("\t... (more folded frames)");
                break;
            }
        }
    }

    @Override
    public void onEventEmit(Object object, Event event) {
        switch (event) {
            case SceneEvent sceneEvent -> handleSceneEvent(object, sceneEvent);
            case EditorEvent editorEvent -> handleEditorEvent(object, editorEvent);
            case RuntimeEvent runtimeEvent -> handleRuntimeEvent(object, runtimeEvent);
            default -> {}
        }
    }

    private static void changeScene(SceneLoader sceneLoader) {
        String sceneName = currentScene == null ? null : currentScene.name();
        changeScene(sceneLoader, sceneName);
    }

    private static void changeScene(SceneLoader sceneLoader, String sceneName) {
        if (currentScene != null) currentScene.destroy();
        Properties.clearSelection();
        SceneTree.clearSelection();
        currentScene = new Scene(sceneLoader);
        if (SceneManager.validSceneName(sceneName)) SceneManager.loadScene(currentScene, sceneName);
        currentScene.init();
        if (runtimeMode) currentScene.start();
        else currentScene.editorStart();
    }

    private static void reloadScene() {
        if (currentScene == null) return;
        String sceneName = currentScene.name();
        if (!SceneManager.validSceneName(sceneName)) return;
        changeScene(new SceneEditor(), sceneName);
    }

    private void handleSceneEvent(Object object, SceneEvent event) {
        if (!event.type.equals(SceneEvent.Type.SceneLeaved)) return;
        currentScene = null;
    }

    private void handleRuntimeEvent(Object object, RuntimeEvent event) {
        switch (event.type) {
            case RuntimeEvent.Type.RuntimeStarted -> {
                SceneManager.saveCurrentScene();
                runtimeMode = true;
                reloadScene();
                Logger.info(String.format("Test play started for '%s'", currentSceneName()));
            }
            case RuntimeEvent.Type.RuntimeStopped -> {
                runtimeMode = false;
                reloadScene();
                Logger.info(String.format("Test play stopped for '%s'", currentSceneName()));
            }
            case RuntimeEvent.Type.RuntimeCrashed -> {
                runtimeMode = false;
                runtimeCrashed = false;
                reloadScene();
            }
        }
    }

    private void handleEditorEvent(Object object, EditorEvent event) {
        switch (event.type) {
            case SaveEditingSceneToDisk -> {
                SceneManager.saveCurrentScene();
                Logger.debug("Saving current level...");
            }
            case LoadProjectFromDisk -> {
                String projectPath = object.toString();
                Logger.info("Loading project file at " + projectPath);
                Project.loadFromYaml(projectPath);
                boolean projectLoaded = (Project.currentProject() != null && Project.projectRoot() != null);
                if (!projectLoaded) return;
                MouseListener.setStartupMode(false);
                List<String> availScenes = Project.getSceneNames();
                if (availScenes.isEmpty()) return;
                RecentProject project = UserPreference.recentProject(projectPath);
                String lastOpenScene = project != null ? project.lastOpenScene() : null;
                if (lastOpenScene == null || !availScenes.contains(lastOpenScene)) lastOpenScene = availScenes.getFirst();
                changeScene(new SceneEditor(), lastOpenScene);
            }
            case LoadEditingSceneFromDisk -> {
                runtimeMode = false;
                String sceneName = (String) object;
                String path = Project.projectYMLPath();
                if (path != null) {
                    ProjectPreference preference = Project.preference();
                    RecentProject update = new RecentProject(preference.name(), path, sceneName);
                    UserPreference.updateRecentProject(update);
                }
                changeScene(new SceneEditor(), sceneName);
                Logger.debug("Requested to load Scene: " + sceneName);
            }
        }
    }
}
