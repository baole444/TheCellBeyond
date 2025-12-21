package TheCellBeyond.internal;

import TheCellBeyond.MouseListener;
import editor.Properties;
import editor.SceneTree;
import editor.preference.RecentProject;
import editor.preference.UserPreference;
import eventviewer.EngineEventCallback;
import eventviewer.EngineEventListener;
import eventviewer.event.Event;
import physic2d.Physic2D;
import project.Project;
import project.ProjectPreference;
import scene.Scene;
import scene.SceneEditor;
import scene.SceneInit;
import utility.log.EngineLog;

import java.util.List;

public class LogicServer implements EngineEventListener {
    private static final LogicServer instance;
    private static final EngineLog LOGGER = new EngineLog(LogicServer.class);
    private static Scene currentScene;
    private static String currentSceneName;
    private static boolean runtimeMode = false;

    static {
        instance = new LogicServer();
    }

    private LogicServer() {
        EngineEventCallback.register(this);
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
        if (runtimeMode) {
            currentScene.update(dt);
            return;
        }

        currentScene.editorUpdate(dt);
    }

    @Override
    public void onEventEmit(Object object, Event event) {
        switch (event.type) {
            case EngineStart -> {
                runtimeMode = true;
                currentScene.saveLevel();
                changeScene(new SceneEditor());
                LOGGER.info("Test play started.");
            }
            case EngineStop -> {
                runtimeMode = false;
                changeScene(new SceneEditor());
                LOGGER.info("Test play stopped.");
            }
            case LoadEditingScene -> {
                runtimeMode = false;
                changeScene(new SceneEditor());
                LOGGER.debug("Loading current level...");
            }
            case SaveEditingScene -> {
                if (runtimeMode) {
                    LOGGER.warning("Saving scene data structure in runtime mode is forbidden!");
                    return;
                }
                currentScene.saveLevel();
                LOGGER.debug("Saving current level...");
            }
            case LoadProject -> {
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
            case LoadSceneData -> {
                LogicServer.runtimeMode = false;
                String sceneName = (String) object;
                LogicServer.currentSceneName(sceneName);
                String path = Project.projectYMLPath();
                if (path != null) {
                    ProjectPreference preference = Project.preference();
                    RecentProject update = new RecentProject(preference.name(), path, sceneName);
                    UserPreference.updateRecentProject(update);
                }

                LogicServer.changeScene(new SceneEditor());
                LOGGER.debug("Requested to load Scene: " + sceneName);
            }
        }
    }
}
