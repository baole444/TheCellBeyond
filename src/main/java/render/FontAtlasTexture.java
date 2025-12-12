package render;

import render.text.FontManager;
import render.text.GlyphRange;
import render.texture.TextureHandle;
import render.texture.TextureManager;
import utility.AssetReference;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;

public class FontAtlasTexture {
    private AssetReference assetReference;
    private GlyphRange glyphRange;
    private transient TextureHandle handle;
    private transient int width, height, channels;
    private transient boolean isSizeInitialized = false;

    public FontAtlasTexture() {
        width = -1;
        height = -1;
    }

    public void init(String filepath, GlyphRange glyphRange, int width, int height, int channels) {
        assetReference = new AssetReference(filepath);
        this.glyphRange = glyphRange;
        this.width = width;
        this.height = height;
        this.channels = channels;
    }

    private void loadTextureData() {
        if (assetReference == null || glyphRange == null) return;

        ByteBuffer atlasData = FontManager.get().getFontAtlas(assetReference, glyphRange);
        if (atlasData == null) return;

        handle = TextureManager.get().getFontAtlasHandle(atlasData, assetReference, glyphRange, width, height, channels);
    }

    public void bind() {
        checkInitialization();
        int textureId = getID();
        if (textureId > 0) {
            glBindTexture(GL_TEXTURE_2D, textureId);
        }
    }

    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public int getWidth() {
        checkInitialization();
        if (handle == null) return width;

        if (!isSizeInitialized && handle.isReady()) {
            updateSizeFromHandle();
        }

        return width;
    }

    public int getHeight() {
        checkInitialization();
        if (handle == null) return height;

        if (!isSizeInitialized && handle.isReady()) {
            updateSizeFromHandle();
        }

        return height;
    }

    public int getID() {
        checkInitialization();
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
        checkInitialization();
        return handle != null && handle.isReady();
    }

    public boolean isFailed() {
        return handle != null && handle.isFailed();
    }

    public String getCanonicalPath() {
        return assetReference != null ? assetReference.canonicalPath() : null;
    }

    public void dispose() {
        if (handle == null) return;

        String canonicalPath = getCanonicalPath();
        TextureManager.get().disposeFontAtlasTexture(handle, canonicalPath, glyphRange);

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
}
