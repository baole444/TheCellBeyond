package render;

import TheCellBeyond.TileMap;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector4f;
import render.commands.MeshCommand;
import render.commands.TransformCommand;
import render.texture.Tile;
import render.texture.TileSet;
import utility.WorldUnit;

import java.util.*;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class TileBatch {
    private static final int PosSize = 2;
    private static final int ColorSize = 4;
    private static final int TextureCoordinateSize = 2;
    private static final int TextureIdSize = 1;
    private static final int ObjectIdSize = 1;
    private static final int VerticesPerQuad = 4;
    private static final int IndicesPerQuad = 6;
    private static final int VertexSize = PosSize + ColorSize + TextureCoordinateSize + TextureIdSize + ObjectIdSize;
    private static final int[] TextureSlot = {0, 1, 2, 3, 4, 5, 6, 7};

    private static class CachedTileData {
        float[] vertices;
        int[] indices;
        int tileCount;
        TransformCommand transform;
        long commandVersion;
        long transformVersion;
        boolean dirty = true;
        boolean seen = false;
    }

    private final IdentityHashMap<MeshCommand, CachedTileData> commandCache = new IdentityHashMap<>();
    private final IdentityHashMap<MeshCommand, Integer> commandZIndex = new IdentityHashMap<>();
    private final TreeMap<Integer, List<MeshCommand>> zBuckets = new TreeMap<>();

    private int vaoID, vboID, eboID;
    private Matrix4f projectionMatrix;
    private Matrix4f viewMatrix;
    private boolean initialized = false;

    public TileBatch() {}

    public void init() {
        if (initialized) return;
        vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);
        vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        eboID = glGenBuffers();
        int stride = VertexSize * Float.BYTES;
        glVertexAttribPointer(0, PosSize, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        int colorOffset = PosSize * Float.BYTES;
        glVertexAttribPointer(1, ColorSize, GL_FLOAT, false, stride, colorOffset);
        glEnableVertexAttribArray(1);
        int textureCoordinateOffset = colorOffset + ColorSize * Float.BYTES;
        glVertexAttribPointer(2, TextureCoordinateSize, GL_FLOAT, false, stride, textureCoordinateOffset);
        glEnableVertexAttribArray(2);
        int textureIDOffset = textureCoordinateOffset + TextureCoordinateSize * Float.BYTES;
        glVertexAttribPointer(3, TextureIdSize, GL_FLOAT, false, stride, textureIDOffset);
        glEnableVertexAttribArray(3);
        int objectIDOffset = textureIDOffset + TextureIdSize * Float.BYTES;
        glVertexAttribPointer(4, ObjectIdSize, GL_FLOAT, false, stride, objectIDOffset);
        glEnableVertexAttribArray(4);
        initialized = true;
    }

    public void beginFrame() {
        for (CachedTileData data : commandCache.values()) data.seen = false;
    }

    public void endFrame() {
        Iterator<Map.Entry<MeshCommand, CachedTileData>> iterator = commandCache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<MeshCommand, CachedTileData> entry = iterator.next();
            if (entry.getValue().seen) continue;
            MeshCommand command = entry.getKey();
            Integer zIndex = commandZIndex.remove(command);
            if (zIndex != null) {
                List<MeshCommand> bucket = zBuckets.get(zIndex);
                if (bucket != null) bucket.remove(command);
            }
            iterator.remove();
        }
        zBuckets.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public void submit(MeshCommand command, TransformCommand transform) {
        int zIndex= transform.zIndex;
        CachedTileData cached = commandCache.get(command);
        if (cached != null) {
            cached.seen = true;
            Integer previousZIndex = commandZIndex.get(command);
            if (previousZIndex != null && previousZIndex != zIndex) {
                List<MeshCommand> oldBucket = zBuckets.get(previousZIndex);
                if (oldBucket != null) oldBucket.remove(command);
                zBuckets.computeIfAbsent(zIndex, _ -> new ArrayList<>()).add(command);
                commandZIndex.put(command, zIndex);
            }
            if (cached.transform != transform) {
                cached.transform = transform;
                cached.transformVersion = transform.version;
                cached.dirty = true;
                return;
            }
            if (command.version == cached.commandVersion && transform.version == cached.transformVersion) return;
            cached.commandVersion = command.version;
            cached.transformVersion = transform.version;
            cached.dirty = true;
            return;
        }
        cached = new CachedTileData();
        cached.transform = transform;
        cached.commandVersion = command.version;
        cached.transformVersion = transform.version;
        cached.seen = true;
        commandCache.put(command, cached);
        commandZIndex.put(command, zIndex);
        zBuckets.computeIfAbsent(zIndex, _ -> new ArrayList<>()).add(command);
    }

    public void clearSubmitted() {
        commandCache.clear();
        commandZIndex.clear();
        zBuckets.clear();
    }

    public void prepareRender(Matrix4f projectionMatrix, Matrix4f viewMatrix) {
        if (commandCache.isEmpty()) return;
        if (!initialized) init();
        this.projectionMatrix = projectionMatrix != null ? projectionMatrix : new Matrix4f().identity();
        this.viewMatrix = viewMatrix != null ? viewMatrix : new Matrix4f().identity();
    }

    public void renderZIndex(int zIndex) {
        List<MeshCommand> bucket = zBuckets.get(zIndex);
        if (bucket == null) return;
        Shader shader = RendererState.getCurrentShader();
        shader.use();
        shader.loadMat4f("uProject", projectionMatrix);
        shader.loadMat4f("uView", viewMatrix);
        shader.loadIntA("uTex", TextureSlot);
        for (MeshCommand command : bucket) {
            if (command.tileSet == null || command.tilePlacements == null) continue;
            CachedTileData cached = commandCache.get(command);
            if (cached == null) continue;
            if (cached.dirty) {
                genTileVertices(command, cached);
                cached.dirty = false;
            }
            if (cached.tileCount <= 0) continue;
            Texture texture = command.tileSet.texture();
            if (texture != null) {
                glActiveTexture(GL_TEXTURE0 + 1);
                texture.bind();
            }
            glBindVertexArray(vaoID);
            glBindBuffer(GL_ARRAY_BUFFER, vboID);
            glBufferData(GL_ARRAY_BUFFER, cached.vertices, GL_DYNAMIC_DRAW);
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, cached.indices, GL_STATIC_DRAW);
            glEnableVertexAttribArray(0);
            glEnableVertexAttribArray(1);
            glDrawElements(GL_TRIANGLES, cached.tileCount * IndicesPerQuad, GL_UNSIGNED_INT, 0);
            glDisableVertexAttribArray(0);
            glDisableVertexAttribArray(1);
            glBindVertexArray(0);
            if (texture != null) texture.unbind();
        }
    }

    private void genTileVertices(MeshCommand command, CachedTileData cached) {
        Map<Vector2i, TileMap.TilePlacement> placements = command.tilePlacements;
        TileSet tileSet = command.tileSet;
        if (placements == null || placements.isEmpty() || tileSet == null) {
            cached.tileCount = 0;
            return;
        }
        int count = placements.size();
        cached.vertices = new float[count * VerticesPerQuad * VertexSize];
        cached.indices = new int[count * IndicesPerQuad];
        Vector2i gridSize = tileSet.gridSize();
        Vector2f gridWorldSize = WorldUnit.pixelToWorld(gridSize.x, gridSize.y);
        Vector2f position = cached.transform.position;
        Vector4f color = command.modulate;
        int objectID = command.submitterID;
        int tileIndex = 0;
        for (Map.Entry<Vector2i, TileMap.TilePlacement> entry : placements.entrySet()) {
            Vector2i mapCoordinate = entry.getKey();
            TileMap.TilePlacement placement = entry.getValue();
            Tile tile = tileSet.tile(placement.sourceCoordinate());
            if (tile == null || tile.textureCoordinates == null) continue;
            float x = position.x + mapCoordinate.x * gridWorldSize.x + gridWorldSize.x / 2.0f;
            float y = position.y + mapCoordinate.y * gridWorldSize.y + gridWorldSize.y / 2.0f;
            genTileVertexProperties(cached.vertices, tileIndex, new Vector2f(x, y), gridWorldSize, tile.textureCoordinates, color, objectID);
            genTileIndices(cached.indices, tileIndex);
            tileIndex++;
        }
        cached.tileCount = tileIndex;
    }

    private void genTileVertexProperties(float[] target, int index, Vector2f position, Vector2f size, Vector2f[] textureCoordinate, Vector4f color, int objectID) {
        int offset = index * VerticesPerQuad * VertexSize;
        float textureID = 1.0f;
        float xAdd = 0.5f;
        float yAdd = 0.5f;
        Vector2f[] corners = {
                new Vector2f(position.x + size.x * xAdd, position.y + size.y * yAdd),
                new Vector2f(position.x + size.x * xAdd, position.y - size.y * yAdd),
                new Vector2f(position.x - size.x * xAdd, position.y - size.y * yAdd),
                new Vector2f(position.x - size.x * xAdd, position.y + size.y * yAdd)
        };
        for (int i = 0; i < 4; i++) {
            target[offset] = corners[i].x;
            target[offset + 1] = corners[i].y;
            target[offset + 2] = color.x;
            target[offset + 3] = color.y;
            target[offset + 4] = color.z;
            target[offset + 5] = color.w;
            target[offset + 6] = textureCoordinate[i].x;
            target[offset + 7] = textureCoordinate[i].y;
            target[offset + 8] = textureID;
            target[offset + 9] = objectID;
            offset += VertexSize;
        }
    }

    private void genTileIndices(int[] target, int index) {
        int offset = index * IndicesPerQuad;
        int vertexOffset = index * VerticesPerQuad;
        target[offset] = vertexOffset + 3;
        target[offset + 1] = vertexOffset + 2;
        target[offset + 2] = vertexOffset;
        target[offset + 3] = vertexOffset;
        target[offset + 4] = vertexOffset + 2;
        target[offset + 5] = vertexOffset + 1;
    }
}
