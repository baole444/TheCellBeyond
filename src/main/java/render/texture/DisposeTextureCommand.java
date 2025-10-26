package render.texture;

import static org.lwjgl.opengl.GL11.glDeleteTextures;

public class DisposeTextureCommand extends TextureCommand {
    final TextureHandle handle;

    DisposeTextureCommand(TextureHandle handle) {
        this.handle = handle;
    }

    @Override
    void execute() {
        TextureManager textureManager = TextureManager.get();

        if (handle.isReady()) {
            glDeleteTextures(handle.getTextureId());
            textureManager.stepDisposedTexture();
            TextureManager.LOGGER.debug("Disposed OpenGL texture " + handle.getTextureId());
        }

        handle.setStatus(TextureHandle.Status.DISPOSED);
        handle.releaseId();
        textureManager.removeActiveHandle(handle.getHandleId());
    }
}
