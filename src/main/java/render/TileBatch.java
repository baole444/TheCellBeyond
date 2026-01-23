package render;

import TheCellBeyond.GameObject;
import TheCellBeyond.TileMap;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector4f;
import render.texture.Tile;
import render.texture.TileSet;
import utility.WorldUnit;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class TileBatch implements Comparable<TileBatch> {
    private static final int PosSize = 2;
    private static final int ColorSize = 4;
    private static final int TextureCoordinateSize = 2;
    private static final int TextureIdSize = 1;
    private static final int ObjectIdSize = 1;
    private static final int VertexSize = PosSize + ColorSize + TextureCoordinateSize + TextureIdSize + ObjectIdSize;

    private TileMap tileMap;
    private final int zIndex;
    private final Renderer renderer;

    private float[] vertices;
    private int[] indices;
    private final int[] TextureSlot = {0, 1, 2, 3, 4, 5, 6, 7};
    private int vaoID, vboID, eboID;

    private int tileCount;

    private Matrix4f projectionMatrix = null;
    private Matrix4f viewMatrix = null;

    public TileBatch(TileMap tileMap, int zIndex, Renderer renderer) {
        this.tileMap = tileMap;
        this.zIndex = zIndex;
        this.renderer = renderer;
        this.tileCount = 0;
    }

    public void setProjectionMatrix(Matrix4f projectionMatrix) {
        this.projectionMatrix = projectionMatrix;
    }

    public void setViewMatrix(Matrix4f viewMatrix) {
        this.viewMatrix = viewMatrix;
    }

    public void start() {
        vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);

        vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);

        eboID = glGenBuffers();

        adjustVertexATTB();
    }

    private void adjustVertexATTB() {
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
    }

    public void updateBatch() {
        if (tileMap == null || tileMap.tileSet() == null) {
            tileCount = 0;
            return;
        }

        HashMap<Vector2i, TileMap.TilePlacement> placements = tileMap.tilePlacements();
        if (placements.isEmpty()) {
            tileCount = 0;
            return;
        }

        tileCount = placements.size();
        vertices = new float[tileCount * 4 * VertexSize];
        indices = new int[tileCount * 6];

        TileSet tileSet = tileMap.tileSet();
        Vector2f tileMapPosition = tileMap.globalPosition();
        Vector2i gridSize = tileSet.gridSize();
        Vector2f gridSizeWorld = WorldUnit.pixelToWorld(new Vector2f(gridSize));
        Vector4f color = new Vector4f(1.0f);

        int tileIndex = 0;
        for (Map.Entry<Vector2i, TileMap.TilePlacement> entry : placements.entrySet()) {
            Vector2i mapCoordinate = entry.getKey();
            TileMap.TilePlacement placement = entry.getValue();
            Tile tile = tileSet.tile(placement.sourceCoordinate());

            if (tile == null || tile.textureCoordinates == null) continue;
            Vector2f tilePosition = calculateTilePosition(tileMapPosition, mapCoordinate, gridSizeWorld);
            genVertexProperties(tileIndex, tilePosition, gridSizeWorld, tile.textureCoordinates, color);
            genIndices(tileIndex);

            tileIndex++;
        }

        tileCount = tileIndex;

        uploadDrawData();
    }

    private Vector2f calculateTilePosition(Vector2f tileMapPosition, Vector2i mapCoordinate, Vector2f gridSizeWorld) {
        float x = tileMapPosition.x + mapCoordinate.x * gridSizeWorld.x + gridSizeWorld.x / 2.0f;
        float y = tileMapPosition.y + mapCoordinate.y * gridSizeWorld.y + gridSizeWorld.y / 2.0f;

        return new Vector2f(x, y);
    }

    private void genVertexProperties(int index, Vector2f position, Vector2f size,Vector2f[] textureCoordinate, Vector4f color) {
        int offset = index * 4 * VertexSize;
        float textureID = 1.0f;
        float objectID = tileMap!= null ? tileMap.getUID() : 0.0f;

        float xAdd = 0.5f;
        float yAdd = 0.5f;

        Vector2f[] corners = {
                new Vector2f(position.x + size.x * xAdd, position.y + size.y * yAdd),
                new Vector2f(position.x + size.x * xAdd, position.y - size.y * yAdd),
                new Vector2f(position.x - size.x * xAdd, position.y - size.y * yAdd),
                new Vector2f(position.x - size.x * xAdd, position.y + size.y * yAdd)
        };

        for (int i = 0; i < 4; i++) {
            vertices[offset] = corners[i].x;
            vertices[offset + 1] = corners[i].y;
            vertices[offset + 2] = color.x;
            vertices[offset + 3] = color.y;
            vertices[offset + 4] = color.z;
            vertices[offset + 5] = color.w;
            vertices[offset + 6] = textureCoordinate[i].x;
            vertices[offset + 7] = textureCoordinate[i].y;
            vertices[offset + 8] = textureID;
            vertices[offset + 9] = objectID;

            offset += VertexSize;
        }
    }

    private void genIndices(int index) {
        int offset = index * 6;
        int vertexOffset = index * 4;

        indices[offset] = vertexOffset + 3;
        indices[offset + 1] = vertexOffset + 2;
        indices[offset + 2] = vertexOffset;

        indices[offset + 3] = vertexOffset;
        indices[offset + 4] = vertexOffset + 2;
        indices[offset + 5] = vertexOffset + 1;
    }

    private void uploadDrawData() {
        if (tileCount <= 0) return;

        glBindVertexArray(vaoID);

        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void render() {
        if (tileMap == null || tileMap.tileSet() == null) return;

        TileMap map = tileMap;
        if (map.globalZIndex() != zIndex) {
            removeIfExist(map);
            renderer.switchZIndex(map);
            return;
        }

        if (map.isTileDirty()) {
            updateBatch();
            map.setTileDirty(false);
        }

        if (tileCount <= 0) return;

        Shader shader = RendererState.getCurrentShader();
        shader.use();

        Matrix4f projMatrix;
        Matrix4f vMatrix;

        if (projectionMatrix != null) {
            projMatrix = projectionMatrix;
        } else projMatrix = new Matrix4f().identity();

        if (viewMatrix != null) {
            vMatrix = viewMatrix;
        } else vMatrix = new Matrix4f().identity();

        shader.loadMat4f("uProject", projMatrix);
        shader.loadMat4f("uView", vMatrix);

        Texture texture = map.tileSet().texture();
        if (texture != null) {
            glActiveTexture(GL_TEXTURE + 1);
            texture.bind();
        }

        shader.loadIntA("uTex", TextureSlot);

        glBindVertexArray(vaoID);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);

        glDrawElements(GL_TRIANGLES, tileCount * 6, GL_UNSIGNED_INT, 0);

        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glBindVertexArray(0);

        if (texture != null) texture.unbind();

        shader.detach();
    }

    public boolean removeIfExist(GameObject go) {
        if (tileMap == null) return false;

        if (!(go instanceof  TileMap map) || map != tileMap) return false;
        tileMap = null;
        tileCount = 0;
        return true;
    }

    public int zIndex() {
        return zIndex;
    }

    public boolean hasSpace() {
        return tileMap == null;
    }

    public boolean hasMap(TileMap tileMap) {
        if (tileMap == null || tileMap.getUUID() == null) return false;

        return this.tileMap != null && this.tileMap.getUUID().equals(tileMap.getUUID());
    }

    public void loadTileMap(TileMap map) {
        if (map == null) return;
        tileCount = 0;
        tileMap = map;
        updateBatch();
    }

    @Override
    public int compareTo(TileBatch o) {
        return Integer.compare(this.zIndex, o.zIndex());
    }
}
