package render;

import TCB_Field.GameObject;
import TCB_Field.Window;
import components.SpriteRender;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class Batch implements Comparable<Batch> {

    private final int MAX_TEX_BATCH = 7;
    //Vertices

    //Position      Color               Coordinate          TexID
    //f, f,          f, f, f, f,        f, f,               f

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

    private SpriteRender[] sprites;
    private int countSprite;
    private boolean hasSpace;
    private float[] vertices;
    private int[] texSlot = {0, 1, 2, 3, 4, 5, 6, 7};

    private List<Texture> textures;
    private int vaoID, vboID;
    private int maxBatchSize;
    private Renderer renderer;
    private  int zIndex;

    public Batch(int maxBatchSize, int zIndex, Renderer renderer) {
        this.renderer = renderer;
        this.zIndex = zIndex;
        this.sprites = new SpriteRender[maxBatchSize];
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
        glBufferData(GL_ARRAY_BUFFER, vertices.length * Float.BYTES, GL_DYNAMIC_DRAW);

        // Indices buffer gen

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

    public void loadSprite(SpriteRender spt) {
        // Indexing render object
        int index = this.countSprite;
        this.sprites[index] = spt;
        this.countSprite++;

        if (spt.loadTexture() != null) {
            if (!textures.contains(spt.loadTexture())) {
                textures.add(spt.loadTexture());
            }
        }

        // Add property to vertex array
        loadVertexProp(index);

        if (countSprite >= this.maxBatchSize) {
            this.hasSpace = false;
        }
    }

    public void render() {
        boolean rebufferData = false;
        for (int i = 0; i < countSprite; i++) {
            SpriteRender spr = sprites[i];
            if (spr.isDamage()) {
                loadVertexProp(i);
                spr.notDamage();
                rebufferData = true;
            }

            //TODO: temporary
            if(spr.gameObject.transform.zIndex != this.zIndex) {
                isExistToRemove(spr.gameObject);
                renderer.add(spr.gameObject);
                i--;
            }

        }
        if (rebufferData) {
            //Legacy: Re-buffer data/frame | New: Re-buffer data/change only
            glBindBuffer(GL_ARRAY_BUFFER, vboID);
            glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
        }
        // Shader

        Shader shader = Renderer.loadShader();

        shader.use();
        shader.loadMat4f("uProject", Window.getScene().viewport().getProjectMatrix());
        shader.loadMat4f("uView", Window.getScene().viewport().getViewMatrix());
        for (int i = 0; i <textures.size(); i++) {
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

        for (int i = 0; i <textures.size(); i++) {
            textures.get(i).unbind();
        }

        shader.detach();
    }

    public boolean isExistToRemove(GameObject obj) {
        SpriteRender sprite = obj.getComponent(SpriteRender.class);
        for (int i = 0; i < countSprite; i++) {
            if (sprites[i] == sprite) {
                for (int j = i; j < countSprite - 1; j++) {
                    sprites[j] = sprites[j + 1];
                    sprites[j].setDamage();
                }
                countSprite--;

                return true;
            }
        }

        return false;
    }

    private void loadVertexProp(int index) {
        SpriteRender spt = this.sprites[index];

        // Set offset in array (4/spt)
        int offset = index * 4 * VERTEX_SIZE;

        Vector4f color = spt.loadColor();
        Vector2f[] tCoord = spt.loadTexCoord();

        int ID = 0;
        //[0, tex, tex, tex, tex]

        if (spt.loadTexture() != null) {
            for (int i = 0; i < textures.size(); i++) {
                if (textures.get(i).equals(spt.loadTexture())) {
                    ID = i + 1;
                    break;
                }

            }
        }

        boolean isRotate = spt.gameObject.transform.rotate != 0.0f;
        Matrix4f transformMatrix = new Matrix4f().identity();
        if (isRotate) {
            transformMatrix.translate(spt.gameObject.transform.position.x, spt.gameObject.transform.position.y, 0);

            transformMatrix.rotate((float)Math.toRadians(spt.gameObject.transform.rotate) , 0, 0, 1);

            transformMatrix.scale(spt.gameObject.transform.scale.x, spt.gameObject.transform.scale.y, 1);
        }

        // Load match vertex
        float xAdd = 0.5f;
        float yAdd = 0.5f;
        for (int i = 0; i < 4; i++) {
            if ( i == 1) {
                yAdd = -0.5f;
            } else if (i == 2) {
                xAdd = -0.5f;
            } else if (i == 3) {
                yAdd = 0.5f;
            }

            Vector4f instPos = new Vector4f(spt.gameObject.transform.position.x + (xAdd * spt.gameObject.transform.scale.x),
                    spt.gameObject.transform.position.y + (yAdd * spt.gameObject.transform.scale.y),
                    0, 1
            );
            if (isRotate) {
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
            vertices[offset + 6] = tCoord[i].x;
            vertices[offset + 7] = tCoord[i].y;

            // Load id
            vertices[offset + 8] = ID;

            // Load obj Id
            vertices[offset + 9] = spt.gameObject.loadUid() + 1;

            offset += VERTEX_SIZE;
        }
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
        elements[offsetArrayI + 2] = offset + 0;

        //Tris 2
        elements[offsetArrayI + 3] = offset + 0;
        elements[offsetArrayI + 4] = offset + 2;
        elements[offsetArrayI + 5] = offset + 1;
    }

    public boolean hasSpace() {
        return this.hasSpace;
    }

    public boolean isTexCapValid () {
        return this.textures.size() < MAX_TEX_BATCH;
    }
     public boolean isTex(Texture t) {
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
