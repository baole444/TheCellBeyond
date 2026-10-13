package render.texture;

import org.joml.Vector2f;
import org.joml.Vector2i;
import org.junit.jupiter.api.Test;
import utility.AssetManager;
import utility.Settings;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNull;

final class SpriteSheetTest {
    private static final String Path = Settings.TexturePath.EditorControls;
    private static final int SpriteSize = 16;
    private static final int SpriteCount = 4;
    private static final float Epsilon = 0.0001f;

    @Test
    public void cutBeforeUpload() {
        AssetManager.unloadTexture(Path);
        SpriteSheet sheet = new SpriteSheet(Path, SpriteSize, SpriteSize, SpriteCount);
        assertEquals(SpriteCount, sheet.availableSpriteCount(), "the sheet should cut before the upload completes");
        Sprite first = sheet.spriteIndex(0);
        assertNotNull(first, "a valid index must never return null while the texture loads");
        assertEquals(SpriteSize, first.width());
        assertEquals(SpriteSize, first.height());
    }

    @Test
    public void rejectOutOfBoundIndex() {
        SpriteSheet sheet = new SpriteSheet(Path, SpriteSize, SpriteSize, SpriteCount);
        assertNull(sheet.spriteIndex(SpriteCount), "an index past the end names no sprite");
        assertNull(sheet.spriteIndex(-1));
        assertNotNull(sheet.spriteIndex(SpriteCount - 1), "the last valid index still resolves");
    }

    @Test
    public void cutPackedSheets() {
        Vector2i image = ImageHeader.size(Path);
        assertNotNull(image, "the test needs a readable engine image");
        SpriteSheet upper = new SpriteSheet(Path, SpriteSize, SpriteSize, 4);
        upper.endPosition(image.x, SpriteSize);
        SpriteSheet lower = new SpriteSheet(Path, new Vector2i(SpriteSize), 4, null, new Vector2i(0, SpriteSize));
        lower.endPosition(image.x, SpriteSize * 2);
        assertEquals(upper.textureRID(), lower.textureRID(), "two sheets of one image share its texture");
        float upperTop = upper.spriteIndex(0).textureCoordinates()[0].y;
        float lowerTop = lower.spriteIndex(0).textureCoordinates()[0].y;
        assertEquals(1.0f, upperTop, Epsilon, "the upper band starts at the image's top edge");
        assertEquals((image.y - SpriteSize) / (float) image.y, lowerTop, Epsilon, "the lower band starts one sprite down, not at the top");
    }

    @Test
    public void clampNegativeStart() {
        SpriteSheet sheet = new SpriteSheet(Path, new Vector2i(SpriteSize), 1, null, new Vector2i(-SpriteSize / 2));
        Vector2f[] uv = sheet.spriteIndex(0).textureCoordinates();
        assertEquals(0.0f, uv[2].x, Epsilon, "a start before the image cuts from its left edge");
        assertEquals(1.0f, uv[0].y, Epsilon, "a start above the image cuts from its top edge");
    }

    @Test
    public void rejectOversizedSprite() {
        Vector2i image = ImageHeader.size(Path);
        assertNotNull(image);
        assertEquals(0, new SpriteSheet(Path, image.x + 1, SpriteSize, 1).availableSpriteCount(), "a sprite wider than the image is not cut");
    }

    @Test
    public void stopCutAtRegionEnd() {
        Vector2i image = ImageHeader.size(Path);
        assertNotNull(image);
        int perRow = image.x / SpriteSize;
        SpriteSheet bounded = new SpriteSheet(Path, SpriteSize, SpriteSize, perRow * 3);
        bounded.endPosition(image.x, SpriteSize);
        assertEquals(perRow, bounded.availableSpriteCount(), "the cut should stop at the region's bottom edge, not run on into the rest of the image");
    }
}
