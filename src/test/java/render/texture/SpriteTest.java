package render.texture;

import TheCellBeyond.internal.ResourceID;
import org.junit.jupiter.api.Test;
import utility.AssetManager;
import utility.Settings;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class SpriteTest {
    private static final String Path = Settings.TexturePath.EditorControls;

    @Test
    public void resolvingAnEvictedTextureReloadsItUnderAFreshId() {
        Sprite sprite = new Sprite();
        sprite.setTexture(Path);
        ResourceID first = sprite.textureRID();
        assertNotNull(first, "a sprite with a texture should resolve to a RID");
        AssetManager.unloadTexture(Path);
        assertNull(AssetManager.getTextureRID(Path), "the texture should be evicted after unload");
        ResourceID healed = sprite.textureRID();
        assertNotNull(healed, "resolving after eviction should reload, not strand the sprite");
        assertNotEquals(first, healed, "the reload should carry a fresh id, the old one is retired");
        assertEquals(healed, AssetManager.getTextureRID(Path), "the healed RID should be the one now cached");
    }

    @Test
    public void aSpriteWithNoTextureResolvesToNull() {
        assertNull(new Sprite().textureRID(), "a sprite that claims no texture resolves to null, not a placeholder");
    }
}
