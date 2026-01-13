package TheCellBeyond.internal;

import TheCellBeyond.MouseListener;
import editor.Properties;
import editor.SceneTree;
import editor.preference.RecentProject;
import editor.preference.UserPreference;
import eventviewer.EngineEventCallback;
import eventviewer.EngineEventListener;
import eventviewer.event.EditorEvent;
import eventviewer.event.RuntimeEvent;
import eventviewer.event.Event;
import physic2d.Physic2D;
import project.Project;
import project.ProjectPreference;
import scene.Scene;
import scene.SceneEditor;
import scene.SceneLoader;
import utility.log.EngineLog;

import java.util.List;

public class LogicServer implements EngineEventListener {
    private static final LogicServer instance;
    private static final EngineLog LOGGER = new EngineLog(LogicServer.class);
    private static Scene currentScene;
    private static String currentSceneName;
    private static boolean runtimeMode = false;

    private static boolean runtimeCrashed = false;

    static {
        instance = new LogicServer();
    }

    private LogicServer() {
        EngineEventCallback.register(this);
    }

    public static void changeScene(SceneLoader sceneLoader) {
        if (currentScene != null) currentScene.destroy();

        Properties.clearSelection();
        SceneTree.clearSelection();

        currentScene = new Scene(sceneLoader);
        currentScene.loadLevel();
        currentScene.init();
        if (runtimeMode) currentScene.start();
        else currentScene.editorStart();
    }

    public static Scene currentScene() {
        return currentScene;
    }

    public static String currentSceneName() {
        return currentSceneName;
    }

    public static void currentSceneName(String currentSceneName) {
        LogicServer.currentSceneName = currentSceneName;
    }

    public static Physic2D physic2D() {
        return currentScene.getPhysic2D();
    }

    public static boolean runtimeMode() {
        return runtimeMode;
    }

    public static void loop(float dt)  {
        if (runtimeCrashed) return;

        if (runtimeMode) {
            try {
                currentScene.update(dt);
            } catch (Exception e) {
                runtimeCrashed = true;
                LOGGER.warning("Test play stopped due to exception");
                logCrash(e);
                EngineEventCallback.emit(null, new RuntimeEvent(RuntimeEvent.Type.RuntimeCrashed));
            }

            return;
        }

        currentScene.editorUpdate(dt);
    }

    private static void logCrash(Exception e) {
        LOGGER.error("Runtime exception occurred:");
        LOGGER.error(String.format("Message: %s", e.getMessage()));

        int stackCount = 0;
        for (StackTraceElement element : e.getStackTrace()) {
            String className = element.getClassName();

            if (className.startsWith("java.") || className.startsWith("sun.") || className.startsWith("javax.") || className.startsWith("jdk.")) continue;

            LOGGER.error(String.format("    at %s", element));
            stackCount++;

            if (className.equals(LogicServer.class.getCanonicalName()) && element.getMethodName().equals("loop")) break;

            if (stackCount >= 15) {
                LOGGER.error("    ... (more folded frames)");
                break;
            }
        }
    }

    @Override
    public void onEventEmit(Object object, Event event) {
        if (event instanceof EditorEvent editorEvent) handleEditorEvent(object, editorEvent);
        if (event instanceof RuntimeEvent runtimeEvent) handleRuntimeEvent(object, runtimeEvent);
    }

    private void handleRuntimeEvent(Object object, RuntimeEvent event) {
        switch (event.type) {
            case RuntimeEvent.Type.RuntimeStarted -> {
                runtimeMode = true;
                currentScene.saveLevel();
                changeScene(new SceneEditor());
                LOGGER.info("Test play started.");
            }
            case RuntimeEvent.Type.RuntimeStopped -> {
                runtimeMode = false;
                changeScene(new SceneEditor());
                LOGGER.info("Test play stopped.");
            }
            case RuntimeEvent.Type.RuntimeCrashed -> {
                runtimeMode = false;
                runtimeCrashed = false;
                changeScene(new SceneEditor());
            }
        }
    }

    private void handleEditorEvent(Object object, EditorEvent event) {
        switch (event.type) {
            case SaveEditingSceneToDisk -> {
                if (runtimeMode) {
                    LOGGER.warning("Saving scene data structure in runtime mode is forbidden!");
                    return;
                }
                currentScene.saveLevel();
                LOGGER.debug("Saving current level...");
            }
            case LoadProjectFromDisk -> {
                String projectPath = object.toString();
                LOGGER.info("Loading project file at " + projectPath);
                Project.loadFromYaml(projectPath);
                boolean projectLoaded = (Project.currentProject() != null && Project.projectRoot() != null);
                if (!projectLoaded) return;

                MouseListener.setStartupMode(false);
                List<String> availScenes = Project.getSceneNames();
                if (availScenes.isEmpty()) {
                    LogicServer.currentSceneName(null);
                    LogicServer.changeScene(new SceneEditor());
                    return;
                }

                RecentProject project = UserPreference.recentProject(projectPath);
                String lastOpenScene = project != null ? project.lastOpenScene() : null;
                if (lastOpenScene == null || !availScenes.contains(lastOpenScene)) {
                    lastOpenScene = availScenes.getFirst();
                }

                currentSceneName(lastOpenScene);
                changeScene(new SceneEditor());
            }
            case LoadEditingSceneFromDisk -> {
                runtimeMode = false;
                String sceneName = (String) object;
                currentSceneName(sceneName);
                String path = Project.projectYMLPath();
                if (path != null) {
                    ProjectPreference preference = Project.preference();
                    RecentProject update = new RecentProject(preference.name(), path, sceneName);
                    UserPreference.updateRecentProject(update);
                }

                changeScene(new SceneEditor());
                LOGGER.debug("Requested to load Scene: " + sceneName);
            }
        }
    }
}
