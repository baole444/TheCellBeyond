package render.texture;

import utility.AssetReference;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

class CreateFontAtlasTextureCommand extends TextureCommand {
    final TextureHandle handle;
    final ByteBuffer atlasData;
    final AssetReference assetReference;
    final int width;
    final int height;
    final int channels;

    CreateFontAtlasTextureCommand(TextureHandle handle, ByteBuffer atlasData, AssetReference assetReference, int width, int height, int channels) {
        this.handle = handle;
        this.atlasData = atlasData;
        this.assetReference = assetReference;
        this.width = width;
        this.height = height;
        this.channels = channels;
    }

    @Override
    void execute() {
        TextureManager textureManager = TextureManager.get();
        handle.setStatus(TextureHandle.Status.LOADING);
        String canonicalPath = assetReference.getCanonicalPath();

        try {
            int textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);
            int format = channels == 3 ? GL_RGB : GL_RED;
            glTexImage2D(GL_TEXTURE_2D, 0, format, width, height, 0, format, GL_UNSIGNED_BYTE, atlasData);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glGenerateMipmap(GL_TEXTURE_2D);

            handle.setTextureId(textureId);
            handle.setSize(width, height);
            handle.setStatus(TextureHandle.Status.READY);

            textureManager.stepCreatedTexture();
            TextureManager.LOGGER.fine("Created texture with id'" + textureId);
        } catch (Exception e) {
            handle.setError("OpenGL texture creation failed: " + e.getMessage());

            TextureManager.LOGGER.severe("Failed to create texture for " + canonicalPath + e);
        }
    }

    @Override
    public String toString() {
        return "CreateFontAtlasTextureCommand{path=" + assetReference.getCanonicalPath() +
                ", handle=" + handle.getHandleId() +
                "}";
    }
}
