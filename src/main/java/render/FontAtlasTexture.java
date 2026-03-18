package render;

import TheCellBeyond.internal.ResourceID;
import render.text.FontAtlasLayout;
import render.text.FontManager;
import render.text.GlyphRange;
import render.texture.TextureHandle;
import render.texture.TextureManager;
import utility.AssetReference;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;

public class FontAtlasTexture {
    public final ResourceID RID = new ResourceID(RenderResourceType.FontAtlas);
    private AssetReference assetReference;
    private GlyphRange glyphRange;
    private transient TextureHandle handle;
    private transient int width = -1;
    private transient int height = -1;

    public FontAtlasTexture() {}

    public void init(String filepath, GlyphRange glyphRange) {
        assetReference = new AssetReference(filepath);
        this.glyphRange = glyphRange;
        width = FontAtlasLayout.atlasWidth(glyphRange);
        height = FontAtlasLayout.atlasHeight(glyphRange);
    }

    private void loadTextureData() {
        if (assetReference == null || glyphRange == null) return;
        ByteBuffer atlasData = FontManager.get().getFontAtlas(assetReference, glyphRange);
        if (atlasData == null) return;
        handle = TextureManager.get().getFontAtlasHandle(atlasData, assetReference, glyphRange, width, height, FontAtlasLayout.Channels, RID);
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
        return width;
    }

    public int getHeight() {
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

    public String getCanonicalPath() {
        return assetReference != null ? assetReference.canonicalPath() : null;
    }

    public void dispose() {
        if (handle == null) return;
        TextureManager.get().disposeFontAtlasTexture(handle, getCanonicalPath(), glyphRange);
        handle = null;
    }

    private void checkInitialization() {
        if (assetReference != null && handle == null) loadTextureData();
    }
}
