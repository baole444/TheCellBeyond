package components;

import TheCellBeyond.Transform;
import editor.ImEditorGui;
import org.joml.Vector2f;
import org.joml.Vector4f;
import render.Texture;

/**
 * A class dedicated to rendering a sprite, and it's life cycle.
 */
public class SpriteRenderer extends Component {
    private final Vector4f color = new Vector4f(1, 1, 1 , 1);
    private Sprite sprite = new Sprite();
    private transient Transform instTransform;
    private transient boolean isDirty = true;

    @Override
    public void start() {
        instTransform = this.gameObject.transform.copy();
    }

    @Override
    public void editorUpdate(float dt) {
        if (!instTransform.equals(this.gameObject.transform)) {
            instTransform.copyFrom(this.gameObject.transform);
            isDirty = true;
        }
    }

    @Override
    public void update(float dt) {
        if (!instTransform.equals(this.gameObject.transform)) {
            instTransform.copyFrom(this.gameObject.transform);
            isDirty = true;
        }
    }

    @Override
    public void imgui() {
        if (ImEditorGui.colorCtrl("Color", this.color)) {
            this.isDirty = true;
        }
    }

    public void setDirty(boolean isDamage) {
        this.isDirty = isDamage;
    }

    public Vector4f getColor() {
        return this.color;
    }

    public Texture getTexture() {
        return sprite.getTexture();
    }

    public Vector2f[] getTextureCoordinates() {
        return sprite.getTextureCoordinates();
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
        this.sprite.setTex(texture);
    }

}
