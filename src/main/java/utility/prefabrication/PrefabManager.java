package utility.prefabrication;

import TheCellBeyond.GameObject;
import TheCellBeyond.GameObject2D;
import TheCellBeyond.GameObject2DSerializer;
import TheCellBeyond.GameObjectSerializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import components.Component;
import components.ComponentSerializer;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static editor.project.Project.ProjectRoot;

public class PrefabManager {
    private static PrefabManager prefabManager;
    private final Map<String, PrefabData> loadedPrefabs = new ConcurrentHashMap<>();
    private static final String PREFABS_DIR = "prefabs";
    private static final String EXTENSION = ".prefab";
    private static final String INCLUDE_CHILD = "includesChildren";
    private static final String NAME_PATTERN = "[^a-zA-Z0-9_-]";

    private PrefabManager() {}

    public static PrefabManager get() {
        if (prefabManager == null) prefabManager = new PrefabManager();

        return prefabManager;
    }

    public boolean savePrefab(GameObject gameObject, String prefabName, boolean includeChildren) {
        if (gameObject == null || prefabName == null || prefabName.isEmpty()) return false;

        if (ProjectRoot == null) {
            System.err.println("No project loaded to save prefab. How did you managed to call this anyways?");
            return false;
        }

        try {
            Path prefabsPath = Paths.get(ProjectRoot, PREFABS_DIR);

            if (!Files.exists(prefabsPath)) Files.createDirectories(prefabsPath);

            GameObject prefabObject = gameObject.copy();

            // TODO: potentially faster way is to set the children list to null, could have consequence for that.
            if (!includeChildren) {
                for (GameObject child : new ArrayList<>(prefabObject.getChildren())) {
                    prefabObject.removeChild(child);
                }

                prefabObject.setChildrenUUIDs(new ArrayList<>());
            }

            prepareForSerialization(prefabObject);

            Gson gson = createGson();

            JsonObject prefabJson = new JsonObject();
            prefabJson.addProperty("name", prefabName);
            prefabJson.addProperty("type", gameObject instanceof GameObject2D ? "GameObject2D" : "GameObject");
            prefabJson.addProperty(INCLUDE_CHILD, includeChildren);
            prefabJson.addProperty("timestamp", System.currentTimeMillis());

            JsonElement objectData;
            if (gameObject instanceof GameObject2D) {
                objectData = gson.toJsonTree(prefabObject, GameObject2D.class);
            } else {
                objectData = gson.toJsonTree(prefabObject, GameObject.class);
            }

            prefabJson.add("data", objectData);

            String filename = prefabName.replaceAll(NAME_PATTERN, "_") + EXTENSION;
            Path prefabFile = prefabsPath.resolve(filename);

            try (FileWriter writer = new FileWriter(prefabFile.toFile())) {
                gson.toJson(prefabJson, writer);
            }

            String jsonString = gson.toJson(prefabJson);
            loadedPrefabs.put(prefabName, new PrefabData(
                    prefabName,
                    jsonString,
                    gameObject instanceof GameObject2D,
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
        if (ProjectRoot == null) return;

        loadedPrefabs.clear();

        Path prefabsPath = Paths.get(ProjectRoot, PREFABS_DIR);

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
            System.err.println("Prefab not found: " + prefabName);
            return null;
        }

        try {
            Gson gson = createGson();
            JsonObject prefabJson = gson.fromJson(prefabData.json(), JsonObject.class);
            JsonElement objectData = prefabJson.get("data");

            GameObject instance;
            if (prefabData.is2DObject()) {
                instance = gson.fromJson(objectData, GameObject2D.class);
            } else {
                instance = gson.fromJson(objectData, GameObject.class);
            }

            regenUUIDs(instance);

            return instance;
        } catch (Exception e) {
            System.err.println("Failed to instantiate prefab: " + e.getMessage());
            return null;
        }
    }

    public boolean deletePrefab(String prefabName) {
        if (ProjectRoot == null) return false;

        String filename = prefabName.replaceAll(NAME_PATTERN, "_") + EXTENSION;
        Path prefabFile = Paths.get(ProjectRoot, PREFABS_DIR, filename);

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

            String name = prefabJson.get("name").getAsString();
            boolean is2DObject = "GameObject2D".equals(prefabJson.get("type").getAsString());

            String description = name;
            if (prefabJson.has(INCLUDE_CHILD)
                    && prefabJson.get(INCLUDE_CHILD).getAsBoolean()
            ) description += " (with children)";

            loadedPrefabs.put(name, new PrefabData(name, content, is2DObject, description));
        } catch (Exception e) {
            System.err.println("Failed to load prefab file '" + file + "': " + e.getMessage());
        }
    }

    private void regenUUIDs(GameObject gameObject) {
        gameObject.setUUID(UUID.randomUUID().toString());
        gameObject.regenerateUID();

        for (Component c : gameObject.getComponents()) {
            c.setUUID(UUID.randomUUID().toString());
        }

        for (GameObject child : gameObject.getChildren()) {
            regenUUIDs(child);
        }
    }

    private void prepareForSerialization(GameObject gameObject) {
        gameObject.prepareForSerialization();
        for (GameObject child : gameObject.getChildren()) {
            prepareForSerialization(child);
        }
    }
    private Gson createGson() {
        return new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Component.class, new ComponentSerializer())
                .registerTypeAdapter(GameObject.class, new GameObjectSerializer())
                .registerTypeAdapter(GameObject2D.class, new GameObject2DSerializer())
                .enableComplexMapKeySerialization()
                .create();
    }

    public void clear() {
        loadedPrefabs.clear();
    }
}
