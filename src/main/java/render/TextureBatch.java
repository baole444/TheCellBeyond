package render;

import TheCellBeyond.GameObject;
import TheCellBeyond.TileMap;
import components.Component;
import components.SpriteRenderer;
import editor.components.EditorObjectIndicator;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

import java.util.*;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class TextureBatch implements Comparable<TextureBatch> {
    // Define how much texture each batch can have.
    // By default, is 8, will change on the limitation of the hardware.
    private int MAX_TEX_BATCH = 8;

    // Vertices
    // |Position| |   Color  | |Coordinate| |TexID|
    // |  f, f  | |f, f, f, f| |   f, f   | |  f  |
    private final int vertexSize = 10;

    private final SpriteRenderer[] sprites;
    private int countSprite;
    private boolean hasSpace;
    private final float[] vertices;
    private final int[] texSlot = {0, 1, 2, 3, 4, 5, 6, 7};

    private final List<Texture> textures;
    private int vaoID, vboID;
    private final int maxBatchSize;
    private final Renderer renderer;
    private final int zIndex;

    private Matrix4f projectionMatrix = null;
    private Matrix4f viewMatrix = null;

    public void setProjectionMatrix(Matrix4f projectionMatrix) {
        this.projectionMatrix = projectionMatrix;
    }

    public void setViewMatrix(Matrix4f viewMatrix) {
        this.viewMatrix = viewMatrix;
    }

    public TextureBatch(int maxBatchSize, int zIndex, Renderer renderer) {
        int _trueLimit = GL11.glGetInteger(GL_MAX_TEXTURE_IMAGE_UNITS);
        if (MAX_TEX_BATCH > _trueLimit) {
            System.out.println("Encounter texture limit! " + "(Asking " + MAX_TEX_BATCH + "/" + _trueLimit + ")\nSetting new limit...");
            this.MAX_TEX_BATCH = _trueLimit;
        }

        this.renderer = renderer;

        this.zIndex = zIndex;
        this.sprites = new SpriteRenderer[maxBatchSize];
        this.maxBatchSize = maxBatchSize;

        // 4 vertices quads
        vertices = new float[maxBatchSize * 4 * vertexSize];

        this.countSprite = 0;
        this.hasSpace = true;
        this.textures = new ArrayList<>();
    }

    public void start() {
        // Vertex array object binding
        vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);

        // Vertex space alloc
        vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, (long) vertices.length * Float.BYTES, GL_DYNAMIC_DRAW);

        int eboID = glGenBuffers();
        int[] indices = genIndices();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        // Enable buffer attrib pointer
        int vertexBytes = vertexSize * Float.BYTES;
        int positionOffset = 0;
        int positionSize = 2;
        glVertexAttribPointer(0, positionSize, GL_FLOAT, false, vertexBytes, positionOffset);
        glEnableVertexAttribArray(0);

        int colorOffset = positionOffset + positionSize * Float.BYTES;
        int colorSize = 4;
        glVertexAttribPointer(1, colorSize, GL_FLOAT, false, vertexBytes, colorOffset);
        glEnableVertexAttribArray(1);

        int textureCoordinateSize = 2;
        int textureCoordinateOffset = colorOffset + colorSize * Float.BYTES;
        glVertexAttribPointer(2, textureCoordinateSize, GL_FLOAT, false, vertexBytes, textureCoordinateOffset);
        glEnableVertexAttribArray(2);

        int textureIdSize = 1;
        int textureIdOffset = textureCoordinateOffset + textureCoordinateSize * Float.BYTES;
        glVertexAttribPointer(3, textureIdSize, GL_FLOAT, false, vertexBytes, textureIdOffset);
        glEnableVertexAttribArray(3);

        int objectIdSize = 1;
        int objectIdOffset = textureIdOffset + textureIdSize * Float.BYTES;
        glVertexAttribPointer(4, objectIdSize, GL_FLOAT, false, vertexBytes, objectIdOffset);
        glEnableVertexAttribArray(4);
    }

    public void loadSprite(SpriteRenderer spriteRenderer) {
        int index = countSprite;
        sprites[index] = spriteRenderer;
        countSprite++;
        if (spriteRenderer.texture() != null && !textures.contains(spriteRenderer.texture())) {
            textures.add(spriteRenderer.texture());
        }
        genVertexProperties(vertices, index);
        if (countSprite >= maxBatchSize) hasSpace = false;
    }

    public void render() {
        for (int i = 0; i < countSprite; i++) {
            SpriteRenderer spr = sprites[i];
            if (spr.globalZIndex() == zIndex) continue;
            removeIfExist(spr.gameObject);
            renderer.switchZIndex(spr.gameObject);
            i--;
        }
        List<Integer> dirtyIndex = new ArrayList<>();
        for (int i = 0; i < countSprite; i++) {
            SpriteRenderer spr = sprites[i];
            if (!spr.isSpriteDirty()) continue;
            if (spr.texture() != null && !textures.contains(spr.texture()) && isTextureCapacityValid()) textures.add(spr.texture());
            dirtyIndex.add(i);
            spr.spriteDirty(false);
        }
        if (!dirtyIndex.isEmpty()) {
            float[] newVertices = new float[vertices.length];
            System.arraycopy(vertices, 0, newVertices, 0, vertices.length);
            for (int i : dirtyIndex) genVertexProperties(newVertices, i);
            glBindBuffer(GL_ARRAY_BUFFER, vboID);
            glBufferSubData(GL_ARRAY_BUFFER, 0, newVertices);
            System.arraycopy(newVertices, 0 , vertices, 0, vertices.length);
        }

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
        for (int i = 0; i < textures.size(); i++) {
            glActiveTexture(GL_TEXTURE0 + i + 1);
            textures.get(i).bind();
        }
        shader.loadIntA("uTex", texSlot);

        glBindVertexArray(vaoID);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glDrawElements(GL_TRIANGLES, countSprite * 6, GL_UNSIGNED_INT, 0);
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glBindVertexArray(0);

        textures.forEach(Texture::unbind);
        shader.detach();
    }

    private void genVertexProperties(float[] target, int index) {
        SpriteRenderer spriteRenderer = sprites[index];
        int offset = index * 4 * vertexSize;
        Vector4f color = spriteRenderer.color();
        Vector2f[] textureCoordinates = spriteRenderer.textureCoordinates();

        if (textureCoordinates == null || spriteRenderer.texture() == null) {
            textureCoordinates = new Vector2f[] {
                    new Vector2f(1, 1),
                    new Vector2f(1, 0),
                    new Vector2f(0, 0),
                    new Vector2f(0, 1)
            };
            color = new Vector4f(color.x, color.y, color.z, 0.0f);
        }

        boolean flipH = spriteRenderer.flipHorizontally();
        boolean flipV = spriteRenderer.flipVertically();
        if (flipH || flipV) {
            Vector2f[] flipCoordinates = new Vector2f[4];
            for (int i = 0; i < 4; i++) {
                int sourceIndex = flipSourceIndex(i, flipH, flipV);
                flipCoordinates[i] = new Vector2f(textureCoordinates[sourceIndex]);
            }
            textureCoordinates = flipCoordinates;
        }

        int ID = 0;
        //[0, tex, tex, tex, tex]
        if (spriteRenderer.texture() != null) {
            for (int i = 0; i < textures.size(); i++) {
                if (!textures.get(i).equals(spriteRenderer.texture())) continue;
                ID = i + 1;
                break;
            }
        }

        Vector2f worldSize = spriteRenderer.spriteSizeAsWorldUnit();
        Vector2f pos = spriteRenderer.globalPosition();
        Vector2f scale = spriteRenderer.globalScale();
        float rotation = spriteRenderer.globalRotation();
        boolean isTransformed = rotation != 0.0f || !scale.equals(new Vector2f(1.0f, 1.0f));
        boolean isIndicator = spriteRenderer instanceof EditorObjectIndicator;

        Matrix4f transformMatrix = new Matrix4f().identity();
        if (!isIndicator && isTransformed) {
            transformMatrix.translate(pos.x, pos.y, 0.0f);
            transformMatrix.rotate(Math.toRadians(rotation), 0.0f, 0.0f, 1.0f);
            transformMatrix.scale(worldSize.x * scale.x, worldSize.y * scale.y, 1.0f);
        }

        float xAdd = 0.5f;
        float yAdd = 0.5f;
        int uID = spriteRenderer.gameObject == null ? 0 : spriteRenderer.gameObject.getUID();
        for (int i = 0; i < 4; i++) {
            switch (i) {
                case 1 -> yAdd = -0.5f;
                case 2 -> xAdd = -0.5f;
                case 3 -> yAdd = 0.5f;
            }

            Vector4f instPos = new Vector4f(
                    pos.x + (xAdd * worldSize.x),
                    pos.y + (yAdd * worldSize.y),
                    0, 1
            );
            if (!isIndicator && isTransformed) instPos = new Vector4f(xAdd, yAdd, 0, 1).mul(transformMatrix);

            target[offset] = instPos.x / instPos.w;
            target[offset + 1] = instPos.y / instPos.w;
            target[offset + 2] = color.x;
            target[offset + 3] = color.y;
            target[offset + 4] = color.z;
            target[offset + 5] = color.w;
            target[offset + 6] = textureCoordinates[i].x;
            target[offset + 7] = textureCoordinates[i].y;
            target[offset + 8] = ID;
            target[offset + 9] = uID;

            offset += vertexSize;
        }
    }

    private static int flipSourceIndex(int i, boolean flipH, boolean flipV) {
        int sourceIndex = i;
        if (flipH) {
            sourceIndex = switch (i) {
                case 0 -> 3;
                case 1 -> 2;
                case 2 -> 1;
                case 3 -> 0;
                default -> i;
            };
        }

        if (flipV) {
            sourceIndex = switch (sourceIndex) {
                case 0 -> 1;
                case 1 -> 0;
                case 2 -> 3;
                case 3 -> 2;
                default -> sourceIndex;
            };
        }
        return sourceIndex;
    }

    public boolean removeIfExist(GameObject go) {
        List<SpriteRenderer> sps = go.getComponents(SpriteRenderer.class);
        if (sps.isEmpty()) return false;
        int removalCount = 0;
        int i = 0;
        while (i < countSprite) {
            if (!sps.contains(sprites[i])) {
                i++;
                continue;
            }
            for (int j = i; j < countSprite - 1; j++) {
                sprites[j] = sprites[j + 1];
                sprites[j].spriteDirty(true);
            }
            countSprite--;
            sprites[countSprite] = null;
            removalCount++;
        }
        return removalCount > 0;
    }

    public boolean removeIfExist(Component component) {
        if (!(component instanceof SpriteRenderer spriteRenderer)) return false;
        for (int i = 0; i < countSprite; i++) {
            if (sprites[i] != spriteRenderer) continue;
            for (int j = i; j < countSprite - 1; j++) {
                sprites[j] = sprites[j + 1];
                sprites[j].spriteDirty(true);
            }
            countSprite--;
            sprites[countSprite] = null;
            if (countSprite < maxBatchSize) {
                hasSpace = true;
            }
            return true;
        }
        return false;
    }

    private int[] genIndices() {
        // 6 indices / quad (3 per tris)
        int[] elements = new int[6 * maxBatchSize];
        for (int i = 0; i < maxBatchSize; i++) loadEleIndices(elements, i);
        return elements;
    }

    private void loadEleIndices(int[] elements, int index) {
        int offsetArrayI = 6 * index;
        int offset = 4 * index;

        // 3, 2, 0, 0, 2, 1     7, 6, 4, 4, 6, 5
        // Tris 1
        elements[offsetArrayI] = offset + 3;
        elements[offsetArrayI + 1] = offset + 2;
        elements[offsetArrayI + 2] = offset;
        //Tris 2
        elements[offsetArrayI + 3] = offset;
        elements[offsetArrayI + 4] = offset + 2;
        elements[offsetArrayI + 5] = offset + 1;
    }

    public boolean hasSpace() {
        return hasSpace;
    }

    public boolean isTextureCapacityValid() {
        return textures.size() < MAX_TEX_BATCH;
    }

    public boolean hasSprite(SpriteRenderer spriteRenderer) {
        if (spriteRenderer == null || spriteRenderer.getUUID() == null || spriteRenderer.gameObject == null) return false;
        UUID uuid = spriteRenderer.getUUID();
        for (int i = 0; i < countSprite; i++) {
            if (sprites[i] != null && uuid.equals(sprites[i].getUUID())) return true;
        }
        return false;
    }

    public boolean hasTexture(Texture t) {
        if (t == null) return false;

        return textures.contains(t);
    }

    public int zIndex() {
        return this.zIndex;
    }

    @Override
    public int compareTo(TextureBatch o) {
        return Integer.compare(this.zIndex, o.zIndex());
    }
}
