package render.text;

import TCB_Field.Window;
import components.TextComponent;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import render.Shader;
import utility.AssetsPool;
import utility.Settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class TextBatch implements Comparable<TextBatch> {
    // |Position| |   Color  | |Coordinate|
    // |  f, f  | |f, f, f, f| |   f, f   |
    private static final int POS_SIZE = 2;
    private static final int COLOR_SIZE = 4;
    private static final int TEX_COORD_SIZE = 2;
    private static final int VERTEX_SIZE = POS_SIZE + COLOR_SIZE + TEX_COORD_SIZE;

    private final int zIndex;
    private final int maxBatchSize;
    private final List<TextComponent> textComponents;

    // Map fonts to components
    private final Map<TCBFont, List<TextComponent>> fontGroups;

    private int vaoID, vboID;

    private boolean hasRoom;

    private static Shader shader;

    private Matrix4f projectionMatrix = null;
    private Matrix4f viewMatrix = null;

    public void setProjectionMatrix(Matrix4f projectionMatrix) {
        this.projectionMatrix = projectionMatrix;
    }

    public void setViewMatrix(Matrix4f viewMatrix) {
        this.viewMatrix = viewMatrix;
    }

    public TextBatch(int maxBatchSize, int zIndex) {
        this.maxBatchSize = maxBatchSize;
        this.zIndex = zIndex;
        this.textComponents = new ArrayList<>();
        this.fontGroups = new HashMap<>();
        this.hasRoom = true;

        if (shader == null) {
            shader = AssetsPool.loadShader(Settings.PATH.DEFAULT_FONT_SHADER);
            shader.compile();
        }
    }

    public void start() {
        // create vao
        vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);

        // allocate vbo
        vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, maxBatchSize * 6 * VERTEX_SIZE * Float.BYTES, GL_DYNAMIC_DRAW);

        // Enable vertex attributes
        // Position
        glVertexAttribPointer(0, POS_SIZE, GL_FLOAT, false, VERTEX_SIZE * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        // Color
        glVertexAttribPointer(1, COLOR_SIZE, GL_FLOAT, false, VERTEX_SIZE * Float.BYTES, POS_SIZE * Float.BYTES);
        glEnableVertexAttribArray(1);

        // Texture coordinates
        glVertexAttribPointer(2, TEX_COORD_SIZE, GL_FLOAT, false, VERTEX_SIZE * Float.BYTES, (POS_SIZE + COLOR_SIZE) * Float.BYTES);
        glEnableVertexAttribArray(2);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void add(TextComponent textComponent) {
        if (textComponents.size() >= maxBatchSize) {
            hasRoom = false;
            return;
        }

        textComponents.add(textComponent);

        // Group by font
        TCBFont font = textComponent.getFont();
        if (font != null) {
            fontGroups.computeIfAbsent(font, k -> new ArrayList<>()).add(textComponent);
        }
    }

    public void render() {
        if (textComponents.isEmpty()) return;

        // Check for font changes and component changes.
        for (int i = 0; i < textComponents.size(); i++) {
            TextComponent textComponent = textComponents.get(i);

            // if dirty re-render
            if (textComponent.isDirty()) {

                // regroup if font changed
                regroupComponents();

                textComponent.clearDirty();
            }
        }

        // Update OpenGL state
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        shader.use();

        // Set projection and view matrix
        Matrix4f projMatrix;
        Matrix4f vMatrix;

        if (projectionMatrix != null) {
            projMatrix = projectionMatrix;
        } else projMatrix = Window.getScene().viewport().getProjectMatrix();

        if (viewMatrix != null) {
            vMatrix = viewMatrix;
        } else vMatrix = Window.getScene().viewport().getViewMatrix();

        shader.loadMat4f("uProject", projMatrix);
        shader.loadMat4f("uView", vMatrix);

        glBindVertexArray(vaoID);
        glBindBuffer(GL_ARRAY_BUFFER, vboID);

        // Render per group
        for (Map.Entry<TCBFont, List<TextComponent>> entry : fontGroups.entrySet()) {
            TCBFont font = entry.getKey();
            List<TextComponent> components = entry.getValue();

            // Skip if no components use this font
            if (components.isEmpty()) continue;

            glActiveTexture(GL_TEXTURE0 + 1);
            glBindTexture(GL_TEXTURE_2D, font.getTextureId());
            shader.loadInt("uFontTex", 0);

            // Create vertex data for all text components of this group
            float[] vertices = genVertices(components, font);

            // Upload to GPU
            glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);

            // Draw
            int charCount = countChars(components);
            glDrawArrays(GL_TRIANGLES, 0 , charCount * 6);
        }

        // Cleanup
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        glDisable(GL_BLEND);

        shader.detach();
    }

    private float[] genVertices(List<TextComponent> components, TCBFont font) {
        int charCount = countChars(components);
        float[] vertices = new float[charCount * 6 * VERTEX_SIZE];
        int vertexOffset = 0;

        for (TextComponent textComponent: components) {
            String text = textComponent.getText();
            if (text.isEmpty()) continue;

            Vector2f positon = textComponent.getWorldPosition();
            Vector4f color = textComponent.getColor();
            Vector2f textDimensions = textComponent.getTextDimensions();

            // Alignment offsets
            float xOffset = 0;
            switch (textComponent.getHorizontalAlignment()) {
                case CENTER -> xOffset = - textDimensions.x / 2.0f;
                case RIGHT -> xOffset = - textDimensions.x;
            }

            float yOffset = 0;
            switch (textComponent.getVerticalAlignment()) {
                case MIDDLE -> yOffset = textDimensions.y / 2.0f;
                case BOTTOM -> yOffset = textDimensions.y;
            }

            float x = positon.x + xOffset;
            float y = positon.y - yOffset;
            float initialX = x;

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);

                // move to the next line
                if (c == '\n') {
                    y -= font.getFontSize() * Settings.WORLD_SCALE_FACTOR;
                    x = initialX;
                    continue;
                }

                CharInfo charInfo = font.getCharInfo(c);
                if (charInfo == null) continue;

                float charX = x + charInfo.xOffset() * Settings.WORLD_SCALE_FACTOR;

                float width = (charInfo.x1() - charInfo.x0()) * Settings.WORLD_SCALE_FACTOR;
                float height = (charInfo.y1() - charInfo.y0()) * Settings.WORLD_SCALE_FACTOR;

                float charY = y - charInfo.yOffset() * Settings.WORLD_SCALE_FACTOR - height;

                float texX0 = charInfo.x0() / (float) font.getBitmapWidth();
                float texY0 = charInfo.y0() / (float) font.getBitmapHeight();
                float texX1 = charInfo.x1() / (float) font.getBitmapWidth();
                float texY1 = charInfo.y1() / (float) font.getBitmapHeight();

                //<editor-fold defaultstate="collapsed" desc="Add vertices to 2 tris">
                // First triangle
                // Vertex 1 (bottom-left)
                vertices[vertexOffset++] = charX;
                vertices[vertexOffset++] = charY;
                vertices[vertexOffset++] = color.x;
                vertices[vertexOffset++] = color.y;
                vertices[vertexOffset++] = color.z;
                vertices[vertexOffset++] = color.w;
                vertices[vertexOffset++] = texX0;
                vertices[vertexOffset++] = texY1;

                // Vertex 2 (top-left)
                vertices[vertexOffset++] = charX;
                vertices[vertexOffset++] = charY + height;
                vertices[vertexOffset++] = color.x;
                vertices[vertexOffset++] = color.y;
                vertices[vertexOffset++] = color.z;
                vertices[vertexOffset++] = color.w;
                vertices[vertexOffset++] = texX0;
                vertices[vertexOffset++] = texY0;

                // Vertex 3 (bottom-right)
                vertices[vertexOffset++] = charX + width;
                vertices[vertexOffset++] = charY;
                vertices[vertexOffset++] = color.x;
                vertices[vertexOffset++] = color.y;
                vertices[vertexOffset++] = color.z;
                vertices[vertexOffset++] = color.w;
                vertices[vertexOffset++] = texX1;
                vertices[vertexOffset++] = texY1;

                // Second triangle
                // Vertex 4 (top-left)
                vertices[vertexOffset++] = charX;
                vertices[vertexOffset++] = charY + height;
                vertices[vertexOffset++] = color.x;
                vertices[vertexOffset++] = color.y;
                vertices[vertexOffset++] = color.z;
                vertices[vertexOffset++] = color.w;
                vertices[vertexOffset++] = texX0;
                vertices[vertexOffset++] = texY0;

                // Vertex 5 (top-right)
                vertices[vertexOffset++] = charX + width;
                vertices[vertexOffset++] = charY + height;
                vertices[vertexOffset++] = color.x;
                vertices[vertexOffset++] = color.y;
                vertices[vertexOffset++] = color.z;
                vertices[vertexOffset++] = color.w;
                vertices[vertexOffset++] = texX1;
                vertices[vertexOffset++] = texY0;

                // Vertex 6 (bottom-right)
                vertices[vertexOffset++] = charX + width;
                vertices[vertexOffset++] = charY;
                vertices[vertexOffset++] = color.x;
                vertices[vertexOffset++] = color.y;
                vertices[vertexOffset++] = color.z;
                vertices[vertexOffset++] = color.w;
                vertices[vertexOffset++] = texX1;
                vertices[vertexOffset++] = texY1;
                //</editor-fold>

                // advance cursor position
                x += charInfo.advance() * Settings.WORLD_SCALE_FACTOR;
            }
        }

        return vertices;
    }

    private int countChars(List<TextComponent> components) {
        int count = 0;
        for (TextComponent textComponent : components) {
            String text = textComponent.getText();
            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) != '\n') {
                    count++;
                }
            }
        }

        return count;
    }

    private void regroupComponents() {
        fontGroups.clear();
        for (TextComponent textComponent : textComponents) {
            TCBFont font = textComponent.getFont();

            if (font != null) {
                fontGroups.computeIfAbsent(font, k -> new ArrayList<>()).add(textComponent);
            }
        }
    }

    public boolean removeComponent(TextComponent textComponent) {
        boolean removed = textComponents.remove(textComponent);
        if (removed) {
            regroupComponents();
            if (textComponents.size() < maxBatchSize) {
                hasRoom = true;
            }
        }

        return removed;
    }

    public boolean hasRoom() {
        return hasRoom;
    }

    public int getzIndex() {
        return zIndex;
    }

    @Override
    public int compareTo(TextBatch other) {
        return Integer.compare(this.zIndex, other.zIndex);
    }
}
