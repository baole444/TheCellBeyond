package utility.prefabrication;

import TheCellBeyond.GameObject;
import TheCellBeyond.GameObjectSerializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import components.Component;
import components.ComponentSerializer;
import project.Project;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class PrefabManager {
    private static PrefabManager prefabManager;
    private final Map<String, PrefabData> loadedPrefabs = new ConcurrentHashMap<>();
    private final String PREFAB_NAME = "prefab_name";
    private final String ROOT_UUID = "rootUUID";
    private final String TIMESTAMP = "timestamp";
    private final String DATA = "data";
    private final String PREFABS_DIR = "prefabs";
    private final String EXTENSION = ".prefab";
    private final String INCLUDE_CHILD = "includesChildren";
    private final String NAME_PATTERN = "[^a-zA-Z0-9_-]";

    private PrefabManager() {}

    public static PrefabManager get() {
        if (prefabManager == null) prefabManager = new PrefabManager();

        return prefabManager;
    }

    public boolean savePrefab(GameObject gameObject, String prefabName, boolean includeChildren) {
        if (gameObject == null || prefabName == null || prefabName.isEmpty()) return false;

        String root = Project.projectRoot();

        if (root == null) {
            System.err.println("No project loaded to save prefab. How did you managed to call this anyways?");
            return false;
        }

        try {
            Path prefabsPath = Paths.get(root, PREFABS_DIR);

            if (!Files.exists(prefabsPath)) Files.createDirectories(prefabsPath);

            List<GameObject> gosToPrefab = new ArrayList<>();

            if (includeChildren) {
                GameObject prefabRoot = gameObject.copy(true);

                gosToPrefab.add(prefabRoot);
                gosToPrefab.addAll(prefabRoot.getAllDescendants());

                for (GameObject go : gosToPrefab) {
                    go.prepareForSerialization();
                }
            } else {
                GameObject prefabRoot = gameObject.copy(false);
                prefabRoot.prepareForSerialization();
                gosToPrefab.add(prefabRoot);
            }

            Gson gson = createGson();

            JsonObject prefabJson = new JsonObject();
            prefabJson.addProperty(PREFAB_NAME, prefabName);
            prefabJson.addProperty(INCLUDE_CHILD, includeChildren);
            prefabJson.addProperty(TIMESTAMP, System.currentTimeMillis());
            prefabJson.addProperty(ROOT_UUID, gosToPrefab.getFirst().getUUID().toString());

            JsonElement objectData = gson.toJsonTree(gosToPrefab);

            prefabJson.add(DATA, objectData);

            String filename = prefabName.replaceAll(NAME_PATTERN, "_") + EXTENSION;
            Path prefabFile = prefabsPath.resolve(filename);

            try (FileWriter writer = new FileWriter(prefabFile.toFile())) {
                gson.toJson(prefabJson, writer);
            }

            String jsonString = gson.toJson(prefabJson);
            loadedPrefabs.put(prefabName, new PrefabData(
                    prefabName,
                    jsonString,
                    gameObject.name + (includeChildren ? " (with children)" : "")
            ));

            System.out.println("Saved prefab: " + prefabName + " to " + prefabFile);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to save prefab: " + e.getMessage());
            return false;
        }
    }

    public void loadAllPrefabs() {
        String root = Project.projectRoot();

        if (root == null) return;

        loadedPrefabs.clear();

        Path prefabsPath = Paths.get(root, PREFABS_DIR);

        if (!Files.exists(prefabsPath)) return;

        try (Stream<Path> paths = Files.walk(prefabsPath, 1)) {
            paths.filter(p -> p.toString().endsWith(EXTENSION)).forEach(this::loadPrefabFile);
        } catch (IOException e) {
            System.err.println("Failed to load prefabs: " + e.getMessage());
        }
    }

    public GameObject instantiatePrefab(String prefabName) {
        PrefabData prefabData = loadedPrefabs.get(prefabName);
        if (prefabData == null) {
            System.err.println("Prefab not found: '" + prefabName + "'");
            return null;
        }

        try {
            Gson gson = createGson();
            JsonObject prefabJson = gson.fromJson(prefabData.json(), JsonObject.class);
            JsonElement objectData = prefabJson.get(DATA);
            List<GameObject> gameObjects = new ArrayList<>();

            if (objectData.isJsonArray()) {
                for (JsonElement goElement : objectData.getAsJsonArray()) {
                    GameObject go = gson.fromJson(goElement, GameObject.class);
                    gameObjects.add(go);
                }
            }

            if (gameObjects.isEmpty()) {
                System.err.println("No valid GameObject found in prefab: '" + prefabName + "'");
            }

            Map<UUID, GameObject> goMap = new HashMap<>();
            for (GameObject go : gameObjects) {
                goMap.put(go.getUUID(), go);
            }

            for (GameObject go : gameObjects) {
                if (go.getParentUUID() != null) {
                    GameObject parent = goMap.get(go.getParentUUID());
                    if (parent != null) parent.addChild(go);
                }

                if (go.getChildrenUUIDs() != null && !go.getChildrenUUIDs().isEmpty()) {
                    for (UUID childUUID : go.getChildrenUUIDs()) {
                        GameObject child = goMap.get(childUUID);
                        if (child != null && child.getParent() == null) go.addChild(child);
                    }
                }
            }

            JsonElement rootUUID = prefabJson.get(ROOT_UUID);
            GameObject root = goMap.get(UUID.fromString(rootUUID.getAsString()));

            if (root == null) {
                System.err.println("Root object of prefab not found");
                return null;
            }

            boolean includeChildren = false;

            if (prefabJson.has(INCLUDE_CHILD)) {
                includeChildren = prefabJson.get(INCLUDE_CHILD).getAsBoolean();
            }

            return root.copy(includeChildren);
        } catch (Exception e) {
            System.err.println("Failed to instantiate prefab: " + e.getMessage());
            return null;
        }
    }

    public boolean deletePrefab(String prefabName) {
        String root = Project.projectRoot();

        if (root == null) return false;

        String filename = prefabName.replaceAll(NAME_PATTERN, "_") + EXTENSION;
        Path prefabFile = Paths.get(root, PREFABS_DIR, filename);

        try {
            if (Files.exists(prefabFile)) {
                Files.delete(prefabFile);
                loadedPrefabs.remove(prefabName);
                return true;
            }
        } catch (IOException e) {
            System.err.println("Failed to delete prefab: " + e.getMessage());
        }

        return false;
    }

    public List<String> getPrefabNames() {
        return new ArrayList<>(loadedPrefabs.keySet());
    }

    public PrefabData getPrefabData(String name) {
        return loadedPrefabs.get(name);
    }

    private void loadPrefabFile(Path file) {
        try {
            String content = new String(Files.readAllBytes(file));
            Gson gson = createGson();
            JsonObject prefabJson = gson.fromJson(content, JsonObject.class);

            String name = prefabJson.get(PREFAB_NAME).getAsString();

            String description = name;
            if (prefabJson.has(INCLUDE_CHILD)
                    && prefabJson.get(INCLUDE_CHILD).getAsBoolean()
            ) description += " (with children)";

            loadedPrefabs.put(name, new PrefabData(name, content, description));
        } catch (Exception e) {
            System.err.println("Failed to load prefab file '" + file + "': " + e.getMessage());
        }
    }

    private Gson createGson() {
        return new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Component.class, new ComponentSerializer())
                .registerTypeHierarchyAdapter(GameObject.class, new GameObjectSerializer())
                .enableComplexMapKeySerialization()
                .create();
    }

    public void clear() {
        loadedPrefabs.clear();
    }
}
