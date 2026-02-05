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
    private static final LogicServer instance = new LogicServer();
    private static final EngineLog Logger = new EngineLog(LogicServer.class);
    private static Scene currentScene;
    private static String currentSceneName;
    private static boolean runtimeMode = false;
    private static boolean runtimeCrashed = false;

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
        if (event instanceof EditorEvent editorEvent) handleEditorEvent(object, editorEvent);
        if (event instanceof RuntimeEvent runtimeEvent) handleRuntimeEvent(object, runtimeEvent);
    }

    private void handleRuntimeEvent(Object object, RuntimeEvent event) {
        switch (event.type) {
            case RuntimeEvent.Type.RuntimeStarted -> {
                runtimeMode = true;
                currentScene.saveLevel();
                changeScene(new SceneEditor());
                Logger.info("Test play started.");
            }
            case RuntimeEvent.Type.RuntimeStopped -> {
                runtimeMode = false;
                changeScene(new SceneEditor());
                Logger.info("Test play stopped.");
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
                    Logger.warning("Saving scene data structure in runtime mode is forbidden!");
                    return;
                }
                currentScene.saveLevel();
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
                Logger.debug("Requested to load Scene: " + sceneName);
            }
        }
    }
}
