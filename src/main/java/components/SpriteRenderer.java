package components;

import TheCellBeyond.Transform;
import editor.ImEditorGui;
import org.joml.Vector2f;
import org.joml.Vector4f;
import render.Texture;
import render.texture.Sprite;
import utility.Settings;

/**
 * A class dedicated to rendering a sprite, and it's life cycle.
 */
public class SpriteRenderer extends SpatialComponent {
    private final Vector4f color = new Vector4f(1, 1, 1 , 1);
    private Sprite sprite = new Sprite();

    // Caching of effective transform
    private transient Transform instTransform;
    private transient boolean isDirty = true;

    @Override
    public void start() {
        super.start();
        instTransform = new Transform(getEffectiveTransform());
    }

    @Override
    public void editorUpdate(float dt) {
        super.editorUpdate(dt);
        updateInstTransform();
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        updateInstTransform();
    }

    @Override
    public void imgui() {
        super.imgui();

        if (ImEditorGui.colorCtrl("Color", this.color)) {
            this.isDirty = true;
        }
    }

    public void setDirty(boolean needsUpdate) {
        this.isDirty = needsUpdate;
    }

    public Vector4f getColor() {
        return this.color;
    }

    public Vector2f getSpriteSize() {
        if (sprite == null) return new Vector2f(1, 1);

        return new Vector2f(sprite.getWidth(), sprite.getHeight());
    }

    public Vector2f getSpriteSizeAsWorldUnit() {
        return getSpriteSize().mul(Settings.WORLD_SCALE_FACTOR);
    }

    public Texture getTexture() {
        return sprite != null ? sprite.getTexture() : null;
    }

    public Vector2f[] getTextureCoordinates() {
        return sprite != null ? sprite.getTextureCoordinates() : null;
    }

    public void setSprite(Sprite sprite) {
        this.sprite = sprite;
        this.isDirty = true;
    }

    public void setColor(Vector4f color) {
        if(!this.color.equals(color)) {
            this.isDirty = true;
            this.color.set(color);
        }
    }

    public boolean isDirty() {
        return this.isDirty;
    }

    public void setTexture(Texture texture) {
        this.sprite.setTexture(texture);
    }

    private void updateInstTransform() {
        Transform current = getEffectiveTransform();

        if (!current.equals(instTransform)) {
            instTransform.copyFrom(current);
            isDirty = true;
        }
    }
}
