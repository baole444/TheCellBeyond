package scene;

import TheCellBeyond.GameObject;
import TheCellBeyond.GameObjectSerializer;
import TheCellBeyond.internal.LogicServer;
import com.google.gson.*;
import components.Component;
import components.ComponentSerializer;
import editor.dialog.SaveSceneAsDialog;
import eventviewer.EngineEventCallback;
import eventviewer.event.EditorEvent;
import project.Project;
import project.ProjectSceneMap;
import utility.UnifiedPaths;
import utility.log.EngineLog;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Manager for scene's file data.
 */
public final class SceneManager {
    private static final EngineLog Logger = new EngineLog(SceneManager.class);
    private static final String ProjectNotLoaded = "no project loaded";
    private static final String SceneNotLoaded = "no scene loaded";
    private static final String SaveFileFailed = "failed to save scene file";
    private static final String SceneAlreadyExist = "scene of the same name already existed";
    private static final String RegisterSceneFailed = "failed to register scene with the project";
    private static final String NoSuchScene = "no such scene in the project";

    private static final String CannotSaveFormat = "Cannot save scene '%s': %s";
    private static final String CannotSaveAsFormat = "Cannot save scene as '%s': %s";
    private static final String CannotCreateFormat = "Cannot create scene '%s': %s";
    private static final String CannotLoadFormat = "Cannot load scene '%s': %s";

    private SceneManager() {}

    /**
     * Check if scene name is valid (not null or blank).
     * @param name the name string to check
     * @return true if valid
     */
    public static boolean validSceneName(String name) {
        return name != null && !name.isBlank();
    }

    /**
     * Check if a scene name is not taken yet.
     * @param name the name string to check
     * @return true if available
     */
    public static boolean sceneNameAvailable(String name) {
        if (!validSceneName(name) || !Project.loaded()) return false;
        return Project.getScene(name.trim()) == null;
    }

    /**
     * Save the current scene to disk.
     */
    public static void saveCurrentScene() {
        if (runtimeMode()) return;
        String currentName = LogicServer.currentSceneName();
        if (currentName == null) {
            SaveSceneAsDialog.show(SceneManager::saveCurrentScene);
            return;
        }
        Scene currentScene = LogicServer.currentScene();
        if (currentScene == null) {
            Logger.warning(String.format(CannotSaveFormat, currentName, SceneNotLoaded));
            return;
        }
        saveScene(currentName, currentScene);
    }

    /**
     * Save the given scene and write its data to disk.
     * @param sceneName name of the scene to save
     * @param scene the scene instance to save
     * @return true if saved successfully
     */
    public static boolean saveScene(String sceneName, Scene scene) {
        if (runtimeMode()) return false;
        if (invalidName(sceneName)) return false;
        sceneName = sceneName.trim();
        if (scene == null) {
            Logger.error(String.format(CannotSaveFormat, sceneName, SceneNotLoaded));
            return false;
        }
        if (!Project.loaded()) {
            Logger.error(String.format(CannotSaveFormat, sceneName, ProjectNotLoaded));
            return false;
        }
        List<GameObject> objects = scene.getSerializedObject();
        objects.forEach(GameObject::prepareForSerialization);
        SceneFile file = new SceneFile(scene.sceneUUID(), sceneName, "", SceneFile.SaveVersion, objects);
        return saveSceneFile(file);
    }

    /**
     * Save the current scene as a new scene using the given name.
     * @param newSceneName name for the copy
     * @return true if save successfully
     */
    public static boolean saveSceneAs(String newSceneName) {
        if (runtimeMode() || invalidName(newSceneName)) return false;
        newSceneName = newSceneName.trim();
        if (!Project.loaded()) {
            Logger.error(String.format(CannotSaveAsFormat, newSceneName, ProjectNotLoaded));
            return false;
        }
        if (Project.getScene(newSceneName) != null) {
            Logger.error(String.format(CannotSaveAsFormat, newSceneName, SceneAlreadyExist));
            return false;
        }
        Scene currentScene = LogicServer.currentScene();
        if (currentScene == null) {
            Logger.error(String.format(CannotSaveAsFormat, newSceneName, SceneNotLoaded));
            return false;
        }
        String path = createScenePath(newSceneName);
        ProjectSceneMap sceneMap = new ProjectSceneMap(path);
        if (!Project.addScene(newSceneName, sceneMap)) {
            Logger.error(String.format(CannotSaveAsFormat, newSceneName, RegisterSceneFailed));
            return false;
        }
        if (!saveScene(newSceneName, currentScene)) {
            Project.removeScene(newSceneName);
            Logger.error(String.format(CannotSaveAsFormat, newSceneName, SaveFileFailed));
            return false;
        }
        LogicServer.currentSceneName(newSceneName);
        Logger.info(String.format("Saved scene as '%s'", newSceneName));
        return true;
    }

    /**
     * Create a new scene using the given name.
     * @param sceneName name of the new scene
     * @return true if create successfully
     */
    public static boolean createNewScene(String sceneName) {
        if (runtimeMode() || invalidName(sceneName)) return false;
        sceneName = sceneName.trim();
        if (!Project.loaded()) {
            Logger.error(String.format(CannotCreateFormat, sceneName, ProjectNotLoaded));
            return false;
        }
        if (Project.getScene(sceneName) != null) {
            Logger.error(String.format(CannotCreateFormat, sceneName, SceneAlreadyExist));
            return false;
        }
        String path = createScenePath(sceneName);
        ProjectSceneMap sceneMap = new ProjectSceneMap(path);
        if (!Project.addScene(sceneName, sceneMap)) {
            Logger.error(String.format(CannotCreateFormat, sceneName, RegisterSceneFailed));
            return false;
        }
        if (!saveSceneFile(new SceneFile(sceneName))) {
            Project.removeScene(sceneName);
            Logger.error(String.format(CannotCreateFormat, sceneName, SaveFileFailed));
            return false;
        }
        LogicServer.currentSceneName(sceneName);
        Logger.info(String.format("Created new scene '%s'", sceneName));
        return true;
    }

    /**
     * Request to load a scene using the given name.
     * @param sceneName the name of the scene to load
     */
    public static void requestLoadScene(String sceneName) {
        if (invalidName(sceneName)) return;
        sceneName = sceneName.trim();
        if (!Project.loaded()) {
            Logger.error(String.format(CannotLoadFormat, sceneName, ProjectNotLoaded));
            return ;
        }
        if (Project.getScene(sceneName) == null) {
            Logger.error(String.format(CannotLoadFormat, sceneName, NoSuchScene));
            return;
        }

        EngineEventCallback.emit(sceneName, new EditorEvent(EditorEvent.Type.LoadEditingSceneFromDisk));
    }

    /**
     * Load the scene file's data into the given scene instance.
     * @param scene the scene instance to receive the data
     */
    public static void loadScene(Scene scene) {
        if (scene == null) {
            Logger.warning("Cannot load data into null scene");
            return;
        }
        String currentName = LogicServer.currentSceneName();
        if (currentName == null) {
            Logger.warning("Cannot load scene: current scene name not set");
            return;
        }
        SceneFile file = loadSceneFile(currentName);
        if (file == null) {
            Logger.warning(String.format("No data for scene '%s' to be loaded", currentName));
            return;
        }
        scene.loadDataFromFile(file);
    }

    /**
     * Load scene file from disk.
     * @param sceneName name of the scene to load
     * @return a {@link SceneFile} instance
     */
    private static SceneFile loadSceneFile(String sceneName) {
        if (invalidName(sceneName)) return null;
        sceneName = sceneName.trim();
        if (!Project.loaded()) {
            Logger.error(String.format(CannotLoadFormat, sceneName, ProjectNotLoaded));
            return null;
        }
        ProjectSceneMap sceneMap = Project.getScene(sceneName);
        if (sceneMap == null) {
            Logger.error(String.format(CannotLoadFormat, sceneName, NoSuchScene));
            return null;
        }
        String resolvedPath = UnifiedPaths.resolveToAbsolute(Project.projectRoot(), sceneMap.path());
        String fileContent;
        try {
            fileContent = new String(Files.readAllBytes(Paths.get(resolvedPath)));
            if (fileContent.isBlank()) throw new IOException("Empty unformatted scene file");
        } catch (IOException e) {
            Logger.warning(String.format(CannotLoadFormat, sceneName, e.getMessage()));
            SceneFile newFile = new SceneFile(sceneName);
            saveSceneFile(newFile);
            return newFile;
        }

        try {
            Gson gson = buildGson();
            JsonElement element = JsonParser.parseString(fileContent);
            if (element.isJsonArray()) {
                GameObject[] objects = gson.fromJson(element, GameObject[].class);
                Logger.info(String.format("Update scene file format for '%s'", sceneName));
                return SceneFile.fromLegacy(objects != null ? List.of(objects) : null, sceneName);
            }
            if (!element.isJsonObject()) {
                Logger.warning(String.format(CannotLoadFormat, sceneName, "unknow scene data format"));
                return new SceneFile(sceneName);
            }
            JsonObject jsonObject = element.getAsJsonObject();
            if (jsonObject.has("objects") || jsonObject.has("uuid") || jsonObject.has("name")) {
                return gson.fromJson(element, SceneFile.class);
            }
            GameObject[] objects = gson.fromJson(element, GameObject[].class);
            return SceneFile.fromLegacy(objects != null ? List.of(objects) : null, sceneName);
        } catch (JsonSyntaxException e) {
            Logger.error(String.format("Failed to parse scene file for '%s': ", e.getMessage()));
            return new SceneFile(sceneName);
        }
    }

    /**
     * Save scene file to disk.
     * @param file the file to save
     * @return true if saved successfully
     */
    private static boolean saveSceneFile(SceneFile file) {
        if (!Project.loaded()) return false;
        String sceneName = file.name();
        ProjectSceneMap sceneMap = Project.getScene(sceneName);
        if (sceneMap == null) {
            Logger.info(String.format("Registering missing scene '%s' with the project", sceneName));
            String newPath = createScenePath(sceneName);
            sceneMap = new ProjectSceneMap(newPath);
            if (!Project.addScene(sceneName, sceneMap)) {
                Logger.error(String.format(CannotSaveFormat, sceneName, RegisterSceneFailed));
                return false;
            }
        }
        String path = UnifiedPaths.resolveToAbsolute(Project.projectRoot(), sceneMap.path());
        try (FileWriter writer = new FileWriter(path)) {
            writer.write(buildGson().toJson(file));
            return true;
        } catch (IOException e) {
            Logger.error(String.format(CannotSaveFormat, sceneName, e.getMessage()));
            return false;
        }
    }

    /**
     * Runtime mode check to prevent override scene data.
     * @return true if currently in runtime mode
     */
    private static boolean runtimeMode() {
        if (LogicServer.runtimeMode()) {
            Logger.warning("Cannot save scene: currently in runtime mode");
            return true;
        }
        return false;
    }

    /**
     * Check if scene name is null or blank. If it is, log an error.
     * @param name the string to check
     * @return true if the name is invalid
     */
    private static boolean invalidName(String name) {
        if (name == null || name.isBlank()) {
            Logger.error("Invalid scene name: scene name cannot be null or blank");
            return true;
        }
        return false;
    }

    /**
     * Create a relative path to the scene file.
     * @param name name of the scene
     * @return the path string for the given name
     */
    private static String createScenePath(String name) {
        return String.format("scenes/%s.cell", name.replaceAll("[^a-zA-Z0-9_-]", "_"));
    }

    /**
     * Build gson for scene data serialization.
     * This consist of 2 type adapter for component and game object.
     * @return the configured Gson instance
     */
    private static Gson buildGson() {
        return new GsonBuilder()
                .registerTypeAdapter(Component.class, new ComponentSerializer())
                .registerTypeHierarchyAdapter(GameObject.class, new GameObjectSerializer())
                .enableComplexMapKeySerialization()
                .create();
    }
}
