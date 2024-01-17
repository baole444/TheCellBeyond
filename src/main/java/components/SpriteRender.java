package components;

import TCB_Field.Component;
import org.joml.Vector4f;

public class SpriteRender extends Component {

    private Vector4f color;

    public SpriteRender(Vector4f color) {
        this.color = color;
    }
    @Override
    public void start() {

    }
    @Override
    public void update(float dt) {

    }
    public Vector4f getColor() {
        return this.color;
    }
}
