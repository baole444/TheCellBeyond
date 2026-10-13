package render.texture;

import org.junit.jupiter.api.Test;
import utility.Settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

final class SingleSpriteTest {
    private static final String Path = Settings.TexturePath.EditorControls;

    @Test
    public void keepHandedOutSpriteOnUpdate() {
        SingleSprite source = new SingleSprite(Path, 32, 32);
        Sprite taken = source.sprite();
        assertEquals(32.0f, taken.width());
        source.texture(Path, 64, 64);
        assertEquals(32.0f, taken.width(), "a sprite already handed out should keep the size it was given");
        assertEquals(64.0f, source.sprite().width(), "the source should produce the new size for later callers");
        assertNotSame(taken, source.sprite(), "an update should produce a new sprite, not edit the old one");
    }
}
