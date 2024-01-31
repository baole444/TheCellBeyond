package physic_2d.components;

import components.Component;
import org.joml.Vector2f;

public abstract class Collider extends Component {
    private Vector2f offset = new Vector2f();

    public Vector2f loadOffset() {
        return this.offset;
    }
}
