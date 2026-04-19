package render;

import TheCellBeyond.internal.ResourceID;
import TheCellBeyond.internal.ResourceStatus;
import org.lwjgl.BufferUtils;
import render.texture.TextureHandle;
import render.texture.TextureManager;
import utility.AssetReference;
import utility.UnifiedPaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Objects;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.stb.STBImage.stbi_info_from_memory;

public class Texture {
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
            this.handle = TextureManager.get().getTextureHandle(buffer, assetReference, RID);
        } catch (IOException e) {
            System.err.println("Failed to load texture: " + assetReference.canonicalPath());
            System.err.println("Cause: " + e.getMessage());
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
        return handle != null ? handle.getStatus() : ResourceStatus.WAITING;
    }

    public String getErrorMessage() {
        return handle != null ? handle.getErrorMsg() : null;
    }

    public String getCanonicalPath() {
        return assetReference != null ? assetReference.canonicalPath() : null;
    }

    public void dispose() {
        if (handle == null) return;
        String canonicalPath = getCanonicalPath();
        TextureManager.get().disposeTexture(handle, canonicalPath);
        handle = null;
    }

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
        if (this.getCanonicalPath() != null && other.getCanonicalPath() != null) {
            return Objects.equals(this.getCanonicalPath(), other.getCanonicalPath());
        }
        return this.RID.id == other.RID.id;
    }

    @Override
    public int hashCode() {
        if (getCanonicalPath() != null) return Objects.hash(getCanonicalPath());
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
