package physic2d;

import TheCellBeyond.GameObject;
import TheCellBeyond.GameObject2D;
import components.Component;
import org.jbox2d.callbacks.ContactImpulse;
import org.jbox2d.callbacks.ContactListener;
import org.jbox2d.collision.Manifold;
import org.jbox2d.collision.WorldManifold;
import org.jbox2d.dynamics.contacts.Contact;
import org.joml.Vector2f;

public class Physic2DContactListener implements ContactListener {
    @Override
    public void beginContact(Contact contact) {
        GameObject A = (GameObject) contact.getFixtureA().getUserData();
        GameObject B = (GameObject) contact.getFixtureB().getUserData();
        if (sameHierarchy(A, B)) return;
        WorldManifold worldManifold = new WorldManifold();
        contact.getWorldManifold(worldManifold);
        Vector2f aNormal = new Vector2f(worldManifold.normal.x, worldManifold.normal.y);
        Vector2f bNormal = new Vector2f(aNormal).negate();
        for (Component c : A.getComponents()) c.startCollision(B, contact, aNormal);
        for (Component c : B.getComponents()) c.startCollision(A, contact, bNormal);
        emitAreaEnterSignals(A, B);
    }

    @Override
    public void endContact(Contact contact) {
        GameObject A = (GameObject) contact.getFixtureA().getUserData();
        GameObject B = (GameObject) contact.getFixtureB().getUserData();
        if (sameHierarchy(A, B)) return;
        WorldManifold worldManifold = new WorldManifold();
        contact.getWorldManifold(worldManifold);
        Vector2f aNormal = new Vector2f(worldManifold.normal.x, worldManifold.normal.y);
        Vector2f bNormal = new Vector2f(aNormal).negate();
        for (Component c : A.getComponents()) c.endCollision(B, contact, aNormal);
        for (Component c : B.getComponents()) c.endCollision(A, contact, bNormal);
        emitAreaExitSignals(A, B);
    }

    @Override
    public void preSolve(Contact contact, Manifold manifold) {
        GameObject A = (GameObject) contact.getFixtureA().getUserData();
        GameObject B = (GameObject) contact.getFixtureB().getUserData();
        if (sameHierarchy(A, B)) return;
        WorldManifold worldManifold = new WorldManifold();
        contact.getWorldManifold(worldManifold);
        Vector2f aNormal = new Vector2f(worldManifold.normal.x, worldManifold.normal.y);
        Vector2f bNormal = new Vector2f(aNormal).negate();
        for (Component c : A.getComponents()) c.preSolve(B, contact, aNormal);
        for (Component c : B.getComponents()) c.preSolve(A, contact, bNormal);
    }

    @Override
    public void postSolve(Contact contact, ContactImpulse contactImpulse) {
        GameObject A = (GameObject) contact.getFixtureA().getUserData();
        GameObject B = (GameObject) contact.getFixtureB().getUserData();
        if (sameHierarchy(A, B)) return;
        WorldManifold worldManifold = new WorldManifold();
        contact.getWorldManifold(worldManifold);
        Vector2f aNormal = new Vector2f(worldManifold.normal.x, worldManifold.normal.y);
        Vector2f bNormal = new Vector2f(aNormal).negate();
        for (Component c : A.getComponents()) c.postSolve(B, contact, aNormal);
        for (Component c : B.getComponents()) c.postSolve(A, contact, bNormal);
    }

    private boolean sameHierarchy(GameObject A, GameObject B) {
        if (!(A instanceof CollisionObject2D coA)) return false;
        if (!(B instanceof CollisionObject2D coB)) return false;
        return coA.sharePhysicHierarchy(coB);
    }

    private void emitAreaEnterSignals(GameObject A, GameObject B) {
        if (A instanceof Area2D areaA && areaA.monitoring) notifyAreaEnter(areaA, B);
        if (B instanceof Area2D areaB && areaB.monitoring) notifyAreaEnter(areaB, A);
    }

    private void emitAreaExitSignals(GameObject A, GameObject B) {
        if (A instanceof Area2D areaA && areaA.monitoring) notifyAreaExit(areaA, B);
        if (B instanceof Area2D areaB && areaB.monitoring) notifyAreaExit(areaB, A);
    }

    private void notifyAreaEnter(Area2D area, GameObject other) {
        if (other instanceof Area2D otherArea && otherArea.monitorable) {
            area.trackAreaEnter(otherArea);
            area.areaEntered.emit(otherArea);
            return;
        }
        if (!(other instanceof GameObject2D body)) return;
        area.trackBodyEnter(body);
        area.bodyEntered.emit(body);
    }

    private void notifyAreaExit(Area2D area, GameObject other) {
        if (other instanceof Area2D otherArea && otherArea.monitorable) {
            area.trackAreaExit(otherArea);
            area.areaExited.emit(otherArea);
            return;
        }
        if (!(other instanceof GameObject2D body)) return;
        area.trackBodyExit(body);
        area.bodyExited.emit(body);
    }
}
