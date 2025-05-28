package components;

import TheCellBeyond.Transform;
import editor.ImEditorGui;
import org.joml.Vector2f;
import org.joml.Vector4f;
import render.Texture;

/**
 * A class dedicated to rendering a sprite, and it's life cycle.
 */
public class SpriteRender extends Component {

    private final Vector4f color = new Vector4f(1, 1, 1 , 1);
    private Sprite sprite = new Sprite();
    private transient Transform lastT;
    private transient boolean isDamage = true;

    @Override
    public void start() {
        this.lastT = gameObject.transform.copy();
    }

    @Override
    public void editorUpdate(float dt) {
        if (!this.lastT.equals(this.gameObject.transform)) {
            this.gameObject.transform.copy(this.lastT);
            isDamage = true;
        }
    }

    @Override
    public void update(float dt) {
        if (!this.lastT.equals(this.gameObject.transform)) {
            this.gameObject.transform.copy(this.lastT);
            isDamage = true;
        }
    }

    @Override
    public void imgui() {
        if (ImEditorGui.colorCtrl("Color", this.color)) {
            this.isDamage = true;
        }
    }

    public void setDamage(boolean isDamage) {
        this.isDamage = isDamage;
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
        this.isDamage = true;
    }

    public void setColor(Vector4f color) {
        if(!this.color.equals(color)) {
            this.isDamage = true;
            this.color.set(color);
        }
    }

    public boolean isDamage() {
        return this.isDamage;
    }

    public void setTexture(Texture texture) {
        this.sprite.setTex(texture);
    }

}
