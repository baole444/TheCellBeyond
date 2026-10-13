package scene;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class SceneFileMigratorTest {
    private static final String TexturePath = "assets/foo.png";
    private static final String EnginePath = utility.Settings.TexturePath.EditorControls;
    private static final org.joml.Vector2i EngineImageSize = render.texture.ImageHeader.size(EnginePath);

    @Test
    public void wrapTextureReference() {
        JsonObject migrated = migrate("""
                {
                  "version": 2,
                  "root": { "components": [ { "sprite": { "textureCanonicalPath": "assets/foo.png" } } ] }
                }
                """);
        assertEquals(3, migrated.get("version").getAsInt(), "the scene should be stamped at the current version");
        JsonObject sprite = migrated.getAsJsonObject("root").getAsJsonArray("components").get(0).getAsJsonObject().getAsJsonObject("sprite");
        assertFalse(sprite.has("textureCanonicalPath"), "the flat path should not survive the migration");
        assertEquals(TexturePath, sprite.getAsJsonObject("textureReference").get("canonicalPath").getAsString(), "the path value should be carried into the reference unchanged");
    }

    @Test
    public void wrapNestedTextureReferences() {
        JsonObject migrated = migrate("""
                {
                  "version": 2,
                  "root": {
                    "children": [ { "components": [
                      { "sprite": { "textureCanonicalPath": "assets/foo.png" } },
                      { "tileSet": { "tileSetSprite": { "textureCanonicalPath": "assets/foo.png" } } }
                    ] } ]
                  },
                  "objects": [ { "components": [ { "sprite": { "textureCanonicalPath": "assets/foo.png" } } ] } ]
                }
                """);
        assertEquals(3, countReferences(migrated), "every sprite in the tree should be migrated, whatever holds it");
        assertFalse(migrated.toString().contains("textureCanonicalPath"), "no flat path should remain anywhere");
    }

    @Test
    public void skipSpriteWithoutTexture() {
        JsonObject migrated = migrate("""
                {
                  "version": 2,
                  "root": { "components": [ { "sprite": { "width": 16.0 } } ] }
                }
                """);
        JsonObject sprite = migrated.getAsJsonObject("root").getAsJsonArray("components").get(0).getAsJsonObject().getAsJsonObject("sprite");
        assertFalse(sprite.has("textureReference"), "a sprite that claimed no texture should not gain a reference");
        assertEquals(16.0f, sprite.get("width").getAsFloat(), "the sprite's other data should be untouched");
    }

    @Test
    public void skipUpToDateScene() {
        String current = """
                {
                  "version": 3,
                  "root": { "components": [ { "sprite": { "textureReference": { "canonicalPath": "assets/foo.png" } } } ] }
                }
                """;
        assertEquals(JsonParser.parseString(current).getAsJsonObject(), migrate(current), "a scene at the current version should pass through untouched");
    }

    @Test
    public void fillTileSetRegion() {
        JsonObject migrated = migrate(String.format("""
                {
                  "version": 2,
                  "root": { "properties": { "tileSet": { "tileSetSprite": { "textureCanonicalPath": "%s" } } } }
                }
                """, EnginePath));
        JsonObject endPosition = migrated.getAsJsonObject("root").getAsJsonObject("properties").getAsJsonObject("tileSet").getAsJsonObject("endPosition");
        assertEquals(EngineImageSize.x, endPosition.get("x").getAsInt(), "the region should span the sprite's width");
        assertEquals(EngineImageSize.y, endPosition.get("y").getAsInt(), "the region should span the sprite's height");
    }

    @Test
    public void fillTileSetRegionFromCutSprite() {
        JsonObject migrated = migrate(String.format("""
                {
                  "version": 2,
                  "root": { "properties": { "tileSet": { "tileSetSprite": {
                    "textureCanonicalPath": "%s",
                    "textureCoordinates": [
                      {"x":0.5,"y":1.0}, {"x":0.5,"y":0.5}, {"x":0.0,"y":0.5}, {"x":0.0,"y":1.0}
                    ]
                  } } } }
                }
                """, EnginePath));
        JsonObject endPosition = migrated.getAsJsonObject("root").getAsJsonObject("properties").getAsJsonObject("tileSet").getAsJsonObject("endPosition");
        assertEquals(EngineImageSize.x / 2, endPosition.get("x").getAsInt(), "the region should follow the sprite's cut, not the image's full size");
        assertEquals(EngineImageSize.y / 2, endPosition.get("y").getAsInt());
    }

    @Test
    public void keepEmptyRegionForMissingImage() {
        JsonObject migrated = migrate("""
                {
                  "version": 2,
                  "root": { "properties": { "tileSet": { "tileSetSprite": { "textureCanonicalPath": "assets/does_not_exist.png" } } } }
                }
                """);
        JsonObject endPosition = migrated.getAsJsonObject("root").getAsJsonObject("properties").getAsJsonObject("tileSet").getAsJsonObject("endPosition");
        assertEquals(0, endPosition.get("x").getAsInt(), "an unmeasurable region is left empty, not guessed");
        assertEquals(0, endPosition.get("y").getAsInt());
    }

    @Test
    public void reparentLegacyOrphansToRoot() {
        JsonObject migrated = migrate("""
                {
                  "version": 1,
                  "objects": [
                    { "type": "TheCellBeyond.GameObject2D", "properties": {
                        "uuid": "11111111-1111-1111-1111-111111111111", "name": "Orphan",
                        "childrenUUIDs": [],
                        "components": [ { "type": "components.SpriteRenderer", "properties": {
                            "sprite": { "textureCanonicalPath": "assets/foo.png" } } } ] } }
                  ]
                }
                """);
        assertEquals(3, migrated.get("version").getAsInt(), "a legacy scene should migrate all the way to the current version");
        JsonObject rootProps = migrated.getAsJsonObject("root").getAsJsonObject("properties");
        JsonObject orphan = migrated.getAsJsonArray("objects").get(0).getAsJsonObject().getAsJsonObject("properties");
        assertEquals(orphan.get("uuid").getAsString(), rootProps.getAsJsonArray("childrenUUIDs").get(0).getAsString(), "the root should list the orphan as a child");
        assertEquals(rootProps.get("uuid").getAsString(), orphan.get("parentUUID").getAsString(), "the orphan should point back at the root");
        JsonObject sprite = orphan.getAsJsonArray("components").get(0).getAsJsonObject().getAsJsonObject("properties").getAsJsonObject("sprite");
        assertEquals("assets/foo.png", sprite.getAsJsonObject("textureReference").get("canonicalPath").getAsString(), "a legacy scene's sprites should still be wrapped");
    }

    @Test
    public void keepExistingParent() {
        JsonObject migrated = migrate("""
                {
                  "version": 1,
                  "objects": [
                    { "type": "TheCellBeyond.GameObject2D", "properties": {
                        "uuid": "aaaaaaaa-0000-0000-0000-000000000000", "name": "Parent", "childrenUUIDs": [] } },
                    { "type": "TheCellBeyond.GameObject2D", "properties": {
                        "uuid": "bbbbbbbb-0000-0000-0000-000000000000", "name": "Child",
                        "parentUUID": "aaaaaaaa-0000-0000-0000-000000000000", "childrenUUIDs": [] } }
                  ]
                }
                """);
        JsonObject rootProps = migrated.getAsJsonObject("root").getAsJsonObject("properties");
        JsonObject child = migrated.getAsJsonArray("objects").get(1).getAsJsonObject().getAsJsonObject("properties");
        assertEquals("aaaaaaaa-0000-0000-0000-000000000000", child.get("parentUUID").getAsString(), "an object with a parent should keep it, not be reparented to the root");
        assertEquals(1, rootProps.getAsJsonArray("childrenUUIDs").size(), "only the parentless object should be filed under the root");
    }

    private static JsonObject migrate(String sceneJson) {
        return SceneFileMigrator.migrateToLatest(JsonParser.parseString(sceneJson), "test_scene");
    }

    private static int countReferences(JsonElement element) {
        if (element.isJsonArray()) {
            int total = 0;
            for (JsonElement entry : element.getAsJsonArray()) total += countReferences(entry);
            return total;
        }
        if (!element.isJsonObject()) return 0;
        JsonObject jsonObject = element.getAsJsonObject();
        int total = jsonObject.has("textureReference") ? 1 : 0;
        for (var entry : jsonObject.entrySet()) total += countReferences(entry.getValue());
        return total;
    }
}
