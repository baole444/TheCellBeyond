package render.fontRenderer;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import render.Shader;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.*;

public class FontBatch {
    // 26 english chars + 10 numbers (0 -> 9) + 14 common symbols = 50
    // this * 4 * 10 (account for large amount of text on screen).
    private static final int BATCH_SIZE = 1000;

    // |Position| |    Color   | |Coordinate|
    // |  f, f  | | f, f, f, f | |   f, f   |
    private static final int VERTEX_SIZE = 8;

    // Total vertices in a batch.
    private static final int VERTICES_SIZE = BATCH_SIZE * 4;

    // Total elements in vertex array.
    private static final int VERTEX_ARRAY_SIZE = VERTICES_SIZE * VERTEX_SIZE;

    // 2 tris per quad = 6 indices.
    private static final int INDICES_PER_QUAD = 6;

    // Total indices needed
    private static final int INDICES_SIZE = BATCH_SIZE * INDICES_PER_QUAD;

    // Font and shader
    private Shader shader;
    private TCBFont font;

    // OpenGL buffer
    private int vao;
    private int vbo;
    private int ebo;

    // Vertex data of current batch
    private FloatBuffer vertices;

    // index buffer
    private IntBuffer indices;

    // Current batch state
    private int numQuads = 0;
    private boolean hasRoom = true;

    // Projection matrix;
    private Matrix4f projection;

    private record TextEntry(String text, float x, float y, float scale, Vector4f color) {}

    private List<TextEntry> textEntries = new ArrayList<>();

    public FontBatch(String fontPath, int fontSize, Shader shader) {
        try {
            this.font = new TCBFont(fontPath, fontSize, false);
            this.shader = shader;

            this.projection = new Matrix4f().ortho(0, 800, 0 , 600, -1, 1);

            init();
        } catch (IOException e) {
            System.err.println("Failed to  initialize font batch renderer " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Initialize OpenGL buffers and state
    private void init() {
        vertices = memAllocFloat(VERTEX_ARRAY_SIZE);
        indices = memAllocInt(INDICES_SIZE);

        for (int i = 0; i < BATCH_SIZE; i++) {
            int offsetArrayIndex = i * INDICES_PER_QUAD;
            int offset = i * 4;

            // First triangle
            indices.put(offsetArrayIndex, offset + 1); // top right
            indices.put(offsetArrayIndex + 1, offset + 2); // bottom left
            indices.put(offsetArrayIndex + 2, offset + 3); // bottom right

            // Second triangle
            indices.put(offsetArrayIndex + 3, offset + 1);
            indices.put(offsetArrayIndex + 4, offset + 3);
            indices.put(offsetArrayIndex + 5, offset);
        }

        // Create and bind VAO
        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        // Create and bind VBO
        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, (long) vertices.capacity() * Float.BYTES, GL_DYNAMIC_DRAW);

        // Create and bind EBO
        ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        // Setup vertex attributes
        // Position (x, y)
        glVertexAttribPointer(0, 2, GL_FLOAT, false, VERTEX_SIZE * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        // Color (r, g, b, a)
        glVertexAttribPointer(1, 4, GL_FLOAT, false, VERTEX_SIZE * Float.BYTES, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);

        // Texture coordinate (u, v)
        glVertexAttribPointer(2, 2, GL_FLOAT, false, VERTEX_SIZE * Float.BYTES, 6 * Float.BYTES);

        // Unbind VAO
        glBindVertexArray(0);
    }

    public void addText(String text, float x, float y, float scale, Vector4f color) {
        textEntries.add(new TextEntry(text, x, y, scale, color));
    }

    public void addText(String text, float x, float y, float scale, int hexColor) {
        float r = ((hexColor >> 16) & 0xFF) / 255.0f;
        float g = ((hexColor >> 8) & 0xFF) / 255.0f;
        float b = (hexColor & 0xFF) / 255.0f;
        float a = ((hexColor >> 24) & 0xFF) / 255.0f;
        if (a == 0) a = 1.0f;

        addText(text, x, y, scale, new Vector4f(r, g, b, a));
    }

    public void render() {
        if (textEntries.isEmpty()) return;

        shader.use();
        shader.loadMat4f("uProject", projection);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, font.textureId);
        shader.loadTexture("uFontTex", 0);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);



        for (TextEntry entry : textEntries) {
            renderText(entry.text(), entry.x(), entry.y(), entry.scale(), entry.color());
        }

        if (numQuads > 0) {
            flushBatch();
        }

        textEntries.clear();

        glBindVertexArray(0);
        shader.detach();
    }

    private void renderText(String text, float x, float y, float scale, Vector4f color) {
        float xPos = x;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            CharInfo charInfo = font.getCharacter(c);

            if (charInfo.width() == 0) {
                System.err.println("Unknown character: " + c);
                continue;
            }

            if (numQuads >= BATCH_SIZE) flushBatch();

            float charWidth = charInfo.width() * scale;
            float charHeight = charInfo.height() * scale;

            float x0 = xPos;
            float y0 = y;
            float x1 = xPos + charWidth;
            float y1 = y + charHeight;

            float u0 = charInfo.textureCoordinates[0].x;
            float v0 = charInfo.textureCoordinates[0].y;
            float u1 = charInfo.textureCoordinates[1].x;
            float v1 = charInfo.textureCoordinates[1].y;

            // Calculate vertex buffer offset
            int offset = numQuads * 4 * VERTEX_SIZE;

            // Bottom right vertex
            vertices.put(offset, x1);
            vertices.put(offset + 1, y0);
            vertices.put(offset + 2, color.x);
            vertices.put(offset + 3, color.y);
            vertices.put(offset + 4, color.z);
            vertices.put(offset + 5, color.w);
            vertices.put(offset + 6, u1);
            vertices.put(offset + 7, v0);

            // Top right vertex
            vertices.put(offset + 8, x1);
            vertices.put(offset + 9, y1);
            vertices.put(offset + 10, color.x);
            vertices.put(offset + 11, color.y);
            vertices.put(offset + 12, color.z);
            vertices.put(offset + 13, color.w);
            vertices.put(offset + 14, u1);
            vertices.put(offset + 15, v1);

            // Top left vertex
            vertices.put(offset + 16, x0);
            vertices.put(offset + 17, y1);
            vertices.put(offset + 18, color.x);
            vertices.put(offset + 19, color.y);
            vertices.put(offset + 20, color.z);
            vertices.put(offset + 21, color.w);
            vertices.put(offset + 22, u0);
            vertices.put(offset + 23, v1);

            // Bottom left vertex
            vertices.put(offset + 24, x0);
            vertices.put(offset + 25, y0);
            vertices.put(offset + 26, color.x);
            vertices.put(offset + 27, color.y);
            vertices.put(offset + 28, color.z);
            vertices.put(offset + 29, color.w);
            vertices.put(offset + 30, u0);
            vertices.put(offset + 31, v0);

            // Increment quad count
            numQuads++;

            // Move to the next character position
            xPos += charWidth;
        }
    }

    private void flushBatch() {
        if (numQuads == 0) return;

        // Bind vao
        glBindVertexArray(vao);

        // Upload vertex data
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        vertices.limit(numQuads * 4 * VERTEX_SIZE); // Set limit
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);

        // Draw
        glDrawElements(GL_TRIANGLES, numQuads * INDICES_PER_QUAD, GL_UNSIGNED_INT, 0);

        // Reset batch counter
        numQuads = 0;
        vertices.clear(); // Reset position for next batch
    }

    public void dispose() {
        if (vertices != null) {
            memFree(vertices);
            vertices = null;
        }

        if (indices != null) {
            memFree(indices);
            indices = null;
        }

        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
        glDeleteVertexArrays(vao);
    }

    public void setProjection(Matrix4f projection) {
        this.projection = projection;
    }

    public void setFont(TCBFont font) {
        this.font = font;
    }
}
