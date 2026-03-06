package render;

import TheCellBeyond.internal.ResourceStatus;
import org.lwjgl.BufferUtils;
import render.texture.TextureHandle;
import render.texture.TextureManager;
import utility.AssetReference;
import utility.UnifiedPaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

import static org.lwjgl.opengl.GL11.*;

public class Texture {
    private AssetReference assetReference;
    private transient TextureHandle handle;
    private transient int width, height;

    private transient boolean isSizeInitialized = false;

    public Texture() {
        width = -1;
        height = -1;
    }

    public void init(String filepath) {
        assetReference = new AssetReference(filepath);
        loadTextureData();
    }

    private void loadTextureData() {
        UnifiedPaths resolver = UnifiedPaths.get();
        try (InputStream stream = resolver.getAssetStream(assetReference.resolvedPath())) {
            byte[] data = stream.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(data.length);
            buffer.put(data);
            buffer.flip();
            this.handle = TextureManager.get().getTextureHandle(buffer, assetReference);
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
        if (handle == null) return width;
        if (!isSizeInitialized && handle.isReady()) updateSizeFromHandle();
        return width;
    }

    public int getHeight() {
        checkInitialization();
        if (handle == null) return height;
        if (!isSizeInitialized && handle.isReady()) updateSizeFromHandle();
        return height;
    }

    public int getID() {
        checkInitialization();
        if (handle != null && handle.isReady()) return handle.getTextureId();
        return -1;
    }

    public int getHandleId() {
        if (handle != null) return handle.getHandleId();
        return -1;
    }

    public TextureHandle getHandle() {
        return handle;
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

    private void updateSizeFromHandle() {
        if (handle != null && handle.isReady() && !isSizeInitialized) {
            this.width = handle.getWidth();
            this.height = handle.getHeight();
            this.isSizeInitialized = true;
        }
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
        if (!(obj instanceof Texture objTex)) return false;
        if (this.getCanonicalPath() != null && objTex.getCanonicalPath() != null) {
            return Objects.equals(this.getCanonicalPath(), objTex.getCanonicalPath());
        }
        if (this.handle != null && objTex.handle != null) {
            return this.handle.getHandleId() == objTex.handle.getHandleId();
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (getCanonicalPath() != null) return Objects.hash(getCanonicalPath());
        if (handle != null) return Objects.hash(handle.getHandleId());
        return Objects.hash(width, height);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Texture{");
        if (assetReference != null) {
            builder.append("path=").append(assetReference.canonicalPath()).append("', ");
        }
        builder.append("size=").append(getWidth()).append("x").append(getHeight());
        if (handle != null) {
            builder.append(", status=").append(handle.getStatus());
            builder.append(", handleId=").append(handle.getHandleId());
        }
        builder.append("}");
        return builder.toString();
    }
}
