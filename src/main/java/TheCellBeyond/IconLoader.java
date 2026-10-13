package TheCellBeyond;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;
import utility.AssetReference;
import utility.UnifiedPaths;
import utility.log.EngineLog;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.stb.STBImage.stbi_load_from_memory;
import static org.lwjgl.stb.STBImage.stbi_set_flip_vertically_on_load;

public record IconLoader(int width, int height, ByteBuffer icon, AssetReference assetReference) {
    private static final EngineLog Logger = new EngineLog(IconLoader.class);
    public String filePath() {
        return assetReference != null ? assetReference.canonicalPath() : null;
    }

    public static IconLoader loadIcon(String filepath) {
        AssetReference assetRef = new AssetReference(filepath);
        try (InputStream stream = UnifiedPaths.getAssetStream(assetRef.resolvedPath())) {
            byte[] data = stream.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(data.length);
            buffer.put(data);
            buffer.flip();
            return loadFromBuffer(buffer, assetRef);
        } catch (IOException e) {
            Logger.error(String.format("Cannot load icon '%s': %s", assetRef.canonicalPath(),e.getMessage()));
            return null;
        }
    }

    private static IconLoader loadFromBuffer(ByteBuffer buffer, AssetReference assetReference) {
        ByteBuffer icon;
        final int colorChannels = 4;
        int width, height;
        try (MemoryStack s = MemoryStack.stackPush()) {
            IntBuffer w = s.mallocInt(1);
            IntBuffer h = s.mallocInt(1);
            IntBuffer channels = s.mallocInt(1);
            stbi_set_flip_vertically_on_load(false);
            icon = stbi_load_from_memory(buffer, w, h, channels, colorChannels);
            if (icon == null) {
                Logger.warning(String.format("Cannot read image from '%s'", assetReference.canonicalPath()));
                return null;
            }
            width = w.get();
            height = h.get();
        }
        return new IconLoader(width, height, icon, assetReference);
    }
}
