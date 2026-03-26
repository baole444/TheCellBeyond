package render;

import TheCellBeyond.internal.ResourceID;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import render.commands.RectCommand;
import render.commands.TransformCommand;
import utility.AssetManager;
import utility.log.EngineLog;

import java.util.*;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class TextureBatch {
    // Vertices
    // |Position| |   Color  | |Coordinate| |TexID| |ObjID|
    // |  f, f  | |f, f, f, f| |   f, f   | |  f  | |  f  |
    private static final int[] TextureSlot = {0, 1, 2, 3, 4, 5, 6, 7};
    private static final int SeenBuffer = 16;
    private static final int DefaultBucketCapacity = 64;
    private static final int PositionSize = 2;
    private static final int ColorSize = 4;
    private static final int TextureCoordinateSize = 2;
    private static final int TextureSlotIdSize = 1;
    private static final int ObjectIdSize = 1;
    private static final int VertexSize = PositionSize + ColorSize + TextureCoordinateSize + TextureSlotIdSize + ObjectIdSize;
    private static final int VerticesPerQuad = 4;
    private static final int IndicesPerQuad = 6;
    private static final int TextureSlotOffset = PositionSize + ColorSize + TextureCoordinateSize;
    private int maxBindingTexture = 8;
    private final TreeMap<Integer, ZBucket> zBuckets = new TreeMap<>();
    private final IdentityHashMap<RectCommand, Integer> commandZIndex = new IdentityHashMap<>();
    private final List<Texture> drawTextures = new ArrayList<>();
    private Matrix4f projectionMatrix;
    private Matrix4f viewMatrix;

    private static class ZBucket {
        final IdentityHashMap<RectCommand, Integer> commandIndex = new IdentityHashMap<>();
        final List<RectCommand> commands = new ArrayList<>();
        final List<TransformCommand> transforms = new ArrayList<>();
        final List<Long> commandVersions = new ArrayList<>();
        final List<Long> transformVersions = new ArrayList<>();
        boolean[] seen = new boolean[0];
        float[] vertices;
        int vaoID, vboID, eboID;
        int capacity;
        boolean bufferDirty = false;
        boolean initialized = false;

        ZBucket(int initialCapacity) {
            capacity = initialCapacity;
            vertices = new float[capacity * VerticesPerQuad * VertexSize];
        }

        void init() {
            if (initialized) return;
            vaoID = glGenVertexArrays();
            glBindVertexArray(vaoID);
            vboID = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vboID);
            glBufferData(GL_ARRAY_BUFFER, (long) vertices.length * Float.BYTES, GL_DYNAMIC_DRAW);
            eboID = glGenBuffers();
            int[] indices = genIndicesForBuffer(capacity);
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);
            int stride = VertexSize * Float.BYTES;
            int offset = 0;
            glVertexAttribPointer(0, PositionSize, GL_FLOAT, false, stride, offset);
            glEnableVertexAttribArray(0);
            offset += PositionSize;
            glVertexAttribPointer(1, ColorSize, GL_FLOAT, false, stride, offset * Float.BYTES);
            glEnableVertexAttribArray(1);
            offset += ColorSize;
            glVertexAttribPointer(2, TextureCoordinateSize, GL_FLOAT, false, stride, offset * Float.BYTES);
            glEnableVertexAttribArray(2);
            offset += TextureCoordinateSize;
            glVertexAttribPointer(3, TextureSlotIdSize, GL_FLOAT, false, stride, offset * Float.BYTES);
            glEnableVertexAttribArray(3);
            offset += TextureSlotIdSize;
            glVertexAttribPointer(4, ObjectIdSize, GL_FLOAT, false, stride, offset * Float.BYTES);
            glEnableVertexAttribArray(4);
            initialized = true;
        }

        void beginFrame() {
            int size = commands.size();
            if (seen.length < size) seen = new boolean[size + SeenBuffer];
            Arrays.fill(seen, 0, size, false);
        }

        List<RectCommand> endFrame() {
            List<RectCommand> removed = new ArrayList<>();
            for (int i = commands.size() - 1; i >= 0; i--) {
                if (seen[i]) continue;
                removed.add(commands.get(i));
                commandIndex.remove(commands.get(i));
                commands.remove(i);
                transforms.remove(i);
                commandVersions.remove(i);
                transformVersions.remove(i);
            }
            if (removed.isEmpty()) return removed;
            commandIndex.clear();
            for (int i = 0; i < commands.size(); i++) commandIndex.put(commands.get(i), i);
            bufferDirty = true;
            return removed;
        }

        public void submit(RectCommand command, TransformCommand transform) {
            Integer index = commandIndex.get(command);
            if (index != null) {
                seen[index] = true;
                if (transforms.get(index) != transform) {
                    transforms.set(index, transform);
                    transformVersions.set(index, transform.version);
                    bufferDirty = true;
                    return;
                }
                if (command.version == commandVersions.get(index) && transform.version == transformVersions.get(index)) return;
                commandVersions.set(index, command.version);
                transformVersions.set(index, transform.version);
                bufferDirty = true;
                return;
            }
            int newIndex = commands.size();
            commands.add(command);
            transforms.add(transform);
            commandVersions.add(command.version);
            transformVersions.add(transform.version);
            commandIndex.put(command, newIndex);
            if (seen.length <= newIndex) seen = Arrays.copyOf(seen, newIndex + SeenBuffer);
            seen[newIndex] = true;
            bufferDirty = true;
        }

        boolean isEmpty() {
            return commands.isEmpty();
        }

        void clear() {
            commandIndex.clear();
            commands.clear();
            transforms.clear();
            commandVersions.clear();
            transformVersions.clear();
            bufferDirty = false;
        }

        void dispose() {
            if (!initialized) return;
            glDeleteBuffers(vboID);
            glDeleteBuffers(eboID);
            glDeleteVertexArrays(vaoID);
            initialized = false;
        }

        void adjustCapacity(int required) {
            if (required <= capacity) return;
            capacity = required + (required >> 1);
            vertices = new float[capacity * VerticesPerQuad * VertexSize];
            glBindBuffer(GL_ARRAY_BUFFER, vboID);
            glBufferData(GL_ARRAY_BUFFER, (long) vertices.length * Float.BYTES, GL_DYNAMIC_DRAW);
            int[] indices = genIndicesForBuffer(capacity);
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);
        }

        void rebuildAllVertices() {
            for (int i = 0; i < commands.size(); i++) genCommandVertexProperties(vertices, i, commands.get(i), transforms.get(i));
        }

        void uploadIfDirty() {
            if (!bufferDirty) return;
            int count = commands.size();
            if (count == 0) return;
            if (!initialized) init();
            adjustCapacity(count);
            rebuildAllVertices();
            glBindBuffer(GL_ARRAY_BUFFER, vboID);
            glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
            bufferDirty = false;
        }
    }

    public TextureBatch() {
        int trueBindingLimit = GL11.glGetInteger(GL_MAX_TEXTURE_IMAGE_UNITS);
        if (maxBindingTexture > trueBindingLimit) {
            EngineLog.debug(TextureBatch.class.getSimpleName(), String.format("GPU only support up to %d binding texture at a time, adjusting...", trueBindingLimit));
            maxBindingTexture = trueBindingLimit;
        }
    }

    public void beginFrame() {
        for (ZBucket bucket : zBuckets.values()) bucket.beginFrame();
    }

    public void endFrame() {
        for (ZBucket bucket : zBuckets.values()) {
            List<RectCommand> removed = bucket.endFrame();
            for (RectCommand command : removed) commandZIndex.remove(command);
        }
        zBuckets.entrySet().removeIf(entry -> {
           ZBucket bucket = entry.getValue();
           if (!bucket.isEmpty()) return false;
           bucket.dispose();
           return true;
        });
    }

    public void submit(RectCommand command, TransformCommand transform) {
        int zIndex= transform.zIndex;
        Integer previousZIndex = commandZIndex.get(command);
        if (previousZIndex != null && previousZIndex == zIndex) {
            zBuckets.get(zIndex).submit(command, transform);
            return;
        }
        ZBucket bucket = zBuckets.computeIfAbsent(zIndex, _ -> new ZBucket(DefaultBucketCapacity));
        bucket.submit(command, transform);
        commandZIndex.put(command, zIndex);
    }

    public void clearSubmitted() {
        for (ZBucket bucket : zBuckets.values()) {
            bucket.clear();
            bucket.dispose();
        }
        zBuckets.clear();
        commandZIndex.clear();
    }

    public void prepareRender(Matrix4f projectionMatrix, Matrix4f viewMatrix) {
        if (zBuckets.isEmpty()) return;
        this.projectionMatrix = projectionMatrix != null ? projectionMatrix : new Matrix4f().identity();
        this.viewMatrix = viewMatrix != null ? viewMatrix : new Matrix4f().identity();
        for (ZBucket bucket : zBuckets.values()) bucket.uploadIfDirty();
    }

    public void renderZIndex(int zIndex) {
        ZBucket bucket = zBuckets.get(zIndex);
        if (bucket == null || bucket.commands.isEmpty()) return;
        int count = bucket.commands.size();
        Shader shader = RendererState.getCurrentShader();
        shader.use();
        shader.loadMat4f("uProject", projectionMatrix);
        shader.loadMat4f("uView", viewMatrix);
        shader.loadIntA("uTex", TextureSlot);
        glBindVertexArray(bucket.vaoID);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        drawTextures.clear();
        int batchStart = 0;
        for (int i = 0; i < count; i++) {
            RectCommand command = bucket.commands.get(i);
            Texture texture = resolveTexture(command.textureRID);
            if (texture != null && !drawTextures.contains(texture)) {
                if (drawTextures.size() >= maxBindingTexture) {
                    flushGroup(batchStart, i);
                    drawTextures.clear();
                    batchStart = i;
                }
                drawTextures.add(texture);
            }
            int slotID = 0;
            if (texture != null) slotID = drawTextures.indexOf(texture) + 1;
            int baseOffset = i * VerticesPerQuad * VertexSize;
            for (int v = 0; v < VerticesPerQuad; v++) bucket.vertices[baseOffset + v * VertexSize + TextureSlotOffset] = slotID;
        }
        glBindBuffer(GL_ARRAY_BUFFER, bucket.vboID);
        glBufferSubData(GL_ARRAY_BUFFER, 0, bucket.vertices);
        flushGroup(batchStart, count);
        drawTextures.forEach(Texture::unbind);
        drawTextures.clear();
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glBindVertexArray(0);
    }

    private void flushGroup(int startIndex, int endIndex) {
        int groupCount = endIndex - startIndex;
        if (groupCount <= 0) return;
        for (int i = 0; i < drawTextures.size(); i++) {
            glActiveTexture(GL_TEXTURE0 + i + 1);
            drawTextures.get(i).bind();
        }
        glDrawElements(GL_TRIANGLES, groupCount * IndicesPerQuad, GL_UNSIGNED_INT, (long) startIndex * IndicesPerQuad * Integer.BYTES);
    }

    private static Texture resolveTexture(ResourceID textureRID) {
        if (textureRID == null) return null;
        return AssetManager.get().getTexture(textureRID);
    }

    private static int[] genIndicesForBuffer(int capacity) {
        int[] elements = new int[IndicesPerQuad * capacity];
        for (int i = 0; i < capacity; i++) {
            int indicesOffset = IndicesPerQuad * i;
            int quadOffset = VerticesPerQuad * i;
            elements[indicesOffset] = quadOffset + 3;
            elements[indicesOffset + 1] = quadOffset + 2;
            elements[indicesOffset + 2] = quadOffset;
            elements[indicesOffset + 3] = quadOffset;
            elements[indicesOffset + 4] = quadOffset + 2;
            elements[indicesOffset + 5] = quadOffset + 1;
        }
        return elements;
    }

    private static void genCommandVertexProperties(float[] vertices, int index, RectCommand command, TransformCommand transform) {
        int offset = index * VerticesPerQuad * VertexSize;
        Vector4f color = command.modulate;
        Vector2f[] uv = command.uvCoordinates;
        boolean hasTexture = command.textureRID != null;
        if (!hasTexture) color = new Vector4f(color.x, color.y, color.z, 0.0f);
        if (command.flipHorizontally || command.flipVertically) {
            Vector2f[] flipped = new Vector2f[VerticesPerQuad];
            for (int i = 0; i < VerticesPerQuad; i++) {
                int source = flipSourceIndex(i, command.flipHorizontally, command.flipVertically);
                flipped[i] = new Vector2f(uv[source]);
            }
            uv = flipped;
        }
        int textureSlotID = 0;
        Vector2f worldSize = command.size;
        Vector2f pos = transform.position;
        Vector2f scale = transform.scale;
        float rotation = transform.rotationDegrees;
        boolean isTransformed = rotation != 0.0f || scale.x != 1.0f || scale.y != 1.0f;
        Matrix4f transformMatrix = new Matrix4f().identity();
        if (isTransformed) {
            transformMatrix.translate(pos.x, pos.y, 0.0f);
            transformMatrix.rotate(Math.toRadians(rotation), 0.0f, 0.0f, 1.0f);
            transformMatrix.scale(worldSize.x * scale.x, worldSize.y * scale.y, 1.0f);
        }
        float xAdd = 0.5f;
        float yAdd = 0.5f;
        int uID = command.submitterID;
        for (int i = 0; i < VerticesPerQuad; i++) {
            switch (i) {
                case 1 -> yAdd = -0.5f;
                case 2 -> xAdd = -0.5f;
                case 3 -> yAdd = 0.5f;
            }
            Vector4f instPos;
            if (isTransformed) instPos = new Vector4f(xAdd, yAdd, 0, 1).mul(transformMatrix);
            else instPos = new Vector4f(
                    pos.x + (xAdd * worldSize.x),
                    pos.y + (yAdd * worldSize.y),
                    0, 1
            );
            vertices[offset] = instPos.x / instPos.w;
            vertices[offset + 1] = instPos.y / instPos.w;
            vertices[offset + 2] = color.x;
            vertices[offset + 3] = color.y;
            vertices[offset + 4] = color.z;
            vertices[offset + 5] = color.w;
            vertices[offset + 6] = uv[i].x;
            vertices[offset + 7] = uv[i].y;
            vertices[offset + 8] = textureSlotID;
            vertices[offset + 9] = uID;
            offset += VertexSize;
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
}
