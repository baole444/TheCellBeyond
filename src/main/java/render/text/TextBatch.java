package render.text;

import TheCellBeyond.GameObject;
import components.TextRenderer;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import render.RendererState;
import render.Shader;
import utility.AssetManager;
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
    private static final int PositionSize = 2;
    private static final int ColorSize = 4;
    private static final int TextureCoordinateSize = 2;
    private static final int ObjectIdSize = 1;
    private static final int VertexSize = PositionSize + ColorSize + TextureCoordinateSize + ObjectIdSize;

    private final int zIndex;
    private final int maxBatchSize;
    private final List<TextRenderer> textRenderers;

    private final Map<TCBFont, List<TextRenderer>> fontGroups = new HashMap<>();

    private int vaoID, vboID;
    private boolean hasSpace;
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
        this.hasSpace = true;

        if (shader == null) {
            shader = AssetManager.get().getShader(AssetManager.get().loadShader(Settings.ShaderPath.DefaultFontShader));
        }
    }

    public void start() {
        vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);

        vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, (long) maxBatchSize * 6 * VertexSize * Float.BYTES, GL_DYNAMIC_DRAW);

        // Enable vertex attributes
        glVertexAttribPointer(0, PositionSize, GL_FLOAT, false, VertexSize * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, ColorSize, GL_FLOAT, false, VertexSize * Float.BYTES, PositionSize * Float.BYTES);
        glEnableVertexAttribArray(1);

        glVertexAttribPointer(2, TextureCoordinateSize, GL_FLOAT, false, VertexSize * Float.BYTES, (PositionSize + ColorSize) * Float.BYTES);
        glEnableVertexAttribArray(2);

        glVertexAttribPointer(4, ObjectIdSize, GL_FLOAT, false, VertexSize * Float.BYTES, (PositionSize + ColorSize + TextureCoordinateSize) * Float.BYTES);
        glEnableVertexAttribArray(4);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void add(TextRenderer textRenderer) {
        if (textRenderers.size() >= maxBatchSize) {
            hasSpace = false;
            return;
        }
        if (!textRenderers.contains(textRenderer)) {
            textRenderers.add(textRenderer);
            regroupComponent(textRenderer);
        }
    }

    public void render() {
        if (textRenderers.isEmpty()) return;
        boolean requireRegroup = false;
        for (TextRenderer component : textRenderers) {
            if (!component.isTextDirty()) continue;
            component.clearDirty();
            requireRegroup = true;
        }
        if (requireRegroup) regroupComponents();
        Shader instShader = shader;
        if (RendererState.isSelectionPass()) instShader = RendererState.getCurrentShader();
        instShader.use();
        Matrix4f projMatrix;
        Matrix4f vMatrix;
        if (projectionMatrix != null) {
            projMatrix = projectionMatrix;
        } else projMatrix = new Matrix4f().identity();
        if (viewMatrix != null) {
            vMatrix = viewMatrix;
        } else vMatrix = new Matrix4f().identity();
        instShader.loadMat4f("uProject", projMatrix);
        instShader.loadMat4f("uView", vMatrix);
        glBindVertexArray(vaoID);
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        renderFontGroups(instShader);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        if (RendererState.isNormalPass()) instShader.detach();
    }

    private void renderFontGroups(Shader instShader) {
        for (Map.Entry<TCBFont, List<TextRenderer>> entry : fontGroups.entrySet()) {
            TCBFont font = entry.getKey();
            List<TextRenderer> components = entry.getValue();
            if (components.isEmpty()) continue;
            if (font == null || !font.isLoaded()) continue;
            if (RendererState.isNormalPass()) {
                int textureId = font.getTextureID();
                if (textureId < 0) continue;
                glActiveTexture(GL_TEXTURE0);
                glBindTexture(GL_TEXTURE_2D, textureId);
                instShader.loadInt("uFontTex", 0);
            }
            float[] vertices = genVertices(components, font);
            if (vertices.length == 0) continue;
            glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
            int charCount = countChars(components);
            glDrawArrays(GL_TRIANGLES, 0 , charCount * 6);
        }
    }

    private float[] genVertices(List<TextRenderer> components, TCBFont font) {
        int charCount = countChars(components);
        if (charCount == 0) return new float[0];
        float[] vertices = new float[charCount * 6 * VertexSize];
        int vertexOffset = 0;
        for (TextRenderer textRenderer : components) {
            String text = textRenderer.getText();
            if (text.isEmpty()) continue;

            Vector2f positon = textRenderer.globalPosition();
            Vector4f color;
            if (RendererState.isSelectionPass()) {
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
                if (c == '\n') {
                    y -= WorldUnit.pixelToWorld(font.getFontSizePixel());
                    x = initialX;
                    continue;
                }
                CharInfo charInfo = font.getCharInfo(c);
                if (charInfo == null) continue;
                float charX = x - WorldUnit.pixelToWorld((float) charInfo.xOffset());
                float charY = y - WorldUnit.pixelToWorld((float) charInfo.yOffset());
                float width = WorldUnit.pixelToWorld(charInfo.fontSize());
                float height = WorldUnit.pixelToWorld(charInfo.fontSize());
                float texX0 = charInfo.x0() / (float) font.getAtlasWidth();
                float texY0 = charInfo.y0() / (float) font.getAtlasHeight();
                float texX1 = charInfo.x1() / (float) font.getAtlasWidth();
                float texY1 = charInfo.y1() / (float) font.getAtlasHeight();
                float[][] verticesData = {
                        {charX,             charY,          texX0, texY1},
                        {charX,             charY + height, texX0, texY0},
                        {charX + width,     charY,          texX1, texY1},
                        {charX + width,     charY + height, texX1, texY0}
                };
                int[] indices = {0, 1, 2, 1, 3, 2};
                for (int index : indices) {
                    float[] vertexData = verticesData[index];
                    vertices[vertexOffset++] = vertexData[0];
                    vertices[vertexOffset++] = vertexData[1];
                    vertices[vertexOffset++] = color.x;
                    vertices[vertexOffset++] = color.y;
                    vertices[vertexOffset++] = color.z;
                    vertices[vertexOffset++] = color.w;
                    vertices[vertexOffset++] = vertexData[2];
                    vertices[vertexOffset++] = vertexData[3];
                    vertices[vertexOffset++] = objectId;
                }

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

    public boolean removeIfExist(GameObject go) {
        if (go == null) return false;

        List<TextRenderer> trs = go.getComponents(TextRenderer.class);
        if (trs.isEmpty()) return false;

        int removed = 0;
        for (TextRenderer textRenderer : trs) {
            if (removeComponent(textRenderer)) removed++;
        }

        return removed > 0;
    }

    public boolean removeComponent(TextRenderer textRenderer) {
        if (textRenderer == null) return false;

        boolean removed = textRenderers.remove(textRenderer);
        if (removed) {
            for (List<TextRenderer> components : fontGroups.values()) {
                components.remove(textRenderer);
            }

            fontGroups.entrySet().removeIf(entry -> entry.getValue().isEmpty());

            if (textRenderers.size() < maxBatchSize) {
                hasSpace = true;
            }
        }

        return removed;
    }

    public boolean hasSpace() {
        return hasSpace;
    }

    public int getzIndex() {
        return zIndex;
    }

    @Override
    public int compareTo(TextBatch other) {
        return Integer.compare(this.zIndex, other.zIndex);
    }
}
