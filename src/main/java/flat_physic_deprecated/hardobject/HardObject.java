package flat_physic_deprecated.hardobject;

import components.Component;
import org.joml.Vector2f;

public class HardObject extends Component {
    private Vector2f pos = new Vector2f();
    private float rotate = 0.0f;

    public Vector2f loadPos() {
        return pos;
    }

    public void setPos(Vector2f pos) {
        this.pos = pos;
    }

    public float loadRotate() {
        return rotate;
    }

    public void setRotate(float rotate) {
        this.rotate = rotate;
    }
}
