package scene;

import TheCellBeyond.GameObject;
import TheCellBeyond.internal.LogicServer;
import com.google.gson.*;
import eventviewer.EngineEventCallback;
import eventviewer.event.EditorEvent;
import project.Project;
import project.ProjectSceneMap;
import serialization.EngineSerializer;
import utility.UnifiedPaths;
import utility.log.EngineLog;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Manager for scene's file data.
 */
public final class SceneManager {
    private static final EngineLog Logger = new EngineLog(SceneManager.class);
    private static final EngineSerializer Serializer = EngineSerializer.standard();
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

    /**
     * Field order: Old name -> New name -> Reason.
     */
    private static final String CannotRenameFormat = "Cannot rename scene '%s' to '%s': %s";

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
     * Check if a scene name is taken or not.
     * @param name the name string to check
     * @return true if taken
     */
    public static boolean sceneNameTaken(String name) {
        if (!validSceneName(name) || !Project.loaded()) return true;
        return Project.getScene(name.trim()) != null;
    }

    /**
     * Initialize the given scene with a root object as an unsaved scene.
     * @param scene the scene to initialize
     * @param root the root object for the scene
     */
    public static void initUnsavedScene(Scene scene, GameObject root) {
        if (scene == null || root == null) return;
        scene.initWithRoot(root);
    }

    /**
     * Replace the root object of the given scene,
     * mostly use in conjunction with {@link GameObject#changeType(GameObject, Class)} to change object type.
     * <p>
     * THe new root object should have transfer its uuid, hierarchies and components to preserve scene's semantic if needed.
     * @param scene the scene to replace root object
     * @param newRoot the new root object for the scene
     */
    public static void replaceSceneRoot(Scene scene, GameObject newRoot) {
        if (scene == null || newRoot == null) return;
        scene.replaceRoot(newRoot);
    }

    /**
     * Replace an object in the given scene,
     * mostly use in conjunction with {@link GameObject#changeType(GameObject, Class)} to change object type.
     * <p>
     * Unless uuid, components or hierarchies are transferred to the new object, this is basically a remove and add.
     * @param scene the scene to replace the object
     * @param oldObject the old object to replace
     * @param newObject the new object to take its place
     */
    public static void replaceObject(Scene scene, GameObject oldObject, GameObject newObject) {
        if (scene == null || oldObject == null || newObject == null) return;
        scene.replaceObject(oldObject, newObject);
    }

    /**
     * Save the current scene to disk.
     */
    public static void saveCurrentScene() {
        if (runtimeMode("save current scene")) return;
        Scene currentScene = LogicServer.currentScene();
        if (currentScene == null) {
            Logger.warning(String.format(CannotSaveFormat, "unknown", SceneNotLoaded));
            return;
        }
        String currentName = LogicServer.currentSceneName();
        if (currentName == null) {
            EngineEventCallback.emit(new EditorEvent(EditorEvent.Type.RequestSaveSceneAs));
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
        if (runtimeMode("save scene")) return false;
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
        GameObject root = scene.root();
        if (root != null) root.prepareForSerialization();
        List<GameObject> objects = scene.getSerializedObjects();
        objects.removeIf(go -> go == root);
        objects.forEach(GameObject::prepareForSerialization);
        String type = root == null ? "" : root.getClass().getCanonicalName();
        SceneFile file = new SceneFile(scene.sceneUUID(), sceneName, type, SceneFile.SaveVersion, root, objects);
        return saveSceneFile(file);
    }

    /**
     * Save the current scene as a new scene using the given name.
     * @param newSceneName name for the copy
     * @return true if save successfully
     */
    public static boolean saveSceneAs(String newSceneName) {
        if (runtimeMode("save scene as") || invalidName(newSceneName)) return false;
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
        currentScene.name(newSceneName);
        Logger.info(String.format("Saved scene as '%s'", newSceneName));
        return true;
    }

    /**
     * Create a new scene using the given name.
     * @param sceneName name of the new scene
     * @return true if create successfully
     */
    public static boolean createNewScene(String sceneName) {
        if (runtimeMode("create new scene") || invalidName(sceneName)) return false;
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
        Logger.info(String.format("Created new scene '%s'", sceneName));
        return true;
    }

    public static boolean renameScene(String oldName, String newName) {
        if (runtimeMode("renaming scene")) return false;
        if (invalidName(oldName) || invalidName(newName)) return false;
        oldName = oldName.trim();
        newName = newName.trim();
        if (oldName.equals(newName)) return false;
        if (!Project.loaded()) {
            Logger.error(String.format(CannotRenameFormat, oldName, newName, ProjectNotLoaded));
            return false;
        }
        if (sceneNameTaken(newName)) {
            Logger.error(String.format(CannotRenameFormat, oldName, newName, SceneAlreadyExist));
            return false;
        }
        ProjectSceneMap oldSceneMap = Project.getScene(oldName);
        if (oldSceneMap == null) {
            Logger.error(String.format(CannotRenameFormat, oldName, newName, NoSuchScene));
            return false;
        }
        String oldPath = UnifiedPaths.resolveToAbsolute(Project.projectRoot(), oldSceneMap.path());
        String newRelativePath = createScenePath(newName);
        String newAbsolutePath = UnifiedPaths.resolveToAbsolute(Project.projectRoot(), newRelativePath);
        Path source = Paths.get(oldPath);
        Path destination = Paths.get(newAbsolutePath);
        try {
            Files.move(source, destination);
        } catch (IOException e) {
            Logger.error(String.format(CannotRenameFormat, oldName, newName, e.getMessage()));
            return false;
        }
        Project.removeScene(oldName);
        ProjectSceneMap newSceneMap = new ProjectSceneMap(newRelativePath);
        if (!Project.addScene(newName, newSceneMap)) {
            try {
                Files.move(destination, source);
            } catch (IOException _) {}
            Logger.error(String.format(CannotRenameFormat, oldName, newName, RegisterSceneFailed));
            return false;
        }
        Scene currentScene = LogicServer.currentScene();
        if (currentScene != null && oldName.equals(currentScene.name())) {
            currentScene.name(newName);
            saveScene(newName, currentScene);
        }
        Logger.info(String.format("Renamed scene '%s' to '%s'", oldName, newName));
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
     * The data is sourced using the given scene name.
     * @param scene the scene instance to receive the data
     * @param sceneName the name of the scene to get data from
     * @apiNote The scene instance passed into the method is assumed to not
     * have its name set by the Scene Manager yet.
     */
    public static void loadScene(Scene scene, String sceneName) {
        if (scene == null) {
            Logger.warning("Cannot load data into null scene");
            return;
        }
        if (invalidName(sceneName)) {
            Logger.warning("Cannot load data into scene: invalid scene name");
            return;
        }
        sceneName = sceneName.trim();
        SceneFile file = loadSceneFile(sceneName);
        if (file == null) {
            Logger.warning(String.format("No data for scene '%s' to be loaded", sceneName));
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
            JsonElement element = JsonParser.parseString(fileContent);
            JsonObject jsonObject = SceneFileMigrator.migrateToLatest(element, sceneName);
            if (jsonObject == null) throw new JsonSyntaxException("Unknown scene data format");
            return Serializer.deserialize(jsonObject, SceneFile.class);
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
            }                return false;

        }
        String path = UnifiedPaths.resolveToAbsolute(Project.projectRoot(), sceneMap.path());
        try (FileWriter writer = new FileWriter(path)) {
            writer.write(Serializer.serialize(file));
            return true;
        } catch (IOException e) {
            Logger.error(String.format(CannotSaveFormat, sceneName, e.getMessage()));
            return false;
        }
    }

    /**
     * Runtime mode check to prevent override scene data.
     * @param operation the name of the operation to log if is in runtime mode
     * @return true if currently in runtime mode
     */
    private static boolean runtimeMode(String operation) {
        if (LogicServer.runtimeMode()) {
            Logger.warning(String.format("Cannot %s: currently in runtime mode", operation));
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
}
