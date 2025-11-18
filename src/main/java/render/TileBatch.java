package render;

import TheCellBeyond.GameObject;
import components.Component;
import components.TileMap;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector4f;
import render.texture.Tile;
import render.texture.TileSet;
import utility.WorldUnit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class TileBatch implements Comparable<TileBatch> {
    private static final int posSize = 2;
    private static final int colorSize = 4;
    private static final int textureCoordinateSize = 2;
    private static final int textureIdSize = 1;
    private static final int objectIdSize = 1;
    private static final int vertexSize = posSize + colorSize + textureCoordinateSize + textureIdSize + objectIdSize;

    private TileMap tileMap;
    private final int zIndex;
    private final Renderer renderer;

    private float[] vertices;
    private int[] indices;
    private final int[] texSlot = {0, 1, 2, 3, 4, 5, 6, 7};
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
        int stride = vertexSize * Float.BYTES;

        glVertexAttribPointer(0, posSize, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);

        int colorOffset = posSize * Float.BYTES;
        glVertexAttribPointer(1, colorSize, GL_FLOAT, false, stride, colorOffset);
        glEnableVertexAttribArray(1);

        int textureCoordinateOffset = colorOffset + colorSize * Float.BYTES;
        glVertexAttribPointer(2, textureCoordinateSize, GL_FLOAT, false, stride, textureCoordinateOffset);
        glEnableVertexAttribArray(2);

        int textureIDOffset = textureCoordinateOffset + textureCoordinateSize * Float.BYTES;
        glVertexAttribPointer(3, textureIdSize, GL_FLOAT, false, stride, textureIDOffset);
        glEnableVertexAttribArray(3);

        int objectIDOffset = textureIDOffset + textureIdSize * Float.BYTES;
        glVertexAttribPointer(4, objectIdSize, GL_FLOAT, false, stride, objectIDOffset);
        glEnableVertexAttribArray(4);
    }

    public void updateBatch() {
        if (tileMap == null || tileMap.getTileSet() == null) {
            tileCount = 0;
            return;
        }

        HashMap<Vector2i, Tile> tiles = tileMap.getTiles();
        if (tiles.isEmpty()) {
            tileCount = 0;
            return;
        }

        tileCount = tiles.size();

        vertices = new float[tileCount * 4 * vertexSize];
        indices = new int[tileCount * 6];

        TileSet tileSet = tileMap.getTileSet();
        Vector2f tileMapPosition = tileMap.getPosition();
        Vector2i gridSize = tileSet.getGridSize();
        Vector2f gridSizeWorld = WorldUnit.pixelToWorld(new Vector2f(gridSize));
        Vector4f color = new Vector4f(1.0f);

        int tileIndex = 0;
        for (Map.Entry<Vector2i, Tile> entry : tiles.entrySet()) {
            Vector2i mapCoordinate = entry.getKey();
            Tile tile = entry.getValue();

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
        float y = tileMapPosition.y - mapCoordinate.y * gridSizeWorld.y + gridSizeWorld.y / 2.0f;

        return new Vector2f(x, y);
    }

    private void genVertexProperties(int index, Vector2f position, Vector2f size,Vector2f[] textureCoordinate, Vector4f color) {
        int offset = index * 4 * vertexSize;
        float textureID = 1.0f;
        float objectID = tileMap.gameObject != null ? tileMap.gameObject.getUID() : 0.0f;

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

            offset += vertexSize;
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
        if (tileMap == null || tileMap.getTileSet() == null) return;

        TileMap map = tileMap;
        if (map.getzIndex() != zIndex) {
            removeIfExist(map.gameObject);
            renderer.switchZIndex(map.gameObject);
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

        Texture texture = map.getTileSet().getTexture();
        if (texture != null) {
            glActiveTexture(GL_TEXTURE + 1);
            texture.bind();
        }

        shader.loadIntA("uTex", texSlot);

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

        List<TileMap> tms = go.getComponents(TileMap.class);
        if (tms.isEmpty()) return false;

        boolean removed = false;
        for (TileMap map : tms) {
            if (map == tileMap) {
                tileMap = null;
                removed = true;
                tileCount = 0;
                break;
            }
        }

        return removed;
    }

    public boolean removeIfExist(Component component) {
        if (component == null) return false;

        if (component instanceof TileMap map) {
            if (map == tileMap) {
                tileMap = null;
                tileCount = 0;
                return true;
            }
        }

        return false;
    }

    public int zIndex() {
        return zIndex;
    }

    public boolean hasSpace() {
        return tileMap == null;
    }

    public boolean hasMap(TileMap tileMap) {
        if (tileMap == null || tileMap.getUUID() == null || tileMap.gameObject == null) return false;

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
