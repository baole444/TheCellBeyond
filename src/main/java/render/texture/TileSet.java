package render.texture;

import org.joml.Vector2f;
import org.joml.Vector2i;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import render.Texture;
import utility.AssetReference;
import utility.PathResolver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TileSet contains the grid size of tile, start position offset and a hash map of computed
 * coordinates for each tile.<br>
 * TileSet itself also contains a Sprite of which hold the texture of this set.<br>
 * Similar to Sprite, TileSet will mark itself dirty on parameter update ot it's sprite is dirty.
 * If the sprite that it is holding is null, TileSet will disable its dirty flag.
 */
public class TileSet {
    private final Vector2i gridSize;
    private final Vector2i startPosition;
    private Sprite tileSetSprite;
    private final ConcurrentHashMap<Vector2i, Tile> tiles = new ConcurrentHashMap<>();

    private volatile transient boolean tileDirty = true;

    public TileSet(Vector2i gridSize, Vector2i startPosition, Sprite tileSetSprite) {
        this.gridSize = new Vector2i(gridSize);
        this.startPosition = new Vector2i(startPosition);
        this.tileSetSprite = tileSetSprite;
    }

    public void setTileSetSprite(Sprite sprite) {
        if (Objects.equals(tileSetSprite, sprite)) return;
        tileSetSprite = sprite;

        if (sprite == null) {
            tiles.clear();
            tileDirty = false;
            return;
        }

        HashMap<Vector2i, Tile> updates = new HashMap<>(tiles);
        for (Map.Entry<Vector2i, Tile> entry : updates.entrySet()) {
            Vector2i position = entry.getKey();
            Tile tile = entry.getValue();
            Vector2f[] newCoordinates = calculateTileTextureCoordinate(position);
            if (newCoordinates == null) newCoordinates = deadTileCoordinates();
            tile.textureCoordinates = newCoordinates;
        }

        tileDirty = true;
    }

    public void addTile(Vector2i gridPosition) {
        if (gridPosition == null || gridPosition.x < 0 || gridPosition.y < 0 || tiles.containsKey(gridPosition)) return;

        Vector2f[] coordinates = calculateTileTextureCoordinate(gridPosition);
        if (coordinates == null) {
            System.err.println("Tile's coordinate for (" + gridPosition.x + "," + gridPosition.y + ") is dead, no tile was added.");
            return;
        }

        Tile tile = new Tile();
        tile.setCoordinate = gridPosition;
        tile.textureCoordinates = coordinates;

        tiles.put(gridPosition, tile);
        tileDirty = true;
    }

    public void removeTile(Vector2i gridPosition) {
        if (gridPosition == null || gridPosition.x < 0 || gridPosition.y < 0 || !tiles.containsKey(gridPosition)) return;

        tiles.remove(gridPosition);

        tileDirty = true;
    }

    public float getWidth() {
        return tileSetSprite != null ? tileSetSprite.getWidth() : 0.0f;
    }

    public float getHeight() {
        return tileSetSprite != null ? tileSetSprite.getHeight() : 0.0f;
    }

    public Vector2i getGridSize() {
        return new Vector2i(gridSize);
    }

    public Texture getTexture() {
        if (tileSetSprite == null) return null;

        return tileSetSprite.getTexture();
    }

    public int getTextureID() {
        if (tileSetSprite == null) return -1;

        return tileSetSprite.getTextureID();
    }

    public Tile getTile(Vector2i gridPosition) {
        if (gridPosition == null || gridPosition.x < 0 || gridPosition.y < 0 || !tiles.containsKey(gridPosition)) return null;

        return tiles.get(gridPosition);
    }

    public List<Tile> getTiles() {
        if (tiles.isEmpty()) return List.of();

        return tiles.values().stream().toList();
    }

    public void rendererUpdated() {
        if (tileSetSprite != null) tileSetSprite.rendererUpdated();

        tileDirty = false;
    }

    public boolean requestRendererUpdate() {
        if (tileSetSprite == null) return false;

        return tileSetSprite.requestRendererUpdate() || tileDirty;
    }

    private Vector2f[] calculateTileTextureCoordinate(Vector2i gridPosition) {
        if (tileSetSprite == null) return null;
        if (gridPosition.x < 0 || gridPosition.y < 0 || gridSize.x < 0 || gridSize.y < 0) return null;
        if (tileSetSprite.getWidth() <= 0.0f || tileSetSprite.getHeight() <= 0.0f) return null;

        int x = startPosition.x + (gridPosition.x * gridSize.x);
        int y = startPosition.y + (gridPosition.y * gridSize.y);

        float textureW = tileSetSprite.getWidth();
        float textureH = tileSetSprite.getHeight();

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

    private static Vector2f[] deadTileCoordinates() {
        return new Vector2f[] {
                new Vector2f(0.0f),
                new Vector2f(0.0f),
                new Vector2f(0.0f),
                new Vector2f(0.0f)
        };
    }

    public void findTiles() {
        if (tileSetSprite == null) return;

        Texture texture = tileSetSprite.getTexture();
        if (texture == null || texture.getCanonicalPath() == null) return;

        String canonicalPath = texture.getCanonicalPath();
        AssetReference assetReference = new AssetReference(canonicalPath);
        PathResolver resolver = PathResolver.get();

        try (InputStream stream = resolver.getAssetStream(assetReference.getResolvedPath())) {
            byte[] data = stream.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(data.length);
            buffer.put(data);
            buffer.flip();

            findTilesFromBuffer(buffer);
        } catch (IOException e) {
            System.err.println("Failed to find tiles: " + e.getMessage());
        }
    }

    private void findTilesFromBuffer(ByteBuffer buffer) {
        ByteBuffer pixels;
        int width = (int) tileSetSprite.getWidth();
        int height = (int) tileSetSprite.getHeight();
        final int channels = 4;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer c = stack.mallocInt(1);
            pixels = STBImage.stbi_load_from_memory(buffer, w, h, c, channels);

            if (pixels == null) {
                System.err.println("Failed to read image pixels: " + STBImage.stbi_failure_reason());
                return;
            }
        }

        try {
            int cellCountX = (width - startPosition.x) / gridSize.x;
            int cellCountY = (height - startPosition.y) / gridSize.y;

            gridTileSearch(pixels, width, height, cellCountX, cellCountY);
        } finally {
            STBImage.stbi_image_free(pixels);
        }
    }

    private void gridTileSearch(ByteBuffer pixels, int width, int height, int cellCountX, int cellCountY) {
        Set<Vector2i> searched = new HashSet<>();
        List<Vector2i> hitTiles = new ArrayList<>();

        Vector2i startPos = new Vector2i(1);
        if (startPos.x >= cellCountX || startPos.y >= cellCountY) startPos.set(0);

        neighboringSearch(startPos, pixels, width, height, cellCountX, cellCountY, searched, hitTiles);

        while (!hitTiles.isEmpty()) {
            List<Vector2i> neighborHitTiles = new ArrayList<>();
            for (Vector2i tile : hitTiles) neighboringSearch(tile, pixels, width, height, cellCountX, cellCountY, searched, neighborHitTiles);
            hitTiles = neighborHitTiles;
        }

        mindlessSearch(startPos, pixels, width, height, cellCountX, cellCountY, searched, hitTiles);
    }

    private void neighboringSearch(Vector2i centre, ByteBuffer pixels, int w, int h, int countX, int countY, Set<Vector2i> searched, List<Vector2i> hitTiles) {
        for (int y = -1; y <= 1; y++) {
           for (int x = -1; x <= 1; x++) {
               Vector2i coordinate = new Vector2i(centre).add(x, y);
               if (coordinate.x < 0 || coordinate.x >= countX || coordinate.y < 0 || coordinate.y >= countY) continue;

               if (searched.contains(coordinate)) continue;
               searched.add(new Vector2i(coordinate));

               if (tiles.containsKey(coordinate)) continue;
               if (!scanTilePixels(coordinate, pixels, w, h)) continue;

               addTile(new Vector2i(coordinate));
               hitTiles.add(new Vector2i(coordinate));
           }
        }
    }

    private void mindlessSearch(Vector2i startPosition, ByteBuffer pixels, int w, int h, int countX, int countY, Set<Vector2i> searched, List<Vector2i> hitTiles) {
        Vector2i coordinate = new Vector2i(startPosition);

        while (coordinate.y < countY) {
            if (!searched.contains(coordinate)) {
                searched.add(new Vector2i(coordinate));
                if (!tiles.containsKey(coordinate) && scanTilePixels(coordinate, pixels, w, h)) {
                    addTile(new Vector2i(coordinate));

                    hitTiles = new ArrayList<>();
                    neighboringSearch(coordinate, pixels, w, h, countX, countY, searched, hitTiles);

                    while (!hitTiles.isEmpty()) {
                        List<Vector2i> newHitTiles = new ArrayList<>();
                        for (Vector2i grid : hitTiles) neighboringSearch(grid, pixels, w, h, countX, countY, searched, newHitTiles);
                        hitTiles = newHitTiles;
                    }
                }
            }

            coordinate.x += 2;
            if (coordinate.x < countX) continue;
            coordinate.x = startPosition.x;
            coordinate.y += 2;
        }
    }

    private boolean scanTilePixels(Vector2i coordinate, ByteBuffer pixels, int w, int h) {
        Vector2i start = new Vector2i(startPosition).add(coordinate.x * gridSize.x, coordinate.y * gridSize.y);
        Vector2i end = new Vector2i(Math.min(start.x + gridSize.x, w), Math.min(start.y + gridSize.y, h));
        if (end.x > w || end.y > h) return false;

        int width = gridSize.x;
        int height = gridSize.y;
        if (width <= 2 || height <= 2) {
            for (int y = start.y; y < end.y; y++) {
                for (int x = start.x; x < end.x; x++) if (checkPixelAlpha(pixels, w, x, y)) return true;
            }

            return false;
        }

        int outerRing = 0;
        int innerRing = Math.min(width, height) / 2;

        while (outerRing < innerRing) {
            if (scanRingPixels(pixels, width, start, end, outerRing)) return true;

            if (scanRingPixels(pixels, width, start, end, innerRing)) return true;

            outerRing++;
            innerRing--;
        }

        if (outerRing == innerRing) return scanRingPixels(pixels, width, start, end, outerRing);

        return false;
    }

    private boolean checkPixelAlpha(ByteBuffer pixels, int w, int x, int y) {
        int index = (y * w + x) * 4;
        int alphaIndex = index + 3;
        int alpha = pixels.get(alphaIndex) & 0xFF;
        return alpha != 0;
    }

    private boolean scanRingPixels(ByteBuffer pixels, int width, Vector2i start, Vector2i end, int ring) {
        int left = start.x + ring;
        int right = end.x - 1 - ring;
        int top = start.y + ring;
        int bottom = end.y - 1 - ring;

        if (left > right || top > bottom) return false;

        for (int x = left; x <= right; x++) {
            if (checkPixelAlpha(pixels, width, x, top)) return true;
        }

        if (bottom != top) {
            for (int x = left; x <= right; x++) {
                if (checkPixelAlpha(pixels, width, x, bottom)) return true;
            }
        }

        for (int y = top + 1; y < bottom; y++) {
            if (checkPixelAlpha(pixels, width, left, y)) return true;
        }

        if (right != left) {
            for (int y = top + 1; y < bottom; y++) {
                if (checkPixelAlpha(pixels, width, right, y)) return true;
            }
        }

        return false;
    }
}
