package editor;

import TheCellBeyond.internal.LogicServer;
import editor.dialog.ConfirmSaveSceneDialog;
import editor.dialog.SaveSceneAsDialog;
import editor.preference.RecentProject;
import editor.preference.UserPreference;
import editor.template.ScriptExportCache;
import eventviewer.EngineEventCallback;
import eventviewer.EngineEventListener;
import eventviewer.event.EditorEvent;
import eventviewer.event.Event;
import eventviewer.event.SceneEvent;
import project.Project;
import project.ProjectPreference;
import scene.SceneManager;
import utility.log.EngineLog;

import java.util.List;

final class EditorEventHandler implements EngineEventListener {
    private static final EngineLog Logger = new EngineLog(EditorEventHandler.class);
    private static EditorEventHandler instance;
    private EditorEventHandler() {
        register();
    }

    static void init() {
        if (instance == null) instance = new EditorEventHandler();
    }

    @Override
    public void onEventEmit(Object object, Event event) {
        switch (event) {
            case SceneEvent sceneEvent -> handleSceneEvent(sceneEvent);
            case EditorEvent editorEvent -> handleEditorEvent(object, editorEvent);
            default -> {}
        }
    }

    private static void handleSceneEvent(SceneEvent event) {
        if (event.type != SceneEvent.Type.SceneChanging) return;
        Properties.clearSelection();
        SceneTree.clearSelection();
    }

    private static void handleEditorEvent(Object object, EditorEvent event) {
        switch (event.type) {
            case LoadProjectFromDisk -> {
                String projectPath = object.toString();
                Logger.info("Loading project file at " + projectPath);
                Project.loadFromYaml(projectPath);
                boolean projectLoaded = (Project.currentProject() != null && Project.projectRoot() != null);
                if (!projectLoaded) return;
                List<String> availScenes = Project.getSceneNames();
                if (availScenes.isEmpty()) return;
                RecentProject project = UserPreference.recentProject(projectPath);
                String lastOpenScene = project != null ? project.lastOpenScene() : null;
                if (lastOpenScene == null || !availScenes.contains(lastOpenScene)) lastOpenScene = availScenes.getFirst();
                EngineEventCallback.emit(lastOpenScene, new EditorEvent(EditorEvent.Type.LoadEditingSceneFromDisk));
            }
            case LoadEditingSceneFromDisk -> {
                String sceneName = (String) object;
                String path = Project.projectYMLPath();
                if (path == null) return;
                ProjectPreference preference = Project.preference();
                RecentProject update = new RecentProject(preference.name(), path, sceneName);
                UserPreference.updateRecentProject(update);
            }
            case ProjectLoaded, ReloadSceneResource -> {
                Project.loadProjectData();
                ResourcePanel.refreshCache();
            }
            case RequestSaveSceneAs -> SaveSceneAsDialog.show(SceneManager::saveCurrentScene);
            case ScriptClassLoaded -> ScriptExportCache.buildAndCache((Class<?>) object);
            case ScriptClassUnloaded -> ScriptExportCache.clear();
            case ScriptCLassReloaded -> {
                String sceneName = LogicServer.currentSceneName();
                if (sceneName != null) ConfirmSaveSceneDialog.show(() -> SceneManager.requestLoadScene(sceneName));
            }
            default -> {}
        }
    }
}
