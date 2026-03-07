package render.texture;

import static org.lwjgl.opengl.GL11.glDeleteTextures;

/**
 * Command used by engine's render pipeline to dispose a texture safely.
 */
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
            TextureManager.Logger.debug("Disposed OpenGL texture " + handle.getTextureId());
        }
        textureManager.removeActiveHandle(handle.resourceID().id);
        handle.markDisposed();
    }
}
