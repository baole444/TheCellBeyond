package scene;

import TheCellBeyond.GameObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SceneFile store the game objects data in the scene and the scene metadata.
 * @param uuid uuid of the scene
 * @param name name of the scene
 * @param sceneType type of the scene
 * @param version scene file version number
 * @param objects list of serialized game object
 */
public record SceneFile(UUID uuid, String name, String sceneType, int version, List<GameObject> objects) {
    public static final int SaveVersion = 1;

    /**
     * Compact constructor ensure that data of SceneFile is not null.
     * @param uuid uuid of the scene
     * @param name name of the scene
     * @param sceneType type of the scene
     * @param version scene file version number
     * @param objects list of serialized game object
     */
    public SceneFile {
        if (uuid == null) uuid = UUID.randomUUID();
        if (name == null || name.isBlank()) name = "scene_unknown_name_" + uuid;
        if (objects == null) objects = new ArrayList<>();
        else objects = new ArrayList<>(objects);
    }

    /**
     * Create a new SceneFile with empty data, randomized uuid at the current {@link #SaveVersion} using the given name.
     * @param name name of the scene
     */
    public SceneFile(String name) {
        this(UUID.randomUUID(), name, "", SaveVersion, new ArrayList<>());
    }

    /**
     * Convert legacy scene file data to new format.
     * @param objects list of serialized game object
     * @param sceneName name of the scene
     * @return a new {@link SceneFile}
     */
    public static SceneFile fromLegacy(List<GameObject> objects, String sceneName) {
        return new SceneFile(UUID.randomUUID(), sceneName, "", SaveVersion, objects);
    }
}
