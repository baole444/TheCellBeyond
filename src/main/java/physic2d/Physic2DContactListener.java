package physic2d;

import TheCellBeyond.GameObject;
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

        WorldManifold worldManifold = new WorldManifold();

        contact.getWorldManifold(worldManifold);

        Vector2f aNormal = new Vector2f(worldManifold.normal.x, worldManifold.normal.y);
        Vector2f bNormal = new Vector2f(aNormal).negate();

        for (Component c : A.getComponents()) {
            c.startCollision(B, contact, aNormal);
        }

        for (Component c : B.getComponents()) {
            c.startCollision(A, contact, bNormal);
        }
    }

    @Override
    public void endContact(Contact contact) {
        GameObject A = (GameObject) contact.getFixtureA().getUserData();
        GameObject B = (GameObject) contact.getFixtureB().getUserData();

        WorldManifold worldManifold = new WorldManifold();

        contact.getWorldManifold(worldManifold);

        Vector2f aNormal = new Vector2f(worldManifold.normal.x, worldManifold.normal.y);
        Vector2f bNormal = new Vector2f(aNormal).negate();

        for (Component c : A.getComponents()) {
            c.endCollision(B, contact, aNormal);
        }

        for (Component c : B.getComponents()) {
            c.endCollision(A, contact, bNormal);
        }
    }

    @Override
    public void preSolve(Contact contact, Manifold manifold) {
        GameObject A = (GameObject) contact.getFixtureA().getUserData();
        GameObject B = (GameObject) contact.getFixtureB().getUserData();

        WorldManifold worldManifold = new WorldManifold();

        contact.getWorldManifold(worldManifold);

        Vector2f aNormal = new Vector2f(worldManifold.normal.x, worldManifold.normal.y);
        Vector2f bNormal = new Vector2f(aNormal).negate();

        for (Component c : A.getComponents()) {
            c.preSolve(B, contact, aNormal);
        }

        for (Component c : B.getComponents()) {
            c.preSolve(A, contact, bNormal);
        }
    }

    @Override
    public void postSolve(Contact contact, ContactImpulse contactImpulse) {
        GameObject A = (GameObject) contact.getFixtureA().getUserData();
        GameObject B = (GameObject) contact.getFixtureB().getUserData();

        WorldManifold worldManifold = new WorldManifold();

        contact.getWorldManifold(worldManifold);

        Vector2f aNormal = new Vector2f(worldManifold.normal.x, worldManifold.normal.y);
        Vector2f bNormal = new Vector2f(aNormal).negate();

        for (Component c : A.getComponents()) {
            c.postSolve(B, contact, aNormal);
        }

        for (Component c : B.getComponents()) {
            c.postSolve(A, contact, bNormal);
        }
    }
}
