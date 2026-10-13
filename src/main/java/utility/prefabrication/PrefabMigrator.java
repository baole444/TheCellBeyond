package utility.prefabrication;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import scene.ObjectDataMigrator;
import serialization.EngineSerializer;
import utility.log.EngineLog;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

/**
 * PrefabMigrator contains static method for migrating prefab data and file to the latest format.
 */
final class PrefabMigrator {
    private static final EngineLog Logger = new EngineLog(PrefabMigrator.class);
    private static final EngineSerializer Serializer = EngineSerializer.prettyPrint();
    /**
     * The last scene version prefab was written to without a version pin in data.
     */
    private static final int Unversioned = 2;

    private PrefabMigrator() {}

    /**
     * Update the given prefab data if it is out date, and write the updated format back to disk.
     * <p>
     * The object shape is migrated base on {@link ObjectDataMigrator} specification.
     * @param prefabJson the JSON object of the prefab
     * @param prefabName name of the prefab, for reporting
     * @param file the file the prefab was read from
     * @return the updated JSON object
     */
    static JsonObject migrateToLatest(JsonObject prefabJson, String prefabName, Path file) {
        int currentVersion = prefabJson.has(PrefabManager.VersionKey) ? prefabJson.get(PrefabManager.VersionKey).getAsInt() : Unversioned;
        if (currentVersion >= ObjectDataMigrator.currentVersion()) return prefabJson;
        Logger.info(String.format("Migrating prefab '%s' from version %d to %d", prefabName, currentVersion, ObjectDataMigrator.currentVersion()));
        JsonElement objectData = prefabJson.get(PrefabManager.DataKey);
        if (objectData != null) ObjectDataMigrator.migrateToLatest(objectData, currentVersion);
        prefabJson.addProperty(PrefabManager.VersionKey, ObjectDataMigrator.currentVersion());
        write(file, prefabJson, prefabName);
        return prefabJson;
    }

    /**
     * Saved the migrated prefab format back to disk.
     * <p>
     * If write failed, the prefab will be migrated on next launch of the project again. This does not affect the prefab usability.
     * @param file the file to write
     * @param prefabJson the migrated prefab
     * @param prefabName name of the prefab, for reporting
     */
    private static void write(Path file, JsonObject prefabJson, String prefabName) {
        try (FileWriter writer = new FileWriter(file.toFile())) {
            Serializer.serialize(prefabJson, writer);
        } catch (IOException e) {
            Logger.warning(String.format("Failed to write new format to disk for %s prefab: %s", prefabName, e.getMessage()));
        }
    }
}
