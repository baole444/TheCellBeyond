package render.texture;

import org.joml.Vector2f;
import org.joml.Vector2i;
import org.junit.jupiter.api.Test;
import utility.Settings;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

final class TileSetTest {
    private static final String Path = Settings.TexturePath.EditorControls;
    private static final int Grid = 28;
    private static final float Epsilon = 0.0001f;
    private static final int RightTop = 0;
    private static final int RightBottom = 1;
    private static final int LeftBottom = 2;

    @Test
    public void placeTileOnWholeImage() {
        Vector2i image = ImageHeader.size(Path);
        assertNotNull(image, "the test needs a readable engine image");
        Vector2f[] uv = firstTileOf(new Sprite(Path));
        assertEquals(0.0f, uv[LeftBottom].x, Epsilon, "the first column begins at the image's left edge");
        assertEquals(Grid / (float) image.x, uv[RightTop].x, Epsilon, "the cell is one grid wide");
        assertEquals(1.0f, uv[RightTop].y, Epsilon, "the first tile row is the top of the image");
        assertEquals((image.y - Grid) / (float) image.y, uv[RightBottom].y, Epsilon, "the cell is one grid tall");
    }

    @Test
    public void placeTileOnCutSprite() {
        Vector2i image = ImageHeader.size(Path);
        assertNotNull(image);
        Sprite cut = new Sprite(Path);
        cut.textureCoordinates(new Vector2f[] {
                new Vector2f(1.0f, 0.5f),
                new Vector2f(1.0f, 0.0f),
                new Vector2f(0.5f, 0.0f),
                new Vector2f(0.5f, 0.5f)
        });
        Vector2f[] uv = firstTileOf(cut);
        assertEquals(0.5f, uv[LeftBottom].x, Epsilon, "the first column begins at the sprite's left edge, not the image's");
        assertEquals(0.5f + Grid / (float) image.x, uv[RightTop].x, Epsilon, "the cell is one grid wide from there");
        assertEquals(0.5f, uv[RightTop].y, Epsilon, "the first tile row is the sprite's top, not the image's");
        assertEquals(0.5f - Grid / (float) image.y, uv[RightBottom].y, Epsilon);
    }

    @Test
    public void findTilesToSpriteEdge() {
        Vector2i image = ImageHeader.size(Path);
        assertNotNull(image);
        Set<Vector2i> toEdge = findTiles(new Vector2i(), new Vector2i());
        assertFalse(toEdge.isEmpty(), "the test needs an image with opaque cells");
        assertEquals(findTiles(new Vector2i(), image), toEdge, "an end at 0 should reach the sprite's edge");
    }

    @Test
    public void findTilesInRegion() {
        Set<Vector2i> firstRow = findTiles(new Vector2i(), new Vector2i()).stream().filter(c -> c.y == 0).collect(Collectors.toSet());
        assertFalse(firstRow.isEmpty());
        assertEquals(firstRow, findTiles(new Vector2i(), new Vector2i(0, Grid)), "a region one grid tall should only find the first row, across the full width");
    }

    @Test
    public void skipOverhangingCells() {
        Set<Vector2i> found = assertDoesNotThrow(() -> findTiles(new Vector2i(-Grid / 2, 0), new Vector2i()));
        assertFalse(found.isEmpty());
        assertTrue(found.stream().allMatch(c -> c.x > 0), "the first column hangs off the sprite's left edge, so it holds no tiles");
    }

    @Test
    public void rejectCellOffSprite() {
        Vector2i image = ImageHeader.size(Path);
        assertNotNull(image);
        TileSet tileSet = new TileSet(new Vector2i(Grid), new Vector2i(Grid / 2, 0), new Sprite(Path));
        tileSet.addTile(new Vector2i((image.x - Grid / 2) / Grid, 0));
        assertEquals(0, tileSet.tileCount(), "a cell overhanging the sprite's right edge is refused");
    }

    @Test
    public void rejectZeroGrid() {
        TileSet tileSet = new TileSet(new Vector2i(0), new Vector2i(), new Sprite(Path));
        assertDoesNotThrow(tileSet::findTiles);
        tileSet.addTile(new Vector2i());
        assertEquals(0, tileSet.tileCount(), "a zero grid holds no tiles");
    }

    @Test
    public void recomputeAfterLiveEdit() {
        Vector2i image = ImageHeader.size(Path);
        assertNotNull(image);
        TileSet tileSet = new TileSet(new Vector2i(Grid), new Vector2i(), new Sprite(Path));
        Vector2i origin = new Vector2i();
        tileSet.addTile(origin);
        tileSet.gridSize().set(Grid * 2);
        tileSet.requireCompute();
        assertEquals(Grid * 2 / (float) image.x, tileSet.tile(origin).textureCoordinates[RightTop].x, Epsilon, "a requested recompute should pick up the edited grid");
    }

    @Test
    public void copyGridPosition() {
        TileSet tileSet = new TileSet(new Vector2i(Grid), new Vector2i(), new Sprite(Path));
        Vector2i position = new Vector2i(1, 1);
        tileSet.addTile(position);
        position.set(2, 2);
        Tile tile = tileSet.tile(new Vector2i(1, 1));
        assertNotNull(tile, "reusing the caller's vector should not move the tile");
        assertEquals(new Vector2i(1, 1), tile.setCoordinate);
    }

    private static Vector2f[] firstTileOf(Sprite sprite) {
        TileSet tileSet = new TileSet(new Vector2i(Grid), new Vector2i(), sprite);
        Vector2i origin = new Vector2i(0, 0);
        tileSet.addTile(origin);
        Tile tile = tileSet.tile(origin);
        assertNotNull(tile, "the tile at the region's origin should be addable");
        return tile.textureCoordinates;
    }

    private static HashSet<Vector2i> findTiles(Vector2i start, Vector2i end) {
        TileSet tileSet = new TileSet(new Vector2i(Grid), start, end, new Sprite(Path));
        tileSet.findTiles();
        return tileSet.tileCoordinates();
    }
}
