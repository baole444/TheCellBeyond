package components;

import TCB_Field.Component;
import TCB_Field.Transform;
import org.joml.Vector2f;
import org.joml.Vector4f;
import render.Texture;

public class SpriteRender extends Component {

    private Vector4f color;
    private Sprite sprite;
    private Transform lastT;
    private boolean isDamage = false;

    public SpriteRender(Vector4f color) {
        this.color = color;
        this.sprite = new Sprite(null);
        this.isDamage = true;
    }

    public SpriteRender(Sprite sprite) {
        this.sprite = sprite;
        this.color = new Vector4f(1, 1, 1, 1);
        this.isDamage = true;
    }

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
        if(this.color.equals(color)) {
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
}
