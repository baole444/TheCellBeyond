package render;

import org.lwjgl.BufferUtils;
import render.texture.TextureHandle;
import render.texture.TextureManager;
import utility.AssetReference;
import utility.PathResolver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

import static org.lwjgl.opengl.GL11.*;

public class Texture {
    private AssetReference assetReference;
    private transient TextureHandle handle;
    private int width, height;

    private transient boolean isSizeInitialized = false;
    private transient boolean isFrameBufferTexture = false;

    public Texture() {
        // Intended to fail if parameter not set
        width = -1;
        height = -1;
    }

    // Used by FrameBuffer
    static Texture createFrameBufferTexture(int width, int height) {
        Texture texture = new Texture();
        texture.assetReference = null;
        texture.width = width;
        texture.height = height;
        texture.isSizeInitialized = true;
        texture.isFrameBufferTexture = true;

        texture.handle = TextureManager.get().createFrameBufferTexture(width, height);

        return texture;
    }

    public void init(String filepath) {
        assetReference = new AssetReference(filepath);

        loadTextureDate();
    }

    private void loadTextureDate() {
        PathResolver resolver = PathResolver.get();

        try (InputStream stream = resolver.getAssetStream(assetReference.getResolvedPath())) {
            byte[] data = stream.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(data.length);
            buffer.put(data);
            buffer.flip();

            this.handle = TextureManager.get().getTextureHandle(buffer, assetReference);
        } catch (IOException e) {
            System.err.println("Failed to load texture: " + assetReference.getCanonicalPath());
            e.printStackTrace();
        }
    }

    public void bind() {
        int textureId = getID();
        if (textureId > 0) {
            glBindTexture(GL_TEXTURE_2D, textureId);
        }
    }

    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public int getWidth() {
        if (handle != null && handle.isReady()) {
            if (this.width != handle.getWidth()) this.width = handle.getWidth();

            return handle.getWidth();
        }

        return this.width;
    }

    public int getHeight() {
        if (handle != null && handle.isReady()) {
            if (this.height != handle.getHeight()) this.height = handle.getHeight();

            return handle.getHeight();
        }

        return this.height;
    }

    public int getID() {
        if (handle != null && handle.isReady()) {
            return handle.getTextureId();
        }

        return -1;
    }

    public int getHandleId() {
        if (handle != null) {
            return handle.getHandleId();
        }

        return -1;
    }

    public TextureHandle getHandle() {
        return handle;
    }

    public boolean isReady() {
        return handle != null && handle.isReady();
    }

    public boolean isFailed() {
        return handle != null && handle.isFailed();
    }

    public TextureHandle.Status getStatus() {
        return handle != null ? handle.getStatus() : TextureHandle.Status.WAITING;
    }

    public String getErrorMessage() {
        return handle != null ? handle.getErrorMsg() : null;
    }

    public String getFilePath() {
        return assetReference != null ? assetReference.getCanonicalPath() : null;
    }

    public void setFilePath(String path) {
        this.assetReference = new AssetReference(path);
    }

    public void dispose() {
        if (handle != null) {
            if (isFrameBufferTexture) {
                TextureManager.get().forceDisposeTexture(handle);
            } else {
                String canonicalPath = getFilePath();
                TextureManager.get().disposeTexture(handle, canonicalPath);
            }

            handle = null;
        }
    }

    private void updateSizeFromHandle() {
        if (handle != null && handle.isReady() && !isSizeInitialized) {
            this.width = handle.getWidth();
            this.height = handle.getHeight();
            this.isSizeInitialized = true;
        }
    }

    public Texture copy() {
        Texture copy = new Texture();

        if (this.assetReference != null) {
            copy.init(assetReference.getCanonicalPath());
        } else if (isSizeInitialized && isFrameBufferTexture) {
            return createFrameBufferTexture(width, height);
        }

        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof Texture objTex)) return false;

        if (this.getFilePath() != null && objTex.getFilePath() != null) {
            return Objects.equals(this.getFilePath(), objTex.getFilePath());
        }

        if (this.handle != null && objTex.handle != null) {
            return this.handle.getHandleId() == objTex.handle.getHandleId();
        }

        return objTex.getWidth() == this.getWidth() &&
                objTex.getHeight() == this.getHeight() &&
                Objects.equals(objTex.getFilePath(), this.getFilePath());
    }

    @Override
    public int hashCode() {
        if (getFilePath() != null) {
            return Objects.hash(getFilePath());
        }

        if (handle != null) {
            return Objects.hash(handle.getHandleId());
        }

        return Objects.hash(width, height);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Texture{");
        if (assetReference != null) {
            builder.append("path=").append(assetReference.getCanonicalPath()).append("', ");
        }

        builder.append("size=").append(getWidth()).append("x").append(getHeight());
        if (handle != null) {
            builder.append(", status=").append(handle.getStatus());
            builder.append(", handleId=").append(handle.getHandleId());
        }

        if (isFrameBufferTexture) {
            builder.append(", type=framebuffer");
        }

        builder.append("}");
        return builder.toString();
    }
}
