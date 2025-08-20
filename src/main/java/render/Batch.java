package render;

import TheCellBeyond.GameObject;
import components.Component;
import components.SpriteRenderer;
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

public class Batch implements Comparable<Batch> {
    // Define how much texture each batch can have.
    // By default, is 8, will change on the limitation of the hardware.
    private int MAX_TEX_BATCH = 8;

    // Vertices
    // |Position| |   Color  | |Coordinate| |TexID|
    // |  f, f  | |f, f, f, f| |   f, f   | |  f  |

    private final int POS_SIZE = 2;
    private final int COLOR_SIZE = 4;
    private final int TEX_COORD_SIZE = 2;
    private final int TEX_ID_SIZE = 1;
    private final int OBJECT_ID_SIZE = 1;

    private final int POS_OFFSET = 0;
    private final int COLOR_OFFSET = POS_OFFSET + POS_SIZE *Float.BYTES;
    private final int TEX_COORD_OFFSET = COLOR_OFFSET + COLOR_SIZE * Float.BYTES;
    private final int TEX_ID_OFFSET = TEX_COORD_OFFSET + TEX_COORD_SIZE * Float.BYTES;
    private final int VERTEX_SIZE = 10;
    private final int OBJECT_ID_OFFSET = TEX_ID_OFFSET + TEX_ID_SIZE * Float.BYTES;
    private final int VERTEX_SIZE_BYTES = VERTEX_SIZE * Float.BYTES;

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

    public Batch(int maxBatchSize, int zIndex, Renderer renderer) {
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
        vertices = new float[maxBatchSize * 4 * VERTEX_SIZE];

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

        glVertexAttribPointer(0, POS_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, POS_OFFSET);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, COLOR_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, COLOR_OFFSET);
        glEnableVertexAttribArray(1);

        glVertexAttribPointer(2, TEX_COORD_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, TEX_COORD_OFFSET);
        glEnableVertexAttribArray(2);

        glVertexAttribPointer(3, TEX_ID_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, TEX_ID_OFFSET);
        glEnableVertexAttribArray(3);

        glVertexAttribPointer(4, OBJECT_ID_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, OBJECT_ID_OFFSET);
        glEnableVertexAttribArray(4);
    }

    public void loadSprite(SpriteRenderer spriteRenderer) {
        // Indexing render object
        int index = this.countSprite;
        this.sprites[index] = spriteRenderer;
        this.countSprite++;

        if (spriteRenderer.getTexture() != null) {
            if (!textures.contains(spriteRenderer.getTexture())) {
                textures.add(spriteRenderer.getTexture());
            }
        }

        // Add property to the vertex array
        genVertexProperties(index);

        if (countSprite >= this.maxBatchSize) {
            this.hasSpace = false;
        }
    }

    public void render() {
        boolean rebufferData = false;

        for (int i = 0; i < countSprite; i++) {
            SpriteRenderer spr = sprites[i];
            if (spr.isSpriteDirty()) {
                if (spr.getTextureCoordinates() == null) {
                    rebufferData = true;
                    continue;
                }
                genVertexProperties(i);
                spr.setSpriteDirty(false);
                rebufferData = true;
            }

            if(spr.getzIndex() != this.zIndex) {
                removeIfExist(spr.gameObject);
                renderer.queueObjectForUpdate(spr.gameObject);
                i--;
            }
        }
        if (rebufferData) {
            glBindBuffer(GL_ARRAY_BUFFER, vboID);
            glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
        }

        // Shader
        Shader shader = RendererState.get().getCurrentShader();
        shader.use();

        // Set projection and view matrix
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

        glDrawElements(GL_TRIANGLES, this.countSprite * 6, GL_UNSIGNED_INT, 0);

        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glBindVertexArray(0);

        for (Texture texture : textures) {
            texture.unbind();
        }

        shader.detach();
    }

    private void genVertexProperties(int index) {
        SpriteRenderer spriteRenderer = sprites[index];

        // Set offset in the array (4/spt)
        int offset = index * 4 * VERTEX_SIZE;
        Vector4f color = spriteRenderer.getColor();
        Vector2f[] textureCoordinates = spriteRenderer.getTextureCoordinates();

        // No data available yet
        if (textureCoordinates == null) return;

        int ID = 0;
        //[0, tex, tex, tex, tex]
        if (spriteRenderer.getTexture() != null) {
            for (int i = 0; i < textures.size(); i++) {
                if (textures.get(i).equals(spriteRenderer.getTexture())) {
                    ID = i + 1;
                    break;
                }
            }
        }

        Vector2f worldSize = spriteRenderer.getSpriteSizeAsWorldUnit();
        Vector2f pos = spriteRenderer.getPosition();
        Vector2f scale = spriteRenderer.getScale();
        float rotation = spriteRenderer.getRotation();
        boolean isRotated = rotation != 0.0f;
        boolean isScaled = !scale.equals(new Vector2f(1.0f, 1.0f));

        Matrix4f transformMatrix = new Matrix4f().identity();
        if (isRotated || isScaled) {
            transformMatrix.translate(pos.x, pos.y, 0);
            transformMatrix.rotate(Math.toRadians(rotation), 0, 0, 1);
            transformMatrix.scale(worldSize.x, worldSize.y, 1);
            transformMatrix.scale(scale.x, scale.y, 1);
        }

        // Load match vertex
        float xAdd = 0.5f;
        float yAdd = 0.5f;
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
            if (isRotated || isScaled) {
                instPos = new Vector4f(xAdd, yAdd, 0, 1).mul(transformMatrix);
            }

            // Load position
            vertices[offset] = instPos.x;
            vertices[offset + 1] = instPos.y;

            // Load color
            vertices[offset + 2] = color.x;
            vertices[offset + 3] = color.y;
            vertices[offset + 4] = color.z;
            vertices[offset + 5] = color.w;

            // Load coordinate
            vertices[offset + 6] = textureCoordinates[i].x;
            vertices[offset + 7] = textureCoordinates[i].y;

            // Load id
            vertices[offset + 8] = ID;

            // Load obj Id
            vertices[offset + 9] = spriteRenderer.gameObject.getUID();

            offset += VERTEX_SIZE;
        }
    }

    public boolean removeIfExist(GameObject go) {
        List<SpriteRenderer> sps = go.getComponents(SpriteRenderer.class);
        if (sps.isEmpty()) return false;

        int removalCount = 0;

        int i = 0;
        while (i < countSprite) {
            if (sps.contains(sprites[i])) {
                for (int j = i; j < countSprite - 1; j++) {
                    sprites[j] = sprites[j + 1];
                    sprites[j].setSpriteDirty(true);
                }

                countSprite--;
                sprites[countSprite] = null;
                removalCount++;
            } else {
                i++;
            }
        }

        return removalCount > 0;
    }

    public boolean removeIfExist(Component component) {
        if (component == null ) return false;

        if (component instanceof SpriteRenderer spriteRenderer) {
            for (int i = 0; i < countSprite; i++) {
                if (sprites[i] == spriteRenderer) {
                    for (int j = i; j < countSprite - 1; j++) {
                        sprites[j] = sprites[j + 1];
                        sprites[j].setSpriteDirty(true);
                    }

                    countSprite--;
                    sprites[countSprite] = null;

                    if (countSprite < maxBatchSize) {
                        hasSpace = true;
                    }

                    return true;
                }
            }
        }

        return false;
    }

    private int[] genIndices() {
        // 6 indices / quad (3 per tris)
        int[] elements = new int[6 * maxBatchSize];
        for (int i = 0; i < maxBatchSize; i++) {
            loadEleIndices(elements, i);
        }

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
        return this.hasSpace;
    }

    public boolean isTextureCapacityValid() {
        return this.textures.size() < MAX_TEX_BATCH;
    }
     public boolean hasTexture(Texture t) {
        return this.textures.contains(t);
     }

     public int zIndex() {
        return this.zIndex;
     }

    @Override
    public int compareTo(Batch o) {
        return Integer.compare(this.zIndex, o.zIndex());
    }
}
