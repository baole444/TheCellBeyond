package render;

import TheCellBeyond.GameObject;
import TheCellBeyond.internal.ResourceID;
import components.Component;
import components.SpriteRenderer;
import editor.components.EditorObjectIndicator;
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
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class TextureBatch {
    // Vertices
    // |Position| |   Color  | |Coordinate| |TexID|
    // |  f, f  | |f, f, f, f| |   f, f   | |  f  |
    private static final int[] TextureSlot = {0, 1, 2, 3, 4, 5, 6, 7};
    private static final int SeenBuffer = 16;
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
    private final IdentityHashMap<RectCommand, Integer> commandIndex = new IdentityHashMap<>();
    private final ArrayList<RectCommand> registeredCommands = new ArrayList<>();
    private final ArrayList<TransformCommand> registeredTransforms = new ArrayList<>();
    private boolean[] seen = new boolean[0];
    private boolean bufferDirty = false;
    private int vaoID, vboID, eboID;
    private float[] vertices;
    private int bufferCapacity;
    private boolean initialized = false;
    private final List<Texture> drawTextures = new ArrayList<>();

    public TextureBatch(int maxBatchingSize) {
        bufferCapacity = maxBatchingSize;
        int trueBindingLimit = GL11.glGetInteger(GL_MAX_TEXTURE_IMAGE_UNITS);
        if (maxBindingTexture > trueBindingLimit) {
            EngineLog.debug(TextureBatch.class.getSimpleName(), String.format("GPU only support up to %d binding texture at a time, adjusting...", trueBindingLimit));
            maxBindingTexture = trueBindingLimit;
        }
    }

    public void init() {
        if (initialized) return;
        vertices = new float[bufferCapacity * VerticesPerQuad * VertexSize];
        vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);
        vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, (long) vertices.length * Float.BYTES, GL_DYNAMIC_DRAW);
        eboID = glGenBuffers();
        int[] indices = genIndicesForBuffer(bufferCapacity);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);
        int stride = VertexSize * Float.BYTES;
        int offset = 0;
        glVertexAttribPointer(0, PositionSize, GL_FLOAT, false, stride, offset);
        glEnableVertexAttribArray(0);
        offset += PositionSize;
        glVertexAttribPointer(1, ColorSize, GL_FLOAT, false, stride, (long) offset * Float.BYTES);
        glEnableVertexAttribArray(1);
        offset += ColorSize;
        glVertexAttribPointer(2, TextureCoordinateSize, GL_FLOAT, false, stride, (long) offset * Float.BYTES);
        glEnableVertexAttribArray(2);
        offset += TextureCoordinateSize;
        glVertexAttribPointer(3, TextureSlotIdSize, GL_FLOAT, false, stride, (long) offset * Float.BYTES);
        glEnableVertexAttribArray(3);
        offset += TextureSlotIdSize;
        glVertexAttribPointer(4, ObjectIdSize, GL_FLOAT, false, stride, (long) offset * Float.BYTES);
        glEnableVertexAttribArray(4);
        initialized = true;
    }

    public void beginFrame() {
        int size = registeredCommands.size();
        if (seen.length < size) seen = new boolean[size + SeenBuffer];
        Arrays.fill(seen, 0, size, false);
    }

    public void endFrame() {
        boolean removed = false;
        for (int i = registeredCommands.size() - 1; i >= 0; i--) {
            if (seen[i]) continue;
            commandIndex.remove(registeredCommands.get(i));
            registeredCommands.remove(i);
            registeredTransforms.remove(i);
            removed = true;
        }
        if (!removed) return;
        commandIndex.clear();
        for (int i = 0; i < registeredCommands.size(); i++) commandIndex.put(registeredCommands.get(i), i);
        bufferDirty = true;
    }

    public void submit(RectCommand command, TransformCommand transform) {
        Integer index = commandIndex.get(command);
        if (index != null) {
            seen[index] = true;
            if (registeredTransforms.get(index) != transform) {
                registeredTransforms.set(index, transform);
                bufferDirty = true;
            }
            return;
        }
        int newIndex = registeredCommands.size();
        registeredCommands.add(command);
        registeredTransforms.add(transform);
        commandIndex.put(command, newIndex);
        if (seen.length <= newIndex) seen = Arrays.copyOf(seen, newIndex + SeenBuffer);
        seen[newIndex] = true;
        bufferDirty = true;
    }

    public void clearSubmitted() {
        commandIndex.clear();
        registeredCommands.clear();
        registeredTransforms.clear();
        bufferDirty = false;
    }

    public void render(Matrix4f projectionMatrix, Matrix4f viewMatrix) {
        int count = registeredCommands.size();
        if (count == 0) return;
        if (!initialized) init();
        if (bufferDirty) {
            adjustBufferCapacity(count);
            rebuildAllVertices();
            glBindBuffer(GL_ARRAY_BUFFER, vboID);
            glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
            bufferDirty = false;
        }
        Shader shader = RendererState.getCurrentShader();
        shader.use();
        if (projectionMatrix == null) projectionMatrix = new Matrix4f().identity();
        if (viewMatrix == null) viewMatrix = new Matrix4f().identity();
        shader.loadMat4f("uProject", projectionMatrix);
        shader.loadMat4f("uView", viewMatrix);
        shader.loadIntA("uTex", TextureSlot);
        glBindVertexArray(vaoID);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        drawTextures.clear();
        int batchStart = 0;
        for (int i = 0; i < count; i++) {
            RectCommand command = registeredCommands.get(i);
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
            for (int v = 0; v < VerticesPerQuad; v++) vertices[baseOffset + v * VertexSize + TextureSlotOffset] = slotID;
        }
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
        flushGroup(batchStart, count);
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glBindVertexArray(0);
        drawTextures.forEach(Texture::unbind);
        shader.detach();
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

    private void adjustBufferCapacity(int required) {
        if (required <= bufferCapacity) return;
        bufferCapacity = required + (required >> 1);
        vertices = new float[bufferCapacity * VerticesPerQuad * VertexSize];
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, (long) vertices.length * Float.BYTES, GL_DYNAMIC_DRAW);
        int[] indices = genIndicesForBuffer(bufferCapacity);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);
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

    private void rebuildAllVertices() {
        for (int i = 0; i < registeredCommands.size(); i++) genCommandVertexProperties(i);
    }

    private void genCommandVertexProperties(int index) {
        RectCommand command = registeredCommands.get(index);
        TransformCommand transform = registeredTransforms.get(index);
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
