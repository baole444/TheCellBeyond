package render.text;

import components.TextRenderer;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import render.RendererState;
import render.Shader;
import utility.AssetsPool;
import utility.Settings;
import utility.WorldUnit;

import java.util.*;

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
    private static final int OBJECT_ID_SIZE = 1;
    private static final int VERTEX_SIZE = POS_SIZE + COLOR_SIZE + TEX_COORD_SIZE + OBJECT_ID_SIZE;

    private final int zIndex;
    private final int maxBatchSize;
    private final List<TextRenderer> textRenderers;

    // Map fonts to components
    private final Map<TCBFont, List<TextRenderer>> fontGroups = new HashMap<>();

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
        this.textRenderers = new ArrayList<>();
        this.hasRoom = true;

        if (shader == null) {
            shader = AssetsPool.loadShader(Settings.PATH.DEFAULT_FONT_SHADER);
        }
    }

    public void start() {
        // create vao
        vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);

        // allocate vbo
        vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, (long) maxBatchSize * 6 * VERTEX_SIZE * Float.BYTES, GL_DYNAMIC_DRAW);

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

        // Object ID
        glVertexAttribPointer(3, OBJECT_ID_SIZE, GL_FLOAT, false, VERTEX_SIZE * Float.BYTES, (POS_SIZE + COLOR_SIZE + TEX_COORD_SIZE) * Float.BYTES);
        glEnableVertexAttribArray(3);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void add(TextRenderer textRenderer) {
        if (textRenderers.size() >= maxBatchSize) {
            hasRoom = false;
            return;
        }

        if (!textRenderers.contains(textRenderer)) {
            textRenderers.add(textRenderer);
            // Group by font
            regroupComponent(textRenderer);
        }
    }

    public void render() {
        if (textRenderers.isEmpty()) return;

        boolean requireRegroup = false;
        for (TextRenderer component : textRenderers) {
            if (component.isDirty()) {
                component.clearDirty();
                requireRegroup = true;
            }
        }

        if (requireRegroup) {
            regroupComponents();
        }

        RendererState state = RendererState.get();
        RendererState.RenderPass currentPass = state.getCurrentPass();

        Shader instShader;
        if (currentPass == RendererState.RenderPass.SELECTION) {
            instShader = state.getCurrentShader();
        } else {
            instShader = shader;
            instShader.use();
            state.enableTextRendering();
        }

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

        glBindVertexArray(vaoID);
        glBindBuffer(GL_ARRAY_BUFFER, vboID);

        // Render per group
        for (Map.Entry<TCBFont, List<TextRenderer>> entry : fontGroups.entrySet()) {
            TCBFont font = entry.getKey();
            List<TextRenderer> components = entry.getValue();

            // Skip if no components use this font
            if (components.isEmpty()) continue;

            // If font is not available, is stilling loading
            if (font == null || !font.isLoaded() || font.waitingTexture()) continue;

            // Skip texture binding during selection pass
            if (currentPass != RendererState.RenderPass.SELECTION) {
                int textureId = font.getTextureId();
                if (textureId < 0) continue;
                glActiveTexture(GL_TEXTURE0);
                glBindTexture(GL_TEXTURE_2D, textureId);
                shader.loadInt("uFontTex", 0);
            }

            // Create vertex data for all text components of this group
            float[] vertices = genVertices(components, font);

            // If no vertices to render
            if (vertices.length == 0) continue;

            // Upload to GPU
            glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);

            // Draw
            int charCount = countChars(components);
            glDrawArrays(GL_TRIANGLES, 0 , charCount * 6);
        }

        // Cleanup
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        // Detach if font shader is used
        if (currentPass != RendererState.RenderPass.SELECTION) {
            shader.detach();
        }
    }

    private float[] genVertices(List<TextRenderer> components, TCBFont font) {
        int charCount = countChars(components);

        if (charCount == 0) return new float[0];

        float[] vertices = new float[charCount * 6 * VERTEX_SIZE];
        int vertexOffset = 0;

        for (TextRenderer textRenderer : components) {
            String text = textRenderer.getText();
            if (text.isEmpty()) continue;

            Vector2f positon = textRenderer.getWorldPosition();
            Vector4f color;
            if (RendererState.get().getCurrentPass() == RendererState.RenderPass.SELECTION) {
                color = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
            } else {
                color = textRenderer.getColor();
            }
            Vector2f textDimensions = textRenderer.getTextDimensions();

            float objectId = 0;
            if (textRenderer.gameObject != null) {
                objectId = textRenderer.gameObject.getUID();
            }

            // Alignment offsets
            float xOffset = 0;
            switch (textRenderer.getHorizontalAlignment()) {
                case CENTER -> xOffset = - textDimensions.x / 2.0f;
                case RIGHT -> xOffset = - textDimensions.x;
            }

            float yOffset = 0;
            switch (textRenderer.getVerticalAlignment()) {
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
                    y -= WorldUnit.pixelToWorld(font.getFontSize());
                    x = initialX;
                    continue;
                }

                CharInfo charInfo = font.getCharInfo(c);
                if (charInfo == null) continue;

                float charX = x + WorldUnit.pixelToWorld(charInfo.xOffset());

                float width = WorldUnit.pixelToWorld(charInfo.x1() - charInfo.x0());
                float height = WorldUnit.pixelToWorld(charInfo.y1() - charInfo.y0());

                float charY = y - WorldUnit.pixelToWorld(charInfo.yOffset()) - height;

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
                vertices[vertexOffset++] = objectId;

                // Vertex 2 (top-left)
                vertices[vertexOffset++] = charX;
                vertices[vertexOffset++] = charY + height;
                vertices[vertexOffset++] = color.x;
                vertices[vertexOffset++] = color.y;
                vertices[vertexOffset++] = color.z;
                vertices[vertexOffset++] = color.w;
                vertices[vertexOffset++] = texX0;
                vertices[vertexOffset++] = texY0;
                vertices[vertexOffset++] = objectId;

                // Vertex 3 (bottom-right)
                vertices[vertexOffset++] = charX + width;
                vertices[vertexOffset++] = charY;
                vertices[vertexOffset++] = color.x;
                vertices[vertexOffset++] = color.y;
                vertices[vertexOffset++] = color.z;
                vertices[vertexOffset++] = color.w;
                vertices[vertexOffset++] = texX1;
                vertices[vertexOffset++] = texY1;
                vertices[vertexOffset++] = objectId;

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
                vertices[vertexOffset++] = objectId;

                // Vertex 5 (top-right)
                vertices[vertexOffset++] = charX + width;
                vertices[vertexOffset++] = charY + height;
                vertices[vertexOffset++] = color.x;
                vertices[vertexOffset++] = color.y;
                vertices[vertexOffset++] = color.z;
                vertices[vertexOffset++] = color.w;
                vertices[vertexOffset++] = texX1;
                vertices[vertexOffset++] = texY0;
                vertices[vertexOffset++] = objectId;

                // Vertex 6 (bottom-right)
                vertices[vertexOffset++] = charX + width;
                vertices[vertexOffset++] = charY;
                vertices[vertexOffset++] = color.x;
                vertices[vertexOffset++] = color.y;
                vertices[vertexOffset++] = color.z;
                vertices[vertexOffset++] = color.w;
                vertices[vertexOffset++] = texX1;
                vertices[vertexOffset++] = texY1;
                vertices[vertexOffset++] = objectId;
                //</editor-fold>

                // advance cursor position
                x += WorldUnit.pixelToWorld(charInfo.advance());
            }
        }

        return vertices;
    }

    private int countChars(List<TextRenderer> components) {
        int count = 0;
        for (TextRenderer textRenderer : components) {
            String text = textRenderer.getText();
            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) != '\n') {
                    count++;
                }
            }
        }

        return count;
    }

    private void regroupComponent(TextRenderer component) {
        TCBFont font = component.getFont();
        if (font != null) {
            List<TextRenderer> components = fontGroups.computeIfAbsent(font, k -> new ArrayList<>());
            if (!components.contains(component)) {
                components.add(component);
            }
        }
    }

    private void regroupComponents() {
        List<TextRenderer> allComponents = new ArrayList<>(textRenderers);

        fontGroups.clear();

        for (TextRenderer component : allComponents) {
            regroupComponent(component);
        }
    }

    public boolean removeComponent(TextRenderer textRenderer) {
        boolean removed = textRenderers.remove(textRenderer);
        if (removed) {
            for (List<TextRenderer> components : fontGroups.values()) {
                components.remove(textRenderer);
            }

            fontGroups.entrySet().removeIf(entry -> entry.getValue().isEmpty());

            if (textRenderers.size() < maxBatchSize) {
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
