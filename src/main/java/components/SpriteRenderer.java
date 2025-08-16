package components;

import editor.ImEditorGui;
import org.joml.Vector2f;
import org.joml.Vector4f;
import render.Texture;
import render.texture.Sprite;
import utility.WorldUnit;

/**
 * A class dedicated to rendering a sprite, and it's life cycle.
 */
public class SpriteRenderer extends SpatialComponent {
    private final Vector4f color = new Vector4f(1, 1, 1 , 1);
    private Sprite sprite = new Sprite();

    private transient boolean isSpriteDirty = true;

    @Override
    protected void additionalImGuiLogic() {
        // TODO: add ability to add new sprite with drag drop target in the future
        if (ImEditorGui.colorCtrl("Color", this.color)) {
            this.isSpriteDirty = true;
        }
    }

    @Override
    protected void additionalDirtyFlagLogic() {
        if (!isSpriteDirty) isSpriteDirty = true;
    }

    public void setSpriteDirty(boolean needsUpdate) {
        this.isSpriteDirty = needsUpdate;
    }

    public Vector4f getColor() {
        return this.color;
    }

    public Vector2f getSpriteSize() {
        if (sprite == null) return new Vector2f(1, 1);

        return new Vector2f(sprite.getWidth(), sprite.getHeight());
    }

    public Vector2f getSpriteSizeAsWorldUnit() {
        return WorldUnit.pixelToWorld(getSpriteSize());
    }

    public Texture getTexture() {
        return sprite != null ? sprite.getTexture() : null;
    }

    public Vector2f[] getTextureCoordinates() {
        return sprite != null ? sprite.getTextureCoordinates() : null;
    }

    public void setSprite(Sprite sprite) {
        this.sprite = sprite;
        isSpriteDirty = true;
    }

    public void setColor(Vector4f color) {
        if(!this.color.equals(color)) {
            isSpriteDirty = true;
            this.color.set(color);
        }
    }

    public boolean isSpriteDirty() {
        return isSpriteDirty;
    }

    public void setTexture(Texture texture) {
        this.sprite.setTexture(texture);
    }
}
