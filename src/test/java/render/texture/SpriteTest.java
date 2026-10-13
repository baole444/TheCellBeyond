package render.texture;

import TheCellBeyond.internal.ResourceID;
import org.junit.jupiter.api.Test;
import serialization.EngineSerializer;
import utility.AssetManager;
import utility.AssetReference;
import utility.Settings;
import utility.UnifiedPaths;

import static org.junit.jupiter.api.Assertions.*;

final class SpriteTest {
    private static final String Path = Settings.TexturePath.EditorControls;

    @Test
    public void reloadEvictedTexture() {
        Sprite sprite = new Sprite();
        sprite.texture(Path);
        ResourceID first = sprite.textureRID();
        assertNotNull(first, "a sprite with a texture should resolve to a RID");
        AssetManager.unloadTexture(Path);
        assertNull(cachedRID(), "the texture should be evicted after unload");
        ResourceID healed = sprite.textureRID();
        assertNotNull(healed, "resolving after eviction should reload, not strand the sprite");
        assertNotEquals(first, healed, "the reload should carry a fresh id, the old one is retired");
        assertEquals(healed, cachedRID(), "the healed RID should be the one now cached");
    }

    @Test
    public void noTextureResolvesNull() {
        assertNull(new Sprite().textureRID(), "a sprite that claims no texture resolves to null, not a placeholder");
    }

    @Test
    public void sizeToImage() {
        Sprite sprite = new Sprite(Path);
        assertTrue(sprite.hasTexture(), "a sprite built from a path should hold that path's texture");
        assertNotNull(sprite.textureRID());
        assertTrue(sprite.width() > 0 && sprite.height() > 0, "the image's dimensions come from its file header, so they are set without waiting on the upload");
    }

    @Test
    public void resolveFromScene() {
        String canonicalPath = new AssetReference(Path).canonicalPath();
        Sprite sprite = EngineSerializer.standard().deserialize("{\"textureReference\":{\"canonicalPath\":\"" + canonicalPath + "\"}}", Sprite.class);
        assertTrue(sprite.hasTexture(), "the stored reference should survive the read");
        assertEquals(canonicalPath, sprite.textureReference().canonicalPath());
        assertEquals(AssetManager.loadTexture(Path), sprite.textureRID(), "a sprite read from a scene should resolve to the same texture a loaded one does");
    }

    private static ResourceID cachedRID() {
        return TextureManager.get().textureRID(UnifiedPaths.stripMetadata(UnifiedPaths.toCanonicalPath(Path)));
    }
}
