package scene;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.joml.Vector2f;
import org.joml.Vector2i;
import render.texture.ImageHeader;
import render.texture.Sprite;
import render.texture.SpriteRect;
import serialization.EngineSerializer;
import utility.log.EngineLog;

/**
 * ObjectDataMigrator is a collection of static methods used to update and migrate the serialized game object graph stored in scene files and prefab files.
 */
public final class ObjectDataMigrator {
    private static final EngineLog Logger = new EngineLog(ObjectDataMigrator.class);
    private static final EngineSerializer Serializer = EngineSerializer.standard();
    private static final String LegacyTexturePathKey = "textureCanonicalPath";
    private static final String TextureCoordinatesKey = "textureCoordinates";
    private static final String TileSetSpriteKey = "tileSetSprite";
    private static final String EndPositionKey = "endPosition";
    private static final String XKey = "x";
    private static final String YKey = "y";
    private static final String TextureReferenceKey = "textureReference";
    private static final String CanonicalPathKey = "canonicalPath";

    private ObjectDataMigrator() {}

    /**
     * Get the object graph's current version.
     * @return the current save version number
     */
    public static int currentVersion() {
        return SceneFile.SaveVersion;
    }

    /**
     * Bring a serialized object graph up to the current shape.
     * @param objectData the JSON element holding the objects, at any depth
     * @param fromVersion the version the objects were written at
     */
    public static void migrateToLatest(JsonElement objectData, int fromVersion) {
        for (int version = fromVersion + 1; version <= currentVersion(); version++) applyVersion(objectData, version);
    }

    /**
     * Apply the changes to the shape of the data for the given version.
     * @param objectData the JSON element holding the objects
     * @param version the version to apply changes with
     */
    private static void applyVersion(JsonElement objectData, int version) {
        switch (version) {
            case 3 -> {
                wrapTextureReferences(objectData);
                fillTileSetRegions(objectData);
            }
            default -> {}
        }
    }

    /**
     * Apply the implicit full sprite cover region of tile sets.
     * <p>
     * Tile sets are often given the entire image, with the potential that its sprite is already cut, te sprite full cover region is used instead.
     * @param element the JSON element to scan
     */
    private static void fillTileSetRegions(JsonElement element) {
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(ObjectDataMigrator::fillTileSetRegions);
            return;
        }
        if (!element.isJsonObject()) return;
        JsonObject jsonObject = element.getAsJsonObject();
        if (jsonObject.has(TileSetSpriteKey)) writeTileSetRegion(jsonObject);
        jsonObject.entrySet().forEach(entry -> fillTileSetRegions(entry.getValue()));
    }

    /**
     * Write a tile set's covered region from its sprite.
     * <p>
     * If the region cannot be measured, it is assumed that nothing is covered.
     * @param tileSet the JSON object of the tile set to write into
     */
    private static void writeTileSetRegion(JsonObject tileSet) {
        if (tileSet.has(EndPositionKey)) return;
        SpriteRect rect = spriteRect(tileSet.get(TileSetSpriteKey));
        JsonObject endPosition = new JsonObject();
        if (rect == null) {
            Logger.warning("A tile set has an empty region, likely missing image or has no sprite");
            endPosition.addProperty(XKey, 0);
            endPosition.addProperty(YKey, 0);
        } else {
            endPosition.addProperty(XKey, rect.width());
            endPosition.addProperty(YKey, rect.height());
        }
        tileSet.add(EndPositionKey, endPosition);
    }

    /**
     * Read the stored sprite's covered region by measuring the image it pointed to.
     * <p>
     * If the sprite has no texture coordinates, it is assumed to cover the entire image region instead.
     * @param sprite the JSON element of the sprite
     * @return the covered region, or null when the sprite has no readable image
     */
    private static SpriteRect spriteRect(JsonElement sprite) {
        if (sprite == null || !sprite.isJsonObject()) return null;
        JsonObject spriteObject = sprite.getAsJsonObject();
        JsonElement reference = spriteObject.get(TextureReferenceKey);
        if (reference == null || !reference.isJsonObject()) return null;
        JsonElement canonicalPath = reference.getAsJsonObject().get(CanonicalPathKey);
        if (canonicalPath == null || !canonicalPath.isJsonPrimitive()) return null;
        Vector2i size = ImageHeader.size(canonicalPath.getAsString());
        if (size == null) return null;
        JsonElement stored = spriteObject.get(TextureCoordinatesKey);
        if (stored == null || !stored.isJsonArray()) return new SpriteRect(0, 0, size.x, size.y);
        Vector2f[] textureCoordinates = Serializer.deserialize(stored, Vector2f[].class);
        return Sprite.rect(textureCoordinates, size.x, size.y);
    }

    /**
     * Rewrite all the sprite in the tree to wrap their flat texture path into a nested asset reference.
     * <p>
     * The scan is matched by shape to ensure all holders are reached.
     * @param element the JSON element to scan
     */
    private static void wrapTextureReferences(JsonElement element) {
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(ObjectDataMigrator::wrapTextureReferences);
            return;
        }
        if (!element.isJsonObject()) return;
        JsonObject jsonObject = element.getAsJsonObject();
        if (jsonObject.has(LegacyTexturePathKey)) wrapTexturePath(jsonObject);
        jsonObject.entrySet().forEach(entry -> wrapTextureReferences(entry.getValue()));
    }

    /**
     * Replace one sprite's flat texture path with a nested asset reference.
     * A sprite that stored no path keeps no reference, and means it has no texture.
     * @param sprite the JSON object of the sprite to rewrite
     */
    private static void wrapTexturePath(JsonObject sprite) {
        JsonElement canonicalPath = sprite.remove(LegacyTexturePathKey);
        if (!canonicalPath.isJsonPrimitive()) return;
        JsonObject reference = new JsonObject();
        reference.add(CanonicalPathKey, canonicalPath);
        sprite.add(TextureReferenceKey, reference);
    }
}
