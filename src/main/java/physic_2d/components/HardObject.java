package physic_2d.components;

import components.Component;
import org.jbox2d.dynamics.Body;
import org.joml.Vector2f;
import physic_2d.enums.ObjectClassification;

public class HardObject extends Component {
    private Vector2f velocity = new Vector2f();
    private float rollResistance = 0.8f;
    private float translateResistance = 0.9f;
    private float mass = 0;
    private ObjectClassification objectClassification = ObjectClassification.Dynamic;
    private boolean isRotatable = false;
    private boolean isNoneStopCollision = true;
    private Body instObject = null;

    @Override
    public void update(float dt) {
        // Sync object between physic engine and game engine
        if (instObject != null) {
            this.gameObject.transform.position.set(
                    instObject.getPosition().x, instObject.getPosition().y
            );

            this.gameObject.transform.rotate = (float)Math.toDegrees(instObject.getAngle());
        }
    }


    public Vector2f loadVelocity() {
        return velocity;
    }

    public void setVelocity(Vector2f velocity) {
        this.velocity = velocity;
    }

    public float loadRollResistance() {
        return rollResistance;
    }

    public void setRollResistance(float rollResistance) {
        this.rollResistance = rollResistance;
    }

    public float loadTranslateResistance() {
        return translateResistance;
    }

    public void setTranslateResistance(float translateResistance) {
        this.translateResistance = translateResistance;
    }

    public float loadMass() {
        return mass;
    }

    public void setMass(float mass) {
        this.mass = mass;
    }

    public ObjectClassification loadObjectClassification() {
        return objectClassification;
    }

    public void setObjectClassification(ObjectClassification objectClassification) {
        this.objectClassification = objectClassification;
    }

    public boolean isRotatable() {
        return isRotatable;
    }

    public void setRotatable(boolean rotatable) {
        isRotatable = rotatable;
    }

    public boolean isNoneStopCollision() {
        return isNoneStopCollision;
    }

    public void setNoneStopCollision(boolean noneStopCollision) {
        isNoneStopCollision = noneStopCollision;
    }

    public Body loadInstObject() {
        return instObject;
    }

    public void setInstObject(Body instObject) {
        this.instObject = instObject;
    }
}
