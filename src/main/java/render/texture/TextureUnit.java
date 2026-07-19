package render.texture;

import TheCellBeyond.internal.ResourceID;
import TheCellBeyond.internal.ResourceStatus;
import org.joml.Vector2i;
import render.Texture;
import scripting.API;
import utility.AssetManager;
import utility.ResourceTracker;

/**
 * TextureUnit facilitate the texture used by a sprite, allow register callback with Texture Management system.
 * TextureUnit will pass the texture's size and canonical path to the sprite once the texture is ready.
 * @see SpriteSheet calculate multiple sprites from a texture.
 */
@API
public class TextureUnit {
    private Texture texture;
    private final Sprite sprite = new Sprite();
    private transient Vector2i size = null;
    private transient boolean requireCompute = false;
    private transient ResourceTracker tracker;
    private transient int lastHandleId = -1;

    /**
     * Create a texture unit with undetermined texture size.
     * The size of sprite is later updated via TextureHandler using the real size of the image file.
     * @param texture texture of the image file
     */
    public TextureUnit(Texture texture) {
        this.texture = texture;
        textureReadyCheck(texture);
    }

    /**
     * Create a texture unit with determined width and height.
     * The size of sprite is updated using these values.<br>
     * If the given width or height value is 0 or negative,
     * the system will update sprite with TextureHandler reported image size.
     * @param texture texture of the image file
     * @param width desired width of the sprite
     * @param height desired height of the sprite
     */
    public TextureUnit(Texture texture, int width, int height) {
        this.texture = texture;
        if (width > 0 && height > 0) this.size = new Vector2i(width, height);
        textureReadyCheck(texture);
    }

    /**
     * Check if the texture is ready to be use or not, then update tracking accordingly.
     * <p>
     * Sprites are computed after updating, so readied texture end with requirement flag cleared.
     * @param texture the texture to check for
     */
    private void textureReadyCheck(Texture texture) {
        if (texture == null) return;
        int currentId = texture.RID.id;
        if (currentId == lastHandleId) return;
        lastHandleId = currentId;
        if (tracker != null) tracker.cancel();
        tracker = AssetManager.track(texture.RID, this::onTextureStatusChange);
        requireCompute = true;
        if (texture.isReady()) computeSprite();
    }

    private void computeSprite() {
        if (!requireCompute || texture == null || !texture.isReady()) return;
        if (size == null) size = new Vector2i(texture.getWidth(), texture.getHeight());
        sprite.setTexture(texture);
        sprite.setWidth(size.x);
        sprite.setHeight(size.y);
        requireCompute = false;
    }

    public void setTexture(Texture newTexture) {
        setTexture(newTexture, null);
    }

    public void setTexture(Texture newTexture, Vector2i size) {
        if (texture == newTexture) return;
        this.size = null;
        if (size != null && size.x > 0 && size.y > 0) this.size = new Vector2i(size);
        if (tracker != null) {
            tracker.cancel();
            tracker = null;
        }
        lastHandleId = -1;
        texture = newTexture;
        textureReadyCheck(newTexture);
    }

    /**
     * Get the sprite of the image assigned to this TextureUnit.
     * @return sprite of the texture, null if the TextureUnit is not computed
     */
    public Sprite getSprite() {
        if (requireCompute) return null;
        return sprite;
    }

    public void dispose() {
        if (tracker == null) return;
        tracker.cancel();
        tracker = null;
    }

    public void onTextureStatusChange(ResourceID RID, ResourceStatus status) {
        if (texture == null) return;
        switch (status) {
            case Ready -> {
                if (requireCompute) computeSprite();
            }
            case Disposed, Failed -> {
                requireCompute = false;
                lastHandleId = -1;
                tracker = null;
            }
        }
    }
}
