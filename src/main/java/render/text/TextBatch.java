package render.text;

import TheCellBeyond.internal.RenderingServer;
import TheCellBeyond.internal.ResourceID;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import render.FontAtlasTexture;
import render.RendererState;
import render.Shader;
import render.commands.TextCommand;
import render.commands.TransformCommand;
import utility.AssetManager;
import utility.Settings;
import utility.WorldUnit;

import java.util.*;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class TextBatch {
    // |Position| |   Color  | |Coordinate|
    // |  f, f  | |f, f, f, f| |   f, f   |
    private static final int PositionSize = 2;
    private static final int ColorSize = 4;
    private static final int TextureCoordinateSize = 2;
    private static final int ObjectIdSize = 1;
    private static final int VertexSize = PositionSize + ColorSize + TextureCoordinateSize + ObjectIdSize;
    private static final int VerticesPerChar = 6;
    private static final int ColorOffset = PositionSize;

    private static class CachedTextData {
        TransformCommand transform;
        TransformCommand previousTransform;
        long commandVersion;
        long transformVersion;
        float[] vertices = new float[0];
        boolean dirty = true;
        boolean seen = false;
    }

    private final IdentityHashMap<TextCommand, CachedTextData> commandCache = new IdentityHashMap<>();
    private final IdentityHashMap<TextCommand, Integer> commandZIndex = new IdentityHashMap<>();
    private final TreeMap<Integer, List<TextCommand>> zBuckets = new TreeMap<>();

    private int vaoID, vboID;
    private int bufferCapacity;
    private boolean initialized = false;
    private static Shader fontShader;
    private Matrix4f projectionMatrix;
    private Matrix4f viewMatrix;
    private float lastInterpolationFactor = 1.0f;

    public TextBatch(int maxBatchSize) {
        bufferCapacity = maxBatchSize;
        if (fontShader == null) fontShader = AssetManager.getShader(AssetManager.loadShader(Settings.ShaderPath.DefaultFontShader));
    }

    public void init() {
        if (initialized) return;
        vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);
        vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        int stride = VertexSize * Float.BYTES;
        glBufferData(GL_ARRAY_BUFFER, (long) bufferCapacity * VerticesPerChar * stride, GL_DYNAMIC_DRAW);
        glVertexAttribPointer(0, PositionSize, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, ColorSize, GL_FLOAT, false, stride, PositionSize * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, TextureCoordinateSize, GL_FLOAT, false, stride, (PositionSize + ColorSize) * Float.BYTES);
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(4, ObjectIdSize, GL_FLOAT, false, stride, (PositionSize + ColorSize + TextureCoordinateSize) * Float.BYTES);
        glEnableVertexAttribArray(4);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        initialized = true;
    }

    public void beginFrame() {
        for (CachedTextData data : commandCache.values()) data.seen = false;
    }

    public void endFrame() {
        Iterator<Map.Entry<TextCommand, CachedTextData>> iterator = commandCache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<TextCommand, CachedTextData> entry = iterator.next();
            if (entry.getValue().seen) continue;
            TextCommand command = entry.getKey();
            Integer zIndex = commandZIndex.remove(command);
            if (zIndex != null) {
                List<TextCommand> bucket = zBuckets.get(zIndex);
                if (bucket != null) bucket.remove(command);
            }
            iterator.remove();
        }
        zBuckets.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public void submit(TextCommand command, TransformCommand transform, TransformCommand previousTransform) {
        int zIndex = transform.zIndex;
        CachedTextData cached = commandCache.get(command);
        if (cached != null) {
            cached.seen = true;
            cached.previousTransform = previousTransform;
            Integer previousZIndex = commandZIndex.get(command);
            if (previousZIndex != null && previousZIndex != zIndex) {
                List<TextCommand> oldBucket = zBuckets.get(previousZIndex);
                if (oldBucket != null) oldBucket.remove(command);
                zBuckets.computeIfAbsent(zIndex, _ -> new ArrayList<>()).add(command);
                commandZIndex.put(command, zIndex);
            }
            if (cached.transform != transform) {
                cached.transform = transform;
                cached.transformVersion = transform.version;
                cached.dirty = true;
                return;
            }
            if (command.version == cached.commandVersion && transform.version == cached.transformVersion) return;
            cached.commandVersion = command.version;
            cached.transformVersion = transform.version;
            cached.dirty = true;
            return;
        }
        cached = new CachedTextData();
        cached.transform = transform;
        cached.previousTransform = previousTransform;
        cached.commandVersion = command.version;
        cached.transformVersion = transform.version;
        cached.seen = true;
        commandCache.put(command, cached);
        commandZIndex.put(command, zIndex);
        zBuckets.computeIfAbsent(zIndex, _ -> new ArrayList<>()).add(command);
    }

    public void clearSubmitted() {
        commandCache.clear();
        commandZIndex.clear();
        zBuckets.clear();
    }

    public void prepareRender(Matrix4f projectionMatrix, Matrix4f viewMatrix) {
        if (commandCache.isEmpty()) return;
        if (!initialized) init();
        this.projectionMatrix = projectionMatrix != null ? projectionMatrix : new Matrix4f().identity();
        this.viewMatrix = viewMatrix != null ? viewMatrix : new Matrix4f().identity();
        float currentAlpha = RenderingServer.interpolationFactor;
        if (currentAlpha == lastInterpolationFactor) return;
        for (CachedTextData cached : commandCache.values()) {
            if (cached.previousTransform != null) cached.dirty = true;
        }
        lastInterpolationFactor = currentAlpha;
    }

    public void renderZIndex(int zIndex) {
        List<TextCommand> bucket = zBuckets.get(zIndex);
        if (bucket == null || bucket.isEmpty()) return;
        boolean selectionPass = RendererState.isSelectionPass();
        Shader instShader = selectionPass ? RendererState.getCurrentShader() : fontShader;
        instShader.use();
        instShader.loadMat4f("uProject", projectionMatrix);
        instShader.loadMat4f("uView", viewMatrix);
        glBindVertexArray(vaoID);
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        Map<ResourceID,  List<TextCommand>> fontGroups = new LinkedHashMap<>();
        for (TextCommand command : bucket) {
            if (command.fontRID == null || command.text == null || command.text.isEmpty()) continue;
            fontGroups.computeIfAbsent(command.fontRID, _ -> new ArrayList<>()).add(command);
        }
        if (fontGroups.isEmpty()) {
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0);
            return;
        }
        renderFontGroups(instShader, fontGroups, selectionPass);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    private void renderFontGroups(Shader instShader, Map<ResourceID, List<TextCommand>> fontGroups, boolean selectionPass) {
        for (Map.Entry<ResourceID, List<TextCommand>> entry : fontGroups.entrySet()) {
            ResourceID fontRID = entry.getKey();
            List<TextCommand> commands = entry.getValue();
            TCBFont font = AssetManager.getFont(fontRID);
            if (font == null || !font.loaded()) continue;
            if (!selectionPass) {
                ResourceID atlasRID = font.atlasRID();
                if (atlasRID == null) continue;
                FontAtlasTexture atlas = AssetManager.getFontAtlas(atlasRID);
                if (atlas == null || !atlas.isReady()) continue;
                glActiveTexture(GL_TEXTURE0);
                glBindTexture(GL_TEXTURE_2D, atlas.getID());
                instShader.loadInt("uFontTex", 0);
            }
            int totalLength = 0;
            int totalChars = 0;
            for (TextCommand command : commands) {
                CachedTextData cached = commandCache.get(command);
                if (cached.dirty) {
                    cached.vertices = genCommandVertices(command, cached.transform, cached.previousTransform, font);
                    cached.dirty = false;
                }
                totalLength += cached.vertices.length;
                totalChars += countChars(command.text);
            }
            if (totalLength == 0) continue;
            float[] combined = new float[totalLength];
            linkCachedVertexArray(commands, combined);
            overrideSelectionColor(selectionPass, combined);
            adjustBufferCapacity(totalLength);
            glBufferSubData(GL_ARRAY_BUFFER, 0, combined);
            glDrawArrays(GL_TRIANGLES, 0, totalChars * VerticesPerChar);
        }
    }

    private static void overrideSelectionColor(boolean selectionPass, float[] combined) {
        if (!selectionPass) return;
        for (int i = ColorOffset; i < combined.length; i += VertexSize) {
            combined[i] = 1.0f;
            combined[i + 1] = 1.0f;
            combined[i + 2] = 1.0f;
            combined[i + 3] = 1.0f;
        }
    }

    private void linkCachedVertexArray(List<TextCommand> commands, float[] combined) {
        int offset = 0;
        for (TextCommand command : commands) {
            float[] vertices = commandCache.get(command).vertices;
            System.arraycopy(vertices, 0, combined, offset, vertices.length);
            offset += vertices.length;
        }
    }

    private float[] genCommandVertices(TextCommand command, TransformCommand transform, TransformCommand previousTransform, TCBFont font) {
        int charCount = countChars(command.text);
        if (charCount == 0) return new float[0];
        float[] vertices = new float[charCount * VerticesPerChar * VertexSize];
        int vertexOffset = 0;
        float alpha = RenderingServer.interpolationFactor;
        float posX, posY;
        if (previousTransform != null && alpha < 1.0f) {
            posX = previousTransform.position.x + alpha * (transform.position.x - previousTransform.position.x);
            posY = previousTransform.position.y + alpha * (transform.position.y - previousTransform.position.y);
        } else {
            posX = transform.position.x;
            posY = transform.position.y;
        }
        Vector2f position = transform.position;
        Vector4f color = command.modulate;
        Vector2f textDimension = command.textDimension;
        int objectID = command.submitterID;
        float xOffset = 0.0f;
        if (command.horizontalAlignment != null) {
            switch (command.horizontalAlignment) {
                case Centre -> xOffset = -textDimension.x / 2.0f;
                case Right -> xOffset = - textDimension.x;
            }
        }
        float yOffset = 0.0f;
        if (command.verticalAlignment != null) {
            switch (command.verticalAlignment) {
                case Middle -> yOffset = -textDimension.y / 2.0f;
                case Bottom -> yOffset = -textDimension.y;
            }
        }
        float x = posX + xOffset;
        float y = posY+ yOffset;
        float initialX = x;
        String text = command.text != null ? command.text : "";
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                y -= WorldUnit.pixelToWorld(font.fontSizePixels());
                x = initialX;
                continue;
            }
            CharUV uv = font.charUV(c);
            CharMetric metric = font.charMetric(c);
            if (uv == null || metric == null) continue;
            float charX = x - WorldUnit.pixelToWorld((float) uv.xOffset());
            float charY = y - WorldUnit.pixelToWorld((float) uv.yOffset());
            float size = WorldUnit.pixelToWorld(metric.fontSize());
            float texX0 = uv.x0() / (float) font.atlasWidth;
            float texY0 = uv.y0() / (float) font.atlasHeight;
            float texX1 = uv.x1() / (float) font.atlasWidth;
            float texY1 = uv.y1() / (float) font.atlasHeight;
            float[][] verticesData = {
                    {charX,         charY,          texX0,  texY1},
                    {charX,         charY + size,   texX0,  texY0},
                    {charX + size,  charY,          texX1,  texY1},
                    {charX + size,  charY + size,   texX1,  texY0}
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
                vertices[vertexOffset++] = objectID;
            }
            x += WorldUnit.pixelToWorld(metric.advance());
        }
        return vertices;
    }

    private int countChars(String text) {
        if (text == null || text.isEmpty()) return 0;
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) != '\n') count++;
        }
        return count;
    }

    private void adjustBufferCapacity(int requiredFloats) {
        int currentCap = bufferCapacity * VerticesPerChar * VertexSize;
        if (requiredFloats <= currentCap) return;
        bufferCapacity = (requiredFloats / (VerticesPerChar * VertexSize)) + 64;
        glBufferData(GL_ARRAY_BUFFER, (long) bufferCapacity * VerticesPerChar * VertexSize * Float.BYTES, GL_DYNAMIC_DRAW);
    }
}
