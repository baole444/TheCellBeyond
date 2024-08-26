package physic_2d.components;

import components.Component;
import org.joml.Vector2f;

public abstract class Collider extends Component {
    // Use for adjusting assets origin and hit box origin

    protected Vector2f offset = new Vector2f();

    public Vector2f loadOffset() {
        return this.offset;
    }
}
