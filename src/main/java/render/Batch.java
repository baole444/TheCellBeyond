package render;

import TCB_Field.Window;
import components.SpriteRender;
import org.joml.Vector4f;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class Batch {

    //Vertices

    //Position      Color
    //f, f,          f, f, f, f

    private final int POS_SIZE = 2;
    private final int COLOR_SIZE = 4;
    private final int POS_OFFSET = 0;
    private final int COLOR_OFFSET = POS_OFFSET + POS_SIZE *Float.BYTES;
    private final int VERTEX_SIZE = 6;
    private final int VERTEX_SIZE_BYTES = VERTEX_SIZE * Float.BYTES;

    private SpriteRender[] sprites;
    private int countSprite;
    private boolean hasSpace;
    private float[] vertices;

    private int vaoID, vboID;
    private int maxBatchSize;
    private Shader shader;

    public Batch(int maxBatchSize) {
        shader = new Shader("assets/shaders/default.glsl");
        shader.compile();
        this.sprites = new SpriteRender[maxBatchSize];
        this.maxBatchSize = maxBatchSize;

        // 4 vertices quads
        vertices = new float[maxBatchSize * 4 * VERTEX_SIZE];
        this.countSprite = 0;
        this.hasSpace = true;
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
    }

    public void loadSprite(SpriteRender spt) {
        // Indexing render object
        int index = this.countSprite;
        this.sprites[index] = spt;
        this.countSprite++;

        // Add property to vertex array
        loadVertexProp(index);
    }

    public void render() {
        //Re-buffer data/frame
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);

        // Shader
        shader.use();
        shader.loadMat4f("uProject", Window.getScene().viewport().getProjectMatrix());
        shader.loadMat4f("uView", Window.getScene().viewport().getViewMatrix());

        glBindVertexArray(vaoID);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);

        glDrawElements(GL_TRIANGLES, this.countSprite * 6, GL_UNSIGNED_INT, 0);

        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glBindVertexArray(0);

        shader.detach();
    }

    private void loadVertexProp(int index) {
        SpriteRender spt = this.sprites[index];

        // Set offset in array (4/spt)
        int offset = index * 4 * VERTEX_SIZE;

        Vector4f color = spt.getColor();

        // Load match vertex
        float xAdd = 1.0f;
        float yAdd = 1.0f;
        for (int i = 0; i < 4; i++) {
            if ( i == 1) {
                yAdd = 0.0f;
            } else if (i == 2) {
                xAdd = 0.0f;
            } else if (i == 3) {
                yAdd = 1.0f;
            }

            // Load position
            vertices[offset] = spt.gameObject.transform.position.x + (xAdd * spt.gameObject.transform.scale.x);
            vertices[offset + 1] = spt.gameObject.transform.position.y + (yAdd * spt.gameObject.transform.scale.y);

            // Load color
            vertices[offset + 2] = color.x;
            vertices[offset + 3] = color.y;
            vertices[offset + 4] = color.z;
            vertices[offset + 5] = color.w;

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
}
