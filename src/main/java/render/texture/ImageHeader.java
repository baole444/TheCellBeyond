package render.texture;

import org.joml.Vector2i;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;
import utility.AssetReference;
import utility.UnifiedPaths;
import utility.log.EngineLog;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.stb.STBImage.stbi_info;
import static org.lwjgl.stb.STBImage.stbi_info_from_memory;

/**
 * ImageHeader is a collection of static methods for reading the image's dimension from its file header.
 * <p>
 * The read operation happen instantly and does not require the image to be decoded, or to be sent to the GPU.
 */
public final class ImageHeader {
    private static final EngineLog Logger = new EngineLog(ImageHeader.class);

    private ImageHeader() {}

    /**
     * Read the dimensions of the image at the given unified path.
     * @param canonicalPath the unified path to the image
     * @return the image's size in pixels, or null when the image cannot be read
     */
    public static Vector2i size(String canonicalPath) {
        if (canonicalPath == null || canonicalPath.isBlank()) return null;
        return size(new AssetReference(canonicalPath));
    }

    /**
     * Read the dimensions of the image at the given reference.
     * @param reference the reference to the image
     * @return the image's size in pixels, or null when the image cannot be read
     */
    public static Vector2i size(AssetReference reference) {
        if (reference == null) return null;
        try (InputStream stream = UnifiedPaths.getAssetStream(reference.resolvedPath())) {
            byte[] data = stream.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(data.length);
            buffer.put(data);
            buffer.flip();
            return size(buffer);
        } catch (IOException e) {
            Logger.warning(String.format("Cannot read image header at '%s': %s", reference.canonicalPath(), e.getMessage()));
            return null;
        }
    }

    /**
     * Read the dimensions of an image already held in memory.
     * @param imageData the image's bytes, positioned at its start
     * @return the image's size in pixels, or null when the header cannot be read
     */
    public static Vector2i size(ByteBuffer imageData) {
        if (imageData == null) return null;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);
            if (!stbi_info_from_memory(imageData, width, height, channels)) return null;
            return new Vector2i(width.get(0), height.get(0));
        }
    }

    /**
     * Read the dimensions of the image at the given system file path.
     * @param filePath the absolute path to the image on disk
     * @return the image's size in pixels, or null when the image cannot be read
     */
    public static Vector2i sizeOfFile(String filePath) {
        if (filePath == null || filePath.isBlank()) return null;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);
            if (!stbi_info(filePath, width, height, channels)) return null;
            return new Vector2i(width.get(0), height.get(0));
        }
    }
}
