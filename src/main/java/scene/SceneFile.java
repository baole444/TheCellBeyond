package scene;

import TheCellBeyond.GameObject;
import TheCellBeyond.GameObject2D;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SceneFile store the game objects data in the scene and the scene metadata.
 * @param uuid uuid of the scene
 * @param name name of the scene
 * @param sceneType type of the scene
 * @param version scene file version number
 * @param root scene root object
 * @param objects list of serialized game object
 */
public record SceneFile(UUID uuid, String name, String sceneType, int version, GameObject root, List<GameObject> objects) {
    static final int SaveVersion = 2;

    static final String UUIDKey = "uuid";
    static final String NameKey = "name";
    static final String SceneTypeKey = "sceneType";
    static final String VersionKey = "version";
    static final String RootKey = "root";
    static final String ObjectsKey = "objects";

    /**
     * Compact constructor ensure that data of SceneFile is not null.
     * @param uuid uuid of the scene
     * @param name name of the scene
     * @param sceneType type of the scene
     * @param version scene file version number
     * @param root scene root object
     * @param objects list of serialized game object
     */
    public SceneFile {
        if (uuid == null) uuid = UUID.randomUUID();
        if (name == null || name.isBlank()) name = "scene_unknown_name_" + uuid;
        if (root == null) root = new GameObject2D("Root");
        if (objects == null) objects = new ArrayList<>();
        else objects = new ArrayList<>(objects);
    }

    /**
     * Create a new SceneFile with empty data, randomized uuid at the current {@link #SaveVersion} using the given name.
     * @param name name of the scene
     */
    public SceneFile(String name) {
        this(UUID.randomUUID(), name, "", SaveVersion, new GameObject2D("Root"), new ArrayList<>());
    }

    /**
     * Convert legacy scene file data to new format.
     * @param objects list of serialized game object
     * @param sceneName name of the scene
     * @return a new {@link SceneFile}
     */
    public static SceneFile fromLegacy(List<GameObject> objects, String sceneName) {
        return new SceneFile(UUID.randomUUID(), sceneName, "", 1, null, objects);
    }

    /**
     * Get the default root object for scene file
     * @return an instance of {@link GameObject2D} named "Root"
     */
    static GameObject defaultRoot() {
        return new GameObject2D("Root");
    }
}
