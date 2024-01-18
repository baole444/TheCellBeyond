package components;

import TCB_Field.Component;
import org.joml.Vector2f;
import org.joml.Vector4f;
import render.Texture;

public class SpriteRender extends Component {

    private Vector4f color;

    private Vector2f[] texCoord;
    //(0, 1)
    //(0, 0)
    //(1, 1)
    //(1, 0)
    private Texture texture;

    public SpriteRender(Vector4f color) {
        this.color = color;
        this.texture = null;
    }

    public SpriteRender(Texture texture) {
        this.texture = texture;
        this.color = new Vector4f(1, 1, 1, 1);
    }

    @Override
    public void start() {

    }
    @Override
    public void update(float dt) {

    }
    public Vector4f loadColor() {
        return this.color;
    }
    public Texture loadTexture() {
        return this.texture;
    }
    public Vector2f[] loadTexCoord() {
        Vector2f[] texCoord = {
                new Vector2f(1, 1),
                new Vector2f(1, 0),
                new Vector2f(0, 0),
                new Vector2f(0, 1)
        };
        return texCoord;
    }
}
