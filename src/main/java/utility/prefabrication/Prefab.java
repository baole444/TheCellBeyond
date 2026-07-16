package utility.prefabrication;

import TheCellBeyond.GameObject;
import TheCellBeyond.internal.LogicServer;
import scripting.API;

/**
 * Prefab provide static method to instantiate a prefab file and/or add that instance to scene.
 * To instantiate directly to scene, use {@link #instantiateToScene(String)}.
 */
@API
public final class Prefab {
    private Prefab() {}
    /**
     * Instantiate a prefab by name.
     * @param prefabName name of the prefab
     * @return a new GameObject instance or null if prefab not found
     */
    public static GameObject instantiate(String prefabName) {
        return PrefabManager.instantiatePrefab(prefabName);
    }

    /**
     * Instantiate a prefab by name and add it to scene.
     * @param prefabName name of the prefab
     * @return a new GameObject instance that was added to the scene or null if prefab not found
     */
    public static GameObject instantiateToScene(String prefabName) {
        GameObject instance = PrefabManager.instantiatePrefab(prefabName);
        if (instance != null && LogicServer.currentScene() != null) LogicServer.currentScene().queueForObjectAddition(instance);
        return instance;
    }
}
