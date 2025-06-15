package render.texture;

import static org.lwjgl.opengl.GL11.*;

public class CreateFrameBufferTextureCommand extends TextureCommand {
    final TextureHandle handle;
    final int width, height;

    CreateFrameBufferTextureCommand(TextureHandle handle, int width, int height) {
        this.handle = handle;
        this.width = width;
        this.height = height;
    }

    @Override
    void execute() {
        handle.setStatus(TextureHandle.Status.LOADING);
        TextureManager textureManager = TextureManager.get();

        try {
            int textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

            // Generate empty space
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB,
                    width, height,
                    0, GL_RGB, GL_UNSIGNED_BYTE, 0
            );

            handle.setTextureId(textureId);
            handle.setSize(width, height);
            handle.setStatus(TextureHandle.Status.READY);

            textureManager.stepCreatedTexture();

            glBindTexture(GL_TEXTURE_2D, 0);
        } catch (Exception e) {
            handle.setError("FrameBuffer texture creation failed: " + e.getMessage());
            TextureManager.LOGGER.severe("Failed to create FrameBuffer texture:" + e);
        }
    }
}
