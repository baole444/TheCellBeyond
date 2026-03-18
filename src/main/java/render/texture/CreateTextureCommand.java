package render.texture;

import org.lwjgl.BufferUtils;
import utility.AssetReference;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.stb.STBImage.*;

class CreateTextureCommand extends TextureCommand {
    final TextureHandle handle;
    final ByteBuffer imageData;
    final AssetReference assetReference;

    CreateTextureCommand(TextureHandle handle, ByteBuffer imageData, AssetReference assetReference) {
        this.handle = handle;
        this.imageData = imageData;
        this.assetReference = assetReference;
    }

    @Override
    void execute() {
        TextureManager textureManager = TextureManager.get();
        handle.markLoading();
        String canonicalPath = assetReference.canonicalPath();
        try {
            int textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            stbi_set_flip_vertically_on_load(true);
            IntBuffer width = BufferUtils.createIntBuffer(1);
            IntBuffer height = BufferUtils.createIntBuffer(1);
            IntBuffer channels = BufferUtils.createIntBuffer(1);
            ByteBuffer image = stbi_load_from_memory(imageData, width, height, channels, 0);
            if (image != null ) {
                int w = width.get(0);
                int h = height.get(0);
                int c = channels.get(0);
                if (c == 3) {
                    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, w, h, 0, GL_RGB, GL_UNSIGNED_BYTE, image);
                } else if (c == 4) {
                    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
                } else {
                    throw new RuntimeException("Unsupported channel count: " + c);
                }
                handle.setTextureId(textureId);
                handle.markReady();
                stbi_image_free(image);
                textureManager.stepCreatedTexture();
                TextureManager.Logger.debug("Created texture with id " + textureId + " for " + canonicalPath);
            } else {
                glDeleteTextures(textureId);
                handle.markFailed("Failed to load image data");
                TextureManager.Logger.warning("Failed to load texture from " + canonicalPath);
            }
        } catch (Exception e) {
            handle.markFailed("OpenGL texture creation failed: " + e.getMessage());
            TextureManager.Logger.error("Failed to create texture for " + canonicalPath + "Error: " + e);
        }
    }

    @Override
    public String toString() {
        return String.format("%s{path='%s', RID=%d}", CreateTextureCommand.class.getSimpleName(), assetReference.canonicalPath(), handle.resourceID().id);
    }
}
