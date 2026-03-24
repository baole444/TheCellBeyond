package scene;

import TheCellBeyond.GameObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import serialization.EngineSerializer;
import utility.log.EngineLog;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

/**
 * Migrator for SceneFile data.
 */
class SceneFileMigrator {
    private static final EngineLog Logger = new EngineLog(SceneFileMigrator.class);
    private static final Set<Integer> BreakingChanges = Set.of(2);

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
            default -> jsonObject;
        };
    }

    /**
     * From plain array of object (before v1) to v1 standard scene file.
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
        EngineSerializer serializer = EngineSerializer.standard();
        JsonArray jsonArray = jsonObject.has(SceneFile.ObjectsKey) ? jsonObject.getAsJsonArray(SceneFile.ObjectsKey) : new JsonArray();
        GameObject[] objects = serializer.deserialize(jsonArray, GameObject[].class);
        GameObject root = SceneFile.defaultRoot();
        if (objects != null) Arrays.stream(objects).filter(go -> go.getParentUUID() == null).forEach(root::addChild);
        root.prepareForSerialization();
        jsonObject.add(SceneFile.RootKey, serializer.toJsonTree(root));
        jsonObject.addProperty(SceneFile.VersionKey, 2);
        return jsonObject;
    }
}
