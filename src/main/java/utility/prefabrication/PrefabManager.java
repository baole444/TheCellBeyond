package utility.prefabrication;

import TheCellBeyond.GameObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import project.Project;
import serialization.EngineSerializer;
import utility.log.EngineLog;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class PrefabManager {
    private static final EngineLog Logger = new EngineLog(PrefabManager.class);
    private static final Map<String, PrefabData> loadedPrefabs = new ConcurrentHashMap<>();
    private static final Map<String, JsonObject> parsedPrefabCache = new ConcurrentHashMap<>();
    private static final EngineSerializer Serializer = EngineSerializer.standard();
    private static final EngineSerializer PrettySerializer = EngineSerializer.prettyPrint();
    private static final String PrefabName = "prefab_name";
    private static final String RootUUID = "rootUUID";
    private static final String Timestamp = "timestamp";
    private static final String Data = "data";
    private static final String PrefabDir = "prefabs";
    private static final String Extension = ".prefab";
    private static final String IncludeChild = "includesChildren";
    private static final String NamePattern = "[^a-zA-Z0-9_-]";
    private static final String ValidNamePattern = "[a-zA-Z0-9_-]+";

    private PrefabManager() {}

    public static boolean invalidPrefabName(String name) {
        return name == null || name.isBlank() || !name.matches(ValidNamePattern);
    }

    public static boolean prefabNameTaken(String name) {
        return invalidPrefabName(name) || loadedPrefabs.get(name) != null;
    }

    public static void savePrefab(GameObject gameObject, String prefabName, boolean includeChildren) {
        if (gameObject == null || prefabName == null || prefabName.isEmpty()) return;
        String root = Project.projectRoot();
        if (root == null) {
            Logger.debug("No project loaded to save prefab.");
            return;
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
            JsonObject prefabJson = new JsonObject();
            prefabJson.addProperty(PrefabName, prefabName);
            prefabJson.addProperty(IncludeChild, includeChildren);
            prefabJson.addProperty(Timestamp, System.currentTimeMillis());
            prefabJson.addProperty(RootUUID, gosToPrefab.getFirst().getUUID().toString());
            JsonElement objectData = PrettySerializer.toJsonTree(gosToPrefab);
            prefabJson.add(Data, objectData);
            String filename = prefabName.replaceAll(NamePattern, "_") + Extension;
            Path prefabFile = prefabsPath.resolve(filename);
            try (FileWriter writer = new FileWriter(prefabFile.toFile())) {
                PrettySerializer.serialize(prefabJson, writer);
            }
            String jsonString = PrettySerializer.serialize(prefabJson);
            loadedPrefabs.put(prefabName, new PrefabData(prefabName, jsonString, gameObject.name()+ (includeChildren ? " (with children)" : "")));
            parsedPrefabCache.put(prefabName, prefabJson);
            Logger.info(String.format("Saved prefab '%s' to %s", prefabName, prefabFile));
        } catch (IOException e) {
            Logger.error(String.format("Failed to save prefab '%s': %s", prefabName, e.getMessage()));
        }
    }

    public static void loadAllPrefabs() {
        String root = Project.projectRoot();
        if (root == null) return;
        loadedPrefabs.clear();
        parsedPrefabCache.clear();
        Path prefabsPath = Paths.get(root, PrefabDir);
        if (!Files.exists(prefabsPath)) return;
        try (Stream<Path> paths = Files.walk(prefabsPath, 1)) {
            paths.filter(p -> p.toString().endsWith(Extension)).forEach(PrefabManager::loadPrefabFile);
        } catch (IOException e) {
            Logger.error(String.format("Failed to load prefabs from disk: %s", e.getMessage()));
        }
    }

    public static GameObject instantiatePrefab(String prefabName) {
        if (!loadedPrefabs.containsKey(prefabName)) {
            Logger.error(String.format("Cannot instantiate '%s': prefab not found", prefabName));
            return null;
        }
        try {
            JsonObject prefabJson = parsedPrefabCache.get(prefabName);
            JsonElement objectData = prefabJson.get(Data);
            List<GameObject> gameObjects = new ArrayList<>();
            if (objectData.isJsonArray()) {
                for (JsonElement goElement : objectData.getAsJsonArray()) {
                    GameObject go = Serializer.deserialize(goElement, GameObject.class);
                    gameObjects.add(go);
                }
            }
            if (gameObjects.isEmpty()) {
                Logger.error(String.format("Cannot instantiate '%s': no valid GameObject found in prefab", prefabName));
                return null;
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
                Logger.error(String.format("Cannot instantiate '%s': missing prefab root object", prefabName));
                return null;
            }
            boolean includeChildren = false;
            if (prefabJson.has(IncludeChild)) {
                includeChildren = prefabJson.get(IncludeChild).getAsBoolean();
            }
            return root.copy(includeChildren);
        } catch (Exception e) {
            Logger.error(String.format("Failed to instantiate '%s': %s", prefabName, e.getMessage()));
            return null;
        }
    }

    public static void deletePrefab(String prefabName) {
        String root = Project.projectRoot();
        if (root == null) return;
        String filename = prefabName.replaceAll(NamePattern, "_") + Extension;
        Path prefabFile = Paths.get(root, PrefabDir, filename);
        try {
            if (Files.exists(prefabFile)) {
                Files.delete(prefabFile);
                loadedPrefabs.remove(prefabName);
                parsedPrefabCache.remove(prefabName);
                Logger.info(String.format("Prefab '%s' deleted", prefabName));
            }
        } catch (IOException e) {
            Logger.error(String.format("Failed to delete '%s': %s", prefabName, e.getMessage()));
        }
    }

    public static List<String> getPrefabNames() {
        return new ArrayList<>(loadedPrefabs.keySet());
    }

    public static PrefabData getPrefabData(String name) {
        return loadedPrefabs.get(name);
    }

    private static void loadPrefabFile(Path file) {
        try {
            String content = new String(Files.readAllBytes(file));
            JsonObject prefabJson = Serializer.deserialize(content, JsonObject.class);
            String name = prefabJson.get(PrefabName).getAsString();
            String description = name;
            if (prefabJson.has(IncludeChild) && prefabJson.get(IncludeChild).getAsBoolean()) description += " (with children)";
            loadedPrefabs.put(name, new PrefabData(name, content, description));
            parsedPrefabCache.put(name, prefabJson);
        } catch (Exception e) {
            Logger.error(String.format("Failed to load prefab file '%s': %s", file, e.getMessage()));
        }
    }

    public static void clear() {
        loadedPrefabs.clear();
        parsedPrefabCache.clear();
    }
}
