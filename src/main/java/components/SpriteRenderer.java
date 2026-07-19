package components;

import TheCellBeyond.internal.ResourceID;
import org.joml.Vector2f;
import org.joml.Vector4f;
import render.Texture;
import render.commands.RectCommand;
import render.commands.RenderCommand;
import render.texture.Sprite;
import scripting.API;
import utility.WorldUnit;

import java.util.Objects;

/**
 * SpriteRenderer component allow rendering texture via {@link Sprite} in world space, with additional tint colour and transformation.
 * <p>
 * SpriteRenderer marks itself dirty when its sprite, tint colour or transform changed, which will be cleared by the rendering server.
 */
@API
public class SpriteRenderer extends Component2D {
    private final Vector4f color = new Vector4f(1, 1, 1 , 1);
    private volatile Sprite sprite = new Sprite();
    private volatile boolean flipHorizontally = false;
    private volatile boolean flipVertically = false;
    private transient ResourceID lastBuiltTextureRID = null;

    public SpriteRenderer() {
        String name = SpriteRenderer.class.getSimpleName();
        this(name);
    }

    public SpriteRenderer(String name) {
        if (invalidName(name)) name = SpriteRenderer.class.getSimpleName();
        super(name);
    }

    /**
     * Update the dirty flag for this SpriteRenderer.
     * @param needsUpdate true to set sprite dirty
     */
    public void spriteDirty(boolean needsUpdate) {
        renderDirty = needsUpdate;
        if (!needsUpdate && sprite != null) sprite.rendererUpdated();
    }

    /**
     * Get the tint colour that is applied onto the Sprite.
     * @return tint colour vector
     */
    public Vector4f color() {
        return color;
    }

    /**
     * Get the width and height of the Sprite in pixel value.
     * @return size vector of the sprite
     */
    public Vector2f spriteSize() {
        if (sprite == null) return new Vector2f(1.0f);

        return new Vector2f(sprite.getWidth(), sprite.getHeight());
    }

    /**
     * Get the width and height of the Sprite in world unit value.
     * This simply pass {@link SpriteRenderer#spriteSize()} into {@link WorldUnit#pixelToWorld(Vector2f)}
     * @return size vector of the sprite
     */
    public Vector2f spriteSizeAsWorldUnit() {
        return WorldUnit.pixelToWorld(spriteSize());
    }

    /**
     * Get the texture of the Sprite.
     * @return texture reference of the sprite or null if the sprite/texture is null
     */
    public Texture texture() {
        return sprite != null ? sprite.getTexture() : null;
    }

    /**
     * Get the texture coordinates of the Sprite.
     * @return the array of 4 UV corners in the follow order: {@code (1,1), (1,0), (0,0), (0,1)}
     */
    public Vector2f[] textureCoordinates() {
        return sprite != null ? sprite.getTextureCoordinates() : null;
    }

    /**
     * Get the sprite used by this SpriteRenderer.
     * @return the current sprite or null if there is none
     */
    public Sprite sprite() {
        return sprite;
    }

    /**
     * Set the sprite used by this SpriteRenderer.
     * This will trigger sprite dirty flag if the new sprite is different.<br>
     * Set sprite to {@code null} or {@code new Sprite()} will disable rendering of this component.
     * @param sprite the new sprite
     */
    public void sprite(Sprite sprite) {
        if (Objects.equals(this.sprite, sprite)) return;
        this.sprite = sprite;
        renderDirty = true;
    }

    /**
     * Set a new tint color for the Sprite.
     * This will trigger sprite dirty flag if the new color is different.<br>
     * Set the color to {@code (1, 1, 1, 1)} will disable tint color.
     * @param color the new color vector
     */
    public void color(Vector4f color) {
        if(color == null) return;
        if (Objects.equals(this.color, color)) return;
        renderDirty = true;
        this.color.set(color);
    }

    /**
     * Set the horizontal flip status for the Sprite.
     * This will trigger sprite dirty flag if the state changed.<br>
     * This will not affect the original texture.
     * @param flip true to fip the sprite
     */
    public void flipHorizontally(boolean flip) {
        if (flip == flipHorizontally) return;
        flipHorizontally = flip;
        renderDirty = true;
    }

    /**
     * Set the vertical flip status for the Sprite.
     * This will trigger sprite dirty flag if the state changed.<br>
     * This will not affect the original texture.
     * @param flip true to fip the sprite
     */
    public void flipVertically(boolean flip) {
        if (flip == flipVertically) return;
        flipVertically = flip;
        renderDirty = true;
    }

    /**
     * Check if the sprite is dirty or not.
     * <p>
     * If the sprite requesting renderer update or when the sprite is now resolving to a different texture, this flag will be set to true.
     * @return true if the sprite needs update
     */
    public boolean isSpriteDirty() {
        if (sprite != null && sprite.requestRendererUpdate()) renderDirty = true;
        ResourceID currentTextureRID = sprite != null ? sprite.textureRID() : null;
        if (!Objects.equals(currentTextureRID, lastBuiltTextureRID)) renderDirty = true;
        return renderDirty;
    }

    /**
     * Check the horizontal flip status of this SpriteRenderer.
     * @return true if the sprite is flipped vertically
     */
    public boolean flipHorizontally() {
        return flipHorizontally;
    }

    /**
     * Check the vertical flip status of this SpriteRenderer.
     * @return true if the sprite is flipped vertically
     */
    public boolean flipVertically() {
        return flipVertically;
    }

    @Override
    public boolean renderDirty() {
        return isSpriteDirty();
    }

    @Override
    public void renderDirty(boolean dirty) {
        spriteDirty(dirty);
    }

    @Override
    public RenderCommand buildRenderCommand() {
        RectCommand command = RectCommand.acquire();
        command.submitterID = gameObject != null ? gameObject.getUID() : 0;
        ResourceID textureRID = sprite != null ? sprite.textureRID() : null;
        lastBuiltTextureRID = textureRID;
        command.hasTextureReference = sprite != null && sprite.hasTexture();
        command.flipVertically = flipVertically;
        command.flipHorizontally = flipHorizontally;
        command.modulate.set(color());
        command.size.set(spriteSizeAsWorldUnit());
        if (textureRID != null) {
            command.textureRID = textureRID;
            Vector2f[] uvs = textureCoordinates();
            if (uvs != null) {
                for (int i = 0; i < 4; i++) command.uvCoordinates[i].set(uvs[i]);
            }
        }
        RenderCommand renderCommand = super.buildRenderCommand();
        if (renderCommand == null) return command;
        renderCommand.next = command;
        return renderCommand;
    }
}
