package components;

import TCB_Field.Transform;
import editor.ImEditorGui;
import org.joml.Vector2f;
import org.joml.Vector4f;
import render.Texture;

public class SpriteRender extends Component {

    private Vector4f color = new Vector4f(1, 1, 1 , 1);
    private Sprite sprite = new Sprite();
    private transient Transform lastT;
    private transient boolean isDamage = true;

    @Override
    public void start() {
        this.lastT = gameObject.transform.copy();
    }

    @Override
    public void update(float dt) {
        if (!this.lastT.equals(this.gameObject.transform)) {
            this.gameObject.transform.copy(this.lastT);
            isDamage = true;

        }
    }

    @Override
    public void updateEditor(float dt) {
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

    public void setDamage() {
        this.isDamage = true;
    }

    public Vector4f loadColor() {
        return this.color;
    }
    public Texture loadTexture() {
        return sprite.loadTex();
    }
    public Vector2f[] loadTexCoord() {
        return sprite.loadTexCrd();
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

    public void notDamage() {
        this.isDamage = false;
    }

    public void setTex(Texture texture) {
        this.sprite.setTex(texture);
    }

}
