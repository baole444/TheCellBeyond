package utility.prefabrication;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import scene.ObjectDataMigrator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class PrefabMigratorTest {
    private static final String TexturePath = "assets/foo.png";

    @Test
    public void unVersionedPrefabMigrateSuccessfully() throws IOException {
        JsonObject migrated = migrate("""
                {
                  "prefab_name": "Terrain",
                  "data": [ { "properties": { "tileSet": { "tileSetSprite": { "textureCanonicalPath": "assets/foo.png" } } } } ]
                }
                """);
        assertEquals(ObjectDataMigrator.currentVersion(), migrated.get("version").getAsInt(), "a prefab written before it carried a version should be stamped at the current one");
        JsonObject sprite = migrated.getAsJsonArray("data").get(0).getAsJsonObject().getAsJsonObject("properties").getAsJsonObject("tileSet").getAsJsonObject("tileSetSprite");
        assertEquals(TexturePath, sprite.getAsJsonObject("textureReference").get("canonicalPath").getAsString(), "a prefab's objects migrate exactly as a scene's do");
        assertFalse(sprite.has("textureCanonicalPath"), "the flat path should not survive the migration");
    }

    @Test
    public void skipUpToDatePrefabCorrectly() throws IOException {
        String current = """
                {
                  "prefab_name": "Terrain",
                  "version": 3,
                  "data": [ { "properties": { "components": [ { "sprite": { "textureReference": { "canonicalPath": "assets/foo.png" } } } ] } } ]
                }
                """;
        assertEquals(JsonParser.parseString(current).getAsJsonObject(), migrate(current), "a prefab at the current version should pass through untouched");
    }

    @Test
    public void migrateEmptyPrefabSuccessfully() throws IOException {
        JsonObject migrated = migrate("{ \"prefab_name\": \"Empty\" }");
        assertEquals(ObjectDataMigrator.currentVersion(), migrated.get("version").getAsInt(), "a prefab holding no objects should still be stamped rather than fail");
    }

    private static JsonObject migrate(String prefabJson) throws IOException {
        Path file = Files.createTempFile("prefab", ".prefab");
        JsonObject json = JsonParser.parseString(prefabJson).getAsJsonObject();
        try {
            PrefabMigrator.migrateToLatest(json, "test_prefab", file);
            return json;
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
