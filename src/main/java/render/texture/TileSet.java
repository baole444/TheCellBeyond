package render.texture;

import org.joml.Vector2f;
import org.joml.Vector2i;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import physic2d.PhysicLayer;
import scripting.API;
import utility.AssetManager;
import utility.AssetReference;
import utility.UnifiedPaths;
import utility.log.EngineLog;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TileSet contains the grid size of tile, start position offset and a hash map of computed
 * coordinates for each tile.
 * <p>
 * TileSet itself also contains a Sprite of which hold the texture of this set.<br>
 * Similar to Sprite, TileSet will mark itself dirty on parameter update ot it's sprite is dirty.
 * If the sprite that it is holding is null, TileSet will disable its dirty flag.
 */
@API
public class TileSet {
    private static final EngineLog Logger = new EngineLog(TileSet.class);
    private final Vector2i gridSize = new Vector2i(16);
    private final Vector2i startPosition = new Vector2i();
    private final Vector2i endPosition = new Vector2i();
    private final ConcurrentHashMap<Vector2i, Tile> tiles = new ConcurrentHashMap<>();
    private Sprite tileSetSprite = null;
    private int collisionLayer = PhysicLayer.layerToBit(0);
    private int collisionMask = PhysicLayer.layerToBit(0);
    private volatile transient boolean tileDirty = true;
    private transient boolean requireCompute = true;

    private record TileScan(ByteBuffer pixels, SpriteRect rect, int imageWidth, int imageHeight, int cellCountX, int cellCountY) {}

    /**
     * Create a new empty {@link TileSet}.
     */
    public TileSet() {}

    /**
     * Create a new {@link TileSet} for the given source sprite.
     * @param gridSize the size of one tile, in pixels
     * @param startPosition the top left boundary for the tile set's region on the sprite, in pixels
     * @param tileSetSprite the sprite to use for the tile set
     */
    public TileSet(Vector2i gridSize, Vector2i startPosition, Sprite tileSetSprite) {
        this(gridSize, startPosition, null, tileSetSprite);
    }

    /**
     * Create a new empty {@link TileSet} with custom tile region.
     * @param gridSize the size of one tile, in pixels
     * @param startPosition the top left boundary for the tile set's region on the sprite, in pixels
     * @param endPosition the bottom right boundary for the tile set's region on the sprite, in pixels
     */
    public TileSet(Vector2i gridSize, Vector2i startPosition, Vector2i endPosition) {
        this(gridSize, startPosition, endPosition, null);
    }

    /**
     * Create a new {@link TileSet} for the given source sprite, with custom tile region.
     * @param gridSize the size of one tile, in pixels
     * @param startPosition the top left boundary for the tile set's region on the sprite, in pixels
     * @param endPosition the bottom right boundary for the tile set's region on the sprite, in pixels
     * @param tileSetSprite the sprite to use for the tile set
     */
    public TileSet(Vector2i gridSize, Vector2i startPosition, Vector2i endPosition, Sprite tileSetSprite) {
        if (gridSize != null ) this.gridSize.set(gridSize);
        if (startPosition != null) this.startPosition.set(startPosition);
        if (endPosition != null) this.endPosition.set(endPosition);
        this.tileSetSprite = tileSetSprite;
    }

    /**
     * Get the sprite of this tile set.
     * @return the source {@link Sprite}, or null if there is none
     */
    public Sprite tileSetSprite() {
        return tileSetSprite;
    }

    /**
     * Set the source sprite for this tile set.
     * <p>
     * This will require the set to update tiles again if value changed.
     * @param sprite the sprite to use for the tiles
     */
    public void tileSetSprite(Sprite sprite) {
        if (Objects.equals(tileSetSprite, sprite)) return;
        tileSetSprite = sprite;
        if (sprite != null) {
            requireCompute = true;
            return;
        }
        tiles.clear();
        tileDirty = false;
    }

    /**
     * Add (activate) a tile on the grid of this tile set. Out of bound grid coordinates are skipped.
     * <p>
     * If the calculated tile's texture coordinates are null, no new tile will be added.
     * @param gridPosition the grid coordinate to activate tile on
     */
    public void addTile(Vector2i gridPosition) {
        if (gridPosition == null || gridPosition.x < 0 || gridPosition.y < 0 || tiles.containsKey(gridPosition)) return;
        Vector2f[] coordinates = calculateTileTextureCoordinate(gridPosition);
        if (coordinates == null) {
            Logger.warning(String.format("Tile's coordinate for %s is dead, no tile was added", gridPosition));
            return;
        }
        Vector2i key = new Vector2i(gridPosition);
        Tile tile = new Tile();
        tile.setCoordinate = key;
        tile.textureCoordinates = coordinates;
        tiles.put(key, tile);
        tileDirty = true;
    }

    /**
     * Remove (deactivate) a tile on the grid of this tile set. Out of bound grid coordinates are skipped.
     * @param gridPosition the grid coordinate to deactivate tile on
     */
    public void removeTile(Vector2i gridPosition) {
        if (gridPosition == null || gridPosition.x < 0 || gridPosition.y < 0 || !tiles.containsKey(gridPosition)) return;
        tiles.remove(gridPosition);
        tileDirty = true;
    }

    /**
     * Get the tile set sprite's rendering width.
     * @return the width value, in pixels
     */
    public float width() {
        return tileSetSprite != null ? tileSetSprite.width() : 0.0f;
    }

    /**
     * Get the tile set sprite's rendering height.
     * @return the height value, in pixels
     */
    public float height() {
        return tileSetSprite != null ? tileSetSprite.height() : 0.0f;
    }

    /**
     * Get the size of this tile set's grid spaces.
     * @return the grid space size vector {@code (width, height)}, in pixels
     * @see #requireCompute() Manually request updating tiles again
     */
    public Vector2i gridSize() {
        return gridSize;
    }

    /**
     * Set the new size for grid spaces on this tile set.
     * <p>
     * This will require the set to update tiles again if value changed.
     * @param gridSize the new size vector {@code (width, height)}, in pixels
     */
    public void gridSize(Vector2i gridSize) {
        if (gridSize == null) return;
        gridSize(gridSize.x, gridSize.y);
    }

    /**
     * Set the new size for grid spaces on this tile set.
     * <p>
     * This will require the set to update tiles again if value changed.
     * @param width the new width of grid spaces, in pixels
     * @param height the new height of grid spaces, in pixels
     */
    public void gridSize(int width, int height) {
        if (gridSize().equals(width, height)) return;
        gridSize.set(width, height);
        requireCompute = true;
    }

    /**
     * Get the top left bound position of this tile set's region.
     * @return the bounding point vector, in pixels
     * @see #requireCompute() Manually request updating tiles again
     */
    public Vector2i startPosition() {
        return startPosition;
    }

    /**
     * Set the top left bound for the tile set's region. Any tile that have its region ahead this point won't be included.
     * <p>
     * This will require the set to update tiles again if value changed.
     * @param startPosition the new bounding point vector, in pixels
     * @apiNote {@code (0, 0)} is the top left corner of the tile set sprite's rect
     */
    public void startPosition(Vector2i startPosition) {
        if (startPosition == null) return;
        startPosition(startPosition.x, startPosition.y);
    }

    /**
     * Set the top left bound for the tile set's region. Any tile that have its region ahead this point won't be included.
     * <p>
     * This will require the set to update tiles again if value changed.
     * @param x the bound on the x-axis, in pixels
     * @param y the bound on the y-axis, in pixels
     * @apiNote {@code (0, 0)} is the top left corner of the tile set sprite's rect
     */
    public void startPosition(int x, int y) {
        if (startPosition.equals(x, y)) return;
        startPosition.set(x, y);
        requireCompute = true;
    }

    /**
     * Get the bottom right bound position of this tile set's region.
     * @return the bounding point vector, in pixels
     * @see #requireCompute() Manually request updating tiles again
     */
    public Vector2i endPosition() {
        return endPosition;
    }

    /**
     * Set the bottom right bound for the tile set's region. Any tile that have its region pass this point won't be included.
     * <p>
     * This will require the set to update tiles again if value changed.
     * @param endPosition the new bounding point vector, in pixels
     * @apiNote {@code (0, 0)} will default to the bottom right corner of the tile set sprite's rect
     */
    public void endPosition(Vector2i endPosition) {
        if (endPosition == null) return;
        endPosition(endPosition.x, endPosition.y);
    }

    /**
     * Set the bottom right bound for the tile set's region. Any tile that have its region pass this point won't be included.
     * <p>
     * This will require the set to update tiles again if value changed.
     * @param x the bound on the x-axis, in pixels
     * @param y the bound on the y-axis, in pixels
     * @apiNote {@code (0, 0)} will default to the bottom right corner of the tile set sprite's rect
     */
    public void endPosition(int x, int y) {
        if (endPosition.equals(x, y)) return;
        endPosition.set(x, y);
        requireCompute = true;
    }

    /**
     * Get the ID of this tile set sprite's texture on the GPU.
     * @return the texture's ID, or -1 when there is no texture, or it is not loaded on the GPU yet
     */
    public int textureID() {
        if (tileSetSprite == null) return -1;
        return tileSetSprite.textureID();
    }

    /**
     * Get the tile at the given grid space in this tile set.
     * @param gridPosition the grid coordinate to get tile on
     * @return the tile, or null when there is no tile or the grid coordinate is null or out of bound
     */
    public Tile tile(Vector2i gridPosition) {
        if (gridPosition == null || gridPosition.x < 0 || gridPosition.y < 0 || !tiles.containsKey(gridPosition)) return null;
        computeTiles();
        return tiles.get(gridPosition);
    }

    /**
     * Get the grid coordinates of activated tiles from this tile set.
     * @return a view of the tile coordinates set
     */
    public HashSet<Vector2i> tileCoordinates() {
        if (tiles.isEmpty()) return new HashSet<>();
        return new HashSet<>(tiles.keySet());
    }

    /**
     * Get the amount of activated tiles from this tile set.
     * @return the number of tiles
     */
    public int tileCount() {
        return tiles.size();
    }

    /**
     * Get the list of activated tiles from this tile set.
     * @return a view of the tiles list
     */
    public List<Tile> tiles() {
        if (tiles.isEmpty()) return List.of();
        computeTiles();
        return tiles.values().stream().toList();
    }

    /**
     * Remove the dirty flag for this tile set sprite as the Renderer already processed its latest update.
     * <p>
     * Holders are responsible for updating this flag via their own internal tracking.
     */
    public void markClean() {
        if (tileSetSprite != null) tileSetSprite.markClean();
        tileDirty = false;
    }

    /**
     * Check to see if the tile set sprite need to be processed by Renderer again.
     * <p>
     * Holders are responsible for updating this flag via their own internal tracking.
     * @return true if rendering data of this sprite was updated.
     */
    public boolean dirtied() {
        if (tileSetSprite == null) return false;
        computeTiles();
        return tileSetSprite.dirtied() || tileDirty;
    }

    /**
     * Manually mark this tile set and request the tiles to be updated again on next access.
     * <p>
     * This can be used for updating the live parameter objects directly then request an update after.
     */
    public void requireCompute() {
        requireCompute = true;
    }

    public int collisionLayer() {
        return collisionLayer;
    }

    public void addCollisionLayer(int layerIndex) {
        collisionLayer = PhysicLayer.addLayerToMask(collisionLayer, layerIndex);
    }

    public void removeCollisionLayer(int layerIndex) {
        collisionLayer = PhysicLayer.removeLayerFromMask(collisionLayer, layerIndex);
    }

    public int collisionMask() {
        return collisionMask;
    }

    public void addCollisionMask(int layerIndex) {
        collisionMask = PhysicLayer.addLayerToMask(collisionMask, layerIndex);
    }

    public void removeCollisionMask(int layerIndex) {
        collisionMask = PhysicLayer.removeLayerFromMask(collisionMask, layerIndex);
    }

    public void collisionMask(int newMasks) {
        if (!PhysicLayer.validMask(newMasks)) return;
        collisionMask = newMasks;
    }

    public void collisionLayer(int newMasks) {
        if (!PhysicLayer.validMask(newMasks)) return;
        collisionLayer = newMasks;
    }

    /**
     * Reset this tile set's region and remove (deactivate) all its tiles.
     */
    public void resetDefault() {
        startPosition.zero();
        endPosition.zero();
        gridSize.set(16);
        tiles.clear();
        tileSetSprite = null;
        collisionLayer = PhysicLayer.layerToBit(0);
        collisionMask = PhysicLayer.layerToBit(0);
        tileDirty = false;
    }

    /**
     * Find and activate all possible tiles within this tile set's region.
     */
    public void findTiles() {
        if (gridSize.x <= 0 || gridSize.y <= 0) {
            Logger.warning(String.format("Cannot find tiles: grid size %s is not positive", gridSize));
            return;
        }
        if (tileSetSprite == null || tileSetSprite.textureReference() == null || tileSetSprite.rect() == null) {
            Logger.warning("Cannot find tiles: set has no readable image");
            return;
        }
        SpriteRect rect = tileSetSprite.rect();
        int regionRight = endPosition.x > 0 ? Math.min(endPosition.x, rect.width()) : rect.width();
        int regionBottom = endPosition.y > 0 ? Math.min(endPosition.y, rect.height()) : rect.height();
        if (startPosition.x >= regionRight || startPosition.y >= regionBottom) {
            Logger.debug(String.format("No tile found: region from %s to (%d, %d) is empty", startPosition, regionRight, regionBottom));
            return;
        }
        AssetReference reference = tileSetSprite.textureReference();
        try (InputStream stream = UnifiedPaths.getAssetStream(reference.resolvedPath())) {
            byte[] data = stream.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(data.length);
            buffer.put(data);
            buffer.flip();
            ByteBuffer pixels;
            int imageWidth;
            int imageHeight;
            final int channels = 4;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
                IntBuffer c = stack.mallocInt(1);
                STBImage.stbi_set_flip_vertically_on_load(false);
                pixels = STBImage.stbi_load_from_memory(buffer, w, h, c, channels);
                if (pixels == null) {
                    Logger.error(String.format("Failed to read image pixels: %s", STBImage.stbi_failure_reason()));
                    return;
                }
                imageWidth = w.get(0);
                imageHeight = h.get(0);
            }
            try {
                int cellCountX = (regionRight - startPosition.x) / gridSize.x;
                int cellCountY = (regionBottom - startPosition.y) / gridSize.y;
                gridTileSearch(new TileScan(pixels, rect, imageWidth, imageHeight, cellCountX, cellCountY));
            } finally {
                STBImage.stbi_image_free(pixels);
            }
        } catch (IOException e) {
            Logger.error(String.format("Failed to find tiles: %s", e.getMessage()));
        }
    }

    private void computeTiles() {
        if (!requireCompute) return;
        requireCompute = false;
        updateTiles();
        tileDirty = true;
    }

    private void updateTiles() {
        if (tileSetSprite == null) return;
        HashMap<Vector2i, Tile> updates = new HashMap<>(tiles);
        for (Map.Entry<Vector2i, Tile> entry : updates.entrySet()) {
            Vector2i position = entry.getKey();
            Tile tile = entry.getValue();
            Vector2f[] newCoordinates = calculateTileTextureCoordinate(position);
            if (newCoordinates == null) newCoordinates = deadTileCoordinates();
            tile.textureCoordinates = newCoordinates;
        }
    }

    private Vector2f[] calculateTileTextureCoordinate(Vector2i gridPosition) {
        if (tileSetSprite == null) return null;
        if (gridPosition.x < 0 || gridPosition.y < 0 || gridSize.x <= 0 || gridSize.y <= 0) return null;
        SpriteRect rect = tileSetSprite.rect();
        Vector2i textureSize = AssetManager.textureSize(tileSetSprite.textureRID());
        if (rect == null || textureSize == null || tileOutsideSprite(gridPosition, rect)) return null;
        int x = rect.minX() + startPosition.x + gridPosition.x * gridSize.x;
        int y = rect.minY() + startPosition.y + gridPosition.y * gridSize.y;
        float textureW = textureSize.x;
        float textureH = textureSize.y;
        int instY = (int) (textureH - y - gridSize.y);
        float leftX = x / textureW;
        float rightX = (x + gridSize.x) / textureW;
        float bottomY = instY / textureH;
        float topY = (instY + gridSize.y) / textureH;
        return new Vector2f[] {
                new Vector2f(rightX, topY),
                new Vector2f(rightX, bottomY),
                new Vector2f(leftX, bottomY),
                new Vector2f(leftX, topY)
        };
    }

    private boolean tileOutsideSprite(Vector2i gridPosition, SpriteRect rect) {
        int x = rect.minX() + startPosition.x + gridPosition.x * gridSize.x;
        int y = rect.minY() + startPosition.y + gridPosition.y * gridSize.y;
        return x < rect.minX() || y < rect.minY() || x + gridSize.x > rect.maxX() || y + gridSize.y > rect.maxY();
    }

    private static Vector2f[] deadTileCoordinates() {
        return new Vector2f[] {
                new Vector2f(0.0f),
                new Vector2f(0.0f),
                new Vector2f(0.0f),
                new Vector2f(0.0f)
        };
    }

    private void gridTileSearch(TileScan scan) {
        Set<Vector2i> searched = new HashSet<>();
        List<Vector2i> hitTiles = new ArrayList<>();
        Vector2i startPos = new Vector2i(1);
        if (startPos.x >= scan.cellCountX || startPos.y >= scan.cellCountY) startPos.zero();
        neighboringSearch(startPos, scan, searched, hitTiles);
        while (!hitTiles.isEmpty()) {
            List<Vector2i> neighborHitTiles = new ArrayList<>();
            for (Vector2i tile : hitTiles) neighboringSearch(tile, scan, searched, neighborHitTiles);
            hitTiles = neighborHitTiles;
        }
        mindlessSearch(startPos, scan, searched);
    }

    private void neighboringSearch(Vector2i centre, TileScan scan, Set<Vector2i> searched, List<Vector2i> hitTiles) {
        for (int y = -1; y <= 1; y++) {
           for (int x = -1; x <= 1; x++) {
               Vector2i coordinate = new Vector2i(centre).add(x, y);
               if (coordinate.x < 0 || coordinate.x >= scan.cellCountX || coordinate.y < 0 || coordinate.y >= scan.cellCountY) continue;
               if (searched.contains(coordinate)) continue;
               searched.add(new Vector2i(coordinate));
               if (tiles.containsKey(coordinate) || tileOutsideSprite(coordinate, scan.rect)) continue;
               if (!scanTilePixels(coordinate, scan)) continue;
               addTile(new Vector2i(coordinate));
               hitTiles.add(new Vector2i(coordinate));
           }
        }
    }

    private void mindlessSearch(Vector2i startPosition, TileScan scan, Set<Vector2i> searched) {
        Vector2i coordinate = new Vector2i(startPosition);
        while (coordinate.y < scan.cellCountY) {
            List<Vector2i> hitTiles = new ArrayList<>();
            neighboringSearch(coordinate, scan, searched, hitTiles);
            while (!hitTiles.isEmpty()) {
                List<Vector2i> newHitTiles = new ArrayList<>();
                for (Vector2i grid : hitTiles) neighboringSearch(grid, scan, searched, newHitTiles);
                hitTiles = newHitTiles;
            }
            coordinate.x += 2;
            if (coordinate.x < scan.cellCountX) continue;
            coordinate.x = startPosition.x;
            coordinate.y += 2;
        }
    }

    private boolean scanTilePixels(Vector2i coordinate, TileScan scan) {
        SpriteRect rect = scan.rect;
        int pixelX = rect.minX() + startPosition.x + coordinate.x * gridSize.x;
        int pixelY = rect.minY() + startPosition.y + coordinate.y * gridSize.y;
        Vector2i start = new Vector2i(Math.max(pixelX, rect.minX()), Math.max(pixelY, rect.minY()));
        Vector2i end = new Vector2i(
                Math.min(pixelX + gridSize.x, Math.min(rect.maxX(), scan.imageWidth)),
                Math.min(pixelY + gridSize.y, Math.min(rect.maxY(), scan.imageHeight))
        );
        if (start.x >= end.x || start.y >= end.y) return false;
        ByteBuffer pixels = scan.pixels;
        int stride = scan.imageWidth;
        int width = gridSize.x;
        int height = gridSize.y;
        if (width <= 2 || height <= 2) {
            for (int y = start.y; y < end.y; y++) {
                for (int x = start.x; x < end.x; x++) if (checkPixelAlpha(pixels, stride, x, y)) return true;
            }
            return false;
        }
        int outerRing = 0;
        int innerRing = Math.min(width, height) / 2;
        while (outerRing < innerRing) {
            if (scanRingPixels(pixels, stride, start, end, outerRing)) return true;
            if (scanRingPixels(pixels, stride, start, end, innerRing)) return true;
            outerRing++;
            innerRing--;
        }
        if (outerRing == innerRing) return scanRingPixels(pixels, stride, start, end, outerRing);
        return false;
    }

    private boolean checkPixelAlpha(ByteBuffer pixels, int w, int x, int y) {
        final int colorChannels = 4;
        final int alphaOffset = 3;
        int index = (y * w + x) * colorChannels;
        int alphaIndex = index + alphaOffset;
        int alpha = pixels.get(alphaIndex) & 0xFF;
        return alpha != 0;
    }

    private boolean scanRingPixels(ByteBuffer pixels, int stride, Vector2i start, Vector2i end, int ring) {
        int left = start.x + ring;
        int right = end.x - 1 - ring;
        int top = start.y + ring;
        int bottom = end.y - 1 - ring;
        if (left > right || top > bottom) return false;
        for (int x = left; x <= right; x++) {
            if (checkPixelAlpha(pixels, stride, x, top)) return true;
        }
        if (bottom != top) {
            for (int x = left; x <= right; x++) {
                if (checkPixelAlpha(pixels, stride, x, bottom)) return true;
            }
        }
        for (int y = top + 1; y < bottom; y++) {
            if (checkPixelAlpha(pixels, stride, left, y)) return true;
        }
        if (right != left) {
            for (int y = top + 1; y < bottom; y++) {
                if (checkPixelAlpha(pixels, stride, right, y)) return true;
            }
        }
        return false;
    }
}
