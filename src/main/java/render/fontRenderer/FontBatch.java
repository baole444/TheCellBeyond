package render.fontRenderer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import render.fontRenderer.cfont.Shader;
import utility.ColorConverter;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL31.GL_TEXTURE_BUFFER;

public class FontBatch {
    /*
    Texture coordinates layout:
    | x |  | y |
    | 1 |  | 0 |
    | 1 |  | 1 |
    | 0 |  | 1 |
    | 0 |  | 0 |
    (float)
     */

    private final int[] indices = {
            0, 1, 3,
            1, 2, 3,
    };

    // 26 english chars + 10 numbers (0 -> 9) + 14 common symbols = 50
    // Multiply by 4
    // Result in batch size of common char for better access.
    public static int BATCH_SIZE = 200;

    // |Position| |  Color  | |Coordinate|
    // |  f, f  | | f, f, f | |   f, f   |
    public static int VERTEX_SIZE = 7;
    public float[] vertices = new float[BATCH_SIZE * VERTEX_SIZE];

    // Count vertices.
    public int size = 0;

    // The projection to drawn on.
    private final Matrix4f projection = new Matrix4f();

    private int vao;
    private int vbo;

    private Shader shader;

    private Shader sdfShader;

    private TCBFont font;

    // Generate a buffer object large enough for BATCH_SIZE
    private void generateElementBufferObject() {
        int elementSize = BATCH_SIZE * 3; // 2 tris per element.

        int[] elementBuffer = new int[elementSize];

        for (int i = 0; i < elementSize; i++) {
            // Base on indices pattern
            elementBuffer[i] = indices[(i % 6)] + ((i / 6) * 4);
        }

        int ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, elementBuffer, GL_STATIC_DRAW);
    }

    public void initFontRenderBatch() {
        projection.identity();
        projection.ortho(0f, 800f, 0f, 600f, 1f, 100f);

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, (long) Float.BYTES * VERTEX_SIZE * BATCH_SIZE, GL_DYNAMIC_DRAW);

        generateElementBufferObject();

        int stride = 7 * Float.BYTES;
        glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 5 * Float.BYTES);
        glEnableVertexAttribArray(2);
    }

    // GPU likes a large chunk of memory being uploaded rather than multiple small chunks.
    public void flushBatch() {
        // Clear GPU's buffer.
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        // Allocate memory.
        glBufferData(GL_ARRAY_BUFFER, (long) Float.BYTES * VERTEX_SIZE * BATCH_SIZE, GL_DYNAMIC_DRAW);

        // Upload CPU's contents.
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);

        // Draw the uploaded buffer.
        shader.use();

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, font.textureId);
        sdfShader.uploadTexture("uFontTexture", 0);
        sdfShader.uploadMat4f("uProjection", projection);


        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, size * 6, GL_UNSIGNED_INT, 0);

        // Reset the batch for the next draw call.
        size = 0;
    }

    private void addCharacter(float x, float y, float scale, CharInfo charInfo, int rgb) {
        // When the batch is full, flush and start with new batch.
        if (size >= BATCH_SIZE - 4) {
            flushBatch();
        }

        Vector3f color = ColorConverter.fromHexIntToVector(rgb, true);

        float x0 = x;                               float y0 = y;
        float x1 = x + scale + charInfo.width();    float y1 = y + scale + charInfo.height();

        float ux0 = charInfo.textureCoordinates[0].x;   float uy0 = charInfo.textureCoordinates[0].y;
        float ux1 = charInfo.textureCoordinates[1].x;   float uy1 = charInfo.textureCoordinates[1].y;

        //<editor-fold defaultstate="collapsed" desc="Append value to 4 vertices">
        int index = size * 7; // there are 7 floats per vertex.
        vertices[index] = x1;           vertices[index + 1] = y0;
        vertices[index + 2] = color.x;  vertices[index + 3] = color.y;  vertices[index + 4] = color.z;
        vertices[index + 5] = ux1;      vertices[index + 6] = uy0;

        index += 7;
        vertices[index] = x1;           vertices[index + 1] = y1;
        vertices[index + 2] = color.x;  vertices[index + 3] = color.y;  vertices[index + 4] = color.z;
        vertices[index + 5] = ux1;      vertices[index + 6] = uy1;

        index += 7;
        vertices[index] = x0;           vertices[index + 1] = y1;
        vertices[index + 2] = color.x;  vertices[index + 3] = color.y;  vertices[index + 4] = color.z;
        vertices[index + 5] = ux0;      vertices[index + 6] = uy1;

        index += 7;
        vertices[index] = x0;           vertices[index + 1] = y0;
        vertices[index + 2] = color.x;  vertices[index + 3] = color.y;  vertices[index + 4] = color.z;
        vertices[index + 5] = ux0;      vertices[index + 6] = uy0;
        //</editor-fold>

        size += 4;
    }

    /**
     * Add a string of text to the font rendering batch.
     * @param text the text to display.
     * @param x screen x coordinate
     * @param y screen y coordinate
     * @param scale scale compare to initiated font size.
     * @param rgb color vector.
     */
    public void addTextString(String text, int x, int y, float scale, Vector3f rgb) {
        float currentX = x;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            CharInfo charInfo = font.getCharacter(c);
            if (charInfo.width() == 0) {
                System.err.println("Unknown character" + c);
                continue;
            }


            addCharacter(currentX, y, scale, charInfo, ColorConverter.fromVectorToHexInt(rgb));

            // Move to next char in string
            currentX += charInfo.width() * scale;
        }
    }

    public void addTextString(String text, int x, int y, float scale, int rgb) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            CharInfo charInfo = font.getCharacter(c);
            if (charInfo.width() == 0) {
                System.err.println("Unknown character" + c);
                continue;
            }

            float xPos = x;

            addCharacter(xPos, y, scale, charInfo, rgb);

            // Move to next char in string
            x += (int) (charInfo.width() * scale);
        }
    }

    public Shader shader() {
        return shader;
    }

    public FontBatch setShader(Shader shader) {
        this.shader = shader;
        return this;
    }

    public Shader sdfShader() {
        return sdfShader;
    }

    public FontBatch setSdfShader(Shader shader) {
        this.sdfShader = shader;
        return this;
    }

    public TCBFont font() {
        return font;
    }

    public FontBatch setFont(TCBFont font) {
        this.font = font;
        return this;
    }
}
