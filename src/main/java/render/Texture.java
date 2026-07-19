package render;

import TheCellBeyond.internal.ResourceID;
import TheCellBeyond.internal.ResourceStatus;
import org.lwjgl.BufferUtils;
import render.texture.TextureHandle;
import render.texture.TextureManager;
import utility.AssetReference;
import utility.UnifiedPaths;
import utility.log.EngineLog;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Objects;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.stb.STBImage.stbi_info_from_memory;

/**
 * Texture is a native GPU resource represent the corresponding texture image on disk.
 * <p>
 * For each unique texture image loaded from disk, there will only be one texture resource for each image.
 * The resources are shared and can be used by various consumers.
 */
public class Texture {
    private static final EngineLog Logger = new EngineLog(Texture.class);
    public final ResourceID RID = new ResourceID(RenderResourceType.Texture);
    private AssetReference assetReference;
    private transient TextureHandle handle;
    private transient int width = -1; 
    private transient int height = -1;

    public Texture() {}

    public void init(String filepath) {
        assetReference = new AssetReference(filepath);
        loadTextureData();
    }

    /**
     * Load the texture data. When a load failed, it is terminal with failed handle status.
     */
    private void loadTextureData() {
        try (InputStream stream = UnifiedPaths.getAssetStream(assetReference.resolvedPath())) {
            byte[] data = stream.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(data.length);
            buffer.put(data);
            buffer.flip();
            IntBuffer wBuffer = BufferUtils.createIntBuffer(1);
            IntBuffer hBuffer = BufferUtils.createIntBuffer(1);
            IntBuffer cBuffer = BufferUtils.createIntBuffer(1);
            if (stbi_info_from_memory(buffer, wBuffer, hBuffer, cBuffer)) {
                width = wBuffer.get(0);
                height = hBuffer.get(0);
            }
            handle = TextureManager.get().getTextureHandle(buffer, assetReference, RID);
        } catch (IOException e) {
            handle = TextureManager.get().failedHandle(RID, "Failed to read texture file:" + e.getMessage());
            Logger.error(String.format("Failed to load texture %s: %s", assetReference.canonicalPath(), e.getMessage()));
        }
    }

    public void bind() {
        checkInitialization();
        int textureId = getID();
        if (textureId > 0) glBindTexture(GL_TEXTURE_2D, textureId);
    }

    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public int getWidth() {
        checkInitialization();
        return width;
    }

    public int getHeight() {
        checkInitialization();
        return height;
    }

    public int getID() {
        checkInitialization();
        if (handle != null && handle.isReady()) return handle.getTextureId();
        return -1;
    }

    public boolean isReady() {
        checkInitialization();
        return handle != null && handle.isReady();
    }

    public boolean isFailed() {
        return handle != null && handle.isFailed();
    }

    public ResourceStatus getStatus() {
        return handle != null ? handle.getStatus() : ResourceStatus.Waiting;
    }

    public String errorMessage() {
        return handle != null ? handle.getErrorMsg() : null;
    }

    public String canonicalPath() {
        return assetReference != null ? assetReference.canonicalPath() : null;
    }

    public void dispose() {
        if (handle == null) return;
        TextureManager.get().disposeTexture(handle, canonicalPath());
    }

    /**
     * Check for the texture initialization state and load the texture if it was never loaded.
     * <p>
     * A null handle mean the texture was never loaded. If the load process failed, the texture will receive a terminally failed handle instead.
     */
    private void checkInitialization() {
        if (assetReference != null && handle == null) loadTextureData();
    }

    public Texture copy() {
        Texture copy = new Texture();
        if (this.assetReference != null) copy.init(assetReference.canonicalPath());
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof Texture other)) return false;
        if (this.canonicalPath() != null && other.canonicalPath() != null) {
            return Objects.equals(this.canonicalPath(), other.canonicalPath());
        }
        return this.RID.id == other.RID.id;
    }

    @Override
    public int hashCode() {
        if (canonicalPath() != null) return Objects.hash(canonicalPath());
        return Objects.hash(RID.id);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Texture{");
        if (assetReference != null) {
            builder.append("path=").append(assetReference.canonicalPath()).append("', ");
        }
        builder.append("size=").append(width).append("x").append(height);
        if (handle != null) {
            builder.append(", status=").append(handle.getStatus());
        }
        builder.append(", RID=").append(RID.id);
        builder.append("}");
        return builder.toString();
    }
}
