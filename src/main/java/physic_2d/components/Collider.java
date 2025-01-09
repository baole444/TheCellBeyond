package physic_2d.components;

import components.Component;
import org.joml.Vector2f;

// abstraction-inception, an abstract class inherits an abstract class.
public abstract class Collider extends Component {
    // Use for adjusting asset origin and hit box origin

    protected Vector2f offset = new Vector2f();

    public Vector2f loadOffset() {
        return this.offset;
    }
}
