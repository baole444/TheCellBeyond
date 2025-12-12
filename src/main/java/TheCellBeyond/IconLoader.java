package TheCellBeyond;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;
import utility.AssetReference;
import utility.PathResolver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.stb.STBImage.*;

public record IconLoader(int width, int height, ByteBuffer icon, AssetReference assetReference) {
    public String getFilePath() {
        return assetReference != null ? assetReference.canonicalPath() : null;
    }

    public static IconLoader loadIcon(String filepath) {
        try {
            PathResolver.get();
        } catch (IllegalStateException e) {
            PathResolver.initialize(null);
        }

        AssetReference assetRef = new AssetReference(filepath);
        PathResolver resolver = PathResolver.get();

        try (InputStream stream = resolver.getAssetStream(assetRef.resolvedPath())) {
            byte[] data = stream.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(data.length);
            buffer.put(data);
            buffer.flip();

            return loadFromBuffer(buffer, assetRef);
        } catch (IOException e) {
            System.err.println("Failed to load icon: '" + assetRef.canonicalPath() + "': " + e.getMessage());
            return null;
        }
    }

    private static IconLoader loadFromBuffer(ByteBuffer buffer, AssetReference assetReference) {
        ByteBuffer icon;
        int width, height;

        //stbi_set_flip_vertically_on_load(true);

        try (MemoryStack s = MemoryStack.stackPush()) {
            IntBuffer w = s.mallocInt(1);
            IntBuffer h = s.mallocInt(1);
            IntBuffer channels = s.mallocInt(1);

            icon = stbi_load_from_memory(buffer, w, h, channels, 4);

            if (icon == null) {
                System.err.println("Texture failed to load! '" + assetReference.canonicalPath() + "'");
            }

            width = w.get();
            height = h.get();
        }

        return new IconLoader(width, height, icon, assetReference);
    }
}
