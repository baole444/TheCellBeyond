package utility.prefabrication;

import TheCellBeyond.GameObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import project.Project;
import serialization.EngineSerializer;

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
    private final String PrefabName = "prefab_name";
    private final String RootUUID = "rootUUID";
    private final String Timestamp = "timestamp";
    private final String Data = "data";
    private final String PrefabDir = "prefabs";
    private final String Extension = ".prefab";
    private final String IncludeChild = "includesChildren";
    private final String NamePattern = "[^a-zA-Z0-9_-]";

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
            Path prefabsPath = Paths.get(root, PrefabDir);
            if (!Files.exists(prefabsPath)) Files.createDirectories(prefabsPath);
            List<GameObject> gosToPrefab = new ArrayList<>();
            if (includeChildren) {
                GameObject prefabRoot = gameObject.copy(true);
                gosToPrefab.add(prefabRoot);
                gosToPrefab.addAll(prefabRoot.getAllDescendants());
                for (GameObject go : gosToPrefab) go.prepareForSerialization();
            } else {
                GameObject prefabRoot = gameObject.copy(false);
                prefabRoot.prepareForSerialization();
                gosToPrefab.add(prefabRoot);
            }
            EngineSerializer serializer = EngineSerializer.prettyPrint();
            JsonObject prefabJson = new JsonObject();
            prefabJson.addProperty(PrefabName, prefabName);
            prefabJson.addProperty(IncludeChild, includeChildren);
            prefabJson.addProperty(Timestamp, System.currentTimeMillis());
            prefabJson.addProperty(RootUUID, gosToPrefab.getFirst().getUUID().toString());
            JsonElement objectData = serializer.toJsonTree(gosToPrefab);
            prefabJson.add(Data, objectData);
            String filename = prefabName.replaceAll(NamePattern, "_") + Extension;
            Path prefabFile = prefabsPath.resolve(filename);
            try (FileWriter writer = new FileWriter(prefabFile.toFile())) {
                serializer.serialize(prefabJson, writer);
            }
            String jsonString = serializer.serialize(prefabJson);
            loadedPrefabs.put(prefabName, new PrefabData(
                    prefabName,
                    jsonString,
                    gameObject.name()+ (includeChildren ? " (with children)" : "")
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
        Path prefabsPath = Paths.get(root, PrefabDir);
        if (!Files.exists(prefabsPath)) return;
        try (Stream<Path> paths = Files.walk(prefabsPath, 1)) {
            paths.filter(p -> p.toString().endsWith(Extension)).forEach(this::loadPrefabFile);
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
            EngineSerializer serializer = EngineSerializer.standard();
            JsonObject prefabJson = serializer.deserialize(prefabData.json(), JsonObject.class);
            JsonElement objectData = prefabJson.get(Data);
            List<GameObject> gameObjects = new ArrayList<>();
            if (objectData.isJsonArray()) {
                for (JsonElement goElement : objectData.getAsJsonArray()) {
                    GameObject go = serializer.deserialize(goElement, GameObject.class);
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
            JsonElement rootUUID = prefabJson.get(RootUUID);
            GameObject root = goMap.get(UUID.fromString(rootUUID.getAsString()));
            if (root == null) {
                System.err.println("Root object of prefab not found");
                return null;
            }
            boolean includeChildren = false;
            if (prefabJson.has(IncludeChild)) {
                includeChildren = prefabJson.get(IncludeChild).getAsBoolean();
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
        String filename = prefabName.replaceAll(NamePattern, "_") + Extension;
        Path prefabFile = Paths.get(root, PrefabDir, filename);
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
            EngineSerializer serializer = EngineSerializer.standard();
            JsonObject prefabJson = serializer.deserialize(content, JsonObject.class);
            String name = prefabJson.get(PrefabName).getAsString();
            String description = name;
            if (prefabJson.has(IncludeChild) && prefabJson.get(IncludeChild).getAsBoolean()) description += " (with children)";
            loadedPrefabs.put(name, new PrefabData(name, content, description));
        } catch (Exception e) {
            System.err.println("Failed to load prefab file '" + file + "': " + e.getMessage());
        }
    }

    public void clear() {
        loadedPrefabs.clear();
    }
}
