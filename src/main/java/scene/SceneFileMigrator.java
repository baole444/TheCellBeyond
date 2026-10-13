package scene;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import serialization.EngineSerializer;
import utility.log.EngineLog;

import java.util.Set;
import java.util.UUID;

/**
 * Migrator for SceneFile data.
 */
class SceneFileMigrator {
    private static final EngineLog Logger = new EngineLog(SceneFileMigrator.class);
    private static final EngineSerializer Serializer = EngineSerializer.standard();
    private static final Set<Integer> BreakingChanges = Set.of(2, 3);
    private static final String PropertiesKey = "properties";
    private static final String UUIDKey = "uuid";
    private static final String ParentUUIDKey = "parentUUID";
    private static final String ChildrenUUIDsKey = "childrenUUIDs";

    static JsonObject migrateToLatest(JsonElement element, String sceneName) {
        JsonObject jsonObject = convertFromLegacy(element, sceneName);
        if (jsonObject == null) return null;
        int currentVersion = jsonObject.has(SceneFile.VersionKey) ? jsonObject.get(SceneFile.VersionKey).getAsInt() : 0;
        if (currentVersion >= SceneFile.SaveVersion) return jsonObject;
        Logger.info(String.format("Migrating scene file '%s' from version %d to %d", sceneName, currentVersion, SceneFile.SaveVersion));
        while (currentVersion < SceneFile.SaveVersion) {
            int nextBreaking = nextBreakingChange(currentVersion);
            if (nextBreaking == -1) break;
            jsonObject = applyBreakingChange(jsonObject, nextBreaking);
            currentVersion = nextBreaking;
        }
        return jsonObject;
    }

    private static JsonObject convertFromLegacy(JsonElement element, String sceneName) {
        if (element.isJsonArray()) return toV1Standard(element, sceneName);
        if (!element.isJsonObject()) {
            Logger.warning(String.format("Unknown data format for scene '%s'", sceneName));
            return null;
        }
        JsonObject jsonObject = element.getAsJsonObject();
        if (!jsonObject.has(SceneFile.VersionKey)) jsonObject.addProperty(SceneFile.VersionKey, 1);
        return jsonObject;
    }

    private static int nextBreakingChange(int from) {
        for (int v = from + 1; v <= SceneFile.SaveVersion; v++) {
            if (BreakingChanges.contains(v)) return v;
        }
        return -1;
    }

    private static JsonObject applyBreakingChange(JsonObject jsonObject, int version) {
        return switch (version) {
            case 2 -> migrateToV2(jsonObject);
            case 3 -> migrateToV3(jsonObject);
            default -> jsonObject;
        };
    }

    /**
     * From plain array of object (before v1) to v1 standard scene file.
     * <p>
     * The migration to version 2 is done on the serialized data directly, avoid losing data due to changes in classes' members.
     * @param element the JSON element to convert
     * @param sceneName name of the scene
     * @return the converted JSON object
     */
    private static JsonObject toV1Standard(JsonElement element, String sceneName) {
        JsonObject v1Standard = new JsonObject();
        v1Standard.addProperty(SceneFile.UUIDKey, UUID.randomUUID().toString());
        v1Standard.addProperty(SceneFile.NameKey, sceneName);
        v1Standard.addProperty(SceneFile.VersionKey, 1);
        v1Standard.add(SceneFile.ObjectsKey, element.getAsJsonArray());
        return v1Standard;
    }

    /**
     * Migrate scene file version 1 to version 2.
     * Add default root and add root as parent for all old root-level (orphan) objects.
     * @param jsonObject the JSON object to migrate
     * @return the updated JSON object
     */
    private static JsonObject migrateToV2(JsonObject jsonObject) {
        JsonArray objects = jsonObject.has(SceneFile.ObjectsKey) ? jsonObject.getAsJsonArray(SceneFile.ObjectsKey) : new JsonArray();
        JsonObject root = Serializer.toJsonTree(SceneFile.defaultRoot()).getAsJsonObject();
        reparentToRoot(objects, root);
        jsonObject.add(SceneFile.RootKey, root);
        jsonObject.addProperty(SceneFile.VersionKey, 2);
        return jsonObject;
    }

    /**
     * Reparent all orphan objects as root's children. This operation is done entirely on the serialized data, instead of live game objects.
     * @param objects the scene's objects
     * @param root the fresh root, as its serialized JSON
     */
    private static void reparentToRoot(JsonArray objects, JsonObject root) {
        JsonObject rootProperties = root.getAsJsonObject(PropertiesKey);
        if (rootProperties == null) return;
        String rootUUID = rootProperties.get(UUIDKey).getAsString();
        JsonArray rootChildren = rootProperties.has(ChildrenUUIDsKey) ? rootProperties.getAsJsonArray(ChildrenUUIDsKey) : new JsonArray();
        for (JsonElement element : objects) {
            if (!element.isJsonObject()) continue;
            JsonObject properties = element.getAsJsonObject().getAsJsonObject(PropertiesKey);
            if (properties == null || hasParent(properties)) continue;
            properties.addProperty(ParentUUIDKey, rootUUID);
            rootChildren.add(properties.get(UUIDKey).getAsString());
        }
        rootProperties.add(ChildrenUUIDsKey, rootChildren);
    }

    /**
     * Check if an object has parent on its properties or not.
     * @param properties  the object's serialized properties
     * @return true when the object {@value ParentUUIDKey} key's value is presence
     */
    private static boolean hasParent(JsonObject properties) {
        JsonElement parent = properties.get(ParentUUIDKey);
        return parent != null && !parent.isJsonNull();
    }

    /**
     * Migrate scene file version 2 to version 3.
     * Wrap sprites' texture canonical paths into asset references, and update tile sets with their new regions.
     * @param jsonObject the JSON object to migrate
     * @return the updated JSON object
     */
    private static JsonObject migrateToV3(JsonObject jsonObject) {
        ObjectDataMigrator.migrateToLatest(jsonObject, 2);
        jsonObject.addProperty(SceneFile.VersionKey, 3);
        return jsonObject;
    }
}
