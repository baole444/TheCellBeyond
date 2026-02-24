package physic2d;

import org.jbox2d.dynamics.BodyDef;
import physic2d.enums.PhysicBodyType;

public class StaticBody2D extends PhysicBody2D {
    public StaticBody2D() {
        String name = StaticBody2D.class.getSimpleName();
        this(name);
    }

    public StaticBody2D(String name) {
        if (invalidName(name)) name = StaticBody2D.class.getSimpleName();
        super(name, PhysicBodyType.Static);
    }

    @Override
    public void configureBodyDef(BodyDef bodyDef) {}

    @Override
    public void configurePhysicBodyRef() {}

    @Override
    public StaticBody2D copy() {
        return copy(false);
    }

    @Override
    public StaticBody2D copy(boolean copyHierarchy) {
        StaticBody2D copy = (StaticBody2D) copySingleObject();

        if (copyHierarchy && !getChildren().isEmpty()) copyDescendants(this, copy);

        return copy;
    }
}
