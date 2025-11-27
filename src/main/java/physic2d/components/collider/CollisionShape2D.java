package physic2d.components.collider;

import components.SpatialComponent;
import physic2d.components.PhysicBody2D;

public abstract class CollisionShape2D extends SpatialComponent {
    protected PhysicBody2D physicBody2D = null;
    protected transient boolean needsFixtureReset = false;

    
}
